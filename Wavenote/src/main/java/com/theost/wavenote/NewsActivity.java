package com.theost.wavenote;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.google.android.material.button.MaterialButton;
import com.theost.wavenote.utils.NetworkUtils;
import com.theost.wavenote.utils.PrefUtils;

import io.sentry.Sentry;
import io.sentry.SentryLevel;

public class NewsActivity extends AppCompatActivity {

    private static final String TAG_NEWS_SHOWED = "#tag_wavenote_news_showed ";
    private static final String TAG_NEWS_CHECKED = "#tag_wavenote_news_checked ";
    private static final String TAG_NEWS_CONFIRMED = "#tag_wavenote_news_confirmed ";

    private RadioGroup radioGroup;
    private MaterialButton confirmButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news);
        radioGroup = findViewById(R.id.radioGroup);
        confirmButton = findViewById(R.id.confirmButton);

        setupSurvey();
    }

    @Override
    protected void onResume() {
        super.onResume();
        sendShowedAnalytics();
    }

    @Override
    public void onBackPressed() {
        // super.onBackPressed();
    }

    private void setupSurvey() {
        radioGroup.setOnCheckedChangeListener((radioGroup, id) -> {
            confirmButton.setEnabled(true);
            sendCheckedAnalytics(getCheckedOption(id));
        });
        confirmButton.setOnClickListener(view -> {
            if (NetworkUtils.isNetworkAvailable(this)) {
                sendConfirmedAnalytics(getCheckedOption(radioGroup.getCheckedRadioButtonId()));
                completeNews();
            } else {
                Toast.makeText(this, R.string.network_error, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    @SuppressLint("NonConstantResourceId")
    private SurveyOption getCheckedOption(int id) {
        switch (id) {
            case R.id.positive:
                return SurveyOption.POSITIVE;
            case R.id.neutral:
                return SurveyOption.NEUTRAL;
            case R.id.negative:
                return SurveyOption.NEGATIVE;
            default:
                return SurveyOption.UNKNOWN;
        }
    }

    private void sendShowedAnalytics() {

        Sentry.captureMessage(TAG_NEWS_SHOWED, SentryLevel.INFO);
    }

    private void sendCheckedAnalytics(SurveyOption option) {
        Sentry.captureMessage(TAG_NEWS_CHECKED + option.name(), SentryLevel.INFO);
    }

    private void sendConfirmedAnalytics(SurveyOption option) {
        Sentry.captureMessage(TAG_NEWS_CONFIRMED + option.name(), SentryLevel.INFO);
    }

    private void completeNews() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(PrefUtils.PREF_NEWS, true);
        editor.apply();
        finish();
    }

    private enum SurveyOption {
        POSITIVE,
        NEUTRAL,
        NEGATIVE,
        UNKNOWN
    }
}
