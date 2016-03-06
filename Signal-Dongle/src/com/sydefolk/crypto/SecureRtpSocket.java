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

package com.sydefolk.crypto;

import com.sydefolk.crypto.zrtp.HandshakePacket;
import com.sydefolk.network.RtpPacket;

import java.io.IOException;

import com.sydefolk.CustomSocket;

/**
 * A socket that does SRTP.
 *
 * Every outgoing packet is encrypted/authenticated, and every incoming
 * packet is verified/decrypted.
 *
 * EDIT: Now just a shim for passing handshake packets.  TODO
 *
 * @author Moxie Marlinspike
 *
 */

public class SecureRtpSocket {

  private static final String TAG = SecureRtpPacket.class.getSimpleName();

  private final CustomSocket socket;

  public SecureRtpSocket(CustomSocket socket) {
    this.socket = socket;
  }

  public void send(HandshakePacket packet) throws IOException {
    packet.setCRC();
    socket.send(packet);
  }

  public HandshakePacket receiveHandshakePacket(boolean verifyCRC) throws IOException {
    RtpPacket barePacket = null;
    try {
      barePacket = socket.receive();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    if (barePacket == null)
      return null;

    HandshakePacket handshakePacket = new HandshakePacket(barePacket);

    if (!verifyCRC || handshakePacket.verifyCRC()) {
      return handshakePacket;
    } else {
      return null;
    }
  }

  public void setTimeout(int timeoutMillis) {
    socket.setTimeout(timeoutMillis);
  }
}
