package audio;

import java.io.FileDescriptor;
import java.lang.reflect.Field;
import java.net.DatagramSocket;
import java.net.DatagramSocketImpl;
import java.net.SocketException;
import java.util.logging.Logger;

public class CallAudioManager {

  private static final String TAG = CallAudioManager.class.getSimpleName();

  static {
    System.loadLibrary("redphone-audio");
  }

  public CallAudioManager(DatagramSocket socket, String remoteHost, int remotePort,
                          byte[] senderCipherKey, byte[] senderMacKey, byte[] senderSalt,
                          byte[] receiverCipherKey, byte[] receiverMacKey, byte[] receiverSalt)
      throws SocketException, AudioException
  {
    // start listening for packets, and playing them
  }

  public void setMute(boolean enabled) {
      // mute the audio
  }

  public void terminate() {
  }
}
