package com.theost.wavenote.utils;

import android.text.TextUtils;
import android.widget.Toast;

import com.theost.wavenote.R;
import com.theost.wavenote.Wavenote;
import com.theost.wavenote.models.Note;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.Calendar;

// Create a new note after receiving a 'new-note' message from the wearable
public class WavenoteWearListenerService extends WearableListenerService {
    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (messageEvent.getPath().equals("new-note")) {
            if (messageEvent.getData() != null && messageEvent.getData().length > 0) {

                String voiceNoteString = new String(messageEvent.getData());
                Wavenote application = (Wavenote) getApplication();

                if (!TextUtils.isEmpty(voiceNoteString) && application.getNotesBucket() != null) {
                    Note note = application.getNotesBucket().newObject();
                    note.setCreationDate(Calendar.getInstance());
                    note.setModificationDate(note.getCreationDate());
                    note.setContent(voiceNoteString);
                    note.save();

                    Toast.makeText(application, getString(R.string.note_added), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
