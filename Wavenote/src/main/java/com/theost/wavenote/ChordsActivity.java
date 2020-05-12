package com.theost.wavenote;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuCompat;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputLayout;
import com.theost.wavenote.utils.ChordAdapter;
import com.theost.wavenote.utils.ThemeUtils;
import com.theost.wavenote.utils.ViewUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChordsActivity extends ThemedAppCompatActivity {

    private final String[] CHORDS_REPLACEMENT = {"Cb", "B", "B#", "C", "Db", "C#", "D#", "Eb", "Fb", "E", "E#", "F", "Gb", "F#", "G#", "Ab", "A#", "Bb"};
    private final String[] NOTES_ORDER = {"C", "C#", "D", "Eb", "E", "F", "F#", "G", "Ab", "A", "Bb", "B"};
    private final String[] INSTRUMENTS = {"Guitar", "Piano", "Ukulele"};
    private final String[] COLUMNS = {"1", "2", "3", "4", "5", "6"};
    private int itemsInline = 4;
    private List<Drawable> mChordsDrawable;
    private List<String> mChordsReplace;
    private List<String> mNotesOrder;
    private List<String> mChordsList;
    private AutoCompleteTextView mInstrumentInputView;
    private AutoCompleteTextView mColumnsInputView;
    private RecyclerView mChordsRecyclerView;
    private ChordAdapter adapter;
    private ImageView mEmptyViewImage;
    private TextView mEmptyViewText;
    private String activeInstrument;
    private boolean isAllChords;
    private int itemWidth;

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
        mEmptyViewImage = emptyView.findViewById(R.id.image);
        mEmptyViewText = emptyView.findViewById(R.id.text);
        mEmptyViewImage.setImageResource(R.drawable.m_audiotrack_black_24dp);
        mEmptyViewText.setText(R.string.empty_chords);

        isAllChords = getIntent().getBooleanExtra("isAllChords", false);
        mChordsList = getIntent().getStringArrayListExtra("chords");
        activeInstrument = getIntent().getStringExtra("activeInstrument");
        if (activeInstrument == null) {
            activeInstrument = INSTRUMENTS[0];
        }

        mChordsReplace = Arrays.asList(CHORDS_REPLACEMENT);
        mNotesOrder = Arrays.asList(NOTES_ORDER);

        mChordsRecyclerView = findViewById(R.id.chord_view);
        mChordsRecyclerView.setHasFixedSize(false);
        mChordsRecyclerView.setNestedScrollingEnabled(false);
        ViewCompat.setNestedScrollingEnabled(mChordsRecyclerView, false);

        if (mChordsList.size() == 0) {
            mChordsRecyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
            return;
        }

        mInstrumentInputView = findViewById(R.id.instrument);
        mInstrumentInputView.setText(activeInstrument);
        ViewUtils.disableAutoCompleteTextView(this, mInstrumentInputView, INSTRUMENTS);

        if (isAllChords) {
            TextInputLayout mInstrumentLayout = findViewById(R.id.instrument_layout);
            mInstrumentLayout.setEnabled(false);
        }

        if (mChordsList.size() < itemsInline) itemsInline = mChordsList.size();

        mColumnsInputView = findViewById(R.id.columns);
        mColumnsInputView.setText(Integer.toString(itemsInline));
        ViewUtils.disableAutoCompleteTextView(this, mColumnsInputView, COLUMNS);

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
                int tmpItemsInline = Integer.parseInt(s.toString());
                if (tmpItemsInline != itemsInline) {
                    itemsInline = tmpItemsInline;
                    updateItemSize();
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
                String tmpInstrument = s.toString();
                if (!(tmpInstrument.equals(activeInstrument))) {
                    activeInstrument = tmpInstrument;
                    updateDrawables();
                }
            }
        });

        updateItemSize();
        updateDrawables();
        adapter = new ChordAdapter(this, mChordsDrawable, itemWidth);
        mChordsRecyclerView.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if ((isAllChords) || (mChordsList.size() == 0)) return false;
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.chords_list, menu);
        MenuCompat.setGroupDividerEnabled(menu, true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_transposeup:
                transposeChords(1);
                return true;
            case R.id.menu_transposedown:
                transposeChords(-1);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateItemSize();
    }

    private void updateDrawables() {
        mChordsDrawable = new ArrayList<>();
        for (String i : mChordsList) {
            if ((i.length() > 1) && ((i.substring(1, 2).equals("#")) || (i.substring(1, 2).equals("b")))) {
                String key = i.substring(0, 2);
                String end = "";
                if (mChordsReplace.indexOf(key) % 2 == 0) {
                    if (isAllChords) continue;
                    if (i.length() > 2) end = i.substring(2);
                    i = mChordsReplace.get(mChordsReplace.indexOf(key) + 1) + end;
                }
            }
            int id = getResources().getIdentifier(("mu_" + i.replace("#", "s") + "_" + activeInstrument).toLowerCase(), "drawable", this.getPackageName());
            mChordsDrawable.add(ContextCompat.getDrawable(this, id));
        }
        if (adapter != null) adapter.updateItemDrawable(mChordsDrawable);
    }

    private void updateItemSize() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        itemWidth = displayMetrics.widthPixels / itemsInline;
        mChordsRecyclerView.setLayoutManager(new GridLayoutManager(this, itemsInline));
        if (adapter != null) adapter.updateItemSize(itemWidth);
    }

    private void transposeChords(int direction) {
        for (int i = 0; i < mChordsList.size(); i++) {
            String chord = mChordsList.get(i);
            String end = "";
            int chordEnd = 1;
            if (chord.length() > 1) {
                if ((chord.substring(1, 2).equals("#")) || (chord.substring(1, 2).equals("b"))) {
                    end = chord.substring(2);
                    chordEnd++;
                } else {
                    end = chord.substring(1);
                }
            }
            int index = mNotesOrder.indexOf(chord.substring(0, chordEnd)) + direction;
            if (index < 0) {
                index += mNotesOrder.size();
            } else if (index >= mNotesOrder.size()) {
                index -= mNotesOrder.size();
            }
            mChordsList.set(i, mNotesOrder.get(index) + end);
        }
        updateDrawables();
    }

}
