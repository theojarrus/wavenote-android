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

import com.theost.wavenote.R;

public class SyntaxHighlighter {

    private static final int TITLE_MAX_LENGTH = 20;

    @SuppressLint("ResourceAsColor")
    public static void addSyntaxHighlight(Context context, EditText mContentEditText, String backgroundColor) {
        /* .SH CODE ATTENTION

        // Getting resources
        int backTitleColor = context.getResources().getColor(R.color.blue);
        int backWordColor = context.getResources().getColor(R.color.purple);
        int textColor = Color.parseColor(backgroundColor);

        String[] keyTitles = context.getResources().getStringArray(R.array.array_musical_titles);
        String[] keyWords = context.getResources().getStringArray(R.array.array_musical_words);

        Spannable noteContent = mContentEditText.getText();
        String strContent = noteContent.toString();

        // Loop for KeyTitles (finding all word occurences in text > formatting > next word)
        for (int i = 0; i < keyTitles.length; i++) {
            int index = 0;
            while (index != -1) {
                index = strContent.indexOf("\n" + keyTitles[i] + "\n", index);
                if (index != -1) {
                    String spacestart = StrUtils.repeat(" ", (TITLE_MAX_LENGTH - keyTitles[i].length() / 2));
                    String spaceend = StrUtils.repeat(" ", (TITLE_MAX_LENGTH - keyTitles[i].length() / 2 + keyTitles[i].length() % 2));
                    int lastindex = index + spacestart.length() + keyTitles[i].length() + spaceend.length();
                    ((Editable) noteContent).insert(lastindex, spaceend);
                    ((Editable) noteContent).insert(index, spacestart);
                    noteContent.setSpan(new ForegroundColorSpan(backTitleColor), index, lastindex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    noteContent.setSpan(new BackgroundColorSpan(textColor), index, lastindex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    index++;
                }
            }
        }

        // Loop for KeyWords (same as previous, but other other words and formatting)
        for (int i = 0; i < keyWords.length; i++) {
            int index = 0;
            while (index != -1) {
                index = strContent.indexOf(keyWords[i] + " ", index);
                if (index != -1) {
                    noteContent.setSpan(new ForegroundColorSpan(backWordColor), index, index + 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    noteContent.setSpan(new BackgroundColorSpan(textColor), index, index + 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    index++;
                }
            }
        }

        // Setting text and placing cursor at right position
        int cursorPoistion = mContentEditText.getSelectionEnd();
        mContentEditText.setText(noteContent);
        mContentEditText.setSelection(cursorPoistion);

        */
    }

    public static boolean changeTextStyle(EditText mContentEditText, boolean[] textStyle, int selectedColor, String backgroundColor) {

        // Getting resources and checking selection
        int cursorPositionStart = mContentEditText.getSelectionStart();
        int cursorPositionEnd = mContentEditText.getSelectionEnd();
        if (cursorPositionStart == cursorPositionEnd) {
            return false;
        }

        // Clear selected text style
        String strContent = mContentEditText.getText().toString().substring(cursorPositionStart, cursorPositionEnd);
        mContentEditText.getText().delete(cursorPositionStart, cursorPositionEnd);
        mContentEditText.getText().insert(cursorPositionStart, strContent);
        Spannable noteContent = mContentEditText.getText();

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
        return true;

    }

}