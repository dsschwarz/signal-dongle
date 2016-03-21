package com.sydefolk;


import java.util.logging.Level;
import java.util.logging.Logger;

public class FakeDataGrahamSocket {
    FakeDataGrahamSocket otherSocket = null;
    byte[] dataToSendTest = null;
    final Object lock = new Object();

    public synchronized void send(byte[] data) {
//        Logger.getLogger(getClass().getName()).log(Level.INFO, "DataGraham - Sending packet");
        otherSocket.pushData(data);
    }

    /**
     * Block until any packet is received
     * @return
     */
    public byte[] receive() {
        synchronized (lock) {
            try {
                lock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
//        Logger.getLogger(getClass().getName()).log(Level.INFO, "DataGraham - Packet received");
        return dataToSendTest;
    }

  /**
   * temporary test method, not part of api
   * @deprecated
   */
  public void registerOtherSocket(FakeDataGrahamSocket socket) {
        this.otherSocket = socket;
    }

    private void pushData(byte[] data) {
        this.dataToSendTest = data;
        synchronized (lock) {
            lock.notify();
        }
    }
}
