import call.CallManager;
import call.InitiatingCallManager;
import call.ResponderCallManager;
import com.sun.media.jfxmedia.logging.Logger;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Creates call instances
 */
public class CallOrchestration {
    byte[] zid;
    private CallManager currentCallManager;

    public CallOrchestration() {
        try {
            this.zid        = new byte[12];
            SecureRandom.getInstance("SHA1PRNG").nextBytes(zid);
        } catch(NoSuchAlgorithmException e) {
            Logger.logMsg(Logger.ERROR, e.getMessage());
        }
    }

    public void initiateCall() {
        currentCallManager = new InitiatingCallManager(zid);
        currentCallManager.start();
    }

    public void acceptCall() {
        currentCallManager = new ResponderCallManager(zid);
        currentCallManager.start();
    }
}
