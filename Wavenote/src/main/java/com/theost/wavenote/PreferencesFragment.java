package com.theost.wavenote;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.text.InputType;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.folderselector.FileChooserDialog;
import com.afollestad.materialdialogs.folderselector.FolderChooserDialog;
import com.theost.wavenote.models.Note;
import com.theost.wavenote.models.Preferences;
import com.theost.wavenote.utils.CrashUtils;
import com.theost.wavenote.utils.DisplayUtils;
import com.theost.wavenote.utils.ExportUtils;
import com.theost.wavenote.utils.FileUtils;
import com.theost.wavenote.utils.ImportUtils;
import com.theost.wavenote.utils.PermissionUtils;
import com.theost.wavenote.utils.PrefUtils;
import com.theost.wavenote.utils.StrUtils;
import com.theost.wavenote.utils.WidgetUtils;
import com.simperium.Simperium;
import com.simperium.client.Bucket;
import com.simperium.client.User;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import org.wordpress.passcodelock.AppLockManager;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class PreferencesFragment extends PreferenceFragmentCompat implements User.StatusChangeListener,
        Simperium.OnUserCreatedListener {

    private static final String WEB_APP_URL = "https://app.simplenote.com";
    private static final int IMPORT_REQUEST = 0;
    private static final int EXPORT_REQUEST = 1;

    private Bucket<Preferences> mPreferencesBucket;
    private Bucket<Note> mNotesBucket;

    private CharSequence[] exportModes;
    private MaterialDialog mPasswordDialog;
    private MaterialDialog loadingDialog;
    private String resultDialogMessage;
    private String exportPassword;
    private String exportPath;

    private String[] importExtensions;
    private String importMode;
    private String importQuantity;
    private String importPassword;
    private File extractedDirectory;
    private File importFile;

    private int importCount;

    private boolean isImportZip;

    private boolean isImporting;
    private boolean isExporting;
    private boolean isUnzipping;

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

        Preference authenticatePreference = findPreference(PrefUtils.PREF_AUTHENTICATE);
        Wavenote currentApp = (Wavenote) getActivity().getApplication();
        Simperium simperium = currentApp.getSimperium();
        simperium.setUserStatusChangeListener(this);
        simperium.setOnUserCreatedListener(this);
        mNotesBucket = currentApp.getNotesBucket();
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

        findPreference(PrefUtils.PREF_DICTIONARY).setOnPreferenceClickListener(preference -> {
            startActivity(new Intent(getActivity(), DictionaryActivity.class));
            return true;
        });

        findPreference(PrefUtils.PREF_ABOUT).setOnPreferenceClickListener(preference -> {
            startActivity(new Intent(getActivity(), AboutActivity.class));
            return true;
        });

        findPreference(PrefUtils.PREF_EXPORT_DIR).setOnPreferenceClickListener(preference -> {
            if (PermissionUtils.requestFilePermissions(getActivity())) showFolderDialog();
            return true;
        });

        findPreference(PrefUtils.PREF_IMPORT_NOTES).setOnPreferenceClickListener(preference -> {
            if (PermissionUtils.requestFilePermissions(getActivity()) && !isExporting && !isImporting && !isUnzipping)
                showImportDialog();
            return true;
        });

        findPreference(PrefUtils.PREF_EXPORT_NOTES).setOnPreferenceClickListener(preference -> {
            if (PermissionUtils.requestFilePermissions(getActivity()) && !isExporting && !isImporting && !isUnzipping)
                showExportDialog();
            return true;
        });

        final ListPreference themePreference = findPreference(PrefUtils.PREF_THEME);
        themePreference.setSummary(themePreference.getEntries()[Integer.parseInt(PrefUtils.getStringPref(getContext(), PrefUtils.PREF_THEME))]);
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
        sortPreference.setSummary(sortPreference.getEntries()[Integer.parseInt(PrefUtils.getStringPref(getContext(), PrefUtils.PREF_SORT_ORDER))]);
        sortPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            int index = Integer.parseInt(newValue.toString());
            CharSequence[] entries = sortPreference.getEntries();
            sortPreference.setSummary(entries[index]);

            return true;
        });

        updateExportDir();

        Preference versionPref = findPreference(PrefUtils.PREF_BUILD);
        versionPref.setSummary(PrefUtils.versionInfo());

        SwitchPreferenceCompat switchPreference = findPreference(PrefUtils.PREF_CONDENSED);
        switchPreference.setOnPreferenceChangeListener((preference, o) -> true);

    }

    @Override
    public void onPause() {
        super.onPause();
        mPreferencesBucket.stop();
    }

    private MaterialDialog.SingleButtonCallback logOutClickListener = (dialogInterface, i) -> logOut();

    private MaterialDialog.SingleButtonCallback loadWebAppClickListener = (dialogInterface, i) -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(WEB_APP_URL)));

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
                Preference authenticatePreference = findPreference(PrefUtils.PREF_AUTHENTICATE);
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
                new MaterialDialog.Builder(fragment.getContext())
                        .title(R.string.unsynced_notes)
                        .content(R.string.unsynced_notes_message)
                        .positiveText(R.string.delete_notes)
                        .neutralText(R.string.visit_web_app)
                        .negativeText(R.string.cancel)
                        .onPositive(fragment.logOutClickListener)
                        .onNeutral(fragment.loadWebAppClickListener)
                        .show();
            } else {
                fragment.logOut();
            }
        }
    }

    private void updateExportDir() {
        exportPath = PrefUtils.getStringPref(getContext(), PrefUtils.PREF_EXPORT_DIR);
        findPreference(PrefUtils.PREF_EXPORT_DIR).setSummary(exportPath);
    }

    public void changeExportFolder(File folder) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
        editor.putString(PrefUtils.PREF_EXPORT_DIR, folder.getAbsolutePath());
        editor.apply();
        updateExportDir();
    }

    private void showFolderDialog() {
        new FolderChooserDialog.Builder(getActivity())
                .initialPath(exportPath)
                .chooseButton(R.string.choose_folder)
                .show(getActivity());
    }

    private void showLoadingDialog(int titleId, int contentId) {
        new CountDownTimer(200, 200) {
            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                if (isExporting || isImporting || isUnzipping)
                    loadingDialog = DisplayUtils.showLoadingDialog(getContext(), titleId, contentId);
            }
        }.start();
    }

    private void showResultDialog(int request) {
        int title = 0;
        if (request == IMPORT_REQUEST) {
            title = R.string.import_notes;
        } else if (request == EXPORT_REQUEST) {
            title = R.string.export_notes;
            DisplayUtils.showToast(getContext(), getContext().getResources().getString(R.string.path) + ": " + exportPath);
        }
        new MaterialDialog.Builder(getContext())
                .title(title)
                .content(resultDialogMessage)
                .positiveText(android.R.string.ok)
                .show();
    }

    private void showImportDialog() {
        importMode = null;
        importQuantity = null;
        importExtensions = new String[]{FileUtils.ZIP_FORMAT, null};
        new MaterialDialog.Builder(getContext())
                .title(R.string.import_notes)
                .content(R.string.import_notification)
                .positiveText(R.string.import_text)
                .onPositive((dialog, which) -> showChoiceDialog())
                .negativeText(R.string.cancel).show();
    }

    private void showChoiceDialog() {
        int choiceItems;
        if (importMode == null) {
            choiceItems = R.array.import_types;
        } else {
            choiceItems = R.array.import_modes;
        }
        new MaterialDialog.Builder(getContext())
                .title(R.string.import_notes)
                .items(choiceItems)
                .itemsCallback((dialog, view, which, text) -> {
                    String selected = text.toString();
                    if (selected.equals(getResources().getString(R.string.import_plaintext))) {
                        importMode = selected;
                        importQuantity = getResources().getString(R.string.import_single);
                        importExtensions[1] = FileUtils.TEXT_FORMAT;
                    }
                    if (importMode == null) {
                        importMode = selected;
                        showChoiceDialog();
                    } else {
                        if (importQuantity == null) {
                            importQuantity = selected;
                            importExtensions[1] = FileUtils.JSON_FORMAT;
                        }
                        showFileDialog();
                    }
                })
                .show();
    }

    public void selectImportFile(File file) {
        importFile = file;
        isImportZip = StrUtils.getFileExtention(file.getName()).equals(FileUtils.ZIP_FORMAT);
        if (isImportZip) {
            extractZip();
        } else {
            importNotes();
        }
    }

    private void showFileDialog() {
        new FileChooserDialog.Builder(getActivity())
                .initialPath(exportPath)
                .extensionsFilter(importExtensions)
                .show(getActivity());
    }

    private void extractZip() {
        new ExtractZipThread().start();
        showLoadingDialog(R.string.import_text, R.string.extracting);
    }

    private Handler mExtractHandler = new Handler(msg -> {
        if (loadingDialog != null) loadingDialog.dismiss();
        if (msg.what == ImportUtils.RESULT_OK) {
            importNotes();
        } else {
            if (msg.what == ImportUtils.FILE_ERROR) {
                DisplayUtils.showToast(getContext(), getResources().getString(R.string.file_error));
            } else if (msg.what == ImportUtils.PASSWORD_ERROR) {
                showPasswordDialog(IMPORT_REQUEST);
            }
        }
        return true;
    });

    private class ExtractZipThread extends Thread {
        @Override
        public void run() {
            isUnzipping = true;
            boolean isExtracted = false;
            try {
                ZipFile zipFile = null;
                if (new ZipFile(importFile).isEncrypted()) {
                    if (importPassword != null)
                        zipFile = new ZipFile(importFile, importPassword.toCharArray());
                    if (importPassword == null || !FileUtils.verifyZip(zipFile)) {
                        isUnzipping = false;
                        mExtractHandler.sendEmptyMessage(ImportUtils.PASSWORD_ERROR);
                        return;
                    }
                } else {
                    zipFile = new ZipFile(importFile);
                }
                extractedDirectory = new File(importFile.getParent(), StrUtils.getFileName(importFile.getName()));
                if (extractedDirectory.exists()) FileUtils.removeDirectory(extractedDirectory);
                zipFile.extractAll(importFile.getParent());
                importFile = extractedDirectory;
                isExtracted = true;
            } catch (ZipException e) {
                e.printStackTrace();
            }
            if (!isExtracted) {
                mExtractHandler.sendEmptyMessage(ImportUtils.FILE_ERROR);
            } else {
                mExtractHandler.sendEmptyMessage(ImportUtils.RESULT_OK);
            }
            isUnzipping = false;
        }
    }

    private void importNotes() {
        new ImportThread().start();
        showLoadingDialog(R.string.import_notes, R.string.wait_a_bit);
    }

    private Handler mImportHandler = new Handler(msg -> {
        if (loadingDialog != null) loadingDialog.dismiss();
        if (msg.what == ImportUtils.RESULT_OK) {
            resultDialogMessage = String.format(getResources().getString(R.string.import_succesful), importCount);
        } else {
            resultDialogMessage = String.format(getResources().getString(R.string.import_failure), importCount);
        }
        showResultDialog(IMPORT_REQUEST);
        return true;
    });

    private class ImportThread extends Thread {
        @Override
        public void run() {
            isImporting = true;

            int[] importResult = {ImportUtils.FILE_ERROR, 0};

            if (importMode.equals(getResources().getString(R.string.import_plaintext))) {
                importResult = ImportUtils.importPlaintext(getContext(), mNotesBucket, importFile);
            } else if (importMode.equals(getResources().getString(R.string.import_json))) {
                importResult = ImportUtils.importJson(getContext(), mNotesBucket, importFile, importQuantity);
            }

            importCount = importResult[1];

            if (isImportZip) FileUtils.removeDirectory(extractedDirectory);

            if (importResult[0] == ImportUtils.RESULT_OK) {
                mImportHandler.sendEmptyMessage(ImportUtils.RESULT_OK);
            } else {
                mImportHandler.sendEmptyMessage(ImportUtils.FILE_ERROR);
            }

            isImporting = false;
        }
    }

    private void showExportDialog() {
        List<String> exportModes = new ArrayList<>(Arrays.asList(getContext().getResources().getStringArray(R.array.array_export_modes)));
        File exportDir = new File(getContext().getCacheDir() + FileUtils.NOTES_DIR);
        if (mNotesBucket.count() != 0) {
            new MaterialDialog.Builder(getContext())
                    .title(R.string.export_notes)
                    .positiveText(R.string.export)
                    .negativeText(R.string.cancel)
                    .items(exportModes)
                    .itemsCallbackMultiChoice(null, (dialog, which, modes) -> {
                        if (modes.length != 0) {
                            this.exportModes = modes;
                            if (Arrays.toString(modes).contains(getContext().getResources().getString(R.string.zip))) {
                                showPasswordDialog(EXPORT_REQUEST);
                            } else {
                                exportNotes();
                            }
                        }
                        return true;
                    }).show();
        } else {
            DisplayUtils.showToast(getContext(), getResources().getString(R.string.notes_not_found));
        }
    }

    private void showPasswordDialog(int requestCode) {
        CharSequence prefill = "";
        CharSequence inputText = "";
        int title = 0;
        int positiveText = 0;
        if (requestCode == IMPORT_REQUEST) {
            prefill = importPassword;
            importPassword = "";
            title = R.string.encrypted;
            positiveText = R.string.import_text;
            inputText = getResources().getString(R.string.simperium_hint_password);
        } else if (requestCode == EXPORT_REQUEST) {
            exportPassword = "";
            title = R.string.export_notes;
            positiveText = R.string.export;
            inputText = getResources().getString(R.string.hint_password);
        }
        new MaterialDialog.Builder(getContext())
                .title(title)
                .positiveText(positiveText)
                .negativeText(R.string.cancel)
                .inputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)
                .input(inputText, prefill, (dialog, input) -> {
                    if (requestCode == IMPORT_REQUEST) {
                        this.importPassword = input.toString().trim();
                        extractZip();
                    } else if (requestCode == EXPORT_REQUEST) {
                        this.exportPassword = input.toString().trim();
                        exportNotes();
                    }
                }).show();
    }

    private void exportNotes() {
        showLoadingDialog(R.string.export, R.string.exporting);
        new ExportThread().start();
    }

    private Handler mExportHandler = new Handler(msg -> {
        if (loadingDialog != null) loadingDialog.dismiss();
        if (msg.what == ImportUtils.RESULT_OK) {
            resultDialogMessage = getResources().getString(R.string.export_succesful);
        } else {
            resultDialogMessage = getResources().getString(R.string.export_failure);
        }
        showResultDialog(EXPORT_REQUEST);
        return true;
    });

    private class ExportThread extends Thread {
        @Override
        public void run() {
            isExporting = true;
            exportPath = PrefUtils.getStringPref(getContext(), PrefUtils.PREF_EXPORT_DIR);
            boolean isExported = ExportUtils.exportNotes(getActivity(), exportPath + FileUtils.NOTES_DIR, new ArrayList<>(Arrays.asList(exportModes)), exportPassword);
            if (isExported) {
                mExportHandler.sendEmptyMessage(ImportUtils.RESULT_OK);
            } else {
                mExportHandler.sendEmptyMessage(ImportUtils.FILE_ERROR);
            }
            isExporting = false;
        }
    }

}
