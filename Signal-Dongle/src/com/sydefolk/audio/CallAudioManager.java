package com.sydefolk.audio;

import com.audiointerface.audio.AudioInput;
import com.audiointerface.audio.AudioOutput;
import com.sydefolk.CustomSocket;
import com.sydefolk.network.RtpPacket;

import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CallAudioManager {

    private static final String TAG = CallAudioManager.class.getSimpleName();

    private CustomSocket socket = null;
    private Boolean running = false;

    private AudioInput audioInput = null;
    private AudioOutput audioOutput = null;

    public CallAudioManager(CustomSocket socket,
                            byte[] senderCipherKey, byte[] senderMacKey, byte[] senderSalt,
                            byte[] receiverCipherKey, byte[] receiverMacKey, byte[] receiverSalt)
            throws SocketException, AudioException
    {
        this.socket = socket;
        this.audioInput = new AudioInput(null);
        this.audioOutput = new AudioOutput(null);
        this.audioInput.captureMicrophone();
    }

    public void setMute(boolean enabled) {
    }

    public void start() throws AudioException {
        this.running = true;

        (new AudioInputThread(this.audioInput)).start();
        (new AudioOutputThread(this.audioOutput)).start();
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

    // listens for audio input, and sends it over the socket
    private class AudioInputThread extends Thread {
        AudioInput audioInput = null;
        public AudioInputThread(AudioInput audioInput) {
            this.audioInput = audioInput;
        }

        public void run() {
            while(running) {
                byte[] data = this.audioInput.receive(); // raw audio bytes
                RtpPacket packet = new RtpPacket(data, data.length);
                packet = encrypt(packet);
                socket.send(packet);
            }
        }
    }

    // listens for audio input, and sends it over the socket
    private class AudioOutputThread extends Thread {
        AudioOutput audioOutput = null;
        public AudioOutputThread(AudioOutput audioOutput) {
            this.audioOutput = audioOutput;
        }

        public void run() {
            while (running) {
                // on receive from phone
                try {
                    RtpPacket packet = socket.receive();
                    packet = decrypt(packet);
                    this.audioOutput.outputAudio(packet.getPayload());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
