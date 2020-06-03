package com.theost.wavenote.utils;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.os.Build;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import static android.media.AudioFormat.CHANNEL_OUT_BACK_LEFT;
import static android.media.AudioFormat.CHANNEL_OUT_BACK_RIGHT;
import static android.media.AudioFormat.CHANNEL_OUT_FRONT_CENTER;
import static android.media.AudioFormat.CHANNEL_OUT_FRONT_LEFT;
import static android.media.AudioFormat.CHANNEL_OUT_FRONT_RIGHT;
import static android.media.AudioFormat.CHANNEL_OUT_SIDE_LEFT;
import static android.media.AudioFormat.CHANNEL_OUT_SIDE_RIGHT;
import static com.theost.wavenote.configs.AudioConfig.AUDIO_ENCODING;
import static com.theost.wavenote.configs.AudioConfig.AUDIO_IN_CHANNEL;
import static com.theost.wavenote.configs.AudioConfig.AUDIO_SAMPLE_RATE;

public class AudioUtils {

    public static byte[] readWavData(InputStream wavStream) throws IOException {
        wavStream.skip(44);
        byte[] data = new byte[wavStream.available()];
        wavStream.read(data);
        wavStream.close();
        return data;
    }

    public static AudioTrack createTrack(int channelCount) {
        int channelConfig;

        if (channelCount == 1) {
            channelConfig = AudioFormat.CHANNEL_OUT_MONO;
        } else if (channelCount == 2) {
            channelConfig = AudioFormat.CHANNEL_OUT_STEREO;
        } else if (channelCount == 3) {
            channelConfig = CHANNEL_OUT_FRONT_LEFT |
                    CHANNEL_OUT_FRONT_RIGHT | CHANNEL_OUT_FRONT_CENTER;
        } else if (channelCount == 4) {
            channelConfig = AudioFormat.CHANNEL_OUT_SURROUND;
        } else if (channelCount == 5) {
            channelConfig = CHANNEL_OUT_FRONT_LEFT | CHANNEL_OUT_FRONT_RIGHT |
                    CHANNEL_OUT_FRONT_CENTER | CHANNEL_OUT_BACK_RIGHT | CHANNEL_OUT_BACK_LEFT;
        } else if (channelCount == 6) {
            channelConfig = AudioFormat.CHANNEL_OUT_5POINT1;
        } else if (channelCount == 7) {
            channelConfig = CHANNEL_OUT_FRONT_LEFT | CHANNEL_OUT_FRONT_CENTER | CHANNEL_OUT_FRONT_RIGHT |
                    CHANNEL_OUT_SIDE_LEFT | CHANNEL_OUT_SIDE_RIGHT |
                    CHANNEL_OUT_BACK_LEFT | CHANNEL_OUT_BACK_RIGHT;
        } else if (channelCount == 8) {
            channelConfig = AudioFormat.CHANNEL_OUT_7POINT1_SURROUND;
        } else {
            throw new IllegalArgumentException("channelCount not support");
        }

        int bufferSizeInBytes = AudioTrack.getMinBufferSize(AUDIO_SAMPLE_RATE, channelConfig, AUDIO_ENCODING);
        return new AudioTrack(AudioManager.STREAM_MUSIC,
                AUDIO_SAMPLE_RATE,
                channelConfig,
                AUDIO_ENCODING,
                bufferSizeInBytes,
                AudioTrack.MODE_STREAM);
    }

    public static AudioRecord createAudioRecord() {
        final int minBufferSize = AudioRecord.getMinBufferSize(AUDIO_SAMPLE_RATE, AUDIO_IN_CHANNEL, AUDIO_ENCODING);
        return new AudioRecord(MediaRecorder.AudioSource.MIC, AUDIO_SAMPLE_RATE, AUDIO_IN_CHANNEL, AUDIO_ENCODING, minBufferSize);
    }


    public static long[] getAudioParams(String path) {
        MediaExtractor mediaExtractor = new MediaExtractor();
        try {
            mediaExtractor.setDataSource(path);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        MediaFormat mediaFormat = mediaExtractor.getTrackFormat(0);

        long channelCount = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        long sampleRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        long bitRate = mediaFormat.getInteger("bits-per-sample");
        long audioSize = new File(path).length();

        return new long[]{channelCount, sampleRate, bitRate, audioSize};
    }

    public static byte[] getWavBytes(File audioFile) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(audioFile));
            int read;
            byte[] buff = new byte[1024];
            while ((read = in.read(buff)) > 0) {
                out.write(buff, 0, read);
            }
            out.flush();
            return out.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] convertToMono(byte[] stereoBytes) {
        byte[] monoBytes = new byte[stereoBytes.length / 2];
        for (int i = 0; i < monoBytes.length - monoBytes.length % 2; i += 2) {
            monoBytes[i] = stereoBytes[i * 2];
            monoBytes[i + 1] = stereoBytes[i * 2 + 1];
        }
        return monoBytes;
    }

    public static byte[] convertToStereo(byte[] monoBytes) {
        byte[] stereoBytes = new byte[monoBytes.length * 2];
        for (int i = 0; i < monoBytes.length; i += 2) {
            stereoBytes[i * 2] = monoBytes[i];
            stereoBytes[i * 2 + 1] = monoBytes[i + 1];
            stereoBytes[i * 2 + 2] = monoBytes[i];
            stereoBytes[i * 2 + 3] = monoBytes[i + 1];
        }
        return stereoBytes;
    }

    public static byte[] createWaveFileHeader(long rawAudioSize, int channels, long sampleRate, int bitsPerSample) {

        final byte[] header = new byte[44];

        //ChunkID : "RIFF"
        header[0] = 'R';
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';

        //ChunkSize : ChunkSize = 36 + SubChunk2Size
        long totalDataLen = 36 + rawAudioSize;
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);

        //Format
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';

        //Subchunk1ID : "fmt"
        header[12] = 'f';
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';

        //Subchunk1Size : 16 for PCM
        header[16] = 16;
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;

        //AudioFormat : PCM = 1
        header[20] = 1;
        header[21] = 0;

        //NumChannels : Mono = 1, Stereo = 2
        header[22] = (byte) channels;
        header[23] = 0;

        //SampleRate
        header[24] = (byte) (sampleRate & 0xff);
        header[25] = (byte) ((sampleRate >> 8) & 0xff);
        header[26] = (byte) ((sampleRate >> 16) & 0xff);
        header[27] = (byte) ((sampleRate >> 24) & 0xff);

        //ByteRate : ByteRate = SampleRate * NumChannels * BitsPerSample/8
        long byteRate = sampleRate * channels * bitsPerSample / 8;
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);

        //BlockAlign : BlockAlign = NumChannels * BitsPerSample/8
        header[32] = (byte) (2 * 16 / 8);
        header[33] = 0;

        //BitsPerSample : 8 bits = 8, 16 bits = 16, etc
        header[34] = (byte) bitsPerSample;
        header[35] = 0;

        //Subchunk2ID : "data"
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';

        //Subchunk2Size : Subchunk2Size = NumSamples * NumChannels * BitsPerSample/8
        header[40] = (byte) (rawAudioSize & 0xff);
        header[41] = (byte) ((rawAudioSize >> 8) & 0xff);
        header[42] = (byte) ((rawAudioSize >> 16) & 0xff);
        header[43] = (byte) ((rawAudioSize >> 24) & 0xff);

        return header;
    }

}
