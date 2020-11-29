package com.theost.wavenote;

import android.animation.LayoutTransition;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AutoCompleteTextView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.theost.wavenote.adapters.ChordAdapter;
import com.theost.wavenote.adapters.ChordButtonAdapter;
import com.theost.wavenote.models.Note;
import com.theost.wavenote.utils.AniUtils;
import com.theost.wavenote.utils.ChordUtils;
import com.theost.wavenote.utils.DisplayUtils;
import com.theost.wavenote.utils.DrawableUtils;
import com.theost.wavenote.utils.HighlightUtils;
import com.theost.wavenote.utils.ImportUtils;
import com.theost.wavenote.utils.ResUtils;
import com.theost.wavenote.utils.ThemeUtils;
import com.theost.wavenote.utils.ViewUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ChordsActivity extends ThemedAppCompatActivity {

    public static final String ARG_INSTRUMENT = "instrument";
    public static final String ARG_ALL_CHORDS = "all_chords";
    public static final String ARG_TRANSPOSED = "transposed";
    public static final String ARG_CHORDS = "chords";
    public static final String ARG_WORDS = "words";

    private final int DEFAULT_COLUMN = 4;

    public static final String SHARP = "#";
    public static final String BEMOL = "b";

    private int activeInstrument;

    private Map<Integer, String> mWordsMap;
    private Map<Integer, String> mTransposedIndexesMap;
    private HashMap<String, String> mTransposedMap;
    private List<Drawable> mChordsDrawable;
    private List<String> mNotesList;
    private List<String> mNotesHalftonesList;
    private List<String> mChordsList;
    private List<String> mNoteChordsList;
    private List<String> mDrawableChordList;
    private List<String> mWordsIndexes;
    private List<String> mChordStaticSet;
    private List<String> mChordSet;
    private List<String> mSearchSet;
    private String[] mColumnList;
    private String prevRequest;
    private String prevChord;

    private DisplayMetrics displayMetrics;

    private boolean isAllChords;
    private boolean chordGridEnabled;
    private boolean chordSearchGridEnabled;
    private boolean orientationChanged;

    private int[] mInstrumentIntArray;
    private String[] mInstrumentStringArray;
    private List<String> mInstrumentStringList;

    private final int DEFAULT_INSTRUMENT = 0;
    private final int COLUMN_COUNT = 6;

    private final int TRANSPOSE_UP = 1;
    private final int TRANSPOSE_DOWN = -1;

    private int itemsInline;
    private int itemWidth;
    private int wordsWidth;
    private int chordWidth;
    private int itemPadding;

    private AutoCompleteTextView mInstrumentInputView;
    private AutoCompleteTextView mColumnsInputView;
    private AutoCompleteTextView mSearchInputView;
    private RecyclerView mChordsButtonsRecyclerView;
    private RecyclerView mChordsRecyclerView;
    private NestedScrollView mScrollView;
    private LinearLayout mSearchLayout;
    private LinearLayout mDrawableLayout;
    private LinearLayout mChordsButtonsLayout;
    private MenuItem mSearchMenuItem;
    private MenuItem mGridMenuItem;
    private MenuItem mTransposeUpMenuItem;
    private MenuItem mTransposeDownMenuItem;
    private ChordAdapter chordAdapter;
    private ChordButtonAdapter chordButtonAdapter;

    @SuppressLint({"ResourceType", "SetTextI18n"})

    @Override
    public void onCreate(Bundle savedInstanceState) {
        ThemeUtils.setTheme(this);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_chords);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        setTitle(R.string.chords);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        LinearLayout emptyView = findViewById(android.R.id.empty);
        ImageView mEmptyViewImage = emptyView.findViewById(R.id.image);
        TextView mEmptyViewText = emptyView.findViewById(R.id.text);
        mEmptyViewImage.setImageResource(R.drawable.m_audiotrack_black_24dp);
        mEmptyViewText.setText(R.string.empty_chords);

        isAllChords = getIntent().getBooleanExtra(ARG_ALL_CHORDS, false);
        mNoteChordsList = getIntent().getStringArrayListExtra(ARG_CHORDS);
        mChordsList = new ArrayList<>();
        mChordSet = new ArrayList<>();
        mWordsMap = new TreeMap<>();
        mTransposedIndexesMap = new HashMap<>();
        mTransposedMap = new HashMap<>();

        mChordsButtonsLayout = findViewById(R.id.chords_buttons_layout);
        mChordsButtonsRecyclerView = findViewById(R.id.chord_buttons);

        if ((!isAllChords) && (mNoteChordsList.size() == 0)) {
            mChordsButtonsRecyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
            return;
        }

        prevRequest = "";

        mInstrumentIntArray = new int[]{R.string.guitar, R.string.piano, R.string.ukulele};
        mInstrumentStringArray = getResources().getStringArray(R.array.array_musical_instruments);
        mInstrumentStringList = Arrays.asList(mInstrumentStringArray);
        mColumnList = getResources().getStringArray(R.array.array_musical_columns);

        int savedInstrument = Note.getNoteActiveInstrument();
        if (savedInstrument == 0) {
            activeInstrument = mInstrumentIntArray[DEFAULT_INSTRUMENT];
        } else {
            activeInstrument = savedInstrument;
        }

        chordGridEnabled = Note.isChordGridEnabled();

        itemsInline = Note.getNoteActiveColumns();
        if (itemsInline == 0) itemsInline = DEFAULT_COLUMN;

        mNotesList = Arrays.asList(getResources().getStringArray(R.array.array_musical_notes_order));
        mNotesHalftonesList = Arrays.asList(getResources().getStringArray(R.array.array_musical_notes_halftones));

        mDrawableLayout = findViewById(R.id.chords_view_layout);

        mChordsRecyclerView = findViewById(R.id.chord_view);
        mChordsRecyclerView.setHasFixedSize(false);
        mChordsRecyclerView.setNestedScrollingEnabled(false);
        mChordsRecyclerView.setDrawingCacheEnabled(true);
        mChordsRecyclerView.setItemViewCacheSize(20);
        mChordsRecyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);

        mChordsButtonsRecyclerView.setHasFixedSize(false);
        mChordsButtonsRecyclerView.setNestedScrollingEnabled(false);

        FrameLayout chordsLayout = findViewById(R.id.chords_layout);
        chordsLayout.getLayoutTransition().setDuration(LayoutTransition.DISAPPEARING, 130);

        mInstrumentInputView = findViewById(R.id.instrument);
        mInstrumentInputView.setText(activeInstrument);
        ViewUtils.disbaleInput(mInstrumentInputView);
        ViewUtils.updateDropdown(this, mInstrumentInputView, mInstrumentStringArray);
        ViewUtils.removeFocus(mInstrumentInputView);
        mInstrumentInputView.clearFocus();

        mColumnsInputView = findViewById(R.id.columns);
        mColumnsInputView.setText("00%");
        ViewUtils.disbaleInput(mColumnsInputView);
        ViewUtils.updateDropdown(this, mColumnsInputView, mColumnList);
        ViewUtils.removeFocus(mColumnsInputView);
        mColumnsInputView.clearFocus();

        mColumnsInputView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                mColumnsInputView.clearFocus();
                int tmpItemsInline = 100 / Integer.parseInt(s.toString().replace("%", ""));
                if (tmpItemsInline != itemsInline) {
                    itemsInline = tmpItemsInline;
                    Note.setNoteActiveColumns(itemsInline);
                    updateItemSize();
                    updateChord(prevChord);
                }
            }
        });

        mInstrumentInputView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                mInstrumentInputView.clearFocus();
                int tmpInstrument = mInstrumentIntArray[mInstrumentStringList.indexOf(s.toString())];
                if (tmpInstrument != activeInstrument) {
                    activeInstrument = tmpInstrument;
                    Note.setNoteActiveInstrument(activeInstrument);
                    updateChord(prevChord);
                }
            }
        });

        ViewUtils.restoreFocus(mInstrumentInputView);
        ViewUtils.restoreFocus(mColumnsInputView);

        mScrollView = findViewById(R.id.chords_scrollview);

        mSearchLayout = findViewById(R.id.search_chords);
        mSearchInputView = findViewById(R.id.search_items);
        mSearchInputView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                mSearchInputView.dismissDropDown();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                mSearchInputView.dismissDropDown();
                String request = s.toString();
                updateSearch(request);
                prevRequest = request;
            }
        });

        mChordsDrawable = new ArrayList<>();
        chordAdapter = new ChordAdapter(this, mChordsDrawable, itemWidth);
        mChordsRecyclerView.setAdapter(chordAdapter);

        itemPadding = DisplayUtils.dpToPx(this, getResources().getInteger(R.integer.padding_large));

        int colorText = ThemeUtils.getColorFromAttribute(this, R.attr.mainBackgroundColor);
        int colorBackText = ThemeUtils.getColorFromAttribute(this, R.attr.colorAccent);
        int colorButton = ThemeUtils.getColorFromAttribute(this, R.attr.hintTextColor);
        int colorButtonBack = ThemeUtils.getColorFromAttribute(this, R.attr.buttonBackgroundColor);
        int[] colors = {colorText, colorBackText, colorButton, colorButtonBack};

        chordButtonAdapter = new ChordButtonAdapter(this, itemWidth, wordsWidth, colors);
        mChordsButtonsRecyclerView.setAdapter(chordButtonAdapter);

        ViewTreeObserver viewTreeObserver = mChordsButtonsLayout.getViewTreeObserver();
        viewTreeObserver.addOnGlobalLayoutListener(() -> {
            if (orientationChanged) {
                updateItemSize();
                orientationChanged = false;
            }
        });

        new UpdateChordsThread().start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (mChordsList.size() == 0) return false;
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.chords_list, menu);
        mSearchMenuItem = menu.findItem(R.id.search);
        mGridMenuItem = menu.findItem(R.id.grid);
        mTransposeUpMenuItem = menu.findItem(R.id.menu_transposeup);
        mTransposeDownMenuItem = menu.findItem(R.id.menu_transposedown);
        MenuCompat.setGroupDividerEnabled(menu, true);
        updateGridIcon();
        if (isAllChords) {
            menu.setGroupVisible(0, false);
            mSearchMenuItem.setVisible(true);
        }
        return true;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_transposeup:
                transposeChords(TRANSPOSE_UP);
                return true;
            case R.id.menu_transposedown:
                transposeChords(TRANSPOSE_DOWN);
                return true;
            case R.id.search:
                updateSearchData();
                return true;
            case R.id.grid:
                updateGrid(!chordGridEnabled);
                updateChordData();
                updateGridIcon();
                return true;
            case android.R.id.home:
                invalidateOptionsMenu();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        if (!(mChordSet.equals(mChordStaticSet))) {
            updateTransposedMap();
            Intent transposeResult = new Intent();
            transposeResult.putExtra(ARG_TRANSPOSED, mTransposedMap);
            setResult(RESULT_OK, transposeResult);
            finish();
        } else {
            super.onBackPressed();
        }
    }

    private void updateTransposedMap() {
        for (Integer i : mTransposedIndexesMap.keySet()) {
            mTransposedMap.put(mChordStaticSet.get(i), mTransposedIndexesMap.get(i));
        }
    }

    private void updateSearchData() {
        if (mSearchLayout.getVisibility() == View.GONE) {
            mSearchSet = new ArrayList<>(mChordSet);
            if (chordGridEnabled) {
                chordSearchGridEnabled = true;
                updateGrid(false);
                updateChordData();
            } else {
                chordSearchGridEnabled = false;
            }
            AniUtils.fadeIn(mSearchLayout);
            mSearchMenuItem.setIcon(R.drawable.av_close);
            mSearchInputView.requestFocus();
            mSearchInputView.dismissDropDown();
            mTransposeUpMenuItem.setEnabled(false);
            mTransposeDownMenuItem.setEnabled(false);
            mGridMenuItem.setEnabled(false);
            DrawableUtils.setMenuItemAlpha(mTransposeUpMenuItem, 0.3);
            DrawableUtils.setMenuItemAlpha(mTransposeDownMenuItem, 0.3);
            DrawableUtils.setMenuItemAlpha(mGridMenuItem, 0.3);
            DisplayUtils.showKeyboard(this, mSearchInputView);
        } else {
            DisplayUtils.hideKeyboard(mSearchInputView);
            updateGrid(chordSearchGridEnabled);
            chordSearchGridEnabled = false;
            if (!(mSearchSet.equals(mChordSet) && (!chordGridEnabled))) updateChordData();
            mSearchMenuItem.setIcon(R.drawable.av_search_24dp);
            mSearchLayout.setVisibility(View.GONE);
            mTransposeUpMenuItem.setEnabled(true);
            mTransposeDownMenuItem.setEnabled(true);
            mGridMenuItem.setEnabled(true);
            DrawableUtils.setMenuItemAlpha(mTransposeUpMenuItem, 1);
            DrawableUtils.setMenuItemAlpha(mTransposeDownMenuItem, 1);
            DrawableUtils.setMenuItemAlpha(mGridMenuItem, 1);
        }
    }

    @SuppressLint("SetTextI18n")
    private void updateAdapter() {
        if ((mChordsList.size() < itemsInline) && (mChordsList.size() != 0))
            itemsInline = mChordsList.size();
        mColumnsInputView.setText(100 / itemsInline + "%");
        if (!isAllChords) {
            chordButtonAdapter.updateWordsData(mWordsMap);
            if (chordGridEnabled) {
                chordButtonAdapter.updateChordData(mChordsList);
                chordButtonAdapter.showWordsData();
            } else {
                chordButtonAdapter.updateChordData(mChordSet);
            }
        } else {
            chordButtonAdapter.updateChordData(mChordsList);
        }
        updateSearchDropdown();
        updateItemSize();
    }

    private final Handler mUpdateChordsHandler = new Handler(msg -> {
        if (msg.what == ImportUtils.RESULT_OK) updateAdapter();
        return true;
    });

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        orientationChanged = true;
    }

    private class UpdateChordsThread extends Thread {
        public void run() {
            loadChords();
            updateChordSet();
            mChordStaticSet = new ArrayList<>(mChordSet);
            mUpdateChordsHandler.sendEmptyMessage(ImportUtils.RESULT_OK);
        }
    }

    private void loadChords() {
        if (isAllChords) {
            mChordsList = HighlightUtils.getAllChords(this);
            chordGridEnabled = false;
        } else {
            mChordsList = new ArrayList<>(mNoteChordsList);
            mWordsMap = new TreeMap<>((Map<Integer, String>) getIntent().getSerializableExtra(ARG_WORDS));
        }
    }

    private void updateSearchDropdown() {
        ViewUtils.updateFilterDropdown(this, mSearchInputView, mChordSet);
    }

    private void updateGridIcon() {
        int icon;
        if (chordGridEnabled) {
            icon = R.drawable.av_grid_on_24;
        } else {
            icon = R.drawable.av_grid_off_24;
        }
        mGridMenuItem.setIcon(icon);
    }

    private void updateSearch(String request) {
        if ((request.length() < prevRequest.length()) || (!request.contains(prevRequest)) || (mSearchSet == null))
            mSearchSet = new ArrayList<>(mChordSet);
        for (int i = 0; i < mSearchSet.size(); i++) {
            if (!mSearchSet.get(i).contains(request)) {
                mSearchSet.remove(i);
                i -= 1;
            }
        }
        chordButtonAdapter.updateChordData(mSearchSet);
    }

    private void updateChordSet() {
        mChordSet = HighlightUtils.getChordsSet(mChordsList);
    }

    private void updateGrid(boolean isEnabled) {
        chordGridEnabled = isEnabled;
        Note.setChordGridEnabled(chordGridEnabled);
    }

    private void updateChordData() {
        if (chordGridEnabled) {
            chordButtonAdapter.updateChordData(mChordsList);
            chordButtonAdapter.showWordsData();
        } else {
            chordButtonAdapter.updateChordData(mChordSet);
        }
    }

    private void updateDisplayMetrics() {
        displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
    }

    private void updateLayout() {
        GridLayoutManager layoutManager = new GridLayoutManager(this, itemsInline);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if ((!(mWordsMap.containsKey(position))) || (!chordGridEnabled)) {
                    return 1;
                } else {
                    return itemsInline;
                }
            }
        });
        mChordsButtonsRecyclerView.setLayoutManager(layoutManager);
    }

    private void updateItemSize() {
        updateDisplayMetrics();
        int imageCount = 4;
        if (DisplayUtils.isLandscape(this)) imageCount += 4;
        itemWidth = (displayMetrics.widthPixels - itemPadding * (itemsInline + 1)) / itemsInline;
        wordsWidth = displayMetrics.widthPixels - itemPadding * 2;
        chordWidth = displayMetrics.widthPixels / imageCount;
        if (chordButtonAdapter != null) chordButtonAdapter.updateItemSize(itemWidth, wordsWidth);
        if (chordAdapter != null) chordAdapter.updateItemSize(chordWidth);
        updateLayout();
    }

    private void transposeChords(int direction) {
        int activeDrawableIndex = mChordsList.indexOf(prevChord);
        List<String> data = new ArrayList<>(mChordsList);
        for (int i = 0; i < data.size(); i++) {
            String chord = data.get(i);
            String originalPitch = "";
            String transposedPitch = "";
            String chordEnd = "";
            String chordKey = chord.substring(0, 1);
            if (chord.length() > 1) {
                originalPitch = chord.substring(1, 2);
                if (!((originalPitch.equals(SHARP)) || (originalPitch.equals(BEMOL)))) {
                    chordEnd = chord.substring(1);
                    originalPitch = "";
                } else {
                    chordEnd = chord.substring(2);
                }
            }
            int index = mNotesList.indexOf(chordKey) + direction;
            if (direction != 0) {
                if (index < 0) {
                    index += mNotesList.size();
                } else if (index >= mNotesList.size()) {
                    index -= mNotesList.size();
                }
            }
            String transposedKey = mNotesList.get(index);
            String transposedChord;
            if (originalPitch.equals(SHARP)) {
                if (direction == TRANSPOSE_DOWN) {
                    transposedChord = chordKey;
                } else {
                    transposedChord = transposedKey;
                }
            } else if (originalPitch.equals(BEMOL)) {
                if (direction == TRANSPOSE_UP) {
                    transposedChord = chordKey;
                } else {
                    transposedChord = transposedKey;
                }
            } else {
                if (direction == TRANSPOSE_UP) {
                    if (mNotesHalftonesList.contains(chordKey)) {
                        transposedChord = chordKey + SHARP;
                    } else {
                        if (mNotesHalftonesList.contains(transposedKey)) {
                            transposedChord = transposedKey;
                        } else {
                            transposedChord = transposedKey + BEMOL;
                        }
                    }
                } else {
                    if (mNotesHalftonesList.contains(chordKey)) {
                        if (mNotesHalftonesList.contains(transposedKey)) {
                            transposedChord = transposedKey + SHARP;
                        } else {
                            transposedChord = transposedKey;
                        }
                    } else {
                        transposedChord = chordKey + BEMOL;
                    }
                }
            }
            String result = transposedChord + chordEnd;
            data.set(i, result);
            int setIndex = mChordSet.indexOf(chord);
            mTransposedIndexesMap.remove(setIndex);
            mTransposedIndexesMap.put(setIndex, result);
        }
        mChordsList = new ArrayList<>(data);
        updateChordSet();
        updateChordData();

        if (mDrawableLayout.getVisibility() == View.VISIBLE) {
            if (activeDrawableIndex != -1) {
                updateChord(mChordsList.get(activeDrawableIndex));
            } else {
                hideChords();
            }
        }
    }

    public void showChords(String chord) {
        if ((mDrawableLayout.getVisibility() == View.GONE) || (!(chord.equals(prevChord)))) {
            mDrawableLayout.setVisibility(View.VISIBLE);
            prevChord = chord;
            updateDrawables(chord);
            updateMargins();
        } else {
            hideChords();
        }
    }

    public void hideChords() {
        mDrawableLayout.setVisibility(View.GONE);
        updateMargins();
    }

    public void updateChord(String chord) {
        if (mDrawableLayout.getVisibility() == View.VISIBLE) {
            hideChords();
            showChords(chord);
        }
    }

    private void updateMargins() {
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        if (mDrawableLayout.getVisibility() == View.VISIBLE) {
            mDrawableLayout.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            layoutParams.setMargins(0, mDrawableLayout.getMeasuredHeight(), 0, 0);
        } else {
            layoutParams.setMargins(0, 0, 0, 0);
        }
        mScrollView.setLayoutParams(layoutParams);
    }

    private void updateDrawables(String chord) {
        mChordsDrawable = new ArrayList<>();
        chord = ChordUtils.replaceChord(this, chord);
        String instrument = ResUtils.getResStringLanguage(this, activeInstrument, "en");
        int id = getResources().getIdentifier(("mu_" + chord.replace("#", "s") + "_" + instrument).toLowerCase(), "drawable", this.getPackageName());
        mChordsDrawable.add(ContextCompat.getDrawable(this, id));
        if (chordAdapter != null) chordAdapter.updateItemDrawable(mChordsDrawable);
    }

}
