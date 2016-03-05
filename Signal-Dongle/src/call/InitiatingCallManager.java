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

package call;

import audio.AudioException;
import audio.CallAudioManager;
import crypto.SecureRtpSocket;
import crypto.zrtp.MasterSecret;
import crypto.zrtp.ZRTPInitiatorSocket;
import network.RtpSocket;

import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;

/**
 * Call Manager for the coordination of outgoing calls.  It initiates
 * signaling, negotiates ZRTP, and kicks off the call audio manager.
 *
 * @author Moxie Marlinspike
 *
 */
public class InitiatingCallManager extends CallManager {

  private static final String TAG = InitiatingCallManager.class.getSimpleName();

  private final byte[] zid;

  public InitiatingCallManager(byte[] zid)
  {
    super("InitiatingCallManager Thread");
    this.zid            = zid;
  }

  @Override
  public void run() {
    try {
        // TODO dschwarz use Graham's library
//      secureSocket  = new SecureRtpSocket(new RtpSocket(localPort, remoteAddress));

      zrtpSocket    = new ZRTPInitiatorSocket(secureSocket, zid);

      super.run();
    } catch( RuntimeException e ) {
    }
  }

  @Override
  protected void runAudio(DatagramSocket socket, String remoteIp, int remotePort,
                          MasterSecret masterSecret, boolean muteEnabled)
      throws SocketException, AudioException
  {
    this.callAudioManager = new CallAudioManager(socket, remoteIp, remotePort,
                                                 masterSecret.getInitiatorSrtpKey(),
                                                 masterSecret.getInitiatorMacKey(),
                                                 masterSecret.getInitiatorSrtpSalt(),
                                                 masterSecret.getResponderSrtpKey(),
                                                 masterSecret.getResponderMacKey(),
                                                 masterSecret.getResponderSrtpSailt());
    this.callAudioManager.setMute(muteEnabled);
  }
}
