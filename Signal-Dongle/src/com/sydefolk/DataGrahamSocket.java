package com.sydefolk;

public class DataGrahamSocket {
    public void send(byte[] data) {

    }

    /**
     * Block until any packet is received
     * @return
     */
    public byte[] receive() {
        try {
            wait(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }
}
