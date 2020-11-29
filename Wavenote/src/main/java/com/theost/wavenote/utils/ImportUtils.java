package com.theost.wavenote.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.SpannableString;
import android.webkit.URLUtil;

import com.simperium.client.Bucket;
import com.theost.wavenote.R;
import com.theost.wavenote.models.Note;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class ImportUtils {

    public static final int RESULT_OK = 0;
    public static final int FILE_ERROR = 1;
    public static final int LINK_ERROR = 2;
    public static final int URI_ERROR = 3;
    public static final int DATABASE_ERROR = 4;
    public static final int SAMPLE_ERROR = 5;
    public static final int EXIST_ERROR = 6;
    public static final int PASSWORD_ERROR = 7;

    public static int[] importPlaintext(Context context, Bucket<Note> mNotesBucket, File importFile) {
        int[] importResult = {FILE_ERROR, 0}; // status (failed/success), notes imported (count)
        if (importFile.isDirectory())
            importFile = ImportUtils.findFile(new File(importFile + FileUtils.TEXT_DIR), FileUtils.TEXT_FORMAT);

        if (importFile == null || !importFile.exists()) {
            return importResult;
        }

        try {
            String content = FileUtils.readFile(context, importFile);
            Note note = mNotesBucket.newObject();
            note.setCreationDate(Calendar.getInstance());
            note.setModificationDate(note.getCreationDate());
            note.setContent(new SpannableString(content));
            note.save();
            File noteDirectory = importFile.getParentFile().getParentFile();
            ImportUtils.importMedia(context, noteDirectory, note.getSimperiumKey());
            if (mNotesBucket.containsKey(note.getSimperiumKey())) importResult[1] += 1;
            importResult[0] = RESULT_OK;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return importResult;
    }

    public static int[] importJson(Context context, Bucket<Note> mNotesBucket, File importFile, String importQuantity) {
        int[] importResult = {FILE_ERROR, 0}; // status (failed/success), notes imported (count)
        if (importFile.isDirectory()) {
            String textPath = importFile.getPath();
            if (importQuantity.equals(context.getResources().getString(R.string.import_single)))
                textPath += FileUtils.TEXT_DIR;
            importFile = ImportUtils.findFile(new File(textPath), FileUtils.JSON_FORMAT);
        }

        if (importFile == null || !importFile.exists()) return importResult;

        String notePath = "";
        if (importQuantity.equals(context.getResources().getString(R.string.import_single))) {
            notePath = importFile.getParentFile().getParent();
        } else if (importQuantity.equals(context.getResources().getString(R.string.import_multiple))) {
            notePath = importFile.getParent() + "%s%s";
        }

        try {
            String content = FileUtils.readFile(context, importFile);
            JSONObject sourceArray = new JSONObject(content);
            JSONArray notesArray = sourceArray.getJSONArray(ExportUtils.ACTIVE_NOTES);
            JSONArray trashedArray = sourceArray.getJSONArray(ExportUtils.TRASHED_NOTES);
            List<String> trashedNotes = new ArrayList<>();
            for (int i = 0; i < trashedArray.length(); i++) {
                notesArray.put(trashedArray.getJSONObject(i));
                trashedNotes.add(trashedArray.getJSONObject(i).getString(ExportUtils.NOTE_COLUMN_1));
            }
            for (int i = 0; i < notesArray.length(); i++) {
                JSONObject source = notesArray.getJSONObject(i);
                Note note = mNotesBucket.newObject();
                note.setContent(new SpannableString(HtmlCompat.fromHtml(source.getString(ExportUtils.NOTE_COLUMN_2))));
                note.setCreationDate(DateTimeUtils.getDateCalendar(source.getString(ExportUtils.NOTE_COLUMN_3)));
                note.setModificationDate(DateTimeUtils.getDateCalendar(source.getString(ExportUtils.NOTE_COLUMN_4)));
                note.setTags(ArrayUtils.jsonToList(source.getJSONArray(ExportUtils.NOTE_COLUMN_5)));
                note.setPinned(source.getBoolean(ExportUtils.NOTE_COLUMN_6));
                note.setMarkdownEnabled(source.getBoolean(ExportUtils.NOTE_COLUMN_7));
                note.setSyllableEnabled(source.getBoolean(ExportUtils.NOTE_COLUMN_8));
                note.setDeleted(trashedNotes.contains(source.getString(ExportUtils.NOTE_COLUMN_1)));
                note.save();
                String noteType = FileUtils.ACTIVE_DIR;
                if (trashedNotes.contains(source.getString(ExportUtils.NOTE_COLUMN_1)))
                    noteType = FileUtils.TRASHED_DIR;
                File noteDirectory = new File(String.format(notePath, noteType, StrUtils.formatFilename(note.getTitle())));
                ImportUtils.importMedia(context, noteDirectory, note.getSimperiumKey());
                if (mNotesBucket.containsKey(note.getSimperiumKey())) importResult[1] += 1;
            }
            importResult[0] = RESULT_OK;
        } catch (IOException | JSONException | ParseException e) {
            e.printStackTrace();
        }
        return importResult;
    }

    public static void importMedia(Context context, File sourceDirectory, String note) {
        String[] noteDirs = sourceDirectory.list();
        if (noteDirs == null || noteDirs.length == 0) return;
        List<String> mediaDirs = new ArrayList<>(Arrays.asList(noteDirs));
        mediaDirs.remove(new File(FileUtils.TEXT_DIR).getName());
        if (mediaDirs.size() != 0) {
            File noteDirectory = new File(context.getCacheDir() + FileUtils.NOTES_DIR + note);
            if (mediaDirs.contains(new File(FileUtils.PHOTOS_DIR).getName())) {
                boolean isImported = copyMedia(context, sourceDirectory, noteDirectory, FileUtils.PHOTOS_DIR, note);
            }
            if (mediaDirs.contains(new File(FileUtils.AUDIO_DIR).getName())) {
                boolean isImported = copyMedia(context, sourceDirectory, noteDirectory, FileUtils.AUDIO_DIR, note);
            }
            if (mediaDirs.contains(new File(FileUtils.TRACKS_DIR).getName())) {
                boolean isImported = copyMedia(context, sourceDirectory, noteDirectory, FileUtils.TRACKS_DIR, note);
            }
        }
    }

    public static boolean copyMedia(Context context, File sourceDirectory, File noteDirectory, String mediaDir, String note) {
        DatabaseHelper database = new DatabaseHelper(context);
        File mediaSourceDirectory = new File(sourceDirectory + mediaDir);
        String[] mediaFiles = mediaSourceDirectory.list();
        boolean isImported = true;
        if (mediaFiles != null && mediaFiles.length != 0) {
            File mediaNoteDirectory = new File(noteDirectory + mediaDir);
            for (String i : mediaFiles) {
                File mediaFile = new File(mediaSourceDirectory, i);
                if (!mediaFile.isDirectory()) {
                    try {
                        boolean isCopied = FileUtils.copyFile(mediaFile, mediaNoteDirectory, mediaFile.getName());
                        if (isCopied) {
                            switch (mediaDir) {
                                case FileUtils.PHOTOS_DIR:
                                    database.insertImageData(note, "", mediaNoteDirectory + "/" + mediaFile.getName(),
                                            DateTimeUtils.getDateTextString(context, Calendar.getInstance()));
                                    break;
                                case FileUtils.TRACKS_DIR:
                                    database.insertTrackData(note, "", mediaFile.getName(), (int) new Date().getTime());
                                    break;
                            }
                        } else {
                            isImported = false;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        isImported = false;
                    }
                }
            }
        }
        database.close();
        return isImported;
    }

    public static int importPhoto(File file, Bitmap bitmap) {
        if (bitmap != null) {
            try {
                FileUtils.createPhotoFile(bitmap, file);
                return RESULT_OK;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return FILE_ERROR;
    }

    public static Bitmap getLinkImage(String link) {
        try {
            if (URLUtil.isValidUrl(link)) {
                URL imageUrl = new URL(link);
                Bitmap bitmap = BitmapFactory.decodeStream(imageUrl.openStream());
                if (bitmap != null)
                    return bitmap;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static File findFile(File directory, String extension) {
        String[] files = directory.list();
        if (files != null && files.length != 0) {
            List<String> filesList = new ArrayList<>(Arrays.asList(files));
            String fileName = null;
            for (int i = 0; i < filesList.size(); i++) {
                if (!new File(directory, filesList.get(i)).isDirectory()) {
                    if (StrUtils.getFileExtention(filesList.get(i)).equals(extension))
                        fileName = filesList.get(i);
                } else {
                    filesList.remove(i);
                    i -= 1;
                }
            }
            if (filesList.size() != 0) {
                if (fileName == null) fileName = filesList.get(0);
                return new File(directory, fileName);
            }
        }
        return null;
    }

}
