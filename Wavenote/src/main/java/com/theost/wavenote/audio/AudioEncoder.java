package com.theost.wavenote.audio;

import java.io.File;

public abstract class AudioEncoder {

    File rawAudioFile;

    AudioEncoder(File rawAudioFile) {
        this.rawAudioFile = rawAudioFile;
    }

    public static AudioEncoder createAccEncoder(File rawAudioFile, int channelCount) {
        return new AACAudioEncoder(rawAudioFile, channelCount);
    }

    public abstract void encodeToFile(File outEncodeFile);
}