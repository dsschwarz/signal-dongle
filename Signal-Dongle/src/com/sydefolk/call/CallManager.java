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
import com.sydefolk.crypto.zrtp.*;

import java.io.IOException;
import java.net.SocketException;


/**
 * The base class for both Initiating and Responder com.sydefolk.call
 * managers, which coordinate the setup of an outgoing or
 * incoming com.sydefolk.call.
 *
 * @author Moxie Marlinspike
 *
 */

public abstract class CallManager extends Thread {

  private static final String TAG = CallManager.class.getSimpleName();

  private   boolean          terminated;
  protected CallAudioManager callAudioManager;
  private   SASInfo          sasInfo;
  private   boolean          muteEnabled;
  private   boolean          callConnected;

  protected ZRTPSocket        zrtpSocket;
  protected CustomSocket      customSocket;
  protected SecureRtpSocket   secureSocket;

  public CallManager(String threadName)
  {
    super(threadName);
    this.terminated        = false;
  }

  @Override
  public void run() {
    try {
      if (!terminated) {
        zrtpSocket.negotiateStart();
      }

      if (!terminated) {
        zrtpSocket.negotiateFinish();
      }

      if (!terminated) {
        sasInfo = zrtpSocket.getSasInfo();
      }

      if (!terminated) {
        callConnected = true;
        customSocket.callConnected(zrtpSocket.getSasInfo().getSasText());
        runAudio(customSocket, zrtpSocket.getMasterSecret(), muteEnabled);
      }

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void terminate() {
    this.terminated = true;

    if (callAudioManager != null)
      callAudioManager.terminate();

    if (zrtpSocket != null)
      zrtpSocket.close();
  }

  public SASInfo getSasInfo() {
    return this.sasInfo;
  }

  protected abstract void runAudio(CustomSocket datagramSocket,
                                   MasterSecret masterSecret, boolean muteEnabled)
      throws SocketException, AudioException;


  public void setMute(boolean enabled) {
    muteEnabled = enabled;
    if (callAudioManager != null) {
      callAudioManager.setMute(muteEnabled);
    }
  }

  /**
   * Did this com.sydefolk.call ever successfully complete SRTP setup
   * @return true if the com.sydefolk.call connected
   */
  public boolean callConnected() {
    return callConnected;
  }

}
