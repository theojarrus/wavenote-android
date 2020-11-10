package com.theost.wavenote.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

import com.facebook.common.internal.ByteStreams;
import com.theost.wavenote.R;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.EncryptionMethod;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.UUID;

import static com.theost.wavenote.utils.ResUtils.getResId;

public class FileUtils {

    private static final String DEFAULT_EXPORT_DIR = "Documents/Wavenote";
    private static final String PRIMARY_VOLUME_NAME = "primary";

    public static final String METRONOME_DIR = "/Metronome/";
    public static final String NOTES_DIR = "/Notes/";
    public static final String PHOTOS_DIR = "/Photo/";
    public static final String AUDIO_DIR = "/Audio/";
    public static final String TRACKS_DIR = "/Tracks/";
    public static final String TEXT_DIR = "/Text/";
    public static final String ACTIVE_DIR = "/Active/";
    public static final String TRASHED_DIR = "/Trashed/";

    public static final String TEMP_DIR = "/temp/";
    public static final String TMIX_DIR = "/mix/";
    public static final String TFILES_DIR = "/files/";
    public static final String TTRACKS_DIR = "/tracks/";

    public static final String TEXT_FORMAT = ".txt";
    public static final String HTML_FORMAT = ".htm";
    public static final String JSON_FORMAT = ".json";
    public static final String ZIP_FORMAT = ".zip";
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

    public static File createPhotoFile(Context context, String noteId) {
        String path = NOTES_DIR + noteId + PHOTOS_DIR;
        return getCacheFile(context, path, UUID.randomUUID().toString(), PHOTO_FORMAT);
    }

    public static File getCacheFile(Context context, String dir, String name, String format) {
        int fileId = 0;
        File directory = new File(context.getCacheDir() + dir);
        if (!directory.exists()) directory.mkdirs();
        while (true) {
            String path = "/" + name;
            if (fileId > 0) path += "_" + fileId;
            File file = new File(directory.getPath(), path + format);
            if (!file.exists()) {
                return file;
            }
            fileId += 1;
        }
    }

    public static void createPhotoFile(Bitmap imageBitmap, File file) throws IOException {
        FileOutputStream out = new FileOutputStream(file);
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
        out.flush();
        out.close();
    }

    public static void createZip(File file, File directorySource) throws ZipException {
        ZipFile zipFile = new ZipFile(file);
        zipFile.addFolder(directorySource);
    }

    public static void createZipEncrypted(File file, File directorySource, String password) throws ZipException {
        ZipParameters zipParameters = new ZipParameters();
        zipParameters.setEncryptFiles(true);
        zipParameters.setEncryptionMethod(EncryptionMethod.AES);
        ZipFile zipFile = new ZipFile(file, password.toCharArray());
        zipFile.addFolder(directorySource, zipParameters);
    }

    public static File createTempFile(Context context, InputStream in, String format) throws IOException {
        File directory = new File(context.getCacheDir() + TEMP_DIR, FileUtils.TFILES_DIR);
        if (!directory.exists()) directory.mkdirs();
        File tempFile = File.createTempFile(PREFIX, format, directory);
        FileOutputStream out = new FileOutputStream(tempFile);
        ByteStreams.copy(in, out);
        tempFile.deleteOnExit();
        return tempFile;
    }

    public static boolean copyFile(File sourceFile, File directory, String fileName) throws IOException {
        if (!sourceFile.exists()) return false;
        if (!directory.exists()) directory.mkdirs();
        File fileNew = new File(directory, fileName);
        if (sourceFile.isDirectory())
            sourceFile = new File(sourceFile, fileName);
        writeFile(sourceFile, fileNew);
        return true;
    }

    public static String readFile(Context context, File file) throws IOException {
        StringBuilder text = new StringBuilder();
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;
        while ((line = br.readLine()) != null) {
            text.append(line);
            text.append('\n');
        }
        br.close();
        return text.toString();
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

    public static void removeDirectory(File directory) {
        if (directory.exists() && directory.isDirectory()) {
            String[] files = directory.list();
            if (files != null && files.length != 0) {
                for (String i : files) {
                    File file = new File(directory.getPath() + "/" + i);
                    if (file.isDirectory()) removeDirectory(file);
                    file.delete();
                }
            }
            directory.delete();
        }
    }

    public static boolean verifyZip(ZipFile zipFile) {
        try {
            List<FileHeader> fileHeaders = zipFile.getFileHeaders();
            for (FileHeader fileHeader : fileHeaders) {
                InputStream is = zipFile.getInputStream(fileHeader);
                byte[] b = new byte[4 * 4096];
                while (is.read(b) != -1) {
                    //Do nothing as we just want to verify password
                }
                is.close();
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static String getDefaultDir(Context context) {
        File dir = new File(getStorageDir(context), DEFAULT_EXPORT_DIR);
        if (!dir.exists())
            dir.mkdir();
        return dir.getAbsolutePath();
    }

    public static File getStorageDir(Context context) {
        return context.getExternalFilesDir("").getParentFile().getParentFile().getParentFile().getParentFile();
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
