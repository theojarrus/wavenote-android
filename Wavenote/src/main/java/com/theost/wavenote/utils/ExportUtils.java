package com.theost.wavenote.utils;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.text.Spannable;

import com.simperium.client.BucketObjectMissingException;
import com.theost.wavenote.R;
import com.theost.wavenote.Wavenote;
import com.theost.wavenote.models.Note;

import net.lingala.zip4j.exception.ZipException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class ExportUtils {

    public static final String NOTE_COLUMN_1 = "id";
    public static final String NOTE_COLUMN_2 = "content";
    public static final String NOTE_COLUMN_3 = "creationDate";
    public static final String NOTE_COLUMN_4 = "lastModified";
    public static final String NOTE_COLUMN_5 = "tags";
    public static final String NOTE_COLUMN_6 = "pinned";
    public static final String NOTE_COLUMN_7 = "markdown";

    public static final String ACTIVE_NOTES = "activeNotes";
    public static final String TRASHED_NOTES = "trashedNotes";

    public static String getNotesJson(Context context, ArrayList<Note> notes) {
        JSONArray activeArray = new JSONArray();
        JSONArray trashedArray = new JSONArray();

        for (Note note : notes) {
            File directorySource = new File(context.getCacheDir() + FileUtils.NOTES_DIR + note.getSimperiumKey());
            try {
                note.getPublishedUrl();
                JSONObject noteObject = new JSONObject();
                noteObject.put(NOTE_COLUMN_1, note.getSimperiumKey());
                noteObject.put(NOTE_COLUMN_2, HtmlCompat.toHtml(note.getContent()));
                noteObject.put(NOTE_COLUMN_3, DateTimeUtils.getDateTextJson(note.getCreationDate()));
                noteObject.put(NOTE_COLUMN_4, DateTimeUtils.getDateTextJson(note.getModificationDate()));
                noteObject.put(NOTE_COLUMN_5, new JSONArray(note.getTags()));
                noteObject.put(NOTE_COLUMN_6, note.isPinned());
                noteObject.put(NOTE_COLUMN_7, note.isMarkdownEnabled());
                if (note.isDeleted()) {
                    trashedArray.put(noteObject);
                } else {
                    activeArray.put(noteObject);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        JSONObject notesArray = new JSONObject();
        try {
            notesArray.put(ACTIVE_NOTES, activeArray);
            notesArray.put(TRASHED_NOTES, trashedArray);
            return notesArray.toString(4);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static HashMap<String, Boolean> exportNote(Context context, Note note, String path, List<CharSequence> modes, String password) {
        HashMap<String, Boolean> resultMap = new HashMap<>();

        Spannable content = note.getContent();
        String noteName = StrUtils.formatFilename(note.getTitle());
        if (noteName.equals(""))
            noteName = context.getResources().getString(R.string.note) + " - " + note.getSimperiumKey();

        File directorySource = new File(context.getCacheDir() + FileUtils.NOTES_DIR + note.getSimperiumKey());
        File directoryExport = new File(path, noteName);

        if (directoryExport.exists()) FileUtils.removeDirectory(directoryExport);

        String textMode = context.getString(R.string.text);
        String htmlMode = context.getString(R.string.html);
        String jsonMode = context.getString(R.string.json);
        String photoMode = context.getString(R.string.photo);
        String zipMode = context.getString(R.string.zip);

        if (modes.contains(textMode)) {
            boolean isExported = ExportUtils.exportText(new File(directoryExport + FileUtils.TEXT_DIR), noteName, content.toString(), FileUtils.TEXT_FORMAT);
            resultMap.put(textMode, isExported);
        }

        if (modes.contains(htmlMode)) {
            boolean isExported = ExportUtils.exportText(new File(directoryExport + FileUtils.TEXT_DIR), noteName, HtmlCompat.toHtml(content), FileUtils.HTML_FORMAT);
            resultMap.put(htmlMode, isExported);
        }

        if (modes.contains(jsonMode)) {
            ArrayList<Note> noteList = new ArrayList<>();
            noteList.add(note);
            boolean isExported = ExportUtils.exportText(new File(directoryExport + FileUtils.TEXT_DIR), noteName, getNotesJson(context, noteList), FileUtils.JSON_FORMAT);
            resultMap.put(jsonMode, isExported);
        }

        if (modes.contains(photoMode)) {
            boolean isExported = ExportUtils.exportPhoto(directorySource, new File(directoryExport + FileUtils.PHOTOS_DIR));
            resultMap.put(photoMode, isExported);
        }

        if (modes.contains(zipMode)) {
            boolean isExported = ExportUtils.exportZip(directoryExport, password);
            resultMap.put(zipMode, isExported);
        }

        return resultMap;
    }

    public static boolean exportNotes(Activity context, String path, List<CharSequence> modes, String password) {
        String photoMode = context.getResources().getString(R.string.photo); // some notes may be without photos
        String zipMode = context.getResources().getString(R.string.zip);
        boolean isExported = true; // error flag
        boolean isZip = modes.contains(zipMode);
        if (isZip) modes.remove(zipMode);

        File directoryExport = new File(path);
        if (directoryExport.exists()) FileUtils.removeDirectory(directoryExport);

        Wavenote application = (Wavenote) context.getApplication();
        Cursor notes = application.getNotesBucket().allObjects();
        ArrayList<Note> notesList = new ArrayList<>();
        while (notes.moveToNext()) {
            try {
                Note note = application.getNotesBucket().get(notes.getString(2));
                notesList.add(note);
            } catch (BucketObjectMissingException e) {
                e.printStackTrace();
                isExported = false;
            }
        }

        String notesJson = ExportUtils.getNotesJson(context, notesList);
        String jsonName = context.getResources().getString(R.string.notes);

        boolean isCreatedJson = ExportUtils.exportText(directoryExport, jsonName, notesJson, FileUtils.JSON_FORMAT);
        if (!isCreatedJson) isExported = false; // be careful to not clear previous error
        String statusDir;
        for (Note note : notesList) {
            if (note.isDeleted()) {
                statusDir = FileUtils.TRASHED_DIR;
            } else {
                statusDir = FileUtils.ACTIVE_DIR;
            }
            HashMap<String, Boolean> resultMap = exportNote(context, note, path + statusDir, modes, password);
            resultMap.remove(photoMode);
            if (resultMap.containsValue(false)) isExported = false;
        }

        if (isZip) {
            boolean isCreatedZip = ExportUtils.exportZip(directoryExport, password);
            if (!isCreatedZip) isExported = false; // be careful to not clear previous error
        }

        return isExported;
    }

    public static boolean exportText(File directory, String name, String content, String extension) {
        try {
            FileUtils.createFile(directory, name + extension, content.trim());
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean exportPhoto(File directorySource, File directoryExport) {
        File dirPhotoSource = new File(directorySource + FileUtils.PHOTOS_DIR);
        try {
            ArrayList<Boolean> copiedFiles = new ArrayList<>();
            String[] photos = dirPhotoSource.list();
            if (photos == null || photos.length == 0) return false;
            for (String j : photos) {
                copiedFiles.add(FileUtils.copyFile(dirPhotoSource, directoryExport, j));
            }
            if (!copiedFiles.contains(false))
                return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean exportZip(File directory, String password) {
        try {
            File file = new File(directory + FileUtils.ZIP_FORMAT);
            if (file.exists()) file.delete();
            if (!password.equals("")) {
                FileUtils.createZipEncrypted(file, directory, password);
            } else {
                FileUtils.createZip(file, directory);
            }
            FileUtils.removeDirectory(directory);
            return true;
        } catch (ZipException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static HashMap<String, Boolean> exportSounds(Context context, File directory, Set<String> soundList, String password) {
        HashMap<String, Boolean> resultMap = new HashMap<>();
        for (String i : soundList) {
            File[] sampleFiles = FileUtils.getAllSampleFiles(context, i);
            boolean isExported = true;
            for (File file : sampleFiles) {
                try {
                    FileUtils.copyFile(file, directory, file.getName());
                } catch (IOException e) {
                    e.printStackTrace();
                    isExported = false;
                }
            }
            String name = sampleFiles[0].getName().split("_")[1];
            resultMap.put(name, isExported);
        }

        if (password != null) {
            boolean isExported = ExportUtils.exportZip(directory, password);
            resultMap.put(context.getString(R.string.zip), isExported);
        }

        return resultMap;
    }

    public static String getResultMessage(Context context, HashMap<String, Boolean> resultMap) {
        StringBuilder resultMessage = new StringBuilder();

        Iterator<String> iterator = resultMap.keySet().iterator();

        if (resultMap.containsValue(false)) {
            resultMessage.append(context.getResources().getString(R.string.export_failure)).append(": ");
            while (iterator.hasNext()) {
                String mode = iterator.next();
                if (!resultMap.get(mode)) {
                    resultMessage.append(mode.toLowerCase());
                    iterator.remove();
                    if (resultMap.containsValue(false)) {
                        resultMessage.append(", ");
                    } else if (resultMap.size() > 0) {
                        resultMessage.append("\n\n");
                        break;
                    }
                }
            }
        }

        if (resultMap.containsValue(true)) {
            resultMessage.append(context.getResources().getString(R.string.export_succesful)).append(": ");
            while (iterator.hasNext()) {
                String mode = iterator.next();
                resultMessage.append(mode.toLowerCase());
                iterator.remove();
                if (resultMap.containsValue(true))
                    resultMessage.append(", ");
            }
        }

        return resultMessage.toString();
    }

}
