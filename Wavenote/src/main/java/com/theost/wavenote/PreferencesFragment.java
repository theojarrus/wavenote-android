package com.theost.wavenote;

import android.app.Activity;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import com.theost.wavenote.models.Note;
import com.theost.wavenote.models.Preferences;
import com.theost.wavenote.utils.CrashUtils;
import com.theost.wavenote.utils.PrefUtils;
import com.theost.wavenote.utils.WidgetUtils;
import com.simperium.Simperium;
import com.simperium.client.Bucket;
import com.simperium.client.User;

import org.wordpress.passcodelock.AppLockManager;

import java.lang.ref.WeakReference;

/**
 * A simple {@link Fragment} subclass.
 */
public class PreferencesFragment extends PreferenceFragmentCompat implements User.StatusChangeListener,
        Simperium.OnUserCreatedListener {

    private static final String WEB_APP_URL = "https://app.simplenote.com";

    private Bucket<Preferences> mPreferencesBucket;

    public PreferencesFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Preference authenticatePreference = findPreference("pref_key_authenticate");
        Wavenote currentApp = (Wavenote) getActivity().getApplication();
        Simperium simperium = currentApp.getSimperium();
        simperium.setUserStatusChangeListener(this);
        simperium.setOnUserCreatedListener(this);
        mPreferencesBucket = currentApp.getPreferencesBucket();
        mPreferencesBucket.start();

        authenticatePreference.setSummary(currentApp.getSimperium().getUser().getEmail());
        if (simperium.needsAuthorization()) {
            authenticatePreference.setTitle(R.string.log_in);
        } else {
            authenticatePreference.setTitle(R.string.log_out);
        }

        authenticatePreference.setOnPreferenceClickListener(preference -> {
            if (!isAdded()) {
                return false;
            }

            Wavenote currentApp1 = (Wavenote) getActivity().getApplication();
            if (currentApp1.getSimperium().needsAuthorization()) {
                Intent loginIntent = new Intent(getActivity(), WavenoteAuthenticationActivity.class);
                startActivityForResult(loginIntent, Simperium.SIGNUP_SIGNIN_REQUEST);
            } else {
                new LogOutTask(PreferencesFragment.this).execute();
            }
            return true;
        });

        findPreference("pref_key_dictionary").setOnPreferenceClickListener(preference -> {
            startActivity(new Intent(getActivity(), DictionaryActivity.class));
            return true;
        });

        findPreference("pref_key_about").setOnPreferenceClickListener(preference -> {
            startActivity(new Intent(getActivity(), AboutActivity.class));
            return true;
        });

        final ListPreference themePreference = findPreference(PrefUtils.PREF_THEME);
        themePreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                updateTheme(requireActivity(), Integer.parseInt(newValue.toString()));
                return true;
            }

            private void updateTheme(Activity activity, int index) {
                CharSequence[] entries = themePreference.getEntries();
                themePreference.setSummary(entries[index]);

                // recreate the activity so new theme is applied
                activity.recreate();
            }
        });

        final ListPreference sortPreference = findPreference(PrefUtils.PREF_SORT_ORDER);
        sortPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            int index = Integer.parseInt(newValue.toString());
            CharSequence[] entries = sortPreference.getEntries();
            sortPreference.setSummary(entries[index]);

            return true;
        });

        Preference versionPref = findPreference("pref_key_build");
        versionPref.setSummary(PrefUtils.versionInfo());

        SwitchPreferenceCompat switchPreference = findPreference("pref_key_condensed_note_list");
        switchPreference.setOnPreferenceChangeListener((preference, o) -> true);

    }

    @Override
    public void onPause() {
        super.onPause();
        mPreferencesBucket.stop();
    }

    private DialogInterface.OnClickListener logOutClickListener = (dialogInterface, i) -> logOut();

    private DialogInterface.OnClickListener loadWebAppClickListener = (dialogInterface, i) -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(WEB_APP_URL)));

    private boolean hasUnsyncedNotes() {
        Wavenote application = (Wavenote) getActivity().getApplication();
        Bucket<Note> notesBucket = application.getNotesBucket();
        Bucket.ObjectCursor<Note> notesCursor = notesBucket.allObjects();
        while (notesCursor.moveToNext()) {
            Note note = notesCursor.getObject();
            if (note.isNew() || note.isModified()) {
                return true;
            }
        }

        return false;
    }

    private void logOut() {
        Wavenote application = (Wavenote) getActivity().getApplication();
        application.getSimperium().deauthorizeUser();

        application.getNotesBucket().reset();
        application.getTagsBucket().reset();
        application.getPreferencesBucket().reset();

        application.getNotesBucket().stop();
        application.getTagsBucket().stop();
        application.getPreferencesBucket().stop();

        // User back to 'anon' type
        CrashUtils.clearCurrentUser();

        // Remove wp.com token
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
        editor.remove(PrefUtils.PREF_WP_TOKEN);

        // Remove WordPress sites
        editor.remove(PrefUtils.PREF_WORDPRESS_SITES);
        editor.apply();

        // Remove Passcode Lock password
        AppLockManager.getInstance().getAppLock().setPassword("");

        WidgetUtils.updateNoteWidgets(requireActivity().getApplicationContext());

        getActivity().finish();
    }

    @Override
    public void onUserStatusChange(User.Status status) {
        if (isAdded() && status == User.Status.AUTHORIZED) {
            // User signed in
            getActivity().runOnUiThread(() -> {
                Preference authenticatePreference = findPreference("pref_key_authenticate");
                authenticatePreference.setTitle(R.string.log_out);
            });

            Wavenote app = (Wavenote) getActivity().getApplication();
            CrashUtils.setCurrentUser(app.getSimperium().getUser());
        }
    }

    @Override
    public void onUserCreated(User user) {
    }

    private static class LogOutTask extends AsyncTask<Void, Void, Boolean> {
        private WeakReference<PreferencesFragment> mPreferencesFragmentReference;

        LogOutTask(PreferencesFragment fragment) {
            mPreferencesFragmentReference = new WeakReference<>(fragment);
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            PreferencesFragment fragment = mPreferencesFragmentReference.get();
            return fragment == null || fragment.hasUnsyncedNotes();
        }

        @Override
        protected void onPostExecute(Boolean hasUnsyncedNotes) {
            PreferencesFragment fragment = mPreferencesFragmentReference.get();

            if (fragment == null) {
                return;
            }

            // Safety first! Check if any notes are unsynced and warn the user if so.
            if (hasUnsyncedNotes) {
                new AlertDialog.Builder(new ContextThemeWrapper(fragment.requireContext(), R.style.Dialog))
                        .setTitle(R.string.unsynced_notes)
                        .setMessage(R.string.unsynced_notes_message)
                        .setPositiveButton(R.string.delete_notes, fragment.logOutClickListener)
                        .setNeutralButton(R.string.visit_web_app, fragment.loadWebAppClickListener)
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            } else {
                fragment.logOut();
            }
        }
    }
}
