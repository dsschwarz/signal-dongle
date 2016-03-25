package com.sydefolk.audioIO;

import javax.sound.sampled.*;

public class AudioInput {
	private TargetDataLine microphone;
	private Mixer microphoneMixer = null;
	private Integer packetSize = 10000;

	private byte[] latestData = null;
	private final Object lock = new Object();


	public AudioInput(Mixer microphoneMixer) {
		if (microphoneMixer == null) {
			Mixer.Info[] mixerInfo = AudioSystem.getMixerInfo();
			this.microphoneMixer = AudioSystem.getMixer(mixerInfo[4]);
		} else {
			this.microphoneMixer = microphoneMixer;
		}
	}

	/**
	 * Specifies the number of bytes to be read at a time from the microphone
	 */
	public void setPacketSize(Integer packetSize) {
		this.packetSize = packetSize;
	}

	public void captureMicrophone(){
		AudioFormat audioFormat = AudioFormatHelper.getAudioFormat();
		DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, audioFormat);
		try {
			microphone = (TargetDataLine) microphoneMixer.getLine(dataLineInfo);
			microphone.open(audioFormat);
		} catch (LineUnavailableException e) {
			e.printStackTrace();
		}
		microphone.start();
		Thread captureThread = new CaptureThread(this);
		captureThread.start();
	}

	public byte[] receive() {
		synchronized (lock) {
			try {
				lock.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return latestData;
	}

	private void onReceive(byte[] data) {
		latestData = data;
		synchronized (lock) {
			lock.notify();
		}
	}

	private class CaptureThread extends Thread {
		byte tempBuffer[] = new byte[packetSize];
		AudioInput parent = null;

		public CaptureThread(AudioInput parent) {
			this.parent = parent;
		}

		public void run() {
			while (true) {
				int cnt = microphone.read(tempBuffer,0,tempBuffer.length);
				if (cnt > 0) {
					parent.onReceive(tempBuffer);
				}
			}
//			try {
//				microphoneStream.close();
//			} catch (IOException e) {
//			  	e.printStackTrace();
//			}
		}
	}
}


	