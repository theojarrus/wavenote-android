package com.theost.wavenote.utils;

import android.annotation.SuppressLint;
import android.content.Context;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

public class SyntaxHighlighter {

    private static final int TITLE_MAX_LENGTH = 20;
    private static boolean isHighlightChanged = false;
    private static ArrayList<String> noteChords;
    private static int backTextColor;
    private static int frontTextColor;

    @SuppressLint("ResourceAsColor")
    public static void updateSyntaxHighlight(Context context, EditText mContentEditText, String foregroundColor) {
        String[] keyTitles = context.getResources().getStringArray(R.array.array_musical_titles);
        String[] keyWords = context.getResources().getStringArray(R.array.array_musical_words);
        Spannable noteContent = mContentEditText.getText();
        frontTextColor = Color.parseColor(foregroundColor);

        backTextColor = ContextCompat.getColor(context, R.color.blue);
        noteContent = addSyntaxHighlight(keyTitles, noteContent, true);

        backTextColor = ContextCompat.getColor(context, R.color.purple);
        noteContent = addSyntaxHighlight(keyWords, noteContent, false);

        if (isHighlightChanged) {
            int cursorPoistion = mContentEditText.getSelectionEnd();
            mContentEditText.setText(noteContent);
            mContentEditText.setSelection(cursorPoistion);
        }
    }

    public static Spannable addSyntaxHighlight(String[] words, Spannable contentSpan, boolean isTitle) {
        isHighlightChanged = false;

        String contentStr = contentSpan.toString().toLowerCase().replaceAll("[\\t\\n\\r\\u202F\\u00A0]", " ");
        String splitterStart = " ";
        String splitterEnd = " ";
        if (isTitle) {
            splitterStart = "\n[";
            splitterEnd = "]";
            if ((!contentStr.contains(splitterStart)) || (!contentStr.contains(splitterEnd))) return contentSpan;
        }
        for (String i : words) {
            int index = 0;

            while (index != -1) {
                index = contentStr.indexOf(splitterStart + i.toLowerCase() + splitterEnd, index);
                if (index != -1) {
                    BackgroundColorSpan[] markedSpans = contentSpan.getSpans(index + splitterStart.length(), index + splitterStart.length() + 1, BackgroundColorSpan.class);
                    if (markedSpans.length > 0) {
                        index++;
                        continue;
                    }
                    isHighlightChanged = true;
                    int lastindex = index + i.length();
                    if (isTitle) {
                        String spaceStart = StrUtils.repeat(" ", (TITLE_MAX_LENGTH - i.length() - 1) / 2);
                        String spaceEnd = StrUtils.repeat(" ", (TITLE_MAX_LENGTH - i.length() - 1) / 2 + i.length() % 2);
                        lastindex += splitterStart.length() + splitterEnd.length();
                        ((Editable) contentSpan).insert(lastindex, spaceEnd);
                        ((Editable) contentSpan).insert(index + splitterStart.length() / 2, spaceStart);
                        lastindex += spaceStart.length() + spaceEnd.length();

                    } else {
                        index += 1;
                        lastindex += 1;
                    }
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
        content = content.replaceAll("[\\t\\n\\r\\u202F\\u00A0]", " ");
        for (String i : resourceChords) {
            int index = 0;
            while (index != -1) {
                index = content.indexOf(" " + i + " ", index);
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
        if (cursorPositionStart == cursorPositionEnd) { return; }

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

        // Setting text and placing cursor at right position
        mContentEditText.setText(noteContent);
        mContentEditText.setSelection(cursorPositionEnd);
    }

}