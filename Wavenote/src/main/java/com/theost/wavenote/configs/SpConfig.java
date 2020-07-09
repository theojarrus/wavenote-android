package com.theost.wavenote.configs;

import android.content.Context;
import android.content.SharedPreferences;

import com.theost.wavenote.BuildConfig;

public class SpConfig {

    private static final String SP_FILE_NAME = String.format("%s_config", BuildConfig.APPLICATION_ID);

    private static final String KEY_RECORD_ADJUST_BYTES_LEN = "record_adjust_bytes_len";

    private SharedPreferences mSp;

    public SpConfig(Context context) {
        mSp = context.getSharedPreferences(SP_FILE_NAME, Context.MODE_PRIVATE);
    }

    public static SpConfig sp(Context context) {
        return new SpConfig(context);
    }

    public void saveRecordAdjustLen(int len) {
        mSp.edit().putInt(KEY_RECORD_ADJUST_BYTES_LEN,len).apply();
    }

    public int getRecordAdjustLen() {
        return mSp.getInt(KEY_RECORD_ADJUST_BYTES_LEN, 0);
    }

}
