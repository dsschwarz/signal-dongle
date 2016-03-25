package com.sydefolk.audioIO;

import javax.sound.sampled.AudioFormat;

public class AudioFormatHelper {
  public static AudioFormat getAudioFormat(){
    float sampleRate = 8000.0F;
    int sampleSizeInBits = 16;
    int channels = 1;
    boolean signed = true;
    boolean bigEndian = false;
    return new AudioFormat(sampleRate,
            sampleSizeInBits,
            channels,
            signed,
            bigEndian);
  }
}
