package com.theost.wavenote;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.media.AudioTrack;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MenuCompat;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.internal.MDButton;
import com.google.android.material.textfield.TextInputLayout;
import com.theost.wavenote.models.Note;
import com.theost.wavenote.utils.AudioUtils;
import com.theost.wavenote.utils.DatabaseHelper;
import com.theost.wavenote.utils.ExportUtils;
import com.theost.wavenote.utils.ImportUtils;
import com.theost.wavenote.utils.PrefUtils;
import com.theost.wavenote.utils.ResUtils;
import com.theost.wavenote.utils.PermissionUtils;
import com.theost.wavenote.widgets.PCMAnalyser;
import com.theost.wavenote.configs.AudioConfig;
import com.theost.wavenote.configs.BundleKeys;
import com.theost.wavenote.utils.DisplayUtils;
import com.theost.wavenote.utils.FileUtils;
import com.theost.wavenote.utils.StrUtils;
import com.theost.wavenote.utils.ThemeUtils;
import com.theost.wavenote.utils.ViewUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class MetronomeActivity extends ThemedAppCompatActivity {

    private static final String BEAT_STRONG = FileUtils.BEAT_STRONG;
    private static final String BEAT_WEAK = FileUtils.BEAT_WEAK;

    public static final String BEAT_SETTING_ARG = "isBeatSetting";

    private static final int STATUS_PLAY_PREPARE = 1;
    private static final int STATUS_PLAYING = 2;
    private static final int FILE_REQUEST = 0;

    public static final int DEFAULT_SOUND = 1;

    private static final int MODE_EXPORT = 1;
    private static final int MODE_REMOVE = -1;

    private static final int MAX_SOUND_NAME = 10;

    private static final int MS_IN_MINUTE = 60000;
    private static final int TAP_COUNT = 2;

    private static final double TAP_RATIO = 1.2;

    public static final short DEFAULT_SPEED = 120;
    public static final short MIN_SPEED = 20;
    public static final short MAX_SPEED = 300;

    private int tapStart = 0;

    private static final String DEFAULT_BEAT = "4/4";
    private static final String DEFAULT_TONE = "C";

    private static String soundName = "";
    private static String soundType;

    private String exportPassword;

    private AudioTrack audioTrack;
    private byte[] beatStrongBytes;
    private byte[] beetWeakBytes;
    private byte[] playBeatBytes;

    private int playStatus;
    private boolean stopPlay;
    private boolean isBeatSetting;
    private short currentSpeed;
    private String currentBeat;
    private String currentTone;
    private String exportPath;
    private String resultDialogMessage;
    private List<String> toneList = new ArrayList<>();
    private List<Double> tapList = new ArrayList<>();

    HashMap<String, String> soundList;
    List<String> resourceSounds;
    HashMap<String, Integer> customSounds;
    HashMap<String, Boolean> resultMap;
    String[] soundData;

    private boolean isImporting;
    private boolean isExporting;

    private DatabaseHelper localDatabase;
    private File tmpFile;

    private Intent beatResult = new Intent();
    private PCMAnalyser pcmAudioFile;
    private BeatPlayHandler beatPlayHandler;

    private MaterialDialog loadingDialog;
    private EditText mAddSoundEditText;
    private MenuItem mExportItem;
    private MenuItem mRemoveItem;

    int[] dialogColors;

    TextView mBeatTextView;
    AutoCompleteTextView mSoundTextView;
    ImageButton mSpeedDownButton;
    TextView mSpeedTextView;
    ImageButton mSpeedUpButton;
    SeekBar mSpeedBar;
    Button mPlayButton;
    Button mTapButton;
    TextView mTuneTextView;
    LinearLayout mActionsLayout;
    LinearLayout mPlayLayout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeUtils.setTheme(this);
        setContentView(R.layout.activity_metronome);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        setTitle(R.string.metronome);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mBeatTextView = findViewById(R.id.metronome_beat);
        mSoundTextView = findViewById(R.id.metronome_sound);
        mSpeedDownButton = findViewById(R.id.metronome_speed_down);
        mSpeedTextView = findViewById(R.id.beat_speed);
        mSpeedUpButton = findViewById(R.id.metronome_speed_up);
        mSpeedBar = findViewById(R.id.metronome_speed_bar);
        mPlayButton = findViewById(R.id.metronome_play);
        mTapButton = findViewById(R.id.metronome_tap);
        mTuneTextView = findViewById(R.id.metronome_tune);
        mActionsLayout = findViewById(R.id.actions);
        mPlayLayout = findViewById(R.id.play_action);

        findViewById(R.id.metronome_speed_down).setOnClickListener(this::onSpeedDownClick);
        findViewById(R.id.metronome_speed_up).setOnClickListener(this::onSpeedUpClick);
        findViewById(R.id.metronome_tune).setOnClickListener(this::onTuneClick);
        findViewById(R.id.metronome_beat).setOnClickListener(this::onBeatClick);
        findViewById(R.id.metronome_tap).setOnClickListener(this::onTapClick);
        findViewById(R.id.metronome_play).setOnClickListener(this::onPlayClick);

        updateOrientation();

        isBeatSetting = getIntent().getBooleanExtra(BEAT_SETTING_ARG, false);
        currentSpeed = getIntent().getShortExtra(BundleKeys.RESULT_SPEED, Note.getActiveMetronomeSpeed());
        currentBeat = getIntent().getStringExtra(BundleKeys.RESULT_BEAT);
        currentTone = getIntent().getStringExtra(BundleKeys.RESULT_TUNE);
        currentBeat = StrUtils.isEmpty(currentBeat) ? DEFAULT_BEAT : currentBeat;
        currentTone = StrUtils.isEmpty(currentTone) ? DEFAULT_TONE : currentTone;

        if (currentSpeed == 0) currentSpeed = DEFAULT_SPEED;

        mSpeedTextView.setText(String.valueOf(currentSpeed));
        mBeatTextView.setText(currentBeat);
        mTuneTextView.setText(currentTone);
        if (!isBeatSetting) mTuneTextView.setVisibility(View.GONE);

        localDatabase = new DatabaseHelper(this);
        resourceSounds = Arrays.asList(getResources().getStringArray(R.array.metronome_sounds));

        updateSoundData();

        ViewUtils.removeFocus(mSoundTextView);

        if (Note.getActiveMetronomeSound() == null)
            Note.setActiveMetronomeSound(soundData[DEFAULT_SOUND]);
        mSoundTextView.setText(Note.getActiveMetronomeSound());

        checkSoundView();

        ViewUtils.disbaleInput(mSoundTextView);
        mSoundTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                Note.setActiveMetronomeSound(s.toString());
                loadBeatData(s.toString());
            }
        });

        ViewUtils.restoreFocus(mSoundTextView);
        mSoundTextView.requestFocus();
        mSoundTextView.dismissDropDown();

        initToneList();

        audioTrack = AudioUtils.createTrack(AudioConfig.BEAT_CHANNEL_COUNT);
        pcmAudioFile = PCMAnalyser.createPCMAnalyser(AudioConfig.BEAT_CHANNEL_COUNT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            mSpeedBar.setMin(MIN_SPEED);
        }

        mSpeedBar.setMax(MAX_SPEED);
        mSpeedBar.setProgress(currentSpeed);
        mSpeedBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    if (progress < MIN_SPEED) progress = MIN_SPEED;
                    currentSpeed = Integer.valueOf(progress).shortValue();
                    refreshSpeed();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        updateColors();

        HandlerThread playBeatHandlerThread = new HandlerThread("PlayBeatHandlerThread");
        playBeatHandlerThread.start();
        beatPlayHandler = new BeatPlayHandler(playBeatHandlerThread.getLooper());
        loadBeatData(mSoundTextView.getText().toString());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.metronome_bar, menu);
        mExportItem = menu.findItem(R.id.menu_export);
        mRemoveItem = menu.findItem(R.id.menu_remove);
        MenuCompat.setGroupDividerEnabled(menu, true);
        refreshMenuItems();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_import:
                if (PermissionUtils.requestFilePermissions(this))
                    showImportDialog();
                return true;
            case R.id.menu_remove:
                if (PermissionUtils.requestFilePermissions(this))
                    createDialog(MODE_REMOVE);
                return true;
            case R.id.menu_export:
                if (PermissionUtils.requestFilePermissions(this))
                    createDialog(MODE_EXPORT);
                return true;
            case android.R.id.home:
                invalidateOptionsMenu();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateOrientation();
    }

    private void updateOrientation() {
        int padding = DisplayUtils.dpToPx(this, getResources().getInteger(R.integer.custom_space));
        if (DisplayUtils.isLandscape(this)) {
            mPlayLayout.setPadding(padding, 0, 0, 0);
            mActionsLayout.setOrientation(LinearLayout.HORIZONTAL);
        } else {
            mPlayLayout.setPadding(0, padding, 0, 0);
            mActionsLayout.setOrientation(LinearLayout.VERTICAL);
        }
    }

    @Override
    public void onBackPressed() {
        if (isBeatSetting) {
            beatResult.putExtra(BundleKeys.RESULT_SPEED, currentSpeed);
            beatResult.putExtra(BundleKeys.RESULT_BEAT, currentBeat);
            beatResult.putExtra(BundleKeys.RESULT_TUNE, currentTone);
            setResult(RESULT_OK, beatResult);
            finish();
        } else {
            super.onBackPressed();
        }
    }


    void onPlayClick(View v) {
        if (playStatus == STATUS_PLAY_PREPARE) {
            mPlayButton.setText(R.string.stop);
            stopPlay = false;
            audioTrack.play();
            beatPlayHandler.removeMessages(R.integer.PLAY_BEAT);
            beatPlayHandler.sendEmptyMessage(R.integer.PLAY_BEAT);
            playStatus = STATUS_PLAYING;
        } else if (playStatus == STATUS_PLAYING) {
            mPlayButton.setText(R.string.play);
            stopPlay = true;
            beatPlayHandler.removeMessages(R.integer.PLAY_BEAT);
            audioTrack.stop();
            playStatus = STATUS_PLAY_PREPARE;
        }
    }


    void onTapClick(View v) {
        int tapEnd = (int) System.currentTimeMillis();
        double tapSpeed = MS_IN_MINUTE / (double) (tapEnd - tapStart);
        if (tapStart == 0 || tapSpeed < MIN_SPEED / 2) {
            tapStart = (int) System.currentTimeMillis();
        } else {
            if (tapList.size() < TAP_COUNT || tapSpeed * TAP_RATIO > currentSpeed) {
                tapList.add(tapSpeed);
                tapStart = tapEnd;
                if (tapList.size() >= TAP_COUNT) refreshTap();
            } else {
                tapList = new ArrayList<>();
                tapStart = 0;
            }
        }
    }


    void onBeatClick(View v) {
        new MaterialDialog.Builder(this)
                .title(R.string.select_beat)
                .items(R.array.beats_array)
                .itemsCallback((dialog, view, which, text) -> refreshBeat(text.toString())).show();
    }


    void onTuneClick(View v) {
        new MaterialDialog.Builder(this)
                .title(R.string.select_tone)
                .items(toneList)
                .itemsCallback((dialog, view, which, text) -> {
                    dialog.dismiss();
                    refreshTune(text.toString());
                }).show();
    }


    void onSpeedUpClick(View v) {
        if (currentSpeed != MAX_SPEED) {
            currentSpeed++;
            refreshSpeed();
        }
    }


    void onSpeedDownClick(View v) {
        if (currentSpeed != MIN_SPEED) {
            currentSpeed--;
            refreshSpeed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopPlay = true;
        audioTrack.release();
        audioTrack = null;
        beatPlayHandler.removeCallbacksAndMessages(null);
        beatPlayHandler = null;
    }

    private synchronized void generatePlayBeatBytes() {
        this.playBeatBytes = pcmAudioFile.generateBeatBytes(beatStrongBytes, beetWeakBytes, currentBeat, currentSpeed);
        audioTrack.flush();
    }

    private void updateColors() {
        dialogColors = ResUtils.getDialogColors(this);
    }

    private void initToneList() {
        for (char c = 'A'; c != 'H'; c++) {
            toneList.add(String.format("%sb", c));
            toneList.add(String.valueOf(c));
            toneList.add(String.format("%s#", c));
        }
    }

    private void refreshMenuItems() {
        if (customSounds.size() == 0) {
            mExportItem.setEnabled(false);
            mRemoveItem.setEnabled(false);
        } else {
            mExportItem.setEnabled(true);
            mRemoveItem.setEnabled(true);
        }
    }

    private void refreshTap() {
        int tapSum = 0;
        for (Double i : tapList) tapSum += i;
        int tapSpeed = tapSum / tapList.size();
        if (tapSpeed < MIN_SPEED) {
            currentSpeed = MIN_SPEED;
        } else if (tapSpeed > MAX_SPEED) {
            currentSpeed = MAX_SPEED;
        } else {
            currentSpeed = (short) (tapSpeed);
        }
        refreshSpeed();
    }

    private void refreshSpeed() {
        Note.setActiveMetronomeSpeed(currentSpeed);
        mSpeedTextView.setText(String.valueOf(currentSpeed));
        mSpeedBar.setProgress(currentSpeed);
        beatPlayHandler.removeMessages(R.integer.REFRESH_BEAT_DATA);
        beatPlayHandler.sendEmptyMessage(R.integer.REFRESH_BEAT_DATA);
    }

    private void refreshBeat(String beatText) {
        currentBeat = beatText;
        mBeatTextView.setText(beatText);
        beatPlayHandler.removeMessages(R.integer.REFRESH_BEAT_DATA);
        beatPlayHandler.sendEmptyMessage(R.integer.REFRESH_BEAT_DATA);
    }

    private void refreshTune(String tune) {
        currentTone = tune;
        mTuneTextView.setText(tune);
    }

    private void loadBeatData(String sound) {
        new Thread() {
            @Override
            public void run() {
                try {
                    byte[][] beatsData;
                    if (resourceSounds.contains(sound)) {
                        beatsData = FileUtils.getStereoBeatResource(getApplicationContext(), sound);
                    } else {
                        beatsData = FileUtils.getStereoBeatCustom(getApplicationContext(), sound);
                    }
                    beatStrongBytes = beatsData[0];
                    beetWeakBytes = beatsData[1];
                    generatePlayBeatBytes();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                playStatus = STATUS_PLAY_PREPARE;
            }
        }.start();
    }

    private class BeatPlayHandler extends Handler {

        BeatPlayHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == R.integer.REFRESH_BEAT_DATA) {
                generatePlayBeatBytes();
            } else {
                if (stopPlay) {
                    return;
                }
                audioTrack.write(playBeatBytes, 0, playBeatBytes.length);
                super.sendEmptyMessage(R.integer.PLAY_BEAT);
            }

        }
    }

    private void updateSoundData() {
        List<String> updatedData = new ArrayList<>();
        customSounds = new HashMap<>();
        Cursor mMetronomeData = localDatabase.getMetronomeData();

        if (mMetronomeData != null) {
            while (mMetronomeData.moveToNext()) {
                String id = mMetronomeData.getString(0);
                String name = mMetronomeData.getString(1);
                if (checkFilesAvailable(name)) {
                    customSounds.put(name, Integer.parseInt(id));
                } else {
                    DisplayUtils.showToast(this, getResources().getString(R.string.file_error));
                    boolean isRemoved = localDatabase.removeMetronomeData(id);
                    if (!isRemoved) {
                        DisplayUtils.showToast(this, getResources().getString(R.string.database_error));
                    }
                }
            }
        }

        if (mRemoveItem != null && mExportItem != null) refreshMenuItems();

        updatedData.addAll(resourceSounds);
        updatedData.addAll(customSounds.keySet());
        soundData = updatedData.toArray(new String[0]);
        ViewUtils.updateDropdown(this, mSoundTextView, soundData);
        checkSoundView();
    }

    private void checkSoundView() {
        if (!Arrays.asList(soundData).contains(mSoundTextView.getText().toString()))
            mSoundTextView.setText(soundData[DEFAULT_SOUND]);
    }

    private void insertSound() {
        boolean isInserted = localDatabase.insertMetronomeData(soundName);
        if (isInserted) {
            DisplayUtils.showToast(this, getResources().getString(R.string.imported) + ": " + soundName);
            soundName = "";
            updateSoundData();
        } else {
            DisplayUtils.showToast(this, getResources().getString(R.string.database_error));
            clearImport();
        }
    }

    private void exportSounds() {
        exportPath = PrefUtils.getStringPref(this, PrefUtils.PREF_EXPORT_DIR);
        new ExportSampleThread(this, soundList.keySet()).start();
        showLoadingDialog();
    }

    private void removeSounds() {
        List<Boolean> isRemoved = new ArrayList<>();
        for (String i : soundList.keySet()) {
            isRemoved.add(localDatabase.removeMetronomeData(soundList.get(i)));
            removeLocalFiles(i);
        }
        if (isRemoved.contains(false))
            DisplayUtils.showToast(this, getResources().getString(R.string.database_error));
        updateSoundData();
    }

    private void importSound(InputStream wavInput) throws IOException {
        tmpFile = FileUtils.createTempFile(this, wavInput, FileUtils.SAMPLE_FORMAT);
        new ImportSamplesThread(this, tmpFile).start();
        showLoadingDialog();
    }

    private void clearImport() {
        removeLocalFiles(soundName);
    }

    private boolean checkFilesAvailable(String name) {
        File[] sampleFiles = FileUtils.getAllSampleFiles(this, name);
        for (File i : sampleFiles)
            if (!i.exists()) {
                removeLocalFiles(name);
                return false;
            }
        return true;
    }

    private void removeLocalFiles(String name) {
        File[] sampleFiles = FileUtils.getAllSampleFiles(this, name);
        for (File i : sampleFiles) if (i.exists()) i.delete();
    }

    private void creationResult(boolean isCreated) {
        if (tmpFile.exists()) tmpFile.delete();
        if (isCreated) {
            if (soundType.equals(BEAT_STRONG)) {
                soundType = BEAT_WEAK;
                if (loadingDialog != null) loadingDialog.dismiss();
                showBeatDialog(); // start after strong for weak beat
            } else if (soundType.equals(BEAT_WEAK)) {
                insertSound(); // load sound in database
                if (loadingDialog != null) loadingDialog.dismiss();
            }
        } else {
            clearImport();
        }
    }

    private Handler mExportHandler = new Handler(msg -> {
        if (loadingDialog != null) loadingDialog.dismiss();
        showResultDialog();
        return true;
    });

    private class ExportSampleThread extends Thread {

        MetronomeActivity context;
        Set<String> soundList;

        private ExportSampleThread(MetronomeActivity context, Set<String> soundList) {
            this.context = context;
            this.soundList = soundList;
        }

        @Override
        public void run() {
            isExporting = true;
            File exportDir = new File(exportPath + FileUtils.METRONOME_DIR);
            if (!exportDir.exists()) exportDir.mkdirs();
            resultMap = ExportUtils.exportSounds(context, exportDir, soundList, exportPassword);
            resultDialogMessage = ExportUtils.getResultMessage(context, resultMap);
            mExportHandler.sendEmptyMessage(ImportUtils.RESULT_OK);
            isExporting = false;
        }
    }

    private Handler mImportHandler = new Handler(msg -> {
        boolean isCreated = false;
        if (msg.what == ImportUtils.RESULT_OK) {
            isCreated = true;
        } else if (msg.what == ImportUtils.SAMPLE_ERROR) {
            DisplayUtils.showToast(this, (getResources()
                    .getString(R.string.sample_error) + " " + AudioConfig.AUDIO_SAMPLE_RATE));
            return false;
        } else if (msg.what == ImportUtils.FILE_ERROR) {
            DisplayUtils.showToast(this, getResources().getString(R.string.file_error));
        } else if (msg.what == ImportUtils.EXIST_ERROR) {
            DisplayUtils.showToast(this, getResources().getString(R.string.exist_error));
        }
        creationResult(isCreated);
        return true;
    });

    private class ImportSamplesThread extends Thread {

        Context context;
        File audioFile;

        private ImportSamplesThread(Context context, File audioFile) {
            this.context = context;
            this.audioFile = audioFile;
        }

        public void run() {
            isImporting = true;

            byte[] stereoBytes;
            byte[] monoBytes;

            byte[] audioBytes = AudioUtils.getWavBytes(audioFile);
            long[] audioParams = AudioUtils.getAudioParams(audioFile.getAbsolutePath()); // channelCount, sampleRate, bitRate, audioSize

            if (audioBytes == null) {
                mImportHandler.sendEmptyMessage(ImportUtils.FILE_ERROR);
                return;
            }

            if (audioParams[1] != AudioConfig.AUDIO_SAMPLE_RATE)
                mImportHandler.sendEmptyMessage(ImportUtils.SAMPLE_ERROR);


            try {
                switch ((int) audioParams[0]) {
                    case 1:
                        monoBytes = audioBytes;
                        stereoBytes = AudioUtils.convertToStereo(audioBytes);
                        break;
                    case 2:
                        monoBytes = AudioUtils.convertToMono(audioBytes);
                        stereoBytes = audioBytes;
                        break;
                    default:
                        mImportHandler.sendEmptyMessage(ImportUtils.FILE_ERROR);
                        return;
                }

                File[] sampleFiles = FileUtils.getSampleFiles(context, soundName, soundType); // mono, stereo
                if (sampleFiles[0] == null || sampleFiles[1] == null) {
                    mImportHandler.sendEmptyMessage(ImportUtils.EXIST_ERROR);
                    return;
                }

                byte[] monoHeader = AudioUtils.createWaveFileHeader(audioParams[3], 1, audioParams[1], (int) audioParams[2]);
                byte[] stereoHeader = AudioUtils.createWaveFileHeader(audioParams[3] * 2, 2, audioParams[1], (int) audioParams[2]);

                byte[] monoStream = new byte[monoHeader.length + monoBytes.length];
                System.arraycopy(monoHeader, 0, monoStream, 0, monoHeader.length);
                System.arraycopy(monoBytes, 0, monoStream, monoHeader.length, monoBytes.length);

                byte[] stereoStream = new byte[stereoHeader.length + stereoBytes.length];
                System.arraycopy(stereoHeader, 0, stereoStream, 0, stereoHeader.length);
                System.arraycopy(stereoBytes, 0, stereoStream, stereoHeader.length, stereoBytes.length);

                sampleFiles[0].createNewFile();
                sampleFiles[1].createNewFile();
                FileUtils.createWavFile(sampleFiles[0], monoStream);
                FileUtils.createWavFile(sampleFiles[1], stereoStream);
                mImportHandler.sendEmptyMessage(ImportUtils.RESULT_OK);
            } catch (IOException e) {
                e.printStackTrace();
                mImportHandler.sendEmptyMessage(ImportUtils.FILE_ERROR);
            }
            isImporting = false;
        }

    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == FILE_REQUEST) {
                try {
                    InputStream wavInput = getContentResolver().openInputStream(data.getData());
                    importSound(wavInput);
                } catch (IOException e) {
                    e.printStackTrace();
                    DisplayUtils.showToast(this, getResources().getString(R.string.file_error));
                    if (soundType.equals(BEAT_WEAK)) clearImport();
                }
            }
        } else {
            if (soundType.equals(BEAT_WEAK)) clearImport();
        }
    }

    private void pickFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/x-wav");
        startActivityForResult(intent, FILE_REQUEST);
    }

    private void showBeatDialog() {
        String beat = "";
        if (soundType.equals(BEAT_STRONG)) {
            beat = getResources().getString(R.string.strong).toLowerCase();
        } else if (soundType.equals(BEAT_WEAK)) {
            beat = getResources().getString(R.string.weak).toLowerCase();
        }
        String message = String.format(getResources().getString(R.string.choose_beat), beat);
        new MaterialDialog.Builder(this)
                .title(R.string.import_text)
                .content(message)
                .positiveText(R.string.choose)
                .onPositive((dialog, which) -> pickFile())
                .negativeText(R.string.cancel)
                .cancelListener(dialog -> {
                    if (soundType.equals(BEAT_WEAK)) clearImport();
                })
                .show();
    }

    private void showResultDialog() {
        new MaterialDialog.Builder(this)
                .title(R.string.export)
                .content(resultDialogMessage)
                .positiveText(android.R.string.ok)
                .show();
        DisplayUtils.showToast(this, getResources().getString(R.string.path) + ": " + exportPath);
    }

    private void createDialog(int mode) {
        if (isImporting || isExporting) {
            DisplayUtils.showToast(this, getResources()
                    .getString(R.string.wait_a_bit));
        } else {
            soundList = new HashMap<>();
            exportPassword = null;
            List<String> listItems = new ArrayList<>(customSounds.keySet());
            if (mode == MODE_EXPORT)
                listItems.add(getResources().getString(R.string.zip));
            new MaterialDialog.Builder(this)
                    .title(R.string.choose_samples)
                    .positiveText(R.string.choose)
                    .negativeText(R.string.cancel)
                    .items(listItems)
                    .itemsCallbackMultiChoice(null, (dialog, which, text) -> {
                        boolean createZip = false;
                        for (CharSequence i : text) {
                            String item = i.toString();
                            Object key = customSounds.get(item);
                            if (key == null) {
                                String zipMode = getResources().getString(R.string.zip);
                                if (i.equals(zipMode)) {
                                    createZip = true;
                                }
                                continue;
                            }
                            soundList.put(item, key.toString());
                        }
                        if (mode == MODE_EXPORT) {
                            if (createZip) {
                                showPasswordDialog();
                            } else {
                                exportSounds();
                            }
                        } else if (mode == MODE_REMOVE) {
                            removeSounds();
                        }
                        return true;
                    }).show();
        }
    }

    private void showPasswordDialog() {
        exportPassword = "";
        new MaterialDialog.Builder(this)
                .title(R.string.export)
                .positiveText(R.string.export)
                .negativeText(R.string.cancel)
                .inputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)
                .input(R.string.hint_password, 0, (dialog, input) -> {
                    exportPassword = input.toString().trim();
                    exportSounds();
                }).show();
    }

    private void showLoadingDialog() {
        MetronomeActivity context = this;
        new CountDownTimer(200, 200) {
            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                if (isImporting || isExporting)
                    loadingDialog = DisplayUtils.showLoadingDialog(context, R.string.import_text, R.string.importing);
            }
        }.start();
    }

    private void showImportDialog() {
        soundType = BEAT_STRONG;
        MaterialDialog soundDialog = new MaterialDialog.Builder(this)
                .customView(R.layout.add_dialog, false)
                .title(R.string.add_sound)
                .positiveText(R.string.import_text)
                .positiveColor(dialogColors[0])
                .onPositive((dialog, which) -> {
                    String name = mAddSoundEditText.getText().toString().trim();
                    if (!name.equals("")) {
                        soundName = StrUtils.formatFilename(name);
                        showBeatDialog();
                    }
                })
                .negativeText(R.string.cancel)
                .build();
        MDButton addButton = soundDialog.getActionButton(DialogAction.POSITIVE);
        if (!soundName.equals("")) addButton.setTextColor(dialogColors[1]);
        TextInputLayout mAddSoundLayout = soundDialog.getCustomView().findViewById(R.id.dialog_layout);
        mAddSoundLayout.setCounterMaxLength(MAX_SOUND_NAME);
        mAddSoundEditText = soundDialog.getCustomView().findViewById(R.id.dialog_input);
        mAddSoundEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(MAX_SOUND_NAME)});
        mAddSoundEditText.setText(soundName);
        mAddSoundEditText.setHint(R.string.name);
        mAddSoundEditText.requestFocus();
        mAddSoundEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable arg0) {
                if (mAddSoundEditText.getText().length() == 0) {
                    addButton.setTextColor(dialogColors[0]);
                } else {
                    addButton.setTextColor(dialogColors[1]);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
        soundDialog.show();
    }

}
