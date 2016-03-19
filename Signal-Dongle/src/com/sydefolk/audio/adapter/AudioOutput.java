package com.sydefolk.audio.adapter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

public class AudioOutput {

	SourceDataLine speaker;
	Mixer speakerMixer = null;
	InputStream speakerStream;

	public AudioOutput(Mixer speakerMixer) {
		if (speakerMixer == null) {
			getMixers();
			Mixer.Info[] mixerInfo = AudioSystem.getMixerInfo();
			this.speakerMixer = AudioSystem.getMixer(mixerInfo[0]);
		} else {
			this.speakerMixer = speakerMixer;
		}
	}

	public void outputAudio(byte[] audioData){
		AudioFormat audioFormat = AudioFormatHelper.getAudioFormat();
		InputStream speakerStream = new ByteArrayInputStream(audioData);
		speakerStream = new AudioInputStream(speakerStream, audioFormat,
						audioData.length/audioFormat.getFrameSize());
		DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, audioFormat);
		try {
			speaker = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
			speaker.open(audioFormat);
		} catch (LineUnavailableException e) {
			e.printStackTrace();
		}
		speaker.start();
		byte tempBuffer[] = new byte[10000];
		try	{
			int cnt;
			while ((cnt=speakerStream.read(tempBuffer, 0, tempBuffer.length)) != -1) {
				if (cnt > 0) {
					speaker.write(tempBuffer, 0, cnt);
				}
			}
		} catch (Exception e) {
			System.out.println(e);
			System.exit(0);
		}
		speaker.drain();
		speaker.close();
//	    Thread playThread = new PlayThread();
//	    playThread.start();
	}

	public static void getMixers(){
		Mixer.Info[] mixerInfo = AudioSystem.getMixerInfo();
		System.out.println("Available mixers:");
		for (Mixer.Info aMixerInfo : mixerInfo) {
			System.out.println(aMixerInfo.getName());
		}
	}

//	class PlayThread extends Thread{
//		byte tempBuffer[] = new byte[10000];
//		public void run(){
//			try	{
//				int cnt;
//				while ((cnt=speakerStream.read(tempBuffer, 0, tempBuffer.length)) != -1) {
//					if (cnt > 0) {
//						speaker.write(tempBuffer, 0, cnt);
//					}
//				}
//			} catch (Exception e) {
//				System.out.println(e);
//				System.exit(0);
//			}
//			speaker.drain();
//			speaker.close();
//		}
//	}
}
