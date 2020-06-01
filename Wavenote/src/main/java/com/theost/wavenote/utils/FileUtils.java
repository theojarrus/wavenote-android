package com.theost.wavenote.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;

import com.google.android.gms.common.util.IOUtils;
import com.theost.wavenote.R;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;

public class FileUtils {

    private static final String DEFAULT_EXPORT_DIR = "Wavenote";
    private static final String PRIMARY_VOLUME_NAME = "primary";

    public static final String METRONOME_DIR = "/Metronome/";
    public static final String TEMP_DIR = "/Temp/";
    public static final String NOTES_DIR = "/Notes/";
    public static final String PHOTOS_DIR = "/Photo/";
    public static final String AUDIO_DIR = "/Audio/";
    public static final String TEXT_DIR = "/Text/";

    public static final String PHOTO_FORMAT = ".jpg";
    public static final String AUDIO_FORMAT = ".mp3";
    public static final String SAMPLE_FORMAT = ".wav";

    public static final String BEAT_PREFIX = "beat_";
    public static final String BEAT_STEREO = "_stereo";
    public static final String BEAT_STRONG = "_strong";
    public static final String BEAT_WEAK = "_weak";

    private static final String PREFIX = "tmp_";

    public static void createFile(File dir, String fileName, String content) throws IOException {
        if (!dir.exists()) dir.mkdirs();
        File file = new File(dir, fileName);
        if (!file.exists()) file.createNewFile();
        FileWriter writer = new FileWriter(file);
        writer.append(content);
        writer.flush();
        writer.close();
    }

    public static File createNoteFile(Context context, String noteId, String type) {
        File file;
        String path = NOTES_DIR + noteId;
        String format;
        switch (type) {
            case "photo":
                path += PHOTOS_DIR;
                format = PHOTO_FORMAT;
                break;
            case "audio":
                path += AUDIO_DIR;
                format = AUDIO_FORMAT;
                break;
            default:
                return null;
        }
        return getCacheFile(context, path, type, format);
    }

    public static File getCacheFile(Context context, String dir, String name, String format) {
        int fileId = 0;
        File directory = new File(context.getCacheDir() + dir);
        if (!directory.exists()) directory.mkdirs();
        while (true) {
            fileId += 1;
            File file = new File(directory.getPath(), "/" + name + "_" + fileId + format);
            if (!file.exists()) {
                return file;
            }
        }
    }

    public static void createPhotoFile(Bitmap imageBitmap, File file) throws IOException {
        FileOutputStream out = new FileOutputStream(file);
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
        out.flush();
        out.close();
    }

    public static File createTempFile(Context context, InputStream in, String format) throws IOException {
        File directory = new File(context.getCacheDir() + TEMP_DIR);
        if (!directory.exists()) directory.mkdirs();
        File tempFile = File.createTempFile(PREFIX, format, directory);
        FileOutputStream out = new FileOutputStream(tempFile);
        IOUtils.copyStream(in, out);
        tempFile.deleteOnExit();
        return tempFile;
    }

    public static boolean copyFile(File dirOld, File dirNew, String fileName) throws IOException {
        if (!dirOld.exists()) return false;
        if (!dirNew.exists()) dirNew.mkdirs();
        File fileOld = new File(dirOld + "/" + fileName);
        File fileNew = new File(dirNew + "/" + fileName);
        writeFile(fileOld, fileNew);
        return true;
    }

    public static void writeFile(File fileOld, File fileNew) throws IOException {
        fileNew.createNewFile();
        InputStream in = new FileInputStream(fileOld);
        OutputStream out = new FileOutputStream(fileNew);
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
    }

    public static boolean removeFile(String path) {
        File file = new File(path);
        if (file.exists())
            return file.delete();
        return false;
    }

    public static boolean removeFiles(File notedir) {
        if (notedir.exists() && notedir.isDirectory()) {
            String[] subdirs = notedir.list();
            for (String i : subdirs) {
                File dir = new File(notedir.getPath() + "/" + i);
                String[] files = dir.list();
                for (String file : files) new File(dir + "/", file).delete();
                dir.delete();
            }
            return notedir.delete();
        }
        return false;
    }

    public static String getDefaultDir(Context context) {
        File dir = new File(Environment.getExternalStorageDirectory(), DEFAULT_EXPORT_DIR);
        if (!dir.exists())
            dir.mkdirs();
        return dir.getAbsolutePath();
    }

    public static int getResId(String resName, Class<?> c) {
        try {
            Field idField = c.getDeclaredField(resName);
            return idField.getInt(idField);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static File[] getAllSampleFiles(Context context, String name) {
        File[] files = new File[4];
        String path = context.getCacheDir() + FileUtils.METRONOME_DIR + FileUtils.BEAT_PREFIX + name.toLowerCase() + "%s%s" + FileUtils.SAMPLE_FORMAT;
        files[0] = new File(String.format(path, FileUtils.BEAT_STRONG, "")); // _strong
        files[1] = new File(String.format(path, FileUtils.BEAT_WEAK, "")); // _weak
        files[2] = new File(String.format(path, FileUtils.BEAT_STRONG, FileUtils.BEAT_STEREO)); // _strong_stereo
        files[3] = new File(String.format(path, FileUtils.BEAT_WEAK, FileUtils.BEAT_STEREO)); // _weak_stereo
        return files;
    }

    public static File[] getSampleFiles(Context context, String name, String soundType) {
        String path = BEAT_PREFIX + name.toLowerCase() + soundType + "%s" + SAMPLE_FORMAT;
        File[] files = new File[2];
        File directory = new File(context.getCacheDir() + METRONOME_DIR);
        if (!directory.exists()) directory.mkdirs();
        files[0] = new File(directory, String.format(path, ""));
        files[1] = new File(directory, String.format(path, BEAT_STEREO));
        if (files[0].exists() || files[1].exists()) return null;
        return files;
    }

    public static void createWavFile(File file, byte[] audioBytes) throws IOException {
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
        bos.write(audioBytes);
        bos.flush();
        bos.close();
    }

    public static byte[][] getStereoBeatResource(Context context, String sound) throws IOException {
        String beatId = BEAT_PREFIX + sound.toLowerCase() + "%s" + BEAT_STEREO;
        int strongBeatId = getResId(String.format(beatId, BEAT_STRONG), R.raw.class);
        int weakBeatId = getResId(String.format(beatId, BEAT_WEAK), R.raw.class);
        byte[][] beatSoundData = new byte[2][];
        beatSoundData[0] = AudioUtils.readWavData(context.getResources().openRawResource(strongBeatId));
        beatSoundData[1] = AudioUtils.readWavData(context.getResources().openRawResource(weakBeatId));
        return beatSoundData;
    }

    public static byte[][] getStereoBeatCustom(Context context, String sound) throws IOException {
        InputStream inputStreamStrong = context.getContentResolver().openInputStream(Uri.fromFile(new File(context.getCacheDir()
                + METRONOME_DIR + BEAT_PREFIX + sound.toLowerCase() + BEAT_STRONG + BEAT_STEREO + SAMPLE_FORMAT)));
        InputStream inputStreamWeak = context.getContentResolver().openInputStream(Uri.fromFile(new File(context.getCacheDir()
                + METRONOME_DIR + BEAT_PREFIX + sound.toLowerCase() + BEAT_WEAK + BEAT_STEREO + SAMPLE_FORMAT)));
        byte[][] beatSoundData = new byte[2][];
        beatSoundData[0] = AudioUtils.readWavData(inputStreamStrong);
        beatSoundData[1] = AudioUtils.readWavData(inputStreamWeak);
        return beatSoundData;
    }

}
