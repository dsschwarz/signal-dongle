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

package com.sydefolk.call;

import com.sydefolk.CustomSocket;
import com.sydefolk.audio.AudioException;
import com.sydefolk.audio.CallAudioManager;
import com.sydefolk.crypto.SecureRtpSocket;
import com.sydefolk.crypto.zrtp.MasterSecret;
import com.sydefolk.crypto.zrtp.ZRTPInitiatorSocket;

import java.net.SocketException;

/**
 * Call Manager for the coordination of outgoing calls.  It initiates
 * signaling, negotiates ZRTP, and kicks off the com.sydefolk.call com.sydefolk.audio manager.
 *
 * @author Moxie Marlinspike
 *
 */
public class InitiatingCallManager extends CallManager {

  private static final String TAG = InitiatingCallManager.class.getSimpleName();

  private final byte[] zid;

  public InitiatingCallManager(byte[] zid, CustomSocket customSocket)
  {
    super("InitiatingCallManager Thread");
    this.zid            = zid;
    this.customSocket = customSocket;
  }

  @Override
  public void run() {
    try {
        secureSocket  = new SecureRtpSocket(customSocket);

        zrtpSocket    = new ZRTPInitiatorSocket(secureSocket, zid);

        super.run();
    } catch( RuntimeException e ) {
      e.printStackTrace();
    }
  }

  @Override
  protected void runAudio(CustomSocket socket, MasterSecret masterSecret, boolean muteEnabled)
      throws SocketException, AudioException
  {
    this.callAudioManager = new CallAudioManager(socket, true,
                                                 masterSecret.getInitiatorSrtpKey(),
                                                 masterSecret.getInitiatorMacKey(),
                                                 masterSecret.getInitiatorSrtpSalt(),
                                                 masterSecret.getResponderSrtpKey(),
                                                 masterSecret.getResponderMacKey(),
                                                 masterSecret.getResponderSrtpSailt());
    this.callAudioManager.setMute(muteEnabled);
    this.callAudioManager.start();
  }
}
