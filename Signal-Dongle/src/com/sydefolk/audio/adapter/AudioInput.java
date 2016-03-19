package com.sydefolk.audio.adapter;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;

public class AudioInput {
	TargetDataLine microphone;
	Mixer microphoneMixer = null;
	ByteArrayOutputStream microphoneStream;

	public AudioInput(Mixer microphoneMixer) {
		if (microphoneMixer == null) {
			Mixer.Info[] mixerInfo = AudioSystem.getMixerInfo();
			this.microphoneMixer = AudioSystem.getMixer(mixerInfo[0]);
		} else {
			this.microphoneMixer = microphoneMixer;
		}
	}

	private void captureMicrophone(){
		AudioFormat audioFormat = AudioFormatHelper.getAudioFormat();
		DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, audioFormat);
		try {
			microphone = (TargetDataLine) microphoneMixer.getLine(dataLineInfo);
			microphone.open(audioFormat);
		} catch (LineUnavailableException e) {
			e.printStackTrace();
		}
		microphone.start();
		Thread captureThread = new CaptureThread();
		captureThread.start();
	}

	class CaptureThread extends Thread {
		byte tempBuffer[] = new byte[10000];
		public void run() {
			microphoneStream = new ByteArrayOutputStream();
			while (true) {
				int cnt = microphone.read(tempBuffer,0,tempBuffer.length);
				if (cnt > 0) {
					microphoneStream.write(tempBuffer,0,cnt);
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


	