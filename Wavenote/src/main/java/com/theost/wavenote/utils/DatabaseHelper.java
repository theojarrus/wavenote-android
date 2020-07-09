package com.theost.wavenote.utils;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "wavenote-local.db";

    public static final String NAME_IMAGES_TABLE = "images_table";
    public static final String NAME_DICTIONARY_TABLE = "dictionary_table";
    public static final String NAME_METRONOME_TABLE = "metronome_table";
    public static final String NAME_AUDIO_TABLE = "audio_table";
    public static final String NAME_TRACKS_TABLE = "tracks_table";

    public static final String COL_0 = "ID";

    public static final String COL_1_IMAGES = "NOTE_ID";
    public static final String COL_2_IMAGES = "NAME";
    public static final String COL_3_IMAGES = "URI";
    public static final String COL_4_IMAGES = "DATE";

    public static final String COL_1_DICTIONARY = "WORD";
    public static final String COL_2_DICTIONARY = "TYPE";

    public static final String COL_1_METRONOME = "SOUND";

    public static final String COL_1_AUDIO = "NOTE_ID";
    public static final String COL_2_AUDIO = "TUNE";
    public static final String COL_3_AUDIO = "BEAT";
    public static final String COL_4_AUDIO = "SPEED";

    public static final String COL_1_TRACKS = "NOTE_ID";
    public static final String COL_2_TRACKS = "NAME";
    public static final String COL_3_TRACKS = "URI";

    public DatabaseHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + NAME_IMAGES_TABLE + " (" + COL_0 + " INTEGER PRIMARY KEY, " + COL_1_IMAGES
                + " TEXT NOT NULL, " + COL_2_IMAGES + " TEXT, " + COL_3_IMAGES + " TEXT NOT NULL, " + COL_4_IMAGES + " TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + NAME_DICTIONARY_TABLE + " (" + COL_0 + " INTEGER PRIMARY KEY, " + COL_1_DICTIONARY
                + " TEXT NOT NULL, " + COL_2_DICTIONARY + " TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + NAME_METRONOME_TABLE + " (" + COL_0 + " INTEGER PRIMARY KEY, " + COL_1_METRONOME
                + " TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + NAME_AUDIO_TABLE + " (" + COL_1_AUDIO + " TEXT NOT NULL, " + COL_2_AUDIO
                + " TEXT, " + COL_3_AUDIO + " TEXT, " + COL_4_AUDIO + " INTEGER)");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + NAME_TRACKS_TABLE + " (" + COL_0 + " INTEGER PRIMARY KEY, " + COL_1_TRACKS + " TEXT NOT NULL, "
                + COL_2_TRACKS + " TEXT NOT NULL, " + COL_3_TRACKS + " TEXT NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + NAME_IMAGES_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + NAME_DICTIONARY_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + NAME_METRONOME_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + NAME_AUDIO_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + NAME_TRACKS_TABLE);
        onCreate(db);
    }

    @Override
    protected void finalize() throws Throwable {
        this.close();
        super.finalize();
    }

    public int insertImageData(String note, String name, String uri, String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_1_IMAGES, note);
        contentValues.put(COL_2_IMAGES, name);
        contentValues.put(COL_3_IMAGES, uri);
        contentValues.put(COL_4_IMAGES, date);
        return (int) db.insert(NAME_IMAGES_TABLE, null, contentValues);
    }

    public Cursor getImageData(String note) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor result;
        try {
            result = db.rawQuery("SELECT * FROM " + NAME_IMAGES_TABLE
                    + " WHERE " + COL_1_IMAGES + "='" + note + "'", null);
        } catch (Exception e) {
            e.printStackTrace();
            result = null;
        }
        return result;
    }

    @SuppressLint("Recycle")
    public String getImageUri(String id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor result;
        String uri;
        try {
            result = db.rawQuery("SELECT " + COL_3_IMAGES + " FROM " + NAME_IMAGES_TABLE
                    + " WHERE " + COL_0 + "='" + id + "'", null);
        } catch (Exception e) {
            e.printStackTrace();
            result = null;
        }
        if (result.getCount() != 0) {
            result.moveToFirst();
            return result.getString(0);
        }
        return null;
    }

    public boolean renameImageData(String id, String name) {
        SQLiteDatabase db = this.getReadableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_2_IMAGES, name);
        long result = db.update(NAME_IMAGES_TABLE, contentValues, COL_0 + "=" + id, null);
        return result != -1;
    }

    public void removeImageData(String id) {
        SQLiteDatabase db = this.getReadableDatabase();
        long result = db.delete(NAME_IMAGES_TABLE, COL_0 + "=" + id, null);
    }

    public void removeAllImageData(String note) {
        SQLiteDatabase db = this.getReadableDatabase();
        long result = db.delete(NAME_IMAGES_TABLE, COL_1_IMAGES + "='" + note + "'", null);
    }

    public boolean insertDictionaryData(String keyword, String type) {
        SQLiteDatabase db = this.getReadableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_1_DICTIONARY, keyword);
        contentValues.put(COL_2_DICTIONARY, type);
        long result = db.insert(NAME_DICTIONARY_TABLE, null, contentValues);
        return result != -1;
    }

    public Cursor getDictionaryData(String type) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT " + COL_1_DICTIONARY + " FROM " + NAME_DICTIONARY_TABLE
                + " WHERE " + COL_2_DICTIONARY + "='" + type + "'", null);
    }

    public Cursor getAllDictionaryData() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + NAME_DICTIONARY_TABLE, null);
    }

    public boolean renameDictionaryData(String id, String type) {
        SQLiteDatabase db = this.getReadableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_2_DICTIONARY, type);
        long result = db.update(NAME_DICTIONARY_TABLE, contentValues, COL_0 + "=" + id, null);
        return result != -1;
    }

    public boolean removeDictionaryData(String id) {
        SQLiteDatabase db = this.getReadableDatabase();
        long result = db.delete(NAME_DICTIONARY_TABLE, COL_0 + "=" + id, null);
        return result != -1;
    }

    public boolean insertMetronomeData(String sound) {
        SQLiteDatabase db = this.getReadableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_1_METRONOME, sound);
        long result = db.insert(NAME_METRONOME_TABLE, null, contentValues);
        return result != -1;
    }

    public Cursor getMetronomeData() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + NAME_METRONOME_TABLE, null);
    }

    public boolean removeMetronomeData(String id) {
        SQLiteDatabase db = this.getReadableDatabase();
        long result = db.delete(NAME_METRONOME_TABLE, COL_0 + "=" + id, null);
        return result != -1;
    }

    public boolean insertAudioData(String note, String tune, String beat, int speed) {
        SQLiteDatabase db = this.getReadableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_1_AUDIO, note);
        contentValues.put(COL_2_AUDIO, tune);
        contentValues.put(COL_3_AUDIO, beat);
        contentValues.put(COL_4_AUDIO, speed);
        long result = db.insert(NAME_AUDIO_TABLE, null, contentValues);
        return result != -1;
    }

    public boolean renameAudioData(String note, String tune, String beat, int speed) {
        SQLiteDatabase db = this.getReadableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_2_AUDIO, tune);
        contentValues.put(COL_3_AUDIO, beat);
        contentValues.put(COL_4_AUDIO, speed);
        long result = db.update(NAME_AUDIO_TABLE, contentValues, COL_1_AUDIO + "='" + note + "'", null);
        return result != -1;
    }
    
    public Cursor getAudioData(String note) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + NAME_AUDIO_TABLE + " WHERE " + COL_1_AUDIO + "='" + note + "'", null);
    }

    public int insertTrackData(String note, String name, String uri, int date) {
        SQLiteDatabase db = this.getReadableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_1_TRACKS, note);
        contentValues.put(COL_2_TRACKS, name);
        contentValues.put(COL_3_TRACKS, uri);
        return (int) db.insert(NAME_TRACKS_TABLE, null, contentValues);
    }

    public boolean renameTrackData(String id, String name) {
        SQLiteDatabase db = this.getReadableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_2_TRACKS, name);
        long result = db.update(NAME_TRACKS_TABLE, contentValues, COL_0 + "=" + id, null);
        return result != -1;
    }

    public Cursor getTrackData(String note) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + NAME_TRACKS_TABLE + " WHERE " + COL_1_TRACKS + "='" + note + "'", null);
    }

    public boolean removeTrackData(String id) {
        SQLiteDatabase db = this.getReadableDatabase();
        long result = db.delete(NAME_TRACKS_TABLE, COL_0 + "=" + id, null);
        return result != -1;
    }

}
