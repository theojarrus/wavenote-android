package com.theost.wavenote.audio;

import android.content.Context;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import com.theost.wavenote.configs.SpConfig;
import com.theost.wavenote.utils.AudioUtils;
import com.theost.wavenote.utils.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class MusicRecorder {

    private final static int AUDIO_INPUT = MediaRecorder.AudioSource.MIC;

    private static final int STATUS_NOT_READY = 0x0;
    private static final int STATUS_READY = 0x1;
    private static final int STATUS_RECORDING = 0x2;
    private static final int STATUS_STOP = 0x3;

    private AudioRecord audioRecord;
    private File outputFile;
    private int status = STATUS_NOT_READY;

    private OnRecordListener onRecordListener;
    private int frameBufferSize;
    private int recordAdjustLen;

    public MusicRecorder(Context context, File outputFile, int frameBufferSize) {
        this.outputFile = outputFile;
        this.frameBufferSize = frameBufferSize;
        this.recordAdjustLen = SpConfig.sp(context).getRecordAdjustLen();
    }

    public void setOnRecordListener(OnRecordListener onRecordListener) {
        this.onRecordListener = onRecordListener;
    }

    public void start() {
        if (audioRecord == null) {
            initAudioRecord();
        }

        if (status == STATUS_NOT_READY
                || status == STATUS_RECORDING) {
            return;
        }

        audioRecord.startRecording();
        saveToDisk();
    }

    public void stop() {

        if (status != STATUS_RECORDING) {
            return;
        }

        status = STATUS_STOP;
        audioRecord.stop();
    }

    public void release() {
        if (audioRecord != null) {
            audioRecord.release();
            audioRecord = null;
        }
    }

    private void initAudioRecord() {
        this.audioRecord = AudioUtils.createAudioRecord();
        this.status = STATUS_READY;
    }

    private void saveToDisk() {
        new Thread() {
            @Override
            public void run() {
                FileOutputStream tempAudioFile = null;
                FileChannel fileChannel = null;
                try {
                    tempAudioFile = new FileOutputStream(outputFile, true);
                    fileChannel = tempAudioFile.getChannel();
                    final int bufferSize = frameBufferSize;
                    int readSize;
                    ByteBuffer buffer = ByteBuffer.allocateDirect(bufferSize);
                    status = STATUS_RECORDING;
                    if (onRecordListener != null) {
                        onRecordListener.onRecordStart();
                    }

                    audioRecord.read(new byte[recordAdjustLen], 0, recordAdjustLen);

                    while (status == STATUS_RECORDING) {
                        buffer.clear();
                        readSize = audioRecord.read(buffer, bufferSize);
                        if (readSize > 0) {
                            if (onRecordListener != null) {
                                buffer.limit(bufferSize);
                                buffer.rewind();
                                onRecordListener.onRecording(buffer);
                            }
                            buffer.position(bufferSize);
                            buffer.flip();
                            fileChannel.write(buffer);
                        }
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                } finally {
                    IOUtils.closeSilently(fileChannel);
                    IOUtils.closeSilently(tempAudioFile);
                }
            }
        }.start();
    }

    public interface OnRecordListener {

        void onRecordStart();

        void onRecording(ByteBuffer data);
    }
}
