package com.theost.wavenote;

import android.content.Intent;
import android.widget.RemoteViewsService;

public class NoteListWidgetLightService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new NoteListWidgetFactory(getApplicationContext(), intent);
    }
}
