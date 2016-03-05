package com.sydefolk.audio;

import com.sydefolk.CustomSocket;

import java.io.FileDescriptor;
import java.lang.reflect.Field;
import java.net.DatagramSocket;
import java.net.DatagramSocketImpl;
import java.net.SocketException;

public class CallAudioManager {

    private static final String TAG = CallAudioManager.class.getSimpleName();

    static {
        System.loadLibrary("redphone-audio");
    }

    private final long handle;

    public CallAudioManager(CustomSocket socket,
                            byte[] senderCipherKey, byte[] senderMacKey, byte[] senderSalt,
                            byte[] receiverCipherKey, byte[] receiverMacKey, byte[] receiverSalt)
            throws SocketException, AudioException
    {
        try {
            this.handle = create(getFileDescriptor(socket),
                    senderCipherKey, senderMacKey, senderSalt,
                    receiverCipherKey, receiverMacKey, receiverSalt);
        } catch (NativeAudioException e) {
            throw new AudioException("Sorry, there was a problem initiating audio on your device");
        }
    }

    public void setMute(boolean enabled) {
        setMute(handle, enabled);
    }

    public void start() throws AudioException {
        try {
            start(handle);
        } catch (NativeAudioException | NoSuchMethodError e) {
            throw new AudioException("Sorry, there was a problem initiating the audio on your device.");
        }
    }

    public void terminate() {
        stop(handle);
        dispose(handle);
    }

    private static int getFileDescriptor(CustomSocket socket) throws SocketException {
        try {
            socket.setTimeout(5000);
            Field implField = DatagramSocket.class.getDeclaredField("impl");
            implField.setAccessible(true);

            DatagramSocketImpl implValue = (DatagramSocketImpl)implField.get(socket);

            Field fdField = DatagramSocketImpl.class.getDeclaredField("fd");
            fdField.setAccessible(true);

            FileDescriptor fdValue = (FileDescriptor)fdField.get(implValue);

            Field descField = FileDescriptor.class.getDeclaredField("descriptor");
            descField.setAccessible(true);

            return (Integer)descField.get(fdValue);
        } catch (NoSuchFieldException e) {
            throw new AssertionError(e);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    private native long create(int socketFd,
                               byte[] senderCipherKey, byte[] senderMacKey, byte[] senderSalt,
                               byte[] receiverCipherKey, byte[] receiverMacKey, byte[] receiverSalt)
            throws NativeAudioException;

    private native void start(long handle) throws NativeAudioException;

    private native void setMute(long handle, boolean enabled);

    private native void stop(long handle);

    private native void dispose(long handle);

}
