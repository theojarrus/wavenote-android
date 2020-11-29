package com.theost.wavenote.widgets;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.text.Editable;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.style.ImageSpan;
import android.util.AttributeSet;
import android.view.KeyEvent;

import androidx.appcompat.widget.AppCompatEditText;
import androidx.core.content.ContextCompat;

import com.theost.wavenote.R;
import com.theost.wavenote.utils.ChecklistUtils;
import com.theost.wavenote.utils.DisplayUtils;
import com.theost.wavenote.utils.DrawableUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.theost.wavenote.models.Note.NEW_LINE;
import static com.theost.wavenote.models.Note.SPACE;

public class WavenoteEditText extends AppCompatEditText {
    private final List<OnSelectionChangedListener> listeners;
    private OnCheckboxToggledListener mOnCheckboxToggledListener;
    private final int CHECKBOX_LENGTH = 3; // One CheckableSpan + a space character
    public static final String DISABLE_TEXTWATCHER = "disable_textwatcher";

    public interface OnCheckboxToggledListener {
        void onCheckboxToggled();
    }

    public WavenoteEditText(Context context) {
        super(context);
        listeners = new ArrayList<>();
        setTypeface(TypefaceCache.getTypeface(context, TypefaceCache.TYPEFACE_NAME_ROBOTO_REGULAR));
    }

    public WavenoteEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        listeners = new ArrayList<>();
        setTypeface(TypefaceCache.getTypeface(context, TypefaceCache.TYPEFACE_NAME_ROBOTO_REGULAR));
    }

    public WavenoteEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        listeners = new ArrayList<>();
        setTypeface(TypefaceCache.getTypeface(context, TypefaceCache.TYPEFACE_NAME_ROBOTO_REGULAR));
    }

    public void addOnSelectionChangedListener(OnSelectionChangedListener o) {
        listeners.add(o);
    }

    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        super.onSelectionChanged(selStart, selEnd);
        if (listeners != null) {
            for (OnSelectionChangedListener l : listeners)
                l.onSelectionChanged(selStart, selEnd);
        }
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            clearFocus();
        }

        return super.onKeyPreIme(keyCode, event);
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        if (focused) {
            setCursorVisible(true);
        }

        super.onFocusChanged(focused, direction, previouslyFocusedRect);
    }

    public String getTextContent() {
        String content = Objects.requireNonNull(getText()).toString();
        int firstNewLinePosition = content.indexOf(NEW_LINE);
        if (firstNewLinePosition > -1) {
            return content.substring(firstNewLinePosition);
        } else {
            return "";
        }
    }

    public int getTitleEnd() {
        String content = Objects.requireNonNull(getText()).toString();
        return content.indexOf(NEW_LINE);
    }

    public ArrayList<Integer> getNewlineIndexes() {
        ArrayList<Integer> newlineIndexes = new ArrayList<>();
        int nlIndex = 0;
        while (nlIndex != -1) {
            nlIndex = Objects.requireNonNull(getText()).toString().indexOf(NEW_LINE, nlIndex);
            if (nlIndex != -1) {
                newlineIndexes.add(nlIndex);
                nlIndex += 1;
            }
        }
        return newlineIndexes;
    }

    public float getTextLineWidth(CharSequence line) {
        TextPaint paint = new TextPaint();
        float textSize = getTextSize();
        paint.setTextSize(textSize);
        StaticLayout tempLayout = new StaticLayout(line, paint, 10000, android.text.Layout.Alignment.ALIGN_NORMAL, 0f, 0f, false);
        float lineWidth = 0;
        for (int i = 0; i < tempLayout.getLineCount(); i++) lineWidth += tempLayout.getLineWidth(i);
        return lineWidth;
    }

    public float getTextLineHeight(CharSequence line) {
        TextPaint paint = new TextPaint();
        float textSize = getTextSize();
        paint.setTextSize(textSize);
        StaticLayout tempLayout = new StaticLayout(line, paint, 10000, android.text.Layout.Alignment.ALIGN_NORMAL, 0f, 0f, false);
        Rect rect = new Rect();
        tempLayout.getLineBounds(0, rect);
        return rect.height();
    }

    // Updates the ImageSpan drawable to the new checked state
    public void toggleCheckbox(final CheckableSpan checkableSpan) {
        setCursorVisible(false);

        final Editable editable = getText();

        final int checkboxStart = Objects.requireNonNull(editable).getSpanStart(checkableSpan);
        final int checkboxEnd = editable.getSpanEnd(checkableSpan);

        final int selectionStart = getSelectionStart();
        final int selectionEnd = getSelectionEnd();

        final ImageSpan[] imageSpans = editable.getSpans(checkboxStart, checkboxEnd, ImageSpan.class);
        if (imageSpans.length > 0) {
            // ImageSpans are static, so we need to remove the old one and replace :|
            Drawable iconDrawable = ContextCompat.getDrawable(getContext(),
                    checkableSpan.isChecked()
                            ? R.drawable.ic_checkbox_checked_24px
                            : R.drawable.ic_checkbox_unchecked_24px);
            iconDrawable = DrawableUtils.tintDrawableWithResource(getContext(), iconDrawable, R.color.text_title_disabled);
            int iconSize = DisplayUtils.getChecklistIconSize(getContext());
            iconDrawable.setBounds(0, 0, iconSize, iconSize);
            final CenteredImageSpan newImageSpan = new CenteredImageSpan(getContext(), iconDrawable);
            new Handler().post(() -> {
                editable.setSpan(newImageSpan, checkboxStart, checkboxEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                editable.removeSpan(imageSpans[0]);
                fixLineSpacing();

                // Restore the selection
                if (selectionStart >= 0
                        && selectionStart <= editable.length()
                        && selectionEnd <= editable.length() && hasFocus()) {
                    setSelection(selectionStart, selectionEnd);
                    setCursorVisible(true);
                }

                if (mOnCheckboxToggledListener != null) {
                    mOnCheckboxToggledListener.onCheckboxToggled();
                }
            });
        }
    }

    // Returns the line position of the text cursor
    private int getCurrentCursorLine() {
        int selectionStart = getSelectionStart();
        Layout layout = getLayout();

        if (!(selectionStart == -1)) {
            return layout.getLineForOffset(selectionStart);
        }

        return 0;
    }

    public String getSelectedString() {
        return Objects.requireNonNull(getText()).toString().substring(getSelectionStart(), getSelectionEnd());
    }

    public void insertChecklist() {
        int start, end;
        int lineNumber = getCurrentCursorLine();
        start = getLayout().getLineStart(lineNumber) - 1;

        if (getSelectionEnd() > getSelectionStart() && !selectionIsOnSameLine()) {
            end = getSelectionEnd();
        } else {
            end = getLayout().getLineEnd(lineNumber);
        }

        SpannableStringBuilder workingString = new SpannableStringBuilder(Objects.requireNonNull(getText()).subSequence(start, end));
        Editable editable = getText();
        if (editable.length() < start || editable.length() < end) {
            return;
        }

        int previousSelection = getSelectionStart();
        CheckableSpan[] checkableSpans = workingString.getSpans(0, workingString.length(), CheckableSpan.class);
        if (checkableSpans.length > 0) {
            // Remove any CheckableSpans found
            for (CheckableSpan span : checkableSpans) {
                workingString.replace(
                        workingString.getSpanStart(span) - 1,
                        workingString.getSpanEnd(span) + 1,
                        ""
                );
                workingString.removeSpan(span);
            }

            editable.replace(start, end, workingString);

            if (checkableSpans.length == 1) {
                int newSelection = Math.max(previousSelection - CHECKBOX_LENGTH, 0);
                if (editable.length() >= newSelection) {
                    setSelection(newSelection);
                }
            }
        } else {
            editable.insert(getSelectionStart(), "\n- [ ] ");
        }
    }

    // Returns true if the current editor selection is on the same line
    private boolean selectionIsOnSameLine() {
        int selectionStart = getSelectionStart();
        int selectionEnd = getSelectionEnd();
        Layout layout = getLayout();

        if (selectionStart >= 0 && selectionEnd >= 0) {
            return layout.getLineForOffset(selectionStart) == layout.getLineForOffset(selectionEnd);
        }

        return false;
    }

    public void fixLineHeight() {
        setTag(DISABLE_TEXTWATCHER);
        Editable editable = getText();
        ArrayList<Integer> newlineIndexes = getNewlineIndexes();
        boolean isChanged = false;
        int offset = 0;
        for (Integer i : newlineIndexes) {
            if ((i == 0) || (!editable.subSequence(i + offset - 1, i + offset).toString().equals(SPACE))) {
                editable.insert(i + offset, SPACE);
                isChanged = true;
                offset++;
            }
        }
        if (isChanged) setSelection(Math.max(getSelectionStart() - 1, 0));
        setTag(null);
    }

    public void fixLineSpacing() {
        // Prevents line heights from compacting
        // https://issuetracker.google.com/issues/37009353
        float lineSpacingExtra = getLineSpacingExtra();
        float lineSpacingMultiplier = getLineSpacingMultiplier();
        setLineSpacing(0f, 1.2f);
        setLineSpacing(lineSpacingExtra, lineSpacingMultiplier);
    }

    public interface OnSelectionChangedListener {
        void onSelectionChanged(int selStart, int selEnd);
    }

    // Replaces any CheckableSpans with their markdown counterpart (e.g. '- [ ]')
    public Spannable getPlainTextContent() {
        if (getText() == null) {
            return new SpannableString("");
        }

        SpannableStringBuilder content = new SpannableStringBuilder(getText());
        CheckableSpan[] spans = content.getSpans(0, content.length(), CheckableSpan.class);

        for (CheckableSpan span : spans) {
            int start = content.getSpanStart(span);
            int end = content.getSpanEnd(span);
            ((Editable) content).replace(
                    start,
                    end,
                    span.isChecked() ? ChecklistUtils.CHECKED_MARKDOWN : ChecklistUtils.UNCHECKED_MARKDOWN);
        }

        return content;
    }

    // Replaces any CheckableSpans with their markdown preview counterpart (e.g. '- [\u2A2F]')
    public String getPreviewTextContent() {
        if (getText() == null) {
            return "";
        }

        SpannableStringBuilder content = new SpannableStringBuilder(getText());
        CheckableSpan[] spans = content.getSpans(0, content.length(), CheckableSpan.class);
        for (CheckableSpan span : spans) {
            int start = content.getSpanStart(span);
            int end = content.getSpanEnd(span);
            ((Editable) content).replace(
                    start,
                    end,
                    span.isChecked() ? ChecklistUtils.CHECKED_MARKDOWN_PREVIEW : ChecklistUtils.UNCHECKED_MARKDOWN);
        }

        return content.toString();
    }

    public void processChecklists() {
        if (Objects.requireNonNull(getText()).length() == 0 || getContext() == null) {
            return;
        }

        try {
            ChecklistUtils.addChecklistSpansForRegexAndColor(
                    getContext(),
                    getText(),
                    ChecklistUtils.CHECKLIST_REGEX_LINES,
                    R.color.text_title_disabled);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setOnCheckboxToggledListener(OnCheckboxToggledListener listener) {
        mOnCheckboxToggledListener = listener;
    }


}