package com.theost.wavenote.widgets;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

import com.afollestad.materialdialogs.MaterialDialog;
import com.theost.wavenote.FeedbackActivity;
import com.theost.wavenote.R;
import com.theost.wavenote.utils.PrefUtils;

import static com.theost.wavenote.AboutFragment.DEVELOPER_SUPPORT_URL;
import static com.theost.wavenote.AboutFragment.PLAY_STORE_URI;

public class FeedbackDialog {

    public final static int DAYS_UNTIL_PROMPT = 4;
    public final static int LAUNCHES_UNTIL_PROMPT = 8;

    Activity activity;

    public FeedbackDialog(Activity activity) {
        this.activity = activity;
    }

    public void show() {
        new MaterialDialog.Builder(activity)
                .title(R.string.feedback)
                .content(R.string.feedback_like_question)
                .iconRes(R.drawable.ic_wavenote_blue_24dp)
                .limitIconToDefaultSize()
                .positiveText(R.string.yes)
                .negativeText(R.string.no)
                .checkBoxPromptRes(R.string.dont_ask_again, false, null)
                .onPositive((dialog, which) -> showLikeDialog())
                .onNegative((dialog, which) -> showDislikeDialog())
                .onAny((dialog, which) -> {
                    if (dialog.isPromptCheckBoxChecked()) {
                        disableFeedback();
                    }
                })
                .show();
    }

    private void showLikeDialog() {
        new MaterialDialog.Builder(activity)
                .title(R.string.feedback)
                .content(R.string.feedback_like)
                .iconRes(R.drawable.ic_wavenote_blue_24dp)
                .limitIconToDefaultSize()
                .negativeText(R.string.cancel)
                .items(R.array.array_feedback_actions)
                .itemsCallbackSingleChoice(-1, (dialog, view, which, text) -> {
                    onFeedbackListClick(which);
                    return true;
                })
                .show();
    }

    private void showDislikeDialog() {
        new MaterialDialog.Builder(activity)
                .title(R.string.feedback)
                .content(R.string.feedback_dislike)
                .iconRes(R.drawable.ic_wavenote_blue_24dp)
                .limitIconToDefaultSize()
                .positiveText(R.string.yes)
                .negativeText(R.string.no)
                .onPositive((dialog, which) -> startFeedbackActivity())
                .show();
    }

    private void onFeedbackListClick(int action) {
        String rateApp = activity.getString(R.string.rate_us);
        String shareFeedback = activity.getString(R.string.share_thoughts);
        String donateDeveloper = activity.getString(R.string.donate_developer);
        String selectedAction = activity.getResources().getStringArray(R.array.array_feedback_actions)[action];
        if (selectedAction.equals(rateApp)) {
            openLink(PLAY_STORE_URI);
        } else if (selectedAction.equals(shareFeedback)) {
            startFeedbackActivity();
        } else if (selectedAction.equals(donateDeveloper)) {
            openLink(DEVELOPER_SUPPORT_URL);
        }
        showLikeDialog();
    }

    private void disableFeedback() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(PrefUtils.PREF_SHOW_FEEDBACK, false);
        editor.apply();
    }

    private void startFeedbackActivity() {
        activity.startActivity(new Intent(activity, FeedbackActivity.class));
    }

    private void openLink(String link) {
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link)));
        } catch (Exception e) {
            Toast.makeText(activity, R.string.no_browser_available, Toast.LENGTH_LONG).show();
        }
    }

}
