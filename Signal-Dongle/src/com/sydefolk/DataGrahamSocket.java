package com.sydefolk;


import java.util.logging.Level;
import java.util.logging.Logger;

public class DataGrahamSocket {
    byte[] dataToSendTest = null;
    final Object lock = new Object();

    public synchronized void send(byte[] data) {
        Logger.getLogger(getClass().getName()).log(Level.INFO, "DataGraham - Sending packet");
        dataToSendTest = data;
        synchronized (lock) {
            lock.notify();
        }
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
        return dataToSendTest;
    }
}
