package com.theost.wavenote;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentCallbacks2;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;

import com.theost.wavenote.models.Note;
import com.theost.wavenote.models.NoteCountIndexer;
import com.theost.wavenote.models.NoteTagger;
import com.theost.wavenote.models.Preferences;
import com.theost.wavenote.models.Tag;
import com.theost.wavenote.utils.CrashUtils;
import com.theost.wavenote.utils.PrefUtils;
import com.simperium.Simperium;
import com.simperium.client.Bucket;
import com.simperium.client.BucketNameInvalid;

import org.wordpress.passcodelock.AppLockManager;

public class Wavenote extends Application {

    private static final int TEN_SECONDS_MILLIS = 10000;

    // log tag
    public static final String TAG = "Wavenote";

    // intent IDs
    public static final int INTENT_PREFERENCES = 1;
    public static final int INTENT_EDIT_NOTE = 2;
    public static final int INTENT_THEORY = 3;
    public static final String DELETED_NOTE_ID = "deletedNoteId";
    public static final String SELECTED_NOTE_ID = "selectedNoteId";
    private static final String AUTH_PROVIDER = "simplenote.com";
    private Simperium mSimperium;
    private Bucket<Note> mNotesBucket;
    private Bucket<Tag> mTagsBucket;
    private static Bucket<Preferences> mPreferencesBucket;

    public void onCreate() {
        super.onCreate();

        CrashUtils.initWithContext(this);
        AppLockManager.getInstance().enableDefaultAppLockIfAvailable(this);

        mSimperium = Simperium.newClient(
                BuildConfig.SIMPERIUM_APP_ID,
                BuildConfig.SIMPERIUM_APP_KEY,
                this
        );

        mSimperium.setAuthProvider(AUTH_PROVIDER);

        try {
            mNotesBucket = mSimperium.bucket(new Note.Schema());
            Tag.Schema tagSchema = new Tag.Schema();
            tagSchema.addIndex(new NoteCountIndexer(mNotesBucket));
            mTagsBucket = mSimperium.bucket(tagSchema);
            mPreferencesBucket = mSimperium.bucket(new Preferences.Schema());
            mPreferencesBucket.start();
            // Every time a note changes or is deleted we need to reindex the tag counts
            mNotesBucket.addListener(new NoteTagger(mTagsBucket));
        } catch (BucketNameInvalid e) {
            throw new RuntimeException("Could not create bucket", e);
        }

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

        ApplicationLifecycleMonitor applicationLifecycleMonitor = new ApplicationLifecycleMonitor();
        registerComponentCallbacks(applicationLifecycleMonitor);
        registerActivityLifecycleCallbacks(applicationLifecycleMonitor);

        CrashUtils.setCurrentUser(mSimperium.getUser());
    }

    @SuppressWarnings("unused")
    private boolean isFirstLaunch() {
        // NotesActivity sets this pref to false after first launch
        return PrefUtils.getBoolPref(this, PrefUtils.PREF_FIRST_LAUNCH, true);
    }

    public Simperium getSimperium() {
        return mSimperium;
    }

    public Bucket<Note> getNotesBucket() {
        return mNotesBucket;
    }

    public Bucket<Tag> getTagsBucket() {
        return mTagsBucket;
    }

    public Bucket<Preferences> getPreferencesBucket() {
        return mPreferencesBucket;
    }

    private class ApplicationLifecycleMonitor implements Application.ActivityLifecycleCallbacks,
            ComponentCallbacks2 {
        private boolean mIsInBackground = true;

        // ComponentCallbacks
        @Override
        public void onTrimMemory(int level) {
            if (level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
                mIsInBackground = true;

                // Give the buckets some time to finish sync, then stop them
                new Handler().postDelayed(() -> {
                    if (!mIsInBackground) {
                        return;
                    }

                    if (mNotesBucket != null) {
                        mNotesBucket.stop();
                    }

                    if (mTagsBucket != null) {
                        mTagsBucket.stop();
                    }

                    if (mPreferencesBucket != null) {
                        mPreferencesBucket.stop();
                    }
                }, TEN_SECONDS_MILLIS);
            } else {
                mIsInBackground = false;
            }
        }

        @Override
        public void onConfigurationChanged(@NonNull Configuration newConfig) {
        }

        @Override
        public void onLowMemory() {
        }

        // ActivityLifeCycle callbacks
        @Override
        public void onActivityResumed(@NonNull Activity activity) {
            if (mIsInBackground) {
                mIsInBackground = false;
            }
        }

        @Override
        public void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) {
        }

        @Override
        public void onActivityStarted(@NonNull  Activity activity) {
        }

        @Override
        public void onActivityPaused(@NonNull Activity activity) {
        }

        @Override
        public void onActivityStopped(@NonNull Activity activity) {
        }

        @Override
        public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
        }

        @Override
        public void onActivityDestroyed(@NonNull Activity activity) {
        }
    }
}
