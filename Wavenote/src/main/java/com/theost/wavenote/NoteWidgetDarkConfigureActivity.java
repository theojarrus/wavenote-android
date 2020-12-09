package com.theost.wavenote;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.RemoteViews;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.simperium.Simperium;
import com.simperium.client.Bucket;
import com.simperium.client.Query;
import com.simperium.client.User;
import com.theost.wavenote.models.Note;
import com.theost.wavenote.utils.ChecklistUtils;
import com.theost.wavenote.utils.PrefUtils;

import static com.theost.wavenote.models.Note.NEW_LINE;

public class NoteWidgetDarkConfigureActivity extends AppCompatActivity {
    private AppWidgetManager mWidgetManager;
    private NotesCursorAdapter mNotesAdapter;
    private RemoteViews mRemoteViews;
    private Wavenote mApplication;
    private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    public NoteWidgetDarkConfigureActivity() {
        super();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if the user presses the back button.
        setResult(RESULT_CANCELED);

        setContentView(R.layout.note_widget_configure);

        // Verify user authentication.
        mApplication = (Wavenote) getApplicationContext();
        Simperium simperium = mApplication.getSimperium();
        User user = simperium.getUser();

        // Get widget information
        mWidgetManager = AppWidgetManager.getInstance(NoteWidgetDarkConfigureActivity.this);
        mRemoteViews = new RemoteViews(getPackageName(), R.layout.note_widget_dark);
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();

        if (extras != null) {
            mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        showDialog();
    }

    private void showDialog() {
        Bucket<Note> mNotesBucket = mApplication.getNotesBucket();
        Query<Note> query = Note.all(mNotesBucket);
        query.include(Note.TITLE_INDEX_NAME, Note.CONTENT_PREVIEW_INDEX_NAME);
        PrefUtils.sortNoteQuery(query, NoteWidgetDarkConfigureActivity.this, true);
        Bucket.ObjectCursor<Note> cursor = query.execute();

        Context context = new ContextThemeWrapper(NoteWidgetDarkConfigureActivity.this, R.style.Theme_Transparent);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        @SuppressLint("InflateParams")
        final View layout = LayoutInflater.from(context).inflate(R.layout.note_widget_configure_list, null);
        final ListView list = layout.findViewById(R.id.list);
        mNotesAdapter = new NotesCursorAdapter(NoteWidgetDarkConfigureActivity.this, cursor);
        list.setAdapter(mNotesAdapter);

        builder.setView(layout)
                .setTitle(R.string.select_note)
                .setOnDismissListener(
                        dialog -> finish()
                )
                .setNegativeButton(android.R.string.cancel,
                        (dialog, which) -> finish()
                )
                .show();
    }

    private class NotesCursorAdapter extends CursorAdapter {
        private final Bucket.ObjectCursor<Note> mCursor;

        private NotesCursorAdapter(Context context, Bucket.ObjectCursor<Note> cursor) {
            super(context, cursor, 0);
            mCursor = cursor;
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return LayoutInflater.from(context).inflate(R.layout.note_list_row, parent, false);
        }

        @Override
        public Note getItem(int position) {
            mCursor.moveToPosition(position);
            return mCursor.getObject();
        }

        @Override
        public void bindView(View view, final Context context, final Cursor cursor) {
            view.setTag(cursor.getPosition());
            TextView titleTextView = view.findViewById(R.id.note_title);
            TextView contentTextView = view.findViewById(R.id.note_content);
            String title = "";
            String snippet = "";

            if (cursor.getColumnIndex(Note.TITLE_INDEX_NAME) > -1) {
                title =  cursor.getString(cursor.getColumnIndex(Note.TITLE_INDEX_NAME));
            }

            if (cursor.getColumnIndex(Note.CONTENT_PREVIEW_INDEX_NAME) > -1) {
                snippet =  cursor.getString(cursor.getColumnIndex(Note.CONTENT_PREVIEW_INDEX_NAME));
            }

            // Populate fields with extracted properties
            titleTextView.setText(title);
            SpannableStringBuilder snippetSpan = new SpannableStringBuilder(snippet);
            snippetSpan = (SpannableStringBuilder) ChecklistUtils.addChecklistSpansForRegexAndColor(
                    context,
                    snippetSpan,
                    ChecklistUtils.CHECKLIST_REGEX,
                    R.color.text_title_disabled);
            contentTextView.setText(snippetSpan);

            view.setOnClickListener(view1 -> {
                // Get the selected note
                Note note = mNotesAdapter.getItem((int) view1.getTag());

                // Store link between note and widget in SharedPreferences
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                preferences.edit().putString(PrefUtils.PREF_NOTE_WIDGET_NOTE + mAppWidgetId, note.getSimperiumKey()).apply();

                // Prepare bundle for NoteEditorActivity
                Bundle arguments = new Bundle();
                arguments.putBoolean(NoteEditorFragment.ARG_IS_FROM_WIDGET, true);
                arguments.putString(NoteEditorFragment.ARG_ITEM_ID, note.getSimperiumKey());
                arguments.putBoolean(NoteEditorFragment.ARG_MARKDOWN_ENABLED, note.isMarkdownEnabled());
                arguments.putBoolean(NoteEditorFragment.ARG_PREVIEW_ENABLED, note.isPreviewEnabled());

                // Create intent to navigate to selected note on widget click
                Intent intent = new Intent(context, NoteEditorActivity.class);
                intent.putExtras(arguments);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                PendingIntent pendingIntent = PendingIntent.getActivity(context, mAppWidgetId, intent, 0);

                // Remove title from content
                String title1 = note.getTitle();
                String contentWithoutTitle = note.getContent().toString().replace(title1, "");
                int indexOfNewline = contentWithoutTitle.indexOf(NEW_LINE) + 1;
                String content = contentWithoutTitle.substring(indexOfNewline < contentWithoutTitle.length() ? indexOfNewline : 0);

                // Set widget content
                mRemoteViews.setOnClickPendingIntent(R.id.widget_layout, pendingIntent);
                mRemoteViews.setTextViewText(R.id.widget_text, title1);
                mRemoteViews.setTextColor(R.id.widget_text, ContextCompat.getColor(context, R.color.text_title_dark));
                mRemoteViews.setTextViewText(R.id.widget_text_title, title1);
                mRemoteViews.setTextColor(R.id.widget_text_title, ContextCompat.getColor(context, R.color.text_title_dark));
                SpannableStringBuilder contentSpan = new SpannableStringBuilder(content);
                contentSpan = (SpannableStringBuilder) ChecklistUtils.addChecklistUnicodeSpansForRegex(
                        contentSpan,
                        ChecklistUtils.CHECKLIST_REGEX
                );
                mRemoteViews.setTextViewText(R.id.widget_text_content, contentSpan);
                mWidgetManager.updateAppWidget(mAppWidgetId, mRemoteViews);

                // Set the result as successful
                Intent resultValue = new Intent();
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
                setResult(RESULT_OK, resultValue);
                finish();
            });
        }
    }
}
