package com.sydefolk;

import com.sun.media.jfxmedia.logging.Logger;
import com.sydefolk.call.CallManager;
import com.sydefolk.call.InitiatingCallManager;
import com.sydefolk.call.ResponderCallManager;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Creates com.sydefolk.call instances
 */
public class CallOrchestration {
    byte[] zid;
    DataGrahamSocket dataGrahamSocket;
    private CallManager currentCallManager;

    public CallOrchestration(DataGrahamSocket dataGrahamSocket) {
        try {
            this.zid        = new byte[12];
            SecureRandom.getInstance("SHA1PRNG").nextBytes(zid);
        } catch(NoSuchAlgorithmException e) {
            Logger.logMsg(Logger.ERROR, e.getMessage());
        }

        this.dataGrahamSocket = dataGrahamSocket;
    }

    public void initiateCall() {
        currentCallManager = new InitiatingCallManager(zid, dataGrahamSocket);
        currentCallManager.start();
    }

    public void acceptCall() {
        currentCallManager = new ResponderCallManager(zid);
        currentCallManager.start();
    }
}
