package com.theost.wavenote.utils;

import android.app.Activity;
import android.content.Intent;

import com.theost.wavenote.Wavenote;
import com.theost.wavenote.models.Note;

import java.util.Calendar;

public class NoteUtils {

    public static void setNotePin(Note note, boolean isPinned) {
        if (note != null && isPinned != note.isPinned()) {
            note.setPinned(isPinned);
            note.setModificationDate(Calendar.getInstance());
            note.save();
        }
    }

    public static void deleteNote(Note note, Activity activity) {
        if (note != null) {
            note.setDeleted(!note.isDeleted());
            note.setModificationDate(Calendar.getInstance());
            note.save();
            Intent resultIntent = new Intent();
            if (note.isDeleted()) {
                resultIntent.putExtra(Wavenote.DELETED_NOTE_ID, note.getSimperiumKey());
            }
            activity.setResult(Activity.RESULT_OK, resultIntent);
        }
    }
}
