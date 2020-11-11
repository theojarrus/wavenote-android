package com.theost.wavenote;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MenuCompat;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.theost.wavenote.audio.AudioEncoder;
import com.theost.wavenote.audio.MultiAudioMixer;
import com.theost.wavenote.audio.MusicRecorder;
import com.theost.wavenote.configs.AudioConfig;
import com.theost.wavenote.configs.BundleKeys;
import com.theost.wavenote.configs.SpConfig;
import com.theost.wavenote.models.Note;
import com.theost.wavenote.models.Track;
import com.theost.wavenote.utils.ArrayUtils;
import com.theost.wavenote.utils.AudioUtils;
import com.theost.wavenote.utils.DatabaseHelper;
import com.theost.wavenote.utils.DisplayUtils;
import com.theost.wavenote.utils.FileUtils;
import com.theost.wavenote.utils.IOUtils;
import com.theost.wavenote.utils.PermissionUtils;
import com.theost.wavenote.utils.StrUtils;
import com.theost.wavenote.utils.ThemeUtils;
import com.theost.wavenote.widgets.PCMAnalyser;
import com.theost.wavenote.widgets.TrackGroupLayout;
import com.theost.wavenote.widgets.TrackView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import static com.theost.wavenote.MetronomeActivity.DEFAULT_SOUND;

public class StudioActivity extends ThemedAppCompatActivity {

    private static final String ARG_NOTE_ID = "note_id";

    private static final int STATUS_RECORD_PREPARE = 1;
    private static final int STATUS_RECORD_RECORDING = 2;

    private static final int STATUS_PLAY_PREPARE = 1;
    private static final int STATUS_PLAYING = 2;

    private static final int REQ_CODE_BEAT_SETTING = 0x11;
    private static final int REQ_CODE_ADJUST_RECORD = 0x13;

    private static final short DEFAULT_SPEED = 120;
    private static final String DEFAULT_BEAT = "4/4";
    private static final String DEFAULT_TUNE = "C";

    private static final int WAVE_MIN_HEIGHT = 3;
    private static final int WAVE_MIN_WIDTH = 5;
    private static final int PLAY_LINE_WIDTH = 2;
    private static final int WAVE_SPACE_HEIGHT = 2;
    private static final int WAVE_SPACE_WIDTH = 3;

    private DatabaseHelper localDatabase;
    private StudioActivity mActivity;

    private MenuItem manageMenuItem;

    private RelativeLayout adjustTintLayout;
    private ToggleButton mMetronomeToggle;
    private ToggleButton mLoopToggle;
    private TextView mSpeedTextView;
    private TextView mTuneBeatTextView;
    private LinearLayout layoutControl;
    private TrackGroupLayout layoutTracks;
    private FloatingActionButton mRecordButton;
    private ImageButton mAddButton;
    private ImageButton mPlayButton;
    private ImageButton mStopButton;
    private ImageButton mSaveButton;

    private final ArrayList<TrackHolder> trackHolderList = new ArrayList<>();
    private TrackHolder recordingTrackHolder;
    private int trackHeight;

    private MusicRecorder musicRecorder;
    private PCMAnalyser recordPcmAudioFile;
    private int recordStatus;
    private long totalPlayBytes;
    private final MultiAudioMixer audioMixer = MultiAudioMixer.createAudioMixer();
    private AudioTrack musicAudioTrack;

    private byte[] beatStrongBytes;
    private byte[] beetWeakBytes;
    private byte[] playBeatBytes;
    private String currentBeat;
    private String currentTune;
    private short currentSpeed;

    private String audioName;
    private String metronomeSound;

    private boolean isLoopPlay;
    private boolean stopBeatPlay;
    private boolean stopPlay;
    private int playStatus;
    private int bytesPerFrame;

    private boolean stopUpdatePlayFrame;
    private boolean isRendering;

    private List<Track> trackList;
    private List<String> resourceSounds;
    private String noteId;

    private MaterialDialog loadingDialog;

    private final CyclicBarrier recordBarrier = new CyclicBarrier(2);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        ThemeUtils.setTheme(this);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_studio);

        mActivity = this;
        setTitle(R.string.studio);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        if (checkPermission()) onBackPressed();

        adjustTintLayout = findViewById(R.id.tint_layout);
        mMetronomeToggle = findViewById(R.id.metronome_toggle);
        mLoopToggle = findViewById(R.id.loop_toggle);
        mSpeedTextView = findViewById(R.id.beat_speed);
        mTuneBeatTextView = findViewById(R.id.tune_and_beat);
        layoutControl = findViewById(R.id.layout_control);
        layoutTracks = findViewById(R.id.layout_tracks);
        mRecordButton = findViewById(R.id.record_track);
        mAddButton = findViewById(R.id.add_track);
        mPlayButton = findViewById(R.id.play_track);
        mStopButton = findViewById(R.id.reset);
        mSaveButton = findViewById(R.id.save);

        mMetronomeToggle.setOnClickListener(v -> onBeatSoundClick());
        mSpeedTextView.setOnClickListener(v -> onBeatSettingClick());
        mTuneBeatTextView.setOnClickListener(v -> onBeatSettingClick());
        mLoopToggle.setOnClickListener(v -> onRepeatClick());
        mSaveButton.setOnClickListener(v -> onSaveClick());
        mPlayButton.setOnClickListener(v -> onPlayClick());
        mRecordButton.setOnClickListener(v -> onRecordClick());
        mStopButton.setOnClickListener(v -> onResetClick());
        mAddButton.setOnClickListener(v -> onAddTrackClick());

        findViewById(R.id.hide_tint).setOnClickListener(v -> onHideTintClick());
        findViewById(R.id.adjust_tip).setOnClickListener(v -> onAdjustClick());
        findViewById(R.id.adjust_record).setOnClickListener(v -> onAdjustClick());

        noteId = getIntent().getStringExtra(ARG_NOTE_ID);

        trackHeight = getResources().getDimensionPixelSize(R.dimen.track_height);
        recordPcmAudioFile = PCMAnalyser.createPCMAnalyser();

        localDatabase = new DatabaseHelper(this);

        updateNoteData();

        mMetronomeToggle.setChecked(false);
        onBeatSoundClick();

        isLoopPlay = mLoopToggle.isChecked();

        bytesPerFrame = recordPcmAudioFile.bytesPerFrame();
        mSpeedTextView.setText(String.format("= %s", currentSpeed));
        mTuneBeatTextView.setText(String.format("%s %s", currentTune, currentBeat));

        resourceSounds = Arrays.asList(getResources().getStringArray(R.array.metronome_sounds));
        if (Note.getActiveMetronomeSound() == null)
            Note.setActiveMetronomeSound(resourceSounds.get(DEFAULT_SOUND));

        layoutTracks.setOnTrackScrollListener(new TrackGroupLayout.OnTrackScrollListener() {

            @Override
            public void onTrackScrollUp(int nextPlayFramePosition) {
                mAudioHandler.removeMessages(R.integer.REPLAY_ON_SCROLL);
                Message message = Message.obtain();
                message.what = R.integer.REPLAY_ON_SCROLL;
                message.arg1 = bytesPerFrame * nextPlayFramePosition;
                mAudioHandler.sendMessageDelayed(message, 200);
            }

            @Override
            public void onTrackScrollStart() {
                stopUpdatePlayFrame = true;
            }
        });

        updateMetronome();
        updateAdjustTip();
        loadProjectData();
    }

    private void updateNoteData() {
        Cursor noteData = localDatabase.getAudioData(noteId);
        if (noteData != null && noteData.getCount() != 0) {
            while (noteData.moveToNext()) {
                currentTune = noteData.getString(1);
                currentBeat = noteData.getString(2);
                currentSpeed = (short) noteData.getInt(3);
            }
        } else {
            currentTune = DEFAULT_TUNE;
            currentBeat = DEFAULT_BEAT;
            currentSpeed = DEFAULT_SPEED;
            boolean isInserted = localDatabase.insertAudioData(noteId, currentTune, currentBeat, currentSpeed);
            if (!isInserted)
                DisplayUtils.showToast(this, getResources().getString(R.string.database_error));
        }
    }

    private void updateTrackData() {
        trackList = new ArrayList<>();
        Cursor trackData = localDatabase.getTrackData(noteId);
        while (trackData.moveToNext()) {
            Track track = new Track();
            track.setId(trackData.getInt(0));
            track.setName(trackData.getString(2));
            track.setFileName(trackData.getString(3));
            trackList.add(track);
        }
    }

    private void updateMetronome() {
        String newSound = Note.getActiveMetronomeSound();
        if ((newSound != null) && (!newSound.equals(metronomeSound))) metronomeSound = newSound;
        if (metronomeSound == null)
            metronomeSound = getResources().getStringArray(R.array.metronome_sounds)[DEFAULT_SOUND];
    }

    private void updateAdjustTip() {
        if (SpConfig.sp(this).getRecordAdjustLen() == 0) {
            adjustTintLayout.setVisibility(View.VISIBLE);
        } else {
            adjustTintLayout.setVisibility(View.INVISIBLE);
        }
    }

    private boolean checkPermission() {
        boolean isPermissionDenied = false;
        if (!PermissionUtils.requestFilePermissions(this)) isPermissionDenied = true;
        if (!PermissionUtils.requestAudioPermissions(this)) isPermissionDenied = true;
        return isPermissionDenied;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.audio_list, menu);
        menu.setGroupEnabled(0, false);
        MenuCompat.setGroupDividerEnabled(menu, true);
        manageMenuItem = menu.findItem(R.id.menu_manage);
        updateManage();
        return true;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_manage:
                showManageDialog();
                return true;
            case android.R.id.home:
                invalidateOptionsMenu();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showManageDialog() {
        String[] audioList = getAudioList();
        new MaterialDialog.Builder(this)
                .title(R.string.tracks)
                .positiveText(R.string.remove)
                .negativeText(R.string.cancel)
                .items(audioList)
                .itemsCallbackMultiChoice(null, (dialog, which, text) -> {
                    removeAudio(text);
                    return true;
                }).show();
    }

    private final Handler mRemoveHandler = new Handler(msg -> {
        updateManage();
        return true;
    });

    private void removeAudio(CharSequence[] files) {
        new Thread() {
            @Override
            public void run() {
                File audioDir = getAudioDir();
                for (CharSequence i : files)
                    new File(audioDir, i.toString()).delete();
                mRemoveHandler.sendEmptyMessage(RESULT_OK);
            }
        }.start();
    }

    private void updateManage() {
        manageMenuItem.setEnabled(!isAudioEmpty());
    }

    private boolean isAudioEmpty() {
        return getAudioList().length == 0;
    }

    private String[] getAudioList() {
        return getAudioDir().list();
    }

    private File getAudioDir() {
        File dir = new File(getCacheDir() + FileUtils.NOTES_DIR + noteId + FileUtils.AUDIO_DIR);
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    private void onAddTrackClick() {

        if (playStatus == STATUS_PLAYING
                || recordStatus == STATUS_RECORD_RECORDING) {
            return;
        }

        if (trackHolderList.size() >= AudioConfig.MAX_SUPPORT_CHANNEL_COUNT) {
            DisplayUtils.showToast(this, getResources().getString(R.string.exceed_max_support_track_count));
            return;
        }

        new MaterialDialog.Builder(this)
                .title(R.string.track_name)
                .negativeText(R.string.cancel)
                .positiveText(R.string.add)
                .inputType(InputType.TYPE_CLASS_TEXT)
                .input(R.string.name, 0, (dialog, input) -> {
                    String trackName = input.toString().trim();
                    addNewTrack(trackName);
                }).build().show();
    }


    private void onRecordClick() {

        if (checkPermission()) return;

        if (recordingTrackHolder == null) {
            DisplayUtils.showToast(this, getResources().getString(R.string.toast_no_record_track));
            return;
        }

        if (STATUS_RECORD_PREPARE == recordStatus) {
            recordStatus = STATUS_RECORD_RECORDING;
            recordBarrier.reset();
            startPlay();
            musicRecorder.start();
            mRecordButton.setImageResource(R.drawable.ma_stop_24dp);
            disableViews(mAddButton, mPlayButton, mStopButton, mLoopToggle, mSaveButton);
        } else if (STATUS_RECORD_RECORDING == recordStatus) {
            stopPlay = true;
            recordStatus = STATUS_RECORD_PREPARE;
            musicAudioTrack.stop();
            musicRecorder.stop();
            mRecordButton.setImageResource(R.drawable.ma_rec_24dp);
            closeTrackInputs();
            stopPlay();
            enableViews(mAddButton, mPlayButton, mStopButton, mLoopToggle, mSaveButton);
        } else {
            DisplayUtils.showToast(this, getResources().getString(R.string.toast_waiting_data_loading));
        }
    }


    private void onPlayClick() {
        if (ArrayUtils.isNotEmpty(trackHolderList)) {
            if (playStatus == STATUS_PLAY_PREPARE) {
                recordBarrier.reset();
                newEmptyThreadToSync();
                startPlay();
                disableViews(mRecordButton, mAddButton);
            } else if (playStatus == STATUS_PLAYING) {
                stopPlay();
                enableViews(mRecordButton, mAddButton);
            }
        }
    }


    private void onResetClick() {
        if (playStatus == STATUS_PLAY_PREPARE) {
            resetPlay();
        } else if (playStatus == STATUS_PLAYING) {
            stopPlay();
            resetPlay();
            startPlay();
            recordBarrier.reset();
            newEmptyThreadToSync();
            disableViews(mRecordButton, mAddButton);
        }
    }


    private void onRepeatClick() {
        isLoopPlay = mLoopToggle.isChecked();
    }


    private void onBeatSettingClick() {
        Intent intent = new Intent(this, MetronomeActivity.class);
        intent.putExtra(MetronomeActivity.BEAT_SETTING_ARG, true);
        intent.putExtra(BundleKeys.RESULT_BEAT, currentBeat);
        intent.putExtra(BundleKeys.RESULT_SPEED, currentSpeed);
        intent.putExtra(BundleKeys.RESULT_TUNE, currentTune);
        startActivityForResult(intent, REQ_CODE_BEAT_SETTING);
    }

    private void onBeatSoundClick() {
        stopBeatPlay = !mMetronomeToggle.isChecked();
    }

    private void onAdjustClick() {
        Intent intent = new Intent();
        intent.setClass(this, AdjustRecordActivity.class);
        startActivityForResult(intent, REQ_CODE_ADJUST_RECORD);
    }


    private void onHideTintClick() {
        adjustTintLayout.setVisibility(View.INVISIBLE);
    }


    private void onSaveClick() {
        showRenderDialog();
    }

    private void showRenderDialog() {
        audioName = "";
        new MaterialDialog.Builder(this)
                .title(R.string.render)
                .positiveText(R.string.save)
                .negativeText(R.string.cancel)
                .inputType(InputType.TYPE_CLASS_TEXT)
                .input(R.string.name, 0, (dialog, input) -> {
                    audioName = StrUtils.formatFilename(input.toString().trim());
                    if (!audioName.equals("")) renderAudio();
                }).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (musicRecorder != null) {
            musicRecorder.release();
        }
        if (musicAudioTrack != null) {
            stopPlay();
            musicAudioTrack.release();
            musicAudioTrack = null;
        }
    }

    private void initTrackInputs() {
        for (TrackHolder trackHolder : trackHolderList) {
            try {
                if (recordStatus == STATUS_RECORD_RECORDING && trackHolder == recordingTrackHolder) {
                    continue;
                }
                FileInputStream audioStream = new FileInputStream(trackHolder.audioFile);
                audioStream.skip(Math.min(totalPlayBytes, trackHolder.audioFile.length()));
                trackHolder.audioStream = audioStream;
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

    }

    private void loadProjectData() {
        new Thread() {
            @Override
            public void run() {

                File audioDir = new File(getCacheDir() + FileUtils.NOTES_DIR + noteId + FileUtils.TRACKS_DIR);
                if (!audioDir.exists()) audioDir.mkdirs();

                updateTrackData();

                if (ArrayUtils.isNotEmpty(trackList)) {

                    for (Track track : trackList) {
                        TrackHolder trackHolder = new TrackHolder();

                        try {
                            File audioFile = new File(audioDir, track.getFileName());
                            trackHolder.audioFile = audioFile;
                            recordPcmAudioFile.readRawFile(audioFile);
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }

                        trackHolder.track = track;
                        for (double frameGain : recordPcmAudioFile.getFrameGains()) {
                            trackHolder.audioFrames.add(frameGain);
                        }

                        trackHolderList.add(trackHolder);
                    }
                }

                loadBeatData();

                playStatus = STATUS_PLAY_PREPARE;
                recordStatus = STATUS_RECORD_PREPARE;
                mAudioHandler.sendEmptyMessage(R.integer.LOAD_DATA_SUCCESS);
            }
        }.start();
    }

    private void loadBeatData() {
        try {
            byte[][] beatsData;
            if (resourceSounds.contains(metronomeSound)) {
                beatsData = FileUtils.getStereoBeatResource(getApplicationContext(), metronomeSound);
            } else {
                beatsData = FileUtils.getStereoBeatCustom(getApplicationContext(), metronomeSound);
            }
            beatStrongBytes = beatsData[0];
            beetWeakBytes = beatsData[1];
            refreshBeatData();
        } catch (IOException ex) {
            beatStrongBytes = new byte[0];
            beetWeakBytes = new byte[0];
            ex.printStackTrace();
        }
    }

    private void onRenderAudioFail() {
        DisplayUtils.showToast(this, getResources().getString(R.string.mix_failure));
    }

    private void onRenderAudioSuccess(String outputFilePath) {
        new MaterialDialog.Builder(this)
                .title(R.string.mix)
                .content(R.string.mix_succesful)
                .positiveText(android.R.string.ok)
                .build()
                .show();
        updateManage();
    }

    private void onTrackScrollUp(int totalCostBytes) {
        if (playStatus == STATUS_PLAY_PREPARE) {
            totalPlayBytes = totalCostBytes;
        } else if (playStatus == STATUS_PLAYING) {
            totalPlayBytes = totalCostBytes;
            stopPlay();
            startPlay();
            newEmptyThreadToSync();
        }
        stopUpdatePlayFrame = false;
    }

    private void renderAudio() {
        if (!isRendering && ArrayUtils.isNotEmpty(trackHolderList)) {
            new RenderThread().start();
            showLoadingDialog();
        }
    }

    private void showLoadingDialog() {
        StudioActivity context = this;
        new CountDownTimer(200, 200) {
            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                if (isRendering)
                    loadingDialog = DisplayUtils.showLoadingDialog(context, R.string.render, R.string.rendering);
            }
        }.start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_CODE_BEAT_SETTING && resultCode == RESULT_OK) {
            if (data.hasExtra(BundleKeys.RESULT_SPEED)) {
                currentSpeed = data.getShortExtra(BundleKeys.RESULT_SPEED, currentSpeed);
            }
            if (data.hasExtra(BundleKeys.RESULT_BEAT)) {
                currentBeat = data.getStringExtra(BundleKeys.RESULT_BEAT);
            }
            if (data.hasExtra(BundleKeys.RESULT_TUNE)) {
                currentTune = data.getStringExtra(BundleKeys.RESULT_TUNE);
            }
            mSpeedTextView.setText(String.format("= %s", currentSpeed));
            mTuneBeatTextView.setText(String.format("%s  %s", currentTune, currentBeat));
            updateMetronome();
            loadBeatData();
        } else if (requestCode == REQ_CODE_ADJUST_RECORD) {
            updateAdjustTip();
        }
    }

    private void addNewTrack(String trackName) {
        String fileName = UUID.randomUUID().toString();
        Date creationDate = new Date();

        Track track = new Track();
        track.setName(trackName);
        track.setFileName(fileName);

        int trackId = localDatabase.insertTrackData(noteId, trackName, fileName, (int) creationDate.getTime());
        if (trackId == -1) {
            DisplayUtils.showToast(this, getResources().getString(R.string.database_error));
            return;
        }

        track.setId(trackId);

        TrackHolder newTrackHolder = new TrackHolder(new File(getCacheDir() + FileUtils.NOTES_DIR + noteId + FileUtils.TRACKS_DIR + fileName));
        newTrackHolder.track = track;
        addTrackViews(newTrackHolder, true);

        if (recordingTrackHolder != null) {
            recordingTrackHolder.mTrackView.setRecording(false);
        }
        newTrackHolder.mTrackView.setRecording(true);

        recordingTrackHolder = newTrackHolder;
        trackHolderList.add(0, newTrackHolder);

        if (musicRecorder != null) {
            musicRecorder.release();
        }

        totalPlayBytes = 0;
        musicRecorder = new MusicRecorder(this, newTrackHolder.audioFile, bytesPerFrame);
        musicRecorder.setOnRecordListener(new MusicRecorder.OnRecordListener() {

            @Override
            public void onRecordStart() {
                waitRecordToSync();
            }

            @Override
            public void onRecording(ByteBuffer data) {
                data.rewind();
                recordPcmAudioFile.readByteBuffer(data);
                for (double frameGain : recordPcmAudioFile.getFrameGains()) {
                    recordingTrackHolder.audioFrames.add(frameGain);
                }

                mAudioHandler.sendEmptyMessage(R.integer.RECEIVE_RECORDING_DATA);
            }
        });

        updateButtons();
    }

    private void deleteTrack(Track track) {
        boolean isRemoved = localDatabase.removeTrackData(Integer.toString(track.getId()));
        if (!isRemoved)
            DisplayUtils.showToast(this, getResources().getString(R.string.database_error));
        int position = getPositionInTracks(track);

        File file = trackHolderList.get(position).audioFile;
        if (file.exists()) file.delete();

        trackHolderList.remove(position);

        if (recordingTrackHolder != null && recordingTrackHolder.track == track) {
            recordingTrackHolder = null;
        }

        layoutControl.removeViewAt(position);
        layoutTracks.removeViewAt(position);
        updateButtons();
    }

    private void updateTrack(Track track) {
        boolean isRenamed = localDatabase.renameTrackData(Integer.toString(track.getId()), track.getName());
        if (isRenamed) {
            int position = getPositionInTracks(track);
            trackHolderList.get(position).mTrackNameTextView.setText(track.getName());
        } else {
            DisplayUtils.showToast(this, getResources().getString(R.string.database_error));
        }
    }

    private void updateButtons() {
        if (trackHolderList.size() == 0) {
            disableViews(mPlayButton, mStopButton, mSaveButton);
        } else {
            enableViews(mPlayButton, mStopButton, mSaveButton);
        }
    }

    private int getPositionInTracks(Track track) {
        int position = 0;

        for (TrackHolder trackHolder : trackHolderList) {
            if (trackHolder.track == track) {
                break;
            }
            position++;
        }

        return position;
    }

    private void onPlayDone() {
        if (recordStatus != STATUS_RECORD_RECORDING) {
            enableViews(mAddButton, mRecordButton);
            resetPlay();
            playStatus = STATUS_PLAY_PREPARE;
            if (isLoopPlay) {
                mPlayButton.performClick();
            }
        }
    }

    private void onPlayFrame() {
        int currentPlayFrame = (int) (totalPlayBytes / bytesPerFrame);
        setCurrentPlayFrame(currentPlayFrame);
    }

    private void onReceiveRecordingData() {
        final int currentFrame = recordingTrackHolder.audioFrames.size() - 1;
        setCurrentPlayFrame(currentFrame);
    }

    private void setCurrentPlayFrame(int currentFrame) {
        if (!stopUpdatePlayFrame) {
            for (TrackHolder trackHolder : trackHolderList) {
                trackHolder.mTrackView.setPlayingFrame(currentFrame);
            }
        }
    }

    private void closeTrackInputs() {
        for (TrackHolder trackHolder : trackHolderList) {
            IOUtils.closeSilently(trackHolder.audioStream);
        }
    }


    private void onLoadTracksData() {
        if (ArrayUtils.isNotEmpty(trackHolderList)) {
            for (TrackHolder trackHolder : trackHolderList) {
                addTrackViews(trackHolder, false);
            }
        }
    }

    private void addTrackViews(TrackHolder trackHolder, boolean insertToFirst) {
        View trackSoundItem = View.inflate(this, R.layout.item_track, null);
        trackSoundItem.setLayoutParams(new LinearLayout.LayoutParams(DisplayUtils.dpToPx(this, getResources().getInteger(R.integer.track_header)), trackHeight));

        ImageView btnSound = trackSoundItem.findViewById(R.id.sound_toggle);
        TextView tvTrackName = trackSoundItem.findViewById(R.id.track_name);
        tvTrackName.setText(trackHolder.track.getName());

        TrackView trackView = new TrackView(this);
        trackView.setStyle(WAVE_MIN_HEIGHT, WAVE_MIN_WIDTH, WAVE_SPACE_HEIGHT, WAVE_SPACE_WIDTH, PLAY_LINE_WIDTH);
        trackView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, trackHeight));

        if (insertToFirst) {
            layoutControl.addView(trackSoundItem, 0);
            layoutTracks.addView(trackView, 0);
        } else {
            layoutControl.addView(trackSoundItem);
            layoutTracks.addView(trackView);
        }
        trackHolder.setViews(btnSound, trackView, tvTrackName);
    }

    @SuppressLint("WrongConstant")
    private void startPlay() {
        if (ArrayUtils.isNotEmpty(trackHolderList)) {
            int newChannelCount = recordStatus == STATUS_RECORD_RECORDING ? trackHolderList.size() : trackHolderList.size() + 1;
            if (musicAudioTrack == null) {
                musicAudioTrack = AudioUtils.createTrack(newChannelCount);
            } else if (musicAudioTrack.getChannelCount() != newChannelCount) {
                musicAudioTrack.release();
                musicAudioTrack = AudioUtils.createTrack(newChannelCount);
            }

            playStatus = STATUS_PLAYING;
            mPlayButton.setImageResource(R.drawable.ma_pause_24dp);
            stopPlay = false;
            initTrackInputs();
            musicAudioTrack.play();
            new PlayThread().start();
        }
    }

    private void stopPlay() {
        stopPlay = true;
        playStatus = STATUS_PLAY_PREPARE;
        mPlayButton.setImageResource(R.drawable.ma_play_24dp);
        musicAudioTrack.stop();
        closeTrackInputs();
    }

    private void resetPlay() {
        totalPlayBytes = 0;
        setCurrentPlayFrame(0);
        mPlayButton.setImageResource(R.drawable.ma_play_24dp);
    }

    private void refreshBeatData() {
        boolean isChanged = localDatabase.renameAudioData(noteId, currentTune, currentBeat, currentSpeed);
        if (!isChanged)
            DisplayUtils.showToast(this, getResources().getString(R.string.database_error));
        playBeatBytes = recordPcmAudioFile.generateBeatBytes(beatStrongBytes, beetWeakBytes, currentBeat, currentSpeed);
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

    private void newEmptyThreadToSync() {
        new Thread() {
            @Override
            public void run() {
                waitRecordToSync();
            }
        }.start();
    }

    private void enableViews(View... views) {
        for (View view : views) {
            view.setEnabled(true);
            view.setAlpha(1);
        }
    }

    private void disableViews(View... views) {
        for (View view : views) {
            view.setEnabled(false);
            view.setAlpha(0.5f);
        }
    }

    class TrackHolder implements View.OnClickListener, View.OnLongClickListener {

        private static final int MENU_RENAME = 0;
        private static final int MENU_DELETE = 1;

        Track track;
        ImageView mSoundToggle;
        TrackView mTrackView;
        TextView mTrackNameTextView;
        File audioFile;
        List<Double> audioFrames = new ArrayList<>();
        boolean isSoundOn = true;
        InputStream audioStream;

        TrackHolder() {
        }

        TrackHolder(File audioFile) {
            this.audioFile = audioFile;
        }

        private void setViews(ImageView btnSound, TrackView trackView, TextView tvTrackName) {
            this.mSoundToggle = btnSound;
            this.mTrackView = trackView;
            this.mTrackNameTextView = tvTrackName;
            btnSound.setOnClickListener(this);
            this.mSoundToggle.setOnLongClickListener(this);
            this.mTrackView.setOnLongClickListener(this);
            this.mTrackNameTextView.setOnLongClickListener(this);
            this.mTrackView.setWaveData(audioFrames);
        }

        @Override
        public void onClick(View v) {
            if (v == mTrackView) {
                onTrackClick();
            } else if (v == mSoundToggle) {
                onSoundClick();
            }
        }

        @Override
        public boolean onLongClick(View v) {

            if (playStatus == STATUS_PLAYING
                    || recordStatus == STATUS_RECORD_RECORDING) {
                return true;
            }

            new MaterialDialog.Builder(mActivity)
                    .items(R.array.track_menu_array)
                    .itemsCallback((dialog, view, which, text) -> {
                        if (which == MENU_RENAME) {
                            onTrackRenameClick();
                        } else if (which == MENU_DELETE) {
                            onTrackDeleteClick();
                        }
                    }).build().show();
            return true;
        }

        private synchronized void onSoundClick() {
            this.isSoundOn = !isSoundOn;
            if (isSoundOn) {
                mSoundToggle.setImageDrawable(mActivity.getDrawable(R.drawable.ic_volume_up));
            } else {
                mSoundToggle.setImageDrawable(mActivity.getDrawable(R.drawable.ic_volume_off));
            }
        }

        private void onTrackClick() {
            recordingTrackHolder = TrackHolder.this;
        }

        private void onTrackDeleteClick() {
            new MaterialDialog.Builder(mActivity)
                    .title(R.string.remove)
                    .content(R.string.confirm_delete_track)
                    .positiveText(R.string.yes)
                    .negativeText(R.string.no)
                    .onPositive((dialog, which) -> {
                        deleteTrack(track);
                        dialog.dismiss();
                    }).build().show();
        }

        private void onTrackRenameClick() {
            new MaterialDialog.Builder(mActivity)
                    .title(R.string.track_name)
                    .positiveText(R.string.add)
                    .negativeText(R.string.cancel)
                    .inputType(InputType.TYPE_CLASS_TEXT)
                    .input(null, track.getName(), (dialog, input) -> {
                        String trackName = input.toString().trim();
                        track.setName(trackName);
                        updateTrack(track);
                    }).build().show();
        }
    }

    private class PlayThread extends Thread {

        private boolean firstWrite = true;

        @Override
        public void run() {

            final int frameBytes = bytesPerFrame;
            int inputSize = trackHolderList.size();

            if (recordStatus != STATUS_RECORD_RECORDING) {
                inputSize = inputSize + 1;
            }

            boolean[] streamDoneArray = new boolean[inputSize - 1];
            while (!stopPlay && (streamDoneArray.length == 0
                    || !isArrayAllTrue(streamDoneArray))) {

                byte[][] allAudioBytes = new byte[inputSize][];

                InputStream audioStream;
                byte[] readBuffer = new byte[frameBytes];
                TrackHolder trackHolder;
                int streamIndex = 0;
                try {
                    for (int i = 0; i < trackHolderList.size(); ++i) {
                        trackHolder = trackHolderList.get(i);
                        if (recordStatus == STATUS_RECORD_RECORDING
                                && trackHolder == recordingTrackHolder) {
                            continue;
                        }
                        audioStream = trackHolder.audioStream;
                        if (audioStream != null && !streamDoneArray[streamIndex]
                                && (audioStream.read(readBuffer)) != -1) {
                            if (trackHolder.isSoundOn) {
                                allAudioBytes[streamIndex] = Arrays.copyOf(readBuffer, readBuffer.length);
                                System.out.println();
                            } else {
                                allAudioBytes[streamIndex] = new byte[frameBytes];
                            }
                        } else {
                            streamDoneArray[streamIndex] = true;
                            allAudioBytes[streamIndex] = new byte[frameBytes];
                        }

                        streamIndex++;
                    }

                } catch (IOException ex) {
                    ex.printStackTrace();
                }

                if (!stopBeatPlay) {
                    final int beginBeatByteLen = (int) (totalPlayBytes % playBeatBytes.length);
                    final int leftBeatBytesLen = playBeatBytes.length - beginBeatByteLen;
                    System.arraycopy(playBeatBytes, beginBeatByteLen, readBuffer, 0, Math.min(leftBeatBytesLen, readBuffer.length));
                    if (readBuffer.length > leftBeatBytesLen) {
                        System.arraycopy(playBeatBytes, 0, readBuffer, leftBeatBytesLen, readBuffer.length - leftBeatBytesLen);
                    }
                    allAudioBytes[inputSize - 1] = readBuffer;
                } else {
                    Arrays.fill(readBuffer, (byte) 0);
                }

                allAudioBytes[inputSize - 1] = readBuffer;

                byte[] resultBytes = audioMixer.mixRawAudioBytes(allAudioBytes);
                if (firstWrite) {
                    firstWrite = false;
                    waitRecordToSync();
                }

                if (resultBytes == null) {
                    resultBytes = readBuffer;
                }

                musicAudioTrack.write(resultBytes, 0, resultBytes.length);

                totalPlayBytes += frameBytes;

                if (recordStatus != STATUS_RECORD_RECORDING) {
                    mAudioHandler.sendEmptyMessage(R.integer.PLAY_AUDIO_FRAME);
                }
            }

            if (isArrayAllTrue(streamDoneArray)) {
                mAudioHandler.sendEmptyMessage(R.integer.PLAY_DONE);
            }
        }

        private boolean isArrayAllTrue(boolean[] resultArray) {
            boolean done = true;
            for (boolean streamEnd : resultArray) {
                if (!streamEnd) {
                    done = false;
                    break;
                }
            }
            return done;
        }
    }

    @SuppressLint("NonConstantResourceId")
    private final Handler mAudioHandler = new Handler(msg -> {
        if (loadingDialog != null) loadingDialog.dismiss();
        switch (msg.what) {
            case R.integer.RECEIVE_RECORDING_DATA:
                onReceiveRecordingData();
                break;
            case R.integer.LOAD_DATA_SUCCESS:
                onLoadTracksData();
                updateButtons();
                break;
            case R.integer.PLAY_AUDIO_FRAME:
                onPlayFrame();
                break;
            case R.integer.PLAY_DONE:
                onPlayDone();
                break;
            case R.integer.REPLAY_ON_SCROLL:
                onTrackScrollUp(msg.arg1);
                break;
            case R.integer.EXPORT_AUDIO_SUCCESS:
                onRenderAudioSuccess(msg.obj.toString());
                break;
            case R.integer.EXPORT_AUDIO_FAIL:
                onRenderAudioFail();
                break;
        }
        return true;
    });

    private class RenderThread extends Thread {

        @Override
        public void run() {
            isRendering = true;

            if (ArrayUtils.isEmpty(trackHolderList)) {
                return;
            }

            File[] audioFiles = new File[trackHolderList.size()];

            for (int i = 0, size = audioFiles.length; i != size; i++) {
                audioFiles[i] = trackHolderList.get(i).audioFile;
            }

            try {
                File tempDir = new File(getCacheDir() + FileUtils.TEMP_DIR);
                if (!tempDir.exists()) tempDir.mkdirs();
                File tempMixAudioFile = new File(tempDir, UUID.randomUUID().toString());
                final FileOutputStream mixTempOutStream = new FileOutputStream(tempMixAudioFile);
                audioMixer.setOnAudioMixListener(new MultiAudioMixer.OnAudioMixListener() {

                    @Override
                    public void onMixing(byte[] mixBytes) throws IOException {
                        mixTempOutStream.write(mixBytes);
                    }

                    @Override
                    public void onMixError(int errorCode) {

                    }

                    @Override
                    public void onMixComplete() {

                    }
                });
                audioMixer.mixAudios(audioFiles, recordPcmAudioFile.bytesPerSample());
                mixTempOutStream.close();

                File outputDir = getAudioDir();
                File outputFile = new File(outputDir, audioName + FileUtils.AUDIO_FORMAT);
                int channelCount = trackHolderList.size();
                AudioEncoder accEncoder = AudioEncoder.createAccEncoder(tempMixAudioFile, channelCount);
                accEncoder.encodeToFile(outputFile);

                tempMixAudioFile.delete();

                Message successMsg = Message.obtain();
                successMsg.what = R.integer.EXPORT_AUDIO_SUCCESS;
                successMsg.obj = outputFile.getAbsolutePath();
                mAudioHandler.sendMessage(successMsg);
            } catch (IOException ex) {
                ex.printStackTrace();
                mAudioHandler.sendEmptyMessage(R.integer.EXPORT_AUDIO_FAIL);
            }

            isRendering = false;
        }
    }

}
