package com.theost.wavenote;

import android.annotation.SuppressLint;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import com.theost.wavenote.configs.AudioConfig;
import com.theost.wavenote.configs.BundleKeys;
import com.theost.wavenote.configs.SpConfig;
import com.theost.wavenote.models.Note;
import com.theost.wavenote.utils.AudioUtils;
import com.theost.wavenote.utils.DisplayUtils;
import com.theost.wavenote.utils.FileUtils;
import com.theost.wavenote.widgets.PCMAnalyser;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import static com.theost.wavenote.MetronomeActivity.DEFAULT_SOUND;

public class AdjustRecordActivity extends ThemedAppCompatActivity {

    private static final int STATUS_PLAY_PREPARE = 1;
    private static final int STATUS_PLAYING = 2;

    private static final int ANALYZE_BEAT_LEN = 8;

    private TextView mResultTextView;
    private ImageButton mPlayButton;

    private short currentSpeed;

    private AudioTrack audioTrack;
    private AudioRecord audioRecord;
    private byte[] playBeatBytes;

    private int beatFirstSoundBytePos;

    private PCMAnalyser pcmAudioFile;
    private BeatPlayHandler beatPlayHandler;
    private boolean stopPlay;

    private int playStatus;
    private int readRecordBytesLen;
    private int errorRangeByteLen;

    private boolean isFirstRecordWrite;
    private boolean isFirstPlayWrite;
    private CyclicBarrier recordBarrier = new CyclicBarrier(2);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_adjust);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        setTitle(R.string.adjust_record);
        setResult(RESULT_CANCELED);

        currentSpeed = getIntent().getShortExtra(BundleKeys.RESULT_SPEED, Note.getActiveMetronomeSpeed());

        mResultTextView = findViewById(R.id.adjust_result);
        mPlayButton = findViewById(R.id.play_beat);
        findViewById(R.id.play_beat).setOnClickListener(this::onStartPlayBeat);

        audioTrack = AudioUtils.createTrack(AudioConfig.BEAT_CHANNEL_COUNT);
        pcmAudioFile = PCMAnalyser.createPCMAnalyser(AudioConfig.BEAT_CHANNEL_COUNT);
        audioRecord = AudioUtils.createAudioRecord();
        HandlerThread playBeatHandlerThread = new HandlerThread("PlayBeatHandlerThread");
        playBeatHandlerThread.start();
        beatPlayHandler = new BeatPlayHandler(playBeatHandlerThread.getLooper());
        loadBeatData();
    }


    void onStartPlayBeat(View v) {
        if (playStatus == STATUS_PLAY_PREPARE) {
            mResultTextView.setVisibility(View.INVISIBLE);
            stopPlay = false;
            recordBarrier.reset();
            audioTrack.play();
            audioRecord.startRecording();
            beatPlayHandler.removeMessages(R.integer.PLAY_BEAT);
            beatPlayHandler.sendEmptyMessage(R.integer.PLAY_BEAT);
            isFirstRecordWrite = true;
            isFirstPlayWrite = true;
            playStatus = STATUS_PLAYING;
            new AdjustThread().start();
        } else if (playStatus == STATUS_PLAYING) {
            stopPlay = true;
            beatPlayHandler.removeMessages(R.integer.PLAY_BEAT);
            audioTrack.stop();
            audioRecord.stop();
            playStatus = STATUS_PLAY_PREPARE;
        }
        updatePlayImage();
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void updatePlayImage() {
        if (stopPlay) {
            mPlayButton.setImageDrawable(getDrawable(R.drawable.ic_rec_24dp));
        } else {
            mPlayButton.setImageDrawable(getDrawable(R.drawable.ic_stop_24dp));
        }
    }

    private void waitRecordToSync() {
        if (recordBarrier != null) {
            try {
                recordBarrier.await();
            } catch (BrokenBarrierException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopPlay = true;
        audioTrack.release();
        audioRecord.release();
        audioTrack = null;
        beatPlayHandler.removeCallbacksAndMessages(null);
        beatPlayHandler = null;
    }

    private void loadBeatData() {
        new Thread() {
            @Override
            public void run() {
                List<String> resourceSounds = Arrays.asList(getResources().getStringArray(R.array.array_metronome_sounds));

                String metronomeSound = Note.getActiveMetronomeSound();
                if (!resourceSounds.contains(metronomeSound))
                    metronomeSound = resourceSounds.get(DEFAULT_SOUND);
                byte[] beatStrongBytes;
                try {
                    beatStrongBytes = FileUtils.getStereoBeatResource(getApplicationContext(), metronomeSound, true)[0];

                    beatFirstSoundBytePos = pcmAudioFile.getBytesPerBeat(currentSpeed, (byte) 4);
                    playBeatBytes = pcmAudioFile.generateBeatBytes(beatStrongBytes, null, "1/4", currentSpeed);
                    readRecordBytesLen = playBeatBytes.length * ANALYZE_BEAT_LEN;
                    errorRangeByteLen = (int) (pcmAudioFile.bytesPerSecond() / 1000.0 * 10);
                    playStatus = STATUS_PLAY_PREPARE;
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }.start();
    }

    private int getMaxSamplePos(byte[] audioBytes) {

        ByteBuffer beatBuffer = ByteBuffer.wrap(audioBytes);
        beatBuffer.order(ByteOrder.LITTLE_ENDIAN);
        int sampleValue;
        int maxSampleValue = 0;

        int maxPosition = 0;

        while (beatBuffer.hasRemaining()) {
            sampleValue = Math.abs(beatBuffer.getShort());
            if (maxSampleValue < sampleValue) {
                maxSampleValue = sampleValue;
            }
        }

        beatBuffer.rewind();
        while (beatBuffer.hasRemaining()) {
            if (Math.abs(beatBuffer.getShort()) == maxSampleValue) {
                maxPosition = beatBuffer.position() - 2;
                break;
            }
        }

        return maxPosition;
    }

    private class BeatPlayHandler extends Handler {

        BeatPlayHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            if (stopPlay) {
                return;
            }
            if (isFirstPlayWrite) {
                isFirstPlayWrite = false;
                waitRecordToSync();
            }
            audioTrack.write(playBeatBytes, 0, playBeatBytes.length);
            super.sendEmptyMessage(R.integer.PLAY_BEAT);
        }
    }

    private Handler mAdjustHandler = new Handler(Looper.getMainLooper(), msg -> {
        int adjustTip = 0;
        int adjustResult = 0;
        if (msg.what == R.integer.ADJUST_RECORD_DISTANCE_DONE) {
            adjustResult = R.string.adjust_success;
            adjustTip = R.string.adjust_success_tip;
            int distanceBytes = (int) msg.obj;
            SpConfig.sp(this).saveRecordAdjustLen(distanceBytes);
            setResult(RESULT_OK);
        } else if (msg.what == R.integer.ADJUST_RECORD_DISTANCE_FAIL) {
            adjustResult = R.string.adjust_failed;
            adjustTip = R.string.adjust_failed_tip;
            setResult(RESULT_CANCELED);
        }
        updatePlayImage();
        DisplayUtils.showToast(this, getString(adjustTip));
        mResultTextView.setText(getString(adjustResult));
        mResultTextView.setVisibility(View.VISIBLE);
        return true;
    });

    private class AdjustThread extends Thread {

        @Override
        public void run() {
            while (!stopPlay) {
                byte[] readSamples = new byte[readRecordBytesLen];

                if (isFirstRecordWrite) {
                    isFirstRecordWrite = false;
                    waitRecordToSync();
                }

                int readSize = audioRecord.read(readSamples, 0, readRecordBytesLen);
                if (readSize > 0) {
                    byte[] segBytes = new byte[playBeatBytes.length];

                    int[] maxPositions = new int[ANALYZE_BEAT_LEN];
                    for (int i = 0; i != ANALYZE_BEAT_LEN; i++) {
                        System.arraycopy(readSamples, i * playBeatBytes.length, segBytes, 0, playBeatBytes.length);
                        maxPositions[i] = getMaxSamplePos(segBytes);
                    }

                    Arrays.sort(maxPositions);

                    int sampleTotalValue = 0;
                    int sampleLen = ANALYZE_BEAT_LEN / 2;
                    int[] sampleValues = new int[sampleLen];

                    for (int beginIndex = sampleLen / 2, i = 0; i != sampleLen; i++) {
                        sampleValues[i] = maxPositions[i + beginIndex];
                        sampleTotalValue += sampleValues[i];
                    }

                    int averSampleValue = sampleTotalValue / sampleLen;

                    boolean isValid = true;
                    for (int sampleValue : sampleValues) {
                        if (Math.abs(averSampleValue - sampleValue) > errorRangeByteLen) {
                            isValid = false;
                        }
                    }

                    stopPlay = true;
                    if (isValid && readSize == readRecordBytesLen) {
                        Message doneMsg = Message.obtain();
                        doneMsg.what = R.integer.ADJUST_RECORD_DISTANCE_DONE;
                        doneMsg.obj = averSampleValue - beatFirstSoundBytePos / 2;
                        mAdjustHandler.sendMessage(doneMsg);
                    } else {
                        mAdjustHandler.sendEmptyMessage(R.integer.ADJUST_RECORD_DISTANCE_FAIL);
                    }
                }
            }
        }
    }

}