package com.theost.wavenote.widgets;

import com.theost.wavenote.configs.AudioConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

public class PCMAnalyser {

    // Member variables for hack (making it work with old version, until app just uses the samples).
    private int mNumFrames;
    private double[] mFrameGains;

    private int mSampleRate;
    private int mChannelCount;
    private int mNumSamples;  // total number of samples per channel in audio file

    private int mBitDepth;
    private int mBytesPerSample;
    private double mMaxFrameValue;
    private int mBytesPerSecond;

    private PCMAnalyser(int sampleRate, int channelCount, int bitDepth) {
        mSampleRate = sampleRate;
        mChannelCount = channelCount;
        mBytesPerSample = mChannelCount * bitDepth / 8;
        mBitDepth = bitDepth;
        mMaxFrameValue = bitDepth == 8 ? 16 : 181;
        mBytesPerSecond = mSampleRate * mBytesPerSample;
    }

    public static PCMAnalyser createPCMAnalyser() {
        return new PCMAnalyser(AudioConfig.AUDIO_SAMPLE_RATE, 1, 16);
    }

    public static PCMAnalyser createPCMAnalyser(int channelCount) {
        return new PCMAnalyser(AudioConfig.AUDIO_SAMPLE_RATE, channelCount, 16);
    }

    public void readRawFile(File rawPCMFile) throws IOException{

        int fileSize = (int)rawPCMFile.length();
        mNumSamples = (int)rawPCMFile.length() / mBytesPerSample;
        // Average bit rate in kbps.
        int mAvgBitRate = (int) ((fileSize * 8) * ((float) mSampleRate / mNumSamples) / 1000);

        mNumFrames = mNumSamples / getSamplesPerFrame();
        if (mNumSamples % getSamplesPerFrame() != 0) {
            mNumFrames++;
        }

        mFrameGains = new double[mNumFrames];

        int j;
        int gain, value;

        FileInputStream pcmStream = new FileInputStream(rawPCMFile);
        FileChannel fileChannel = pcmStream.getChannel();
        int bufSize = getSamplesPerFrame() * mBytesPerSample;
        ByteBuffer buffer = ByteBuffer.allocate(bufSize);

        for (int i = 0; i<mNumFrames; i++) {
            gain = -1;

            buffer.clear();
            fileChannel.read(buffer);

            buffer.flip();
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            for (j=0; j<getSamplesPerFrame(); j++) {
                value = 0;

                for (int k=0; k< mChannelCount; k++) {
                    if (buffer.remaining() > 0) {
                        value += Math.abs(mBitDepth == 8 ? buffer.get(): buffer.getShort());
                    }
                }

                value /= mChannelCount;//平均通道值
                if (gain < value) {
                    gain = value;
                }
            }

            mFrameGains[i] = Math.sqrt(gain) / getMaxFrameValue();  // here gain = sqrt(max value of 1st channel)...
        }

        fileChannel.close();
        pcmStream.close();
    }

    public void readByteBuffer(ByteBuffer buffer) {
        mNumSamples = buffer.limit() / mBytesPerSample;
        mNumFrames = mNumSamples / getSamplesPerFrame();

        if (mNumSamples % getSamplesPerFrame() != 0) {
            mNumFrames++;
        }

        mFrameGains = new double[mNumFrames];

        int j;
        int gain, value;

        buffer.order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i<mNumFrames; i++) {
            gain = -1;
            for (j=0; j<getSamplesPerFrame(); j++) {
                value = 0;

                for (int k=0; k< mChannelCount; k++) {
                    if (buffer.remaining() > 0) {
                        value += Math.abs(mBitDepth == 8 ? buffer.get(): buffer.getShort());
                    }
                }

                value /= mChannelCount;//平均通道值
                if (gain < value) {
                    gain = value;
                }

            }

            mFrameGains[i] = Math.sqrt(gain) / getMaxFrameValue();  // here gain = sqrt(max value of 1st channel)...
        }
    }

    public byte[] generateBeatBytes(byte[] beatStrongBytes, byte[] beetWeakBytes, String currentBeat, int currentSpeed) {
        String[] beatStr = currentBeat.split("/");
        byte beatNum = Byte.parseByte(beatStr[0]);
        byte beetNote = Byte.parseByte(beatStr[1]);

        double beatPerMilSecond = (60.0 * 1000 / currentSpeed) / (beetNote / 4.0);
        int bytesPerBeat = (int)(beatPerMilSecond * (bytesPerSecond() / 1000.0));

        if (bytesPerBeat % 2 != 0) {
            bytesPerBeat++;
        }

        byte[] newPlayBeatBytes = new byte[bytesPerBeat * beatNum];

        System.arraycopy(beatStrongBytes, 0, newPlayBeatBytes, 0, Math.min(beatStrongBytes.length, bytesPerBeat));
        for (int i = 1; i != beatNum; i++) {
            System.arraycopy(beetWeakBytes, 0 , newPlayBeatBytes, i * bytesPerBeat, Math.min(beetWeakBytes.length, bytesPerBeat));
        }

        return newPlayBeatBytes;
    }

    private int getSamplesPerFrame() {
        return AudioConfig.SAMPLES_PER_FRAME;  // just a fixed value here...
    }

    public int bytesPerFrame() {
        return getSamplesPerFrame() * mChannelCount * 2;
    }

    public int bytesPerSecond() {
        return mBytesPerSecond;
    }

    public int bytesPerSample() {
        return mBytesPerSample;
    }

    public int getNumFrames() {
        return mNumFrames;
    }

    public int getSampleRate() {
        return mSampleRate;
    }

    public double[] getFrameGains() {
        return mFrameGains;
    }

    private double getMaxFrameValue() {
        return mMaxFrameValue;
    }
}
