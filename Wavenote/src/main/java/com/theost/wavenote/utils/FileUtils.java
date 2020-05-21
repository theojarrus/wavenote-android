package com.theost.wavenote.utils;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileUtils {

    private static final String DEFAULT_EXPORT_DIR = "/Wavenote";
    private static final String PRIMARY_VOLUME_NAME = "primary";

    private static int BUFFER_SIZE = 6 * 1024;

    public static final String NOTES_DIR = "/Notes/";
    public static final String PHOTOS_DIR = "/Photo/";
    public static final String AUDIO_DIR = "/Audio/";
    public static final String TEXT_DIR = "/Text/";

    public static void createFile(File dir, String fileName, String content) throws IOException {
        if (!dir.exists()) dir.mkdirs();
        File file = new File(dir, fileName);
        FileWriter writer = new FileWriter(file);
        writer.append(content);
        writer.flush();
        writer.close();
    }

    public static void copyFile(File dirOld, File dirNew, String fileName) throws IOException {
        if (!dirNew.exists()) dirNew.mkdirs();
        File fileOld = new File(dirOld + "/" + fileName);
        File fileNew = new File(dirNew + "/" + fileName);
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
        File dir = new File(context.getExternalFilesDir(null).getAbsolutePath() + DEFAULT_EXPORT_DIR);
        if (!dir.exists())
            dir.mkdirs();
        return dir.getAbsolutePath();
    }

}
