package com.theost.wavenote;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.simperium.Simperium;
import com.simperium.android.WebSocketManager;
import com.simperium.client.Bucket;
import com.simperium.client.BucketNameInvalid;
import com.simperium.client.ChannelProvider;
import com.theost.wavenote.models.Note;
import com.theost.wavenote.models.NoteCountIndexer;
import com.theost.wavenote.models.NoteTagger;
import com.theost.wavenote.models.Preferences;
import com.theost.wavenote.models.Tag;
import com.theost.wavenote.utils.PrefUtils;
import com.theost.wavenote.utils.SyncWorker;

import org.wordpress.passcodelock.AppLockManager;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Wavenote extends Application implements ChannelProvider.HeartbeatListener {
    public static final String DELETED_NOTE_ID = "deletedNoteId";
    public static final String SELECTED_NOTE_ID = "selectedNoteId";
    public static final String SYNC_TIME_PREFERENCES = "sync_time";
    public static final String TAG = "Simplenote";
    public static final int INTENT_EDIT_NOTE = 2;
    public static final int INTENT_PREFERENCES = 1;
    public static final int ONE_MINUTE_MILLIS = 60 * 1000;  // 60 seconds
    public static final int TEN_SECONDS_MILLIS = 10 * 1000;  // 10 seconds
    public static final int TWENTY_SECONDS_MILLIS = 20 * 1000;  // 20 seconds

    private static final String AUTH_PROVIDER = "simplenote.com";
    private static final String TAG_SYNC = "sync";
    private static final long HEARTBEAT_TIMEOUT =  WebSocketManager.HEARTBEAT_INTERVAL * 2;

    private static Bucket<Preferences> mPreferencesBucket;

    private Bucket<Note> mNotesBucket;
    private Bucket<Tag> mTagsBucket;
    private SyncTimes<Note> mNoteSyncTimes;
    private Handler mHeartbeatHandler;
    private Runnable mHeartbeatRunnable;
    private Simperium mSimperium;
    private boolean mIsInBackground = true;

    public void onCreate() {
        super.onCreate();

        Fresco.initialize(this);
        AppLockManager.getInstance().enableDefaultAppLockIfAvailable(this);

        mSimperium = Simperium.newClient(
                BuildConfig.SIMPERIUM_APP_ID,
                BuildConfig.SIMPERIUM_APP_KEY,
                this
        );

        mSimperium.setAuthProvider(AUTH_PROVIDER);
        mSimperium.addHeartbeatListener(this);

        mHeartbeatHandler = new Handler();
        mHeartbeatRunnable = () -> {
            mHeartbeatHandler.removeCallbacks(mHeartbeatRunnable);
            mHeartbeatHandler.postDelayed(mHeartbeatRunnable, HEARTBEAT_TIMEOUT);
        };

        SyncTimePersister syncTimePersister = new SyncTimePersister();
        mNoteSyncTimes = new SyncTimes<>(syncTimePersister.load());
        mNoteSyncTimes.addListener(syncTimePersister);

        try {
            mNotesBucket = mSimperium.bucket(new Note.Schema());
            mNotesBucket.addListener(mNoteSyncTimes.bucketListener);
            Tag.Schema tagSchema = new Tag.Schema();
            tagSchema.addIndex(new NoteCountIndexer(mNotesBucket));
            mTagsBucket = mSimperium.bucket(tagSchema);
            mPreferencesBucket = mSimperium.bucket(new Preferences.Schema());
            // Every time a note changes or is deleted we need to reindex the tag counts
            mNotesBucket.addListener(new NoteTagger(mTagsBucket));
        } catch (BucketNameInvalid e) {
            throw new RuntimeException("Could not create bucket", e);
        }

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

        ApplicationLifecycleMonitor applicationLifecycleMonitor = new ApplicationLifecycleMonitor();
        registerComponentCallbacks(applicationLifecycleMonitor);
        registerActivityLifecycleCallbacks(applicationLifecycleMonitor);
    }

    @Override
    public void onBeat() {
        mHeartbeatHandler.removeCallbacks(mHeartbeatRunnable);
        mHeartbeatHandler.postDelayed(mHeartbeatRunnable, HEARTBEAT_TIMEOUT);
    }

    private boolean isFirstLaunch() {
        // NotesActivity sets this pref to false after first launch
        return PrefUtils.getBoolPref(this, PrefUtils.PREF_FIRST_LAUNCH, true);
    }

    private String getAccountInfo() {
        String email = "Email: " + (mSimperium != null && mSimperium.getUser() != null ? mSimperium.getUser().getEmail() : "?");
        String notes = "Notes: " + (mNotesBucket != null ? mNotesBucket.count() : "?");
        String tags = "Tags: " + (mTagsBucket != null ? mTagsBucket.count() : "?");
        return email + "\n" + notes + "\n" + tags + "\n\n";
    }

    private String getDeviceInfo() {
        String architecture = Build.DEVICE != null && Build.DEVICE.matches(".+_cheets|cheets_.+") ? "Chrome OS " : "Android ";
        String device = "Device: " + Build.MANUFACTURER + " " + Build.MODEL + " (" + Build.DEVICE + ")";
        String system = "System: " + architecture + Build.VERSION.RELEASE + " (" + Build.VERSION.SDK_INT + ")";
        String app = "App: Simplenote " + PrefUtils.versionInfo();
        return device + "\n" + system + "\n" + app + "\n\n";
    }

    public Simperium getSimperium() {
        return mSimperium;
    }

    public Bucket<Note> getNotesBucket() {
        return mNotesBucket;
    }

    public SyncTimes getNoteSyncTimes() {
        return mNoteSyncTimes;
    }

    public Bucket<Tag> getTagsBucket() {
        return mTagsBucket;
    }

    public Bucket<Preferences> getPreferencesBucket() {
        return mPreferencesBucket;
    }

    public boolean isInBackground() {
        return mIsInBackground;
    }

    private class ApplicationLifecycleMonitor implements Application.ActivityLifecycleCallbacks, ComponentCallbacks2 {
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

                PeriodicWorkRequest syncWorkRequest = new PeriodicWorkRequest.Builder(
                        SyncWorker.class,
                        PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS,
                        TimeUnit.MILLISECONDS
                )
                        .setConstraints(new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                        .setBackoffCriteria(BackoffPolicy.LINEAR, ONE_MINUTE_MILLIS, TimeUnit.MILLISECONDS)
                        .setInitialDelay(TWENTY_SECONDS_MILLIS, TimeUnit.MILLISECONDS)
                        .addTag(TAG_SYNC)
                        .build();
                WorkManager.getInstance(getApplicationContext()).enqueueUniquePeriodicWork(
                        TAG_SYNC,
                        ExistingPeriodicWorkPolicy.REPLACE,
                        syncWorkRequest
                );
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

        // ActivityLifecycleCallbacks
        @SuppressLint("LongLogTag")
        @Override
        public void onActivityResumed(@NonNull Activity activity) {
            if (mIsInBackground) {
                mIsInBackground = false;
                WorkManager.getInstance(getApplicationContext()).cancelUniqueWork(TAG_SYNC);
            }
            mPreferencesBucket.start();
            mNotesBucket.start();
            mTagsBucket.start();
        }

        @Override
        public void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) {
        }

        @Override
        public void onActivityStarted(@NonNull Activity activity) {
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

    private class SyncTimePersister implements SyncTimes.SyncTimeListener {
        private final SharedPreferences mPreferences;

        public SyncTimePersister() {
            mPreferences = getSharedPreferences(SYNC_TIME_PREFERENCES, Context.MODE_PRIVATE);
        }

        public HashMap<String, Calendar> load() {
            HashMap<String, Calendar> syncTimes = new HashMap<>();

            //noinspection unchecked
            for (Map.Entry<String, Long> syncTime : ((Map<String, Long>) mPreferences.getAll()).entrySet()) {
                Calendar instant = Calendar.getInstance();
                instant.setTimeInMillis(syncTime.getValue());
                syncTimes.put(syncTime.getKey(), instant);
            }

            return syncTimes;
        }

        @Override
        public void onRemove(String entityId) {
            mPreferences.edit().remove(entityId).apply();
        }

        @Override
        public void onUpdate(String entityId, Calendar lastSyncTime, boolean isSynced) {
            if (lastSyncTime != null) {
                mPreferences.edit().putLong(entityId, lastSyncTime.getTimeInMillis()).apply();
            }
        }
    }
}