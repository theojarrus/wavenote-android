package com.theost.wavenote;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.theost.wavenote.utils.DisplayUtils;
import com.theost.wavenote.utils.ThemeUtils;
import com.theost.wavenote.widgets.ChordGenerator;

import java.util.Random;

public class QuizActivity extends ThemedAppCompatActivity  {

    private GridLayout chordButtonLayout;
    private LinearLayout chordImageLayout;
    private LinearLayout mainQuizLayout;
    private ChordGenerator chordGenerator;

    private TextView scoreTextView;

    private MaterialButton nextButton;
    private MaterialButton answerButton;

    private String[] keysArray;
    private String[] chordsArray;

    private int currentInstrument;
    private int currentScore;

    private int correctColor;
    private int wrongColor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        ThemeUtils.setTheme(this);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_quiz);
        setTitle(R.string.quiz);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        keysArray = getResources().getStringArray(R.array.array_musical_keys);
        chordsArray = getResources().getStringArray(R.array.array_musical_chords);

        correctColor = ContextCompat.getColor(this, R.color.green_10);
        wrongColor = ContextCompat.getColor(this, R.color.red_20);

        scoreTextView = findViewById(R.id.quiz_score);

        nextButton = findViewById(R.id.quiz_next);
        nextButton.setOnClickListener(v -> nextChord());

        mainQuizLayout = findViewById(R.id.main_quiz_layout);
        chordImageLayout = findViewById(R.id.quiz_chord_layout);
        chordButtonLayout = findViewById(R.id.quiz_buttons_layout);
        chordButtonLayout.setColumnCount(3);

        chordGenerator = new ChordGenerator(this, chordImageLayout);
        chordGenerator.setMaxPositionsCount(1);

        currentInstrument = R.string.guitar;

        updateOrientation();
        updateScore(0);
        nextChord();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.quiz_list, menu);
        return true;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.quiz_instrument_guitar:
                updateInstrument(R.string.guitar);
                break;
            case R.id.quiz_instrument_piano:
                updateInstrument(R.string.piano);
                break;
            case R.id.quiz_instrument_ukulele:
                updateInstrument(R.string.ukulele);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateOrientation();
    }

    private void updateOrientation() {
        if (DisplayUtils.isLandscape(this)) {
            mainQuizLayout.setOrientation(LinearLayout.HORIZONTAL);
        } else {
            mainQuizLayout.setOrientation(LinearLayout.VERTICAL);
        }
    }

    private void updateInstrument(int instrument) {
        if (instrument != currentInstrument) {
            currentInstrument = instrument;
            chordImageLayout.removeAllViews();
            chordGenerator.generateChord(answerButton.getText().toString(), instrument);
        }
    }

    @SuppressLint("SetTextI18n")
    private void nextChord() {
        nextButton.setVisibility(View.INVISIBLE);
        chordImageLayout.removeAllViews();
        chordButtonLayout.removeAllViews();
        String key = getRandomItem(keysArray);
        String chord = getRandomItem(chordsArray);
        chordGenerator.generateChord(key + chord, currentInstrument);
        for (String c : chordsArray) {
            MaterialButton button = new MaterialButton(this);
            button.setTransformationMethod(null);
            button.setInsetTop(0);
            button.setInsetBottom(0);
            button.setTextSize(11);
            button.setText(key + c);
            button.setOnClickListener(this::guessChord);
            if (c.equals(chord)) answerButton = button;
            chordButtonLayout.addView(button);
        }
    }

    private void guessChord(View v) {
        if (nextButton.getVisibility() != View.VISIBLE) {
            nextButton.setVisibility(View.VISIBLE);
            if (!((MaterialButton) v).getText().toString().equals(answerButton.getText().toString())) {
                v.setBackgroundColor(wrongColor);
            } else {
                updateScore(10);
            }
            answerButton.setBackgroundColor(correctColor);
        }
    }

    @SuppressLint("SetTextI18n")
    private void updateScore(int points) {
        currentScore += points;
        scoreTextView.setText(getString(R.string.score_quiz) + " " + currentScore);
    }

    private String getRandomItem(String[] array) {
        Random random = new Random();
        int index = random.nextInt(array.length - 1);
        return array[index];
    }

}
