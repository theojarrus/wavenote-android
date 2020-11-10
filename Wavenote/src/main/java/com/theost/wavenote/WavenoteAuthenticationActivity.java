package com.theost.wavenote;

import android.content.Intent;

import com.simperium.android.AuthenticationActivity;

public class WavenoteAuthenticationActivity extends AuthenticationActivity {

    @Override
    protected void buttonLoginClicked() {
        Intent intent = new Intent(WavenoteAuthenticationActivity.this, WavenoteCredentialsActivity.class);
        intent.putExtra(EXTRA_IS_LOGIN, true);
        startActivity(intent);
        finish();
    }

    @Override
    protected void buttonSignupClicked() {
        Intent intent = new Intent(WavenoteAuthenticationActivity.this, WavenoteCredentialsActivity.class);
        intent.putExtra(EXTRA_IS_LOGIN, false);
        startActivity(intent);
        finish();
    }

}
