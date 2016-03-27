/*
 * Copyright (C) 2011 Whisper Systems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.sydefolk.crypto.zrtp;

import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.math.ec.ECPoint;
import com.sydefolk.crypto.SecureRtpSocket;
import com.sydefolk.util.Conversions;

import java.io.IOException;
import java.math.BigInteger;
import java.net.DatagramSocket;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.spec.ECGenParameterSpec;

import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;

/**
 * The base ZRTP socket implementation.
 *
 * ZRTPInitiatorSocket and ZRTPResponderSocket extend this to implement their respective
 * parts in the ZRTP handshake.
 *
 * This is fundamentally just a simple state machine which iterates through the ZRTP handshake.
 *
 * @author Moxie Marlinspike
 *
 */

public abstract class ZRTPSocket {
  static {
    Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
  }

  public static final BigInteger PRIME     = getSafePrimeModulus1536();
  public static final BigInteger GENERATOR = new BigInteger("02", 16);

  private static final int RETRANSMIT_INTERVAL_MILLIS = 150;
  private static final int MAX_RETRANSMIT_COUNT       = 45;

  protected static final int EXPECTING_HELLO            = 0;
  protected static final int EXPECTING_HELLO_ACK        = 1;
  protected static final int EXPECTING_COMMIT           = 2;
  protected static final int EXPECTING_DH_1             = 3;
  protected static final int EXPECTING_DH_2             = 4;
  protected static final int EXPECTING_CONFIRM_ONE      = 5;
  protected static final int EXPECTING_CONFIRM_TWO      = 9;
  protected static final int HANDSHAKE_COMPLETE         = 6;
  protected static final int EXPECTING_CONFIRM_ACK      = 7;
  protected static final int TERMINATED                 = 8;

  protected static final int KA_TYPE_DH3K = 100;
  protected static final int KA_TYPE_EC25 = 200;

  private long transmitStartTime  = 0;
  private int  retransmitInterval = RETRANSMIT_INTERVAL_MILLIS;
  private int  retransmitCount    = 0;
  private int  sequence           = 0;
  private int  state;

  private   final SecureRtpSocket socket;
  protected final byte[] localZid;

  private HandshakePacket lastPacket;
  private KeyPair dh3kKeyPair;
  private KeyPair ec25KeyPair;

  protected HashChain hashChain;
  protected MasterSecret masterSecret;

  public ZRTPSocket(SecureRtpSocket socket,
                    byte[] localZid, int initialState)
  {
    this.localZid          = localZid;
    this.socket            = socket;
    this.state             = initialState;
    this.dh3kKeyPair       = initializeDH3kKeys();
    this.ec25KeyPair       = initializeEC25Keys();
    this.hashChain         = new HashChain();

    this.socket.setTimeout(RETRANSMIT_INTERVAL_MILLIS);
  }

  protected abstract void handleHello(HandshakePacket packet) throws InvalidPacketException;
  protected abstract void handleCommit(HandshakePacket packet) throws InvalidPacketException;
  protected abstract void handleDH(HandshakePacket packet) throws InvalidPacketException;
  protected abstract void handleConfirmOne(HandshakePacket packet) throws InvalidPacketException;
  protected abstract void handleConfirmTwo(HandshakePacket packet) throws InvalidPacketException;
  protected abstract void handleHelloAck(HandshakePacket packet) throws InvalidPacketException;
  protected abstract void handleConfirmAck(HandshakePacket packet) throws InvalidPacketException;

  protected abstract int getKeyAgreementType();
  protected abstract HelloPacket getForeignHello();

  protected byte[] getPublicKey() {
    switch (getKeyAgreementType()) {
    case KA_TYPE_EC25: return getPublicEC25Key();
    case KA_TYPE_DH3K: return getPublicDH3kKey();
    default:           throw new AssertionError("Unknown KA type: " + getKeyAgreementType());
    }
  }

  protected KeyPair getKeyPair() {
    switch (getKeyAgreementType()) {
    case KA_TYPE_EC25: return ec25KeyPair;
    case KA_TYPE_DH3K: return dh3kKeyPair;
    default:           throw new AssertionError("Unknown KA type: " + getKeyAgreementType());
    }
  }

  private byte[] getPublicDH3kKey() {
    byte[] temp = new byte[384];
    Conversions.bigIntegerToByteArray(temp, ((DHPublicKey)dh3kKeyPair.getPublic()).getY());
    return temp;
  }

  private byte[] getPublicEC25Key() {
    ECPublicKey publicKey = (ECPublicKey)ec25KeyPair.getPublic();
    ECPoint q             = publicKey.getQ();

    byte[] x = new byte[32];
    byte[] y = new byte[32];

    Conversions.bigIntegerToByteArray(x, q.getX().toBigInteger());
    Conversions.bigIntegerToByteArray(y, q.getY().toBigInteger());

    return Conversions.combine(x, y);
  }

//  protected RetainedSecrets getRetainedSecrets(String number, byte[] zid) {
//    RetainedSecretsDatabase database = DatabaseFactory.getRetainedSecretsDatabase(context);
//    return database.getRetainedSecrets(number, zid);
//  }

//  protected void cacheRetainedSecret(String number, byte[] zid, byte[] rs1,
//                                     long expiration, boolean continuity)
//  {
//    RetainedSecretsDatabase database = DatabaseFactory.getRetainedSecretsDatabase(context);
//    database.setRetainedSecret(number, zid, rs1, expiration, continuity);
//  }


  // NOTE -- There was a bug in older versions of RedPhone in which the
  // Confirm message IVs were miscalculated.  It didn't seem to be an
  // immediately exploitable problem, but was definitely wrong.  Fixing it,
  // however, results in compatibility issues with devices that do not have
  // the fix.  We're temporarily introducing a backwards compatibility setting
  // here, where we intentionally do the wrong thing for older devices.  We'll
  // phase this out after a couple of months.
  protected boolean isLegacyConfirmConnection() {
    RedPhoneClientId clientId = new RedPhoneClientId(getForeignHello().getClientId());
    return clientId.isLegacyConfirmConnectionVersion();
  }

  protected void setState(int state) {
    this.state = state;
  }

  protected void sendFreshPacket(HandshakePacket packet) {
    retransmitCount    = 0;
    retransmitInterval = RETRANSMIT_INTERVAL_MILLIS;
    sendPacket(packet);
  }

  private void sendPacket(HandshakePacket packet) {
    transmitStartTime = System.currentTimeMillis();
    this.lastPacket   = packet;

    if (packet != null) {
      packet.setSequenceNumber(this.sequence++);
      try {
        socket.send(packet);
      } catch (IOException e) {
//        Log.w("ZRTPSocket", e);
      }
    }
  }

  private void resendPacket() throws NegotiationFailedException {
    if (retransmitCount++ > MAX_RETRANSMIT_COUNT) {
      if (this.lastPacket != null) {
        throw new NegotiationFailedException("Retransmit threshold reached.");
      } else {
        throw new RecipientUnavailableException("Recipient unavailable.");
      }
    }

    retransmitInterval = Math.min(retransmitInterval * 2, 1500);

    sendPacket(lastPacket);
  }

  private KeyPair initializeDH3kKeys() {
    try {
      KeyPairGenerator kg    = KeyPairGenerator.getInstance("DH");
      DHParameterSpec dhSpec = new DHParameterSpec(PRIME, GENERATOR);
      kg.initialize(dhSpec);

      return kg.generateKeyPair();
    } catch (InvalidAlgorithmParameterException e) {
      throw new IllegalArgumentException(e);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalArgumentException(e);
    }
  }

  public static final BigInteger getSafePrimeModulus1536()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD1");
    sb.append("29024E088A67CC74020BBEA63B139B22514A08798E3404DD");
    sb.append("EF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245");
    sb.append("E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7ED");
    sb.append("EE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3D");
    sb.append("C2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F");
    sb.append("83655D23DCA3AD961C62F356208552BB9ED529077096966D");
    sb.append("670C354E4ABC9804F1746C08CA237327FFFFFFFFFFFFFFFF");
    return new BigInteger(sb.toString(), 16);
  }

  private KeyPair initializeEC25Keys() {
    try {
      KeyPairGenerator kg       = KeyPairGenerator.getInstance("ECDH", "BC");
      ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp256r1");
      kg.initialize(ecSpec);

      return kg.generateKeyPair();
    } catch (InvalidAlgorithmParameterException e) {
      throw new AssertionError(e);
    } catch (NoSuchAlgorithmException nsae) {
      throw new AssertionError(nsae);
    } catch (NoSuchProviderException e) {
      throw new AssertionError(e);
    }
  }

  private boolean isRetransmitTime() {
    return (System.currentTimeMillis() - transmitStartTime >= retransmitInterval);
  }

  private void resendPacketIfTimeout() throws NegotiationFailedException {
    if (isRetransmitTime()) {
//      Log.w("ZRTPSocket", "Retransmitting after: " + retransmitInterval);
      resendPacket();
    }
  }

  public MasterSecret getMasterSecret() {
    return this.masterSecret;
  }

  public SASInfo getSasInfo() {
//    RetainedSecretsDatabase database    = DatabaseFactory.getRetainedSecretsDatabase(context);
    String sasText     = SASCalculator.calculateSAS(masterSecret.getSAS());
//    boolean                 sasVerified = database.isVerified(remoteNumber, getForeignHello().getZID());

    return new SASInfo(sasText, false);
  }

//  public void setSasVerified() {
//    DatabaseFactory.getRetainedSecretsDatabase(context).setVerified(remoteNumber, getForeignHello().getZID());
//  }

  public void close() {
    state = TERMINATED;
  }

  public void negotiateStart() throws NegotiationFailedException {
    try {
      while (state == EXPECTING_HELLO) {
        HandshakePacket packet = socket.receiveHandshakePacket(true);

        if (packet == null) {
          resendPacketIfTimeout();
        } else if (packet.getType().equals(HelloPacket.TYPE) && (state == EXPECTING_HELLO)) {
          handleHello(packet);
        } else if (isRetransmitTime()) {
          resendPacket();
        }
      }
    } catch (IOException ioe) {
//      Log.w("ZRTPSocket", ioe);
      if (state != TERMINATED)
        throw new NegotiationFailedException(ioe);
    } catch (InvalidPacketException ipe) {
//      Log.w("ZRTPSocket", ipe);
      throw new NegotiationFailedException(ipe);
    }
  }

  public void negotiateFinish() throws NegotiationFailedException {
    try {
      while (state != HANDSHAKE_COMPLETE && state != TERMINATED) {

        HandshakePacket packet = socket.receiveHandshakePacket(state != EXPECTING_CONFIRM_ACK);

        if( packet != null ) {
        	System.out.println("Received packet: " + (packet != null ? packet.getType() : "null"));
        }
        
        if      (packet == null)                                                                       resendPacketIfTimeout();
        else if ((packet.getType().equals(HelloPacket.TYPE))      && (state == EXPECTING_HELLO))       handleHello(packet);
        else if ((packet.getType().equals(HelloAckPacket.TYPE))   && (state == EXPECTING_HELLO_ACK))   handleHelloAck(packet);
        else if ((packet.getType().equals(CommitPacket.TYPE))     && (state == EXPECTING_COMMIT))      handleCommit(packet);
        else if ((packet.getType().equals(DHPartOnePacket.TYPE))  && (state == EXPECTING_DH_1))        handleDH(packet);
        else if ((packet.getType().equals(DHPartTwoPacket.TYPE))  && (state == EXPECTING_DH_2))        handleDH(packet);
        else if ((packet.getType().equals(ConfirmOnePacket.TYPE)) && (state == EXPECTING_CONFIRM_ONE)) handleConfirmOne(packet);
        else if ((packet.getType().equals(ConfirmTwoPacket.TYPE)) && (state == EXPECTING_CONFIRM_TWO)) handleConfirmTwo(packet);
        else if ((packet.getType().equals(ConfAckPacket.TYPE))    && (state == EXPECTING_CONFIRM_ACK)) handleConfirmAck(packet);
        else if (isRetransmitTime())                                                                   resendPacket();
      }
    } catch (InvalidPacketException ipe) {
//      Log.w("ZRTPSocket", ipe);
      throw new NegotiationFailedException(ipe);
    } catch (IOException ioe) {
//      Log.w("ZRTPSocket", ioe);
      if (state != TERMINATED)
        throw new NegotiationFailedException(ioe);
    }

    if (state != TERMINATED)
      this.socket.setTimeout(1);
  }

}
