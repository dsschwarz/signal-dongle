package com.sydefolk.audio;

import com.sydefolk.CustomSocket;
import com.sydefolk.network.RtpPacket;

import java.io.FileDescriptor;
import java.lang.reflect.Field;
import java.net.DatagramSocket;
import java.net.DatagramSocketImpl;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CallAudioManager {

    private static final String TAG = CallAudioManager.class.getSimpleName();

    static {
        System.loadLibrary("redphone-audio");
    }

    private CustomSocket socket = null;
    private Boolean running = false;

    public CallAudioManager(CustomSocket socket,
                            byte[] senderCipherKey, byte[] senderMacKey, byte[] senderSalt,
                            byte[] receiverCipherKey, byte[] receiverMacKey, byte[] receiverSalt)
            throws SocketException, AudioException
    {
        this.socket = socket;
    }

    public void setMute(boolean enabled) {
    }

    public void start() throws AudioException {
        this.running = true;
        // read in microphone audio
        RtpPacket packet = encrypt(null); // convert audio to rtp packet, and encrypt
        socket.send(packet);

        while (running) {
            // on receive from phone
            try {
                packet = socket.receive();
                packet = decrypt(packet);
                // play audio packet
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    public void terminate() {
        running = false;
        Logger.getLogger(getClass().getName()).log(Level.INFO, "Terminated");
    }

    private RtpPacket encrypt(RtpPacket packet) {
        return packet;
    }
    private RtpPacket decrypt(RtpPacket packet) {
        return packet;
    }

}
