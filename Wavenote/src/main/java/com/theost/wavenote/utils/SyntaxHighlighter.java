package com.theost.wavenote.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.Spannable;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.text.style.UnderlineSpan;
import android.widget.EditText;

import androidx.core.content.ContextCompat;

import com.theost.wavenote.R;
import com.theost.wavenote.models.Note;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class SyntaxHighlighter {

    private static final int TITLE_SPACE_LENGTH = 24;
    private static final String SPACE = " ";
    private static boolean isHighlightChanged = false;
    private static ArrayList<String> noteChords;
    private static List<String> keyTitles;
    private static List<String> keyWords;
    private static int backTextColor;
    private static int frontTextColor;

    @SuppressLint("ResourceAsColor")
    public static void updateSyntaxHighlight(Context context, EditText mContentEditText, String foregroundColor, boolean detectChords) {
        if (Note.isNeedResourcesUpdate()) updateResources(context);

        Spannable noteContent = mContentEditText.getText();
        frontTextColor = Color.parseColor(foregroundColor);

        backTextColor = ContextCompat.getColor(context, R.color.syntax_title);
        noteContent = addSyntaxHighlight(keyTitles, noteContent, true, false);
        backTextColor = ContextCompat.getColor(context, R.color.syntax_word);
        noteContent = addSyntaxHighlight(keyWords, noteContent, false, false);

        if (detectChords) {
            frontTextColor = ContextCompat.getColor(context, R.color.syntax_chord);
            List<String> keyChords = Arrays.asList(context.getResources().getStringArray(R.array.array_musical_chords));
            noteContent = addSyntaxHighlight(keyChords, noteContent, false, true);
        }

        if (isHighlightChanged) {
            int cursorPoistion = mContentEditText.getSelectionEnd();
            mContentEditText.setText(noteContent);
            mContentEditText.setSelection(cursorPoistion);
        }
    }

    public static Spannable addSyntaxHighlight(List<String> words, Spannable contentSpan, boolean isTitle, boolean isChords) {
        isHighlightChanged = false;
        String contentStr = contentSpan.toString().replaceAll("[\\t\\r\\n\\u202F\\u00A0]", SPACE);
        if (!isChords) contentStr = contentStr.toLowerCase();
        StringBuilder contentBuffer = new StringBuilder(contentStr);
        for (String i : words) {
            int index = 0;
            while (index != -1) {
                if (!isChords) i = i.toLowerCase();
                index = contentBuffer.indexOf(SPACE + i + SPACE, index);
                if (index != -1) {
                    index += SPACE.length();
                    ForegroundColorSpan[] markedSpans = contentSpan.getSpans(index, index + 1, ForegroundColorSpan.class);
                    if (markedSpans.length > 0) {
                        index++;
                        continue;
                    }
                    isHighlightChanged = true;
                    int lastindex = index + i.length();
                    if (isTitle) {
                        String extraSpace = StrUtils.repeat(SPACE, (TITLE_SPACE_LENGTH - i.length()) / 2);
                        ((Editable) contentSpan).insert(lastindex, extraSpace);
                        ((Editable) contentSpan).insert(index, extraSpace);
                        contentBuffer.insert(lastindex, extraSpace);
                        contentBuffer.insert(index, extraSpace);
                        lastindex += extraSpace.length() * 2;
                    }
                    if (!isChords)
                        contentSpan.setSpan(new BackgroundColorSpan(backTextColor), index, lastindex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    contentSpan.setSpan(new ForegroundColorSpan(frontTextColor), index, lastindex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    index++;
                }
            }
        }
        return contentSpan;
    }

    public static ArrayList<String> getNoteChords(Context context, String content) {
        String[] resourceChords = context.getResources().getStringArray(R.array.array_musical_chords);
        Map<Integer, String> sortedNoteChords = new TreeMap<>();
        content = content.replaceAll("[\\t\\n\\r\\u202F\\u00A0]", SPACE);
        for (String i : resourceChords) {
            int index = 0;
            while (index != -1) {
                index = content.indexOf(SPACE + i + SPACE, index);
                if (index != -1) {
                    if (!(sortedNoteChords.containsValue(i))) {
                        sortedNoteChords.put(index, i);
                    }
                    index++;
                }
            }
        }
        noteChords = new ArrayList<>(sortedNoteChords.values());
        return noteChords;
    }

    public static ArrayList<String> getAllChords(Context context) {
        String[] resourceChords = context.getResources().getStringArray(R.array.array_musical_chords);
        noteChords = new ArrayList<>(Arrays.asList(resourceChords));
        return noteChords;
    }

    public static void changeTextStyle(EditText mContentEditText, boolean[] textStyle, int selectedColor, String backgroundColor) {

        // Getting resources and checking selection
        int cursorPositionStart = mContentEditText.getSelectionStart();
        int cursorPositionEnd = mContentEditText.getSelectionEnd();
        if (cursorPositionStart == cursorPositionEnd) {
            return;
        }

        // Clear selected text style
        String strContent = mContentEditText.getText().toString().substring(cursorPositionStart, cursorPositionEnd);
        mContentEditText.getText().delete(cursorPositionStart, cursorPositionEnd);
        mContentEditText.getText().insert(cursorPositionStart, strContent);
        Editable noteContent = mContentEditText.getText();

        // Setting new style
        if (textStyle[0]) {
            noteContent.setSpan(new StyleSpan(Typeface.BOLD), cursorPositionStart, cursorPositionEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        if (textStyle[1]) {
            noteContent.setSpan(new StyleSpan(Typeface.ITALIC), cursorPositionStart, cursorPositionEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        if (textStyle[2]) {
            noteContent.setSpan(new TypefaceSpan("monospace"), cursorPositionStart, cursorPositionEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        if (textStyle[3]) {
            noteContent.setSpan(new UnderlineSpan(), cursorPositionStart, cursorPositionEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        if (textStyle[4]) {
            noteContent.setSpan(new StrikethroughSpan(), cursorPositionStart, cursorPositionEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        if (selectedColor != -1) {
            if (textStyle[5]) {
                noteContent.setSpan(new ForegroundColorSpan(Color.parseColor(backgroundColor)), cursorPositionStart, cursorPositionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                noteContent.setSpan(new BackgroundColorSpan(selectedColor), cursorPositionStart, cursorPositionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                noteContent.setSpan(new ForegroundColorSpan(selectedColor), cursorPositionStart, cursorPositionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        mContentEditText.setText(noteContent);
        mContentEditText.setSelection(cursorPositionEnd);
    }

    public static void updateResources(Context context) {
        DatabaseHelper localDatabase = new DatabaseHelper(context);
        Cursor titles = localDatabase.getDictionaryData("Title");
        Cursor words = localDatabase.getDictionaryData("Word");

        keyTitles = new ArrayList<>();
        while (titles.moveToNext()) {
            keyTitles.add(titles.getString(0));
        }

        keyWords = new ArrayList<>();
        while (words.moveToNext()) {
            keyWords.add(words.getString(0));
        }

        Note.setNeedResourcesUpdate(false);
    }

}