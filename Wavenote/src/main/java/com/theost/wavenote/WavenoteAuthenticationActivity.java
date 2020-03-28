package com.theost.wavenote;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextThemeWrapper;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.theost.wavenote.utils.StrUtils;
import com.theost.wavenote.utils.WordPressUtils;
import com.simperium.android.AuthenticationActivity;

import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;

import java.util.UUID;

public class WavenoteAuthenticationActivity extends AuthenticationActivity {
    private static String STATE_AUTH_STATE = "STATE_AUTH_STATE";

    private String mAuthState;

    @Override
    protected void buttonLoginClicked() {
        super.buttonLoginClicked();
    }

    @Override
    protected void buttonSignupClicked() {
        Intent intent = new Intent(WavenoteAuthenticationActivity.this, WavenoteCredentialsActivity.class);
        intent.putExtra(EXTRA_IS_LOGIN, false);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != WordPressUtils.OAUTH_ACTIVITY_CODE || data == null) {
            return;
        }

        AuthorizationResponse authorizationResponse = AuthorizationResponse.fromIntent(data);
        AuthorizationException authorizationException = AuthorizationException.fromIntent(data);

        if (authorizationException != null) {
            Uri dataUri = data.getData();

            if (dataUri == null) {
                return;
            }

            if (StrUtils.isSameStr(dataUri.getQueryParameter("code"), "1")) {
                showDialogError(R.string.wpcom_log_in_error_unverified);
            } else {
                showDialogError(R.string.wpcom_log_in_error_generic);
            }
        } else if (authorizationResponse != null) {
            // Save token and finish activity.
            boolean authSuccess = WordPressUtils.processAuthResponse((Wavenote) getApplication(), authorizationResponse, mAuthState, true);

            if (!authSuccess) {
                showDialogError(R.string.wpcom_log_in_error_generic);
            } else {
                finish();
            }
        }
    }

    @Override
    public void onLoginSheetCanceled() {
        super.onLoginSheetCanceled();
    }

    @Override
    public void onLoginSheetEmailClicked() {
        Intent intent = new Intent(WavenoteAuthenticationActivity.this, WavenoteCredentialsActivity.class);
        intent.putExtra(EXTRA_IS_LOGIN, true);
        startActivity(intent);
        finish();
    }

    @Override
    public void onLoginSheetOtherClicked() {
        AuthorizationRequest.Builder authRequestBuilder = WordPressUtils.getWordPressAuthorizationRequestBuilder();

        // Set unique state value.
        mAuthState = "app-" + UUID.randomUUID();
        authRequestBuilder.setState(mAuthState);

        AuthorizationRequest request = authRequestBuilder.build();
        AuthorizationService authService = new AuthorizationService(WavenoteAuthenticationActivity.this);
        Intent authIntent = authService.getAuthorizationRequestIntent(request);
        startActivityForResult(authIntent, WordPressUtils.OAUTH_ACTIVITY_CODE);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString(STATE_AUTH_STATE, mAuthState);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        if (savedInstanceState.containsKey(STATE_AUTH_STATE)) {
            mAuthState = savedInstanceState.getString(STATE_AUTH_STATE);
        }
    }

    private void showDialogError(@StringRes int message) {
        if (isFinishing() || message == 0) {
            return;
        }

        Context context = new ContextThemeWrapper(WavenoteAuthenticationActivity.this, getTheme());
        new AlertDialog.Builder(context)
            .setTitle(R.string.simperium_dialog_title_error)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show();
    }
}
