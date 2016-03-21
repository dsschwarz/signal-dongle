package com.sydefolk;

import com.sydefolk.call.CallManager;
import com.sydefolk.call.InitiatingCallManager;
import com.sydefolk.call.ResponderCallManager;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Creates com.sydefolk.call instances
 */
public class CallOrchestration {
    byte[] zid;
    protected CustomSocket      customSocket;
    private CallManager currentCallManager;

    public CallOrchestration(FakeDataGrahamSocket dataGrahamSocket) {
        try {
            this.zid        = new byte[12];
            customSocket = new CustomSocket(dataGrahamSocket);

            customSocket.initiatorCallback = this::initiateCall;
            customSocket.respondCallback = this::incomingCall;

            SecureRandom.getInstance("SHA1PRNG").nextBytes(zid);
        } catch(NoSuchAlgorithmException e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, e.getMessage());
        }

    }

    public void initiateCall() {
        Logger.getLogger(getClass().getName()).log(Level.INFO, "initiating call");
        currentCallManager = new InitiatingCallManager(zid, customSocket);
        currentCallManager.start();
    }

    public void incomingCall() {
        currentCallManager = new ResponderCallManager(zid, customSocket);
        currentCallManager.start();
    }
    public void acceptCall() {
        if (currentCallManager instanceof  ResponderCallManager) {
            ((ResponderCallManager) currentCallManager).answer(true);
        }
    }
}
