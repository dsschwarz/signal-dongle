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
import com.sydefolk.crypto.zrtp.ZRTPResponderSocket;

import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * CallManager responsible for coordinating incoming calls.
 *
 * @author Moxie Marlinspike
 *
 */
public class ResponderCallManager extends CallManager {

  private static final String TAG = ResponderCallManager.class.getSimpleName();

  private final byte[] zid;

  private int answer = 0;

  public ResponderCallManager(byte[] zid, CustomSocket customSocket) {
    super("ResponderCallManager Thread");
    this.zid               = zid;
    this.customSocket = customSocket;
  }

  @Override
  public void run() {
    try {
      secureSocket  = new SecureRtpSocket(customSocket);

      zrtpSocket    = new ZRTPResponderSocket(secureSocket, zid, false);
      super.run();
    } catch( RuntimeException e ) {
      e.printStackTrace();
    }
  }

  @Override
  public void terminate() {
    super.terminate();
  }

  @Override
  protected void runAudio(CustomSocket socket, MasterSecret masterSecret, boolean muteEnabled)
      throws SocketException, AudioException
  {
    Logger.getAnonymousLogger().log(Level.INFO, "Starting Audio");
    this.callAudioManager = new CallAudioManager(socket,
                                                 masterSecret.getResponderSrtpKey(),
                                                 masterSecret.getResponderMacKey(),
                                                 masterSecret.getResponderSrtpSailt(),
                                                 masterSecret.getInitiatorSrtpKey(),
                                                 masterSecret.getInitiatorMacKey(),
                                                 masterSecret.getInitiatorSrtpSalt());
    this.callAudioManager.setMute(muteEnabled);
    this.callAudioManager.start();
  }

}
