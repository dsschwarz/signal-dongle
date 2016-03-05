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
import crypto.zrtp.NegotiationFailedException;
import crypto.zrtp.RecipientUnavailableException;
import crypto.zrtp.SASInfo;
import crypto.zrtp.ZRTPSocket;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;


/**
 * The base class for both Initiating and Responder call
 * managers, which coordinate the setup of an outgoing or
 * incoming call.
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
        runAudio(zrtpSocket.getDatagramSocket(), zrtpSocket.getRemoteIp(),
                 zrtpSocket.getRemotePort(), zrtpSocket.getMasterSecret(), muteEnabled);
      }

    } catch (RecipientUnavailableException rue) {
    } catch (NegotiationFailedException nfe) {
    } catch (AudioException e) {
    } catch (IOException e) {
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

  protected abstract void runAudio(DatagramSocket datagramSocket, String remoteIp, int remotePort,
                                   MasterSecret masterSecret, boolean muteEnabled)
      throws SocketException, AudioException;


  public void setMute(boolean enabled) {
    muteEnabled = enabled;
    if (callAudioManager != null) {
      callAudioManager.setMute(muteEnabled);
    }
  }

  /**
   * Did this call ever successfully complete SRTP setup
   * @return true if the call connected
   */
  public boolean callConnected() {
    return callConnected;
  }

}
