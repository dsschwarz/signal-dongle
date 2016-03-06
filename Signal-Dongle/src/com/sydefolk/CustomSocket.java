package com.sydefolk;

import com.sydefolk.network.RtpPacket;

import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CustomSocket {
    DataGrahamSocket dataGrahamSocket;
    final Object lock = new Object();
    RtpPacket latestData;

    Callback initiatorCallback = Callback.emptyCallback();
    Callback respondCallback = Callback.emptyCallback();

    public CustomSocket(DataGrahamSocket socket) {
        dataGrahamSocket = socket;
        (new Thread() {
            @Override
            public void run() {
                listen();
            }
        }).start();
    }

    public void initiateCall() {
        byte[] data = ByteBuffer.allocate(2).putShort((short) MessageTypes.INITIATE.ordinal()).array();
        dataGrahamSocket.send(data);
    }

    private void listen() {
        while (true) {
            byte[] data = dataGrahamSocket.receive();
            handleNewMessage(data);
            Logger.getLogger(getClass().getName()).log(Level.INFO, "Message received!");
            synchronized (lock) {
                lock.notifyAll();
            }
        }
    }

    public void send(RtpPacket packet) {
        byte[] data = packet.getPacket();
        short messageType = (short) MessageTypes.PACKET.ordinal();
        byte[] finalMessage = ByteBuffer.allocate(2 + packet.getPacketLength())
                .putShort(messageType)
                .put(data)
                .array();

        dataGrahamSocket.send(finalMessage);
    }

    public RtpPacket receive() throws InterruptedException {
        lock.wait();
        return latestData;
    }

    public void setTimeout(int timeoutMillis) {
        // not implemented
    }

    private void handleNewMessage(byte[]  packet) {
        ByteBuffer buffer = ByteBuffer.wrap(packet);
        MessageTypes type = MessageTypes.values()[buffer.getShort()];
        if (type == MessageTypes.PACKET) {
            int packetLength = buffer.capacity() - 2;
            byte[] data = new byte[packetLength];
            buffer.get(data);
            latestData = new RtpPacket(data, packetLength);
            lock.notify(); // notify a single receive method that a packet has arrived
        } else if (type == MessageTypes.INITIATE){
            initiatorCallback.doSomething();
        } else if (type == MessageTypes.RESPOND) {
            respondCallback.doSomething();
        } else {
            new Exception("Unknown message type " + type.toString()).printStackTrace();
        }
    }
}