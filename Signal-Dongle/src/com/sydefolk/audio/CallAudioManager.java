package com.sydefolk.audio;

import com.sydefolk.audioIO.AudioInput;
import com.sydefolk.audioIO.AudioOutput;
import com.sydefolk.CustomSocket;
import com.sydefolk.network.RtpPacket;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CallAudioManager {

    private static final String TAG = CallAudioManager.class.getSimpleName();

    private CustomSocket socket = null;
    private Boolean running = false;

    private AudioInput audioInput = null;
    private AudioOutput audioOutput = null;
    private AudioOutput encryptedAudioOutput = null;

    public CallAudioManager(CustomSocket socket,
                            byte[] senderCipherKey, byte[] senderMacKey, byte[] senderSalt,
                            byte[] receiverCipherKey, byte[] receiverMacKey, byte[] receiverSalt)
            throws SocketException, AudioException
    {
        Logger.getLogger("CallAudioManager").log(Level.INFO, "Starting audio manager");
        this.socket = socket;
        this.audioInput = new AudioInput(null);
        this.audioInput.captureMicrophone();
        Mixer.Info[] mixerInfo = AudioSystem.getMixerInfo();
        this.encryptedAudioOutput = new AudioOutput(AudioSystem.getMixer(mixerInfo[5]));
        this.audioOutput = new AudioOutput(AudioSystem.getMixer(mixerInfo[6]));
    }

    public void setMute(boolean enabled) {
    }

    public void start() throws AudioException {
        this.running = true;

        (new AudioInputThread(this.audioInput)).start();
        (new AudioOutputThread(this.audioOutput, this.encryptedAudioOutput)).start();
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
        AudioOutput encryptedAudioOutput = null;
        public AudioOutputThread(AudioOutput audioOutput, AudioOutput encryptedAudioOutput) {
            this.audioOutput = audioOutput;
            this.encryptedAudioOutput = encryptedAudioOutput;
        }

        public void run() {
            while (running) {
                // on receive from phone
                try {
                    RtpPacket packet = socket.receive();
                    packet = decrypt(packet);
                    byte[] payload = packet.getPayload();
                    byte[] backup = Arrays.copyOf(payload, payload.length);
//                    Garble(backup);
//                    this.encryptedAudioOutput.outputAudio(backup);
                    this.audioOutput.outputAudio(payload);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static byte[] Garble(byte[] array){
        Random rgen = new Random();  // Random number generator

        rgen.nextBytes(array);

        return array;
    }

}
