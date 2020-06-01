package com.theost.wavenote.configs;

import android.media.AudioFormat;

public interface AudioConfig {
    int AUDIO_SAMPLE_RATE = 44100;
    int AUDIO_IN_CHANNEL = AudioFormat.CHANNEL_IN_MONO;
    int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    int MAX_SUPPORT_CHANNEL_COUNT = 7;
    int BEAT_CHANNEL_COUNT = 2;
    int SAMPLES_PER_FRAME = 1024;
}