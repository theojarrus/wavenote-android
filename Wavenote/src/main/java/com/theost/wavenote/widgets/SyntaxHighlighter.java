package com.theost.wavenote.widgets;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.ParcelableSpan;
import android.text.Spannable;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.SuperscriptSpan;
import android.text.style.TypefaceSpan;
import android.text.style.UnderlineSpan;

import androidx.core.content.ContextCompat;

import com.theost.wavenote.R;
import com.theost.wavenote.models.Note;
import com.theost.wavenote.utils.DatabaseHelper;
import com.theost.wavenote.utils.PrefUtils;
import com.theost.wavenote.utils.StrUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.theost.wavenote.models.Note.NEW_LINE;
import static com.theost.wavenote.models.Note.SPACE;
import static com.theost.wavenote.widgets.TypefaceCache.TYPEFACE_NAME_ROBOTO_REGULAR;
import static com.theost.wavenote.widgets.WavenoteEditText.DISABLE_TEXTWATCHER;

public class SyntaxHighlighter {

    private static final String KEYWORD_TITLE = "Title";
    private static final String KEYWORD_WORD = "Word";

    private static final int TITLE_SPACE_LENGTH = 24;

    private static List<String> keyTitles;
    private static List<String> keyWords;

    private final Context context;
    private final WavenoteEditText mContentEditText;

    private int frontTextColor;
    private int backTextColor;

    private final int titleColor;
    private final int wordColor;
    private final int chordColor;

    public SyntaxHighlighter(Context context, WavenoteEditText mContentEditText) {
        this.context = context;
        this.mContentEditText = mContentEditText;
        this.titleColor = ContextCompat.getColor(context, R.color.syntax_title);
        this.wordColor = ContextCompat.getColor(context, R.color.syntax_word);
        this.chordColor = ContextCompat.getColor(context, R.color.syntax_chord);
    }

    private void updateResources() {
        DatabaseHelper localDatabase = new DatabaseHelper(context);
        Cursor titles = localDatabase.getDictionaryData(KEYWORD_TITLE);
        Cursor words = localDatabase.getDictionaryData(KEYWORD_WORD);

        keyTitles = new ArrayList<>();
        while (titles.moveToNext()) {
            keyTitles.add(titles.getString(0));
        }

        keyWords = new ArrayList<>();
        while (words.moveToNext()) {
            keyWords.add(words.getString(0));
        }

        Note.setNeedResourceUpdate(false);
        localDatabase.close();
    }

    public void updateSyntaxHighlight(String foregroundColor) {
        mContentEditText.setTag(DISABLE_TEXTWATCHER);

        if (Note.isNeedResourceUpdate()) updateResources();

        Spannable noteContent = mContentEditText.getText();

        if (PrefUtils.getBoolPref(context, PrefUtils.PREF_DETECT_KEYWORDS)) {
            frontTextColor = Color.parseColor(foregroundColor);
            backTextColor = titleColor;
            addSyntaxHighlight(keyTitles, null, noteContent, true, true);
            frontTextColor = wordColor;
            backTextColor = 0;
            addSyntaxHighlight(keyWords, null, noteContent, true, false);
        }

        if (PrefUtils.getBoolPref(context, PrefUtils.PREF_DETECT_CHORDS)) {
            frontTextColor = chordColor;
            backTextColor = 0;
            List<String> keyKeys = Arrays.asList(context.getResources().getStringArray(R.array.array_musical_keys));
            List<String> keyChords = Arrays.asList(context.getResources().getStringArray(R.array.array_musical_chords));
            addSyntaxHighlight(keyKeys, keyChords, noteContent, false, false);
        }

        mContentEditText.setTag(null);
    }

    public void addSyntaxHighlight(List<String> words, List<String> wordsExtra, Spannable contentSpan, boolean isKeyword, boolean isTitle) {
        int[] selectionIndexes = mContentEditText.getSelectionIndexes();
        int titleEnd = getTitleEnd(contentSpan);
        String contentStr = contentSpan.toString().replaceAll("[\\t\\r\\n\\u202F\\u00A0]", SPACE);
        if (isKeyword) contentStr = contentStr.toLowerCase();
        StringBuilder contentBuffer = new StringBuilder(contentStr);
        for (String i : words) {
            if (wordsExtra == null) wordsExtra = new ArrayList<>();
            if (wordsExtra.size() == 0) wordsExtra.add("");
            for (String e : wordsExtra) {
                String word = i + e;
                int index = titleEnd;
                while (index != -1) {
                    if (isKeyword) word = word.toLowerCase();
                    index = contentBuffer.indexOf(SPACE + word + SPACE, index);
                    if (index != -1) {
                        ForegroundColorSpan[] markedSpans = contentSpan.getSpans(index + 1, index + 1 + word.length(), ForegroundColorSpan.class);
                        index += SPACE.length();
                        if (markedSpans.length > 0) {
                            index++;
                            continue;
                        }
                        int lastIndex = index + word.length();
                        if (isTitle) {
                            String extraSpace = StrUtils.repeat(SPACE, (TITLE_SPACE_LENGTH - i.length()) / 2);
                            ((Editable) contentSpan).insert(lastIndex, extraSpace);
                            ((Editable) contentSpan).insert(index, extraSpace);
                            contentBuffer.insert(lastIndex, extraSpace);
                            contentBuffer.insert(index, extraSpace);
                            lastIndex += extraSpace.length() * 2;
                        }
                        if (index < lastIndex) {
                            if (isTitle) contentSpan.setSpan(new BackgroundColorSpan(backTextColor), index, lastIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            contentSpan.setSpan(new ForegroundColorSpan(frontTextColor), index, lastIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                        index++;
                    }
                }
            }
        }
        mContentEditText.restoreSelection(selectionIndexes, mContentEditText.getText().length());
    }

    private int getTitleEnd(Spannable content) {
        int titleEnd = content.toString().indexOf(NEW_LINE);
        if (titleEnd == -1) titleEnd = content.length();
        return titleEnd;
    }

    public void changeTextStyle(int selectedColor, String backgroundColor) {

        // Getting resources and checking selection
        int[] cursorPositions = mContentEditText.getSelectionIndexes();
        if (cursorPositions[0] == cursorPositions[1]) return;
        int titleEnd = mContentEditText.getTitleEnd();
        if (cursorPositions[0] < titleEnd) {
            if (cursorPositions[1] > titleEnd) {
                cursorPositions[0] = titleEnd;
            } else {
                return;
            }
        }

        mContentEditText.setTag(DISABLE_TEXTWATCHER);

        // Clear selected text style
        Editable noteContent = mContentEditText.getText();
        if (noteContent == null) return;
        ParcelableSpan[] spans = noteContent.getSpans(cursorPositions[0], cursorPositions[1], ParcelableSpan.class);
        if (spans.length > 0) {
            for (ParcelableSpan span : spans) {
                ParcelableSpan newSpanStart = null;
                ParcelableSpan newSpanEnd = null;
                if (span instanceof TypefaceSpan) {
                    newSpanStart = new TypefaceSpan(((TypefaceSpan) span).getFamily());
                    newSpanEnd = new TypefaceSpan(((TypefaceSpan) span).getFamily());
                } else if (span instanceof StyleSpan) {
                    newSpanStart = new StyleSpan(((StyleSpan) span).getStyle());
                    newSpanEnd = new StyleSpan(((StyleSpan) span).getStyle());
                } else if (span instanceof RelativeSizeSpan) {
                    newSpanStart = new RelativeSizeSpan(((RelativeSizeSpan) span).getSizeChange());
                    newSpanEnd = new RelativeSizeSpan(((RelativeSizeSpan) span).getSizeChange());
                } else if (span instanceof SuperscriptSpan) {
                    newSpanStart = new SuperscriptSpan();
                    newSpanEnd = new SuperscriptSpan();
                } else if (span instanceof UnderlineSpan) {
                    newSpanStart = new UnderlineSpan();
                    newSpanEnd = new UnderlineSpan();
                } else if (span instanceof StrikethroughSpan) {
                    newSpanStart = new StrikethroughSpan();
                    newSpanEnd = new StrikethroughSpan();
                } else if (span instanceof ForegroundColorSpan) {
                    newSpanStart = new ForegroundColorSpan(((ForegroundColorSpan) span).getForegroundColor());
                    newSpanEnd = new ForegroundColorSpan(((ForegroundColorSpan) span).getForegroundColor());
                } else if (span instanceof BackgroundColorSpan) {
                    newSpanStart = new BackgroundColorSpan(((BackgroundColorSpan) span).getBackgroundColor());
                    newSpanEnd = new BackgroundColorSpan(((BackgroundColorSpan) span).getBackgroundColor());
                }
                if (newSpanStart != null) {
                    int start = noteContent.getSpanStart(span);
                    int end = noteContent.getSpanEnd(span);
                    if (start < cursorPositions[0]) {
                        noteContent.setSpan(newSpanStart, start, cursorPositions[0], Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                    if (end > cursorPositions[1]) {
                        noteContent.setSpan(newSpanEnd, cursorPositions[1], end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
                noteContent.removeSpan(span);
            }
        }

        // Setting new style
        String font = Note.getActiveTextFont();
        if (font.equals("")) {
            noteContent.setSpan(new TypefaceSpan(TYPEFACE_NAME_ROBOTO_REGULAR), cursorPositions[0], cursorPositions[1], Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else {
            noteContent.setSpan(new TypefaceSpan(Note.getActiveTextFont()), cursorPositions[0], cursorPositions[1], Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        if (Note.isIsTextStyleBold()) {
            noteContent.setSpan(new StyleSpan(Typeface.BOLD), cursorPositions[0], cursorPositions[1], Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        if (Note.isIsTextStyleItalic()) {
            noteContent.setSpan(new StyleSpan(Typeface.ITALIC), cursorPositions[0], cursorPositions[1], Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        if (Note.isIsTextStyleUpper()) {
            noteContent.setSpan(new RelativeSizeSpan(0.8f), cursorPositions[0], cursorPositions[1], Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            noteContent.setSpan(new SuperscriptSpan(), cursorPositions[0], cursorPositions[1], Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        if (Note.isIsTextStyleUnderline()) {
            noteContent.setSpan(new UnderlineSpan(), cursorPositions[0], cursorPositions[1], Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        if (Note.isIsTextStyleStrikethrough()) {
            noteContent.setSpan(new StrikethroughSpan(), cursorPositions[0], cursorPositions[1], Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        if (selectedColor != -1) {
            if (Note.isIsTextStyleStroke()) {
                noteContent.setSpan(new ForegroundColorSpan(Color.parseColor(backgroundColor)), cursorPositions[0], cursorPositions[1], Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                noteContent.setSpan(new BackgroundColorSpan(selectedColor), cursorPositions[0], cursorPositions[1], Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                noteContent.setSpan(new ForegroundColorSpan(selectedColor), cursorPositions[0], cursorPositions[1], Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        mContentEditText.setSelection(cursorPositions[1]);
        mContentEditText.setTag(null);
    }

}