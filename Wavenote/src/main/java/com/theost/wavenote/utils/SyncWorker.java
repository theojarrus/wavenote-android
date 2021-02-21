package com.theost.wavenote.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;

import com.google.common.util.concurrent.ListenableFuture;
import com.simperium.client.Bucket;
import com.theost.wavenote.Wavenote;
import com.theost.wavenote.models.Note;
import com.theost.wavenote.models.Preferences;
import com.theost.wavenote.models.Tag;

import static com.theost.wavenote.Wavenote.TEN_SECONDS_MILLIS;

public class SyncWorker extends ListenableWorker {
    private final Bucket<Note> mBucketNote;
    private final Bucket<Preferences> mBucketPreference;
    private final Bucket<Tag> mBucketTag;

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        Wavenote application = (Wavenote) context.getApplicationContext();
        mBucketNote = application.getNotesBucket();
        mBucketTag = application.getTagsBucket();
        mBucketPreference = application.getPreferencesBucket();
    }

    @Override
    public void onStopped() {
        super.onStopped();
        stopBuckets();
    }

    @NonNull
    @Override
    public ListenableFuture<Result> startWork() {
        return CallbackToFutureAdapter.getFuture(
                completer -> {
                    if (mBucketNote != null) {
                        mBucketNote.start();
                    }

                    if (mBucketTag != null) {
                        mBucketTag.start();
                    }

                    if (mBucketPreference != null) {
                        mBucketPreference.start();
                    }

                    new Handler(Looper.getMainLooper()).postDelayed(
                            () -> {
                                stopBuckets();
                                completer.set(Result.success());
                            },
                            TEN_SECONDS_MILLIS
                    );

                    return null;
                }
        );
    }

    private void stopBuckets() {
        if (((Wavenote) getApplicationContext()).isInBackground()) {
            if (mBucketNote != null) {
                mBucketNote.stop();
            }

            if (mBucketTag != null) {
                mBucketTag.stop();
            }

            if (mBucketPreference != null) {
                mBucketPreference.stop();
            }
        }
    }
}