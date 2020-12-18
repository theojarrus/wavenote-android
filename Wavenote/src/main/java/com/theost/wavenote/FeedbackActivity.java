package com.theost.wavenote;

import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MenuCompat;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.material.textfield.TextInputLayout;
import com.theost.wavenote.models.Note;
import com.theost.wavenote.utils.DisplayUtils;
import com.theost.wavenote.utils.DrawableUtils;
import com.theost.wavenote.utils.NetworkUtils;

import io.sentry.Sentry;
import io.sentry.UserFeedback;
import io.sentry.protocol.SentryId;

public class FeedbackActivity extends ThemedAppCompatActivity {

    private static final int MIN_NAME_LENGTH = 1;
    private static final int MIN_MESSAGE_LENGTH = 1;

    private RelativeLayout mFeedbackLayout;
    private TextInputLayout mNameLayout;
    private TextInputLayout mEmailLayout;
    private TextInputLayout mMessageLayout;

    private MenuItem mSendMenuItem;

    private boolean isNameValid;
    private boolean isEmailValid;
    private boolean isMessageValid;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        setTitle(R.string.feedback);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mFeedbackLayout = findViewById(R.id.feedback_layout);
        mNameLayout = findViewById(R.id.feedback_name);
        mEmailLayout = findViewById(R.id.feedback_email);
        mMessageLayout = findViewById(R.id.feedback_message);

        if (mNameLayout.getEditText() != null)
            mNameLayout.getEditText().setText(Note.getFeedbackName());
        if (mEmailLayout.getEditText() != null)
            mEmailLayout.getEditText().setText(Note.getFeedbackEmail());
        if (mMessageLayout.getEditText() != null)
            mMessageLayout.getEditText().setText(Note.getFeedbackMessage());

        isNameValid = isValidName(mNameLayout.getEditText().getText().toString());
        isEmailValid = isValidName(mEmailLayout.getEditText().getText().toString());
        isMessageValid = isValidName(mMessageLayout.getEditText().getText().toString());

        mNameLayout.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String name = s.toString().trim();
                Note.setFeedbackName(name);
                isNameValid = isValidName(name);
                updateSendButton();
            }
        });

        mEmailLayout.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String email = s.toString().trim();
                Note.setFeedbackEmail(email);
                isEmailValid = isValidEmail(email);
                updateSendButton();
            }
        });

        mMessageLayout.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String message = s.toString().trim();
                Note.setFeedbackMessage(message);
                isMessageValid = isValidMessage(message);
                updateSendButton();
            }
        });

        mNameLayout.getEditText().setOnFocusChangeListener((view, hasFocus) -> {
            if (!hasFocus) {
                checkNameInput();
                DisplayUtils.hideKeyboard(view);
            } else {
                mNameLayout.setError("");
            }
        });

        mEmailLayout.getEditText().setOnFocusChangeListener((view, hasFocus) -> {
            if (!hasFocus) {
                checkEmailInput();
                DisplayUtils.hideKeyboard(view);
            } else {
                mEmailLayout.setError("");
            }
        });

        mMessageLayout.getEditText().setOnFocusChangeListener((view, hasFocus) -> {
            if (!hasFocus) {
                DisplayUtils.hideKeyboard(view);
            } else {
                mMessageLayout.setError("");
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.feedback_list, menu);
        DrawableUtils.tintMenuWithAttribute(this, menu, R.attr.toolbarIconColor);
        mSendMenuItem = menu.findItem(R.id.feedback_send);
        MenuCompat.setGroupDividerEnabled(menu, true);
        updateSendButton();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.feedback_send) onSendClick();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        DisplayUtils.hideKeyboard(mNameLayout);
    }

    private void onSendClick() {
        boolean isNetwork = NetworkUtils.isNetworkAvailable(this);
        if (isNetwork && !getString(R.string.SENTRY_DSN).equals("")) {
            sendFeedback();
        } else {
            showNetworkDialog();
        }
    }

    private void sendFeedback() {
        if (mNameLayout.getEditText() != null && mEmailLayout.getEditText() != null && mMessageLayout.getEditText() != null) {
            mSendMenuItem.setVisible(false);
            String name = mNameLayout.getEditText().getText().toString().trim();
            String email = mEmailLayout.getEditText().getText().toString().trim();
            String message = mMessageLayout.getEditText().getText().toString().trim();
            sendSentry(name, email, message);
            mFeedbackLayout.setVisibility(View.GONE);
            showEmptyView();
            clearSavedData();
        }
    }

    private void sendSentry(String name, String email, String message) {
        SentryId sentryId = Sentry.captureMessage("Feedback by " + name + " (" + Build.MODEL + ")");
        UserFeedback userFeedback = new UserFeedback(sentryId);
        userFeedback.setName(name);
        userFeedback.setEmail(email);
        userFeedback.setComments(message);
        Sentry.captureUserFeedback(userFeedback);
    }

    private void showNetworkDialog() {
        new MaterialDialog.Builder(this)
                .title(R.string.feedback)
                .content(R.string.simperium_dialog_message_network)
                .positiveText(R.string.yes)
                .build().show();
    }

    private void showEmptyView() {
        LinearLayout emptyView = findViewById(android.R.id.empty);
        ImageView mEmptyViewImage = emptyView.findViewById(R.id.image);
        TextView mEmptyViewText = emptyView.findViewById(R.id.text);
        mEmptyViewText.setText(R.string.feedback_successful);
        mEmptyViewImage.setImageDrawable(
                DrawableUtils.tintDrawableWithAttribute(
                        this, R.drawable.ic_email_24dp, R.attr.toolbarIconColor)
        );
        emptyView.setVisibility(View.VISIBLE);
    }

    private void clearSavedData() {
        Note.setFeedbackName("");
        Note.setFeedbackEmail("");
        Note.setFeedbackMessage("");
    }

    private void updateSendButton() {
        if (isNameValid && isEmailValid && isMessageValid) {
            mSendMenuItem.setEnabled(true);
            DrawableUtils.setMenuItemAlpha(mSendMenuItem, 1);
        } else {
            mSendMenuItem.setEnabled(false);
            DrawableUtils.setMenuItemAlpha(mSendMenuItem, 0.3);
        }
    }

    private void checkNameInput() {
        if (!isNameValid) {
            mNameLayout.setError(getString(R.string.feedback_error_name));
        } else {
            mNameLayout.setError("");
        }
    }

    private void checkEmailInput() {
        if (!isEmailValid) {
            mEmailLayout.setError(getString(R.string.simperium_error_email));
        } else {
            mEmailLayout.setError("");
        }
    }

    private boolean isValidName(String text) {
        return text.length() >= MIN_NAME_LENGTH;
    }

    private boolean isValidEmail(String text) {
        return Patterns.EMAIL_ADDRESS.matcher(text).matches();
    }

    private boolean isValidMessage(String text) {
        return text.length() >= MIN_MESSAGE_LENGTH;
    }

}
