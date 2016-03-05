package com.sydefolk;

import com.sun.media.jfxmedia.logging.Logger;

public class CustomSocket {
    DataGrahamSocket dataGrahamSocket;

    public CustomSocket(DataGrahamSocket socket) {
        dataGrahamSocket = socket;
    }

    public <T> void send(T packet) {
        // encode to bytes using protobuff and send
    }

    /**
     * Pass in a packet to fill up with data
     * @param <T> Type of the packet desired
     * @return
     */
    public <T> T receive(Class<T> classReference) {
        Logger.logMsg(Logger.DEBUG, classReference.getSimpleName());
        boolean satisfied = false;
        T packet = null;
        while (!satisfied) {
            byte[] data = dataGrahamSocket.receive();
            Object parsed = fromBytes(data);

            // TODO dschwarz test this code
            if (classReference.isAssignableFrom(parsed.getClass())) {
                satisfied = true;
                packet = (T) parsed;
            }
        }
        return packet;
    }

    public void setTimeout(int timeoutMillis) {
        // not implemented
    }

    private Object fromBytes(byte[]  packet) {
        return null; // not implemented
    }
}

