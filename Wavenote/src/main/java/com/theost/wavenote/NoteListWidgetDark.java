package com.theost.wavenote;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;

import androidx.core.content.ContextCompat;

import com.simperium.Simperium;
import com.simperium.client.Bucket;
import com.simperium.client.Query;
import com.simperium.client.User;
import com.theost.wavenote.models.Note;
import com.theost.wavenote.utils.PrefUtils;

public class NoteListWidgetDark extends AppWidgetProvider {
    public static final String KEY_LIST_WIDGET_IDS_DARK = "key_list_widget_ids_dark";

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.note_list_widget_dark);
        resizeWidget(context, newOptions, views);
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
    }

    @Override
    public void onEnabled(Context context) {
    }

    @Override
    public void onDisabled(Context context) {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getExtras() != null && intent.hasExtra(KEY_LIST_WIDGET_IDS_DARK)) {
            int[] ids = intent.getExtras().getIntArray(KEY_LIST_WIDGET_IDS_DARK);
            if (ids != null) {
                this.onUpdate(context, AppWidgetManager.getInstance(context), ids);
            } else {
                super.onReceive(context, intent);
            }
        } else {
            super.onReceive(context, intent);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            Bundle appWidgetOptions = appWidgetManager.getAppWidgetOptions(appWidgetId);
            updateWidget(context, appWidgetManager, appWidgetId, appWidgetOptions);
        }
    }

    private void resizeWidget(Context context, Bundle appWidgetOptions, RemoteViews views) {
        views.setViewPadding(R.id.widget_list, 0, 0, 0, 0);
        views.setViewVisibility(R.id.widget_button, View.GONE);
    }

    private void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle appWidgetOptions) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.note_list_widget_dark);
        resizeWidget(context, appWidgetOptions, views);

        // Verify user authentication
        Wavenote currentApp = (Wavenote) context.getApplicationContext();
        Simperium simperium = currentApp.getSimperium();
        User user = simperium.getUser();

        Bucket<Note> notesBucket = currentApp.getNotesBucket();
        Query<Note> query = Note.all(notesBucket);
        query.include(Note.TITLE_INDEX_NAME, Note.CONTENT_PREVIEW_INDEX_NAME);
        PrefUtils.sortNoteQuery(query, context, true);
        Bucket.ObjectCursor<Note> cursor = query.execute();

        if (cursor.getCount() > 0) {
            // Create intent to navigate to notes activity on widget click while loading
            Intent intentLoading = new Intent(context, NotesActivity.class);
            intentLoading.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pendingIntentLoading = PendingIntent.getActivity(context, appWidgetId, intentLoading, 0);
            views.setOnClickPendingIntent(R.id.widget_layout, pendingIntentLoading);

            // Create intent for note list widget service
            Intent intent = new Intent(context, NoteListWidgetDarkService.class);
            intent.putExtra(NoteListWidgetFactory.EXTRA_IS_LIGHT, false);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            views.setRemoteAdapter(R.id.widget_list, intent);

            // Create intent to navigate to note editor on note list item click
            Intent intentItem = new Intent(context, NoteEditorActivity.class);
            PendingIntent pendingIntentItem = PendingIntent.getActivity(context, appWidgetId, intentItem, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setPendingIntentTemplate(R.id.widget_list, pendingIntentItem);

            // Create intent to navigate to note editor on note list add button click
            Intent intentButton = new Intent(context, NotesActivity.class);
            intentButton.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pendingIntentButton = PendingIntent.getActivity(context, appWidgetId, intentButton, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.widget_button, pendingIntentButton);

            views.setEmptyView(R.id.widget_list, R.id.widget_text);
            views.setTextColor(R.id.widget_text, ContextCompat.getColor(context, R.color.text_title_dark));
            views.setTextViewText(R.id.widget_text, context.getString(R.string.empty_notes_widget));
            views.setViewVisibility(R.id.widget_text, View.GONE);
            views.setViewVisibility(R.id.widget_list, View.VISIBLE);
        } else {
            // Create intent to navigate to notes activity on widget click
            Intent intent = new Intent(context, NotesActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, appWidgetId, intent, PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.widget_layout, pendingIntent);

            // Create intent to navigate to note editor on note list add button click
            Intent intentButton = new Intent(context, NotesActivity.class);
            intentButton.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pendingIntentButton = PendingIntent.getActivity(context, appWidgetId, intentButton, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.widget_button, pendingIntentButton);

            views.setTextColor(R.id.widget_text, ContextCompat.getColor(context, R.color.text_title_dark));
            views.setTextViewText(R.id.widget_text, context.getString(R.string.empty_notes_widget));
            views.setViewVisibility(R.id.widget_text, View.VISIBLE);
            views.setViewVisibility(R.id.widget_list, View.GONE);
        }

        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list);
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}