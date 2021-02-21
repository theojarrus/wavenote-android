package com.theost.wavenote.utils;

import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.theost.wavenote.Wavenote;

import org.wordpress.passcodelock.AppLockManager;

public class AuthUtils {
    public static void logOut(Wavenote application) {
        application.getSimperium().deauthorizeUser();

        application.getNotesBucket().reset();
        application.getTagsBucket().reset();
        application.getPreferencesBucket().reset();

        application.getNotesBucket().stop();
        application.getTagsBucket().stop();
        application.getPreferencesBucket().stop();

        // Remove wp.com token
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(application).edit();
        editor.remove(PrefUtils.PREF_WP_TOKEN);

        // Remove WordPress sites
        editor.remove(PrefUtils.PREF_WORDPRESS_SITES);
        editor.apply();

        // Remove Passcode Lock password
        AppLockManager.getInstance().getAppLock().setPassword("");

        WidgetUtils.updateNoteWidgets(application);
    }
}