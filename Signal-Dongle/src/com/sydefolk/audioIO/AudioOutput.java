package com.sydefolk.audioIO;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class AudioOutput {

	SourceDataLine speaker;
	Mixer speakerMixer = null;
	InputStream speakerStream;
	AudioFormat audioFormat;
	Thread playThread;

	public AudioOutput(Mixer speakerMixer) {
		getMixers();
		if (speakerMixer == null) {
			Mixer.Info[] mixerInfo = AudioSystem.getMixerInfo();
			this.speakerMixer = AudioSystem.getMixer(mixerInfo[2]);
		} else {
			this.speakerMixer = speakerMixer;
		}
		audioFormat = AudioFormatHelper.getAudioFormat();
		DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, audioFormat);

		try {
			speaker = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
			speaker.open(audioFormat);
		} catch (LineUnavailableException e) {
			e.printStackTrace();
			return;
		}
		speaker.start();
		speakerStream = new ByteArrayInputStream(new byte[10]);
		playThread = new PlayThread();
		playThread.start();
	}
	public void outputAudio(byte[] audioData){
		speakerStream = new ByteArrayInputStream(audioData);
		speakerStream = new AudioInputStream(speakerStream, audioFormat,
						audioData.length/audioFormat.getFrameSize());
	}

	public static void getMixers(){
		Mixer.Info[] mixerInfo = AudioSystem.getMixerInfo();
		System.out.println("Available mixers:");
		for (Mixer.Info aMixerInfo : mixerInfo) {
			System.out.println(aMixerInfo.getName());
		}
	}

	class PlayThread extends Thread{
		boolean running = true;
		public PlayThread() {
			this.setName("Play Thread");
		}
		byte tempBuffer[] = new byte[10000];
		public void run(){
			while(running) {
				try	{
					int cnt;
					while ((cnt=speakerStream.read(tempBuffer, 0, tempBuffer.length)) != -1) {
						if (cnt > 0) {
							speaker.write(tempBuffer, 0, cnt);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(0);
				}
			}
			speaker.drain();
			speaker.close();
		}

		public void endPlayback() {
			this.running = false;
		}
	}
}
