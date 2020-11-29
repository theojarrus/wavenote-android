package com.theost.wavenote;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.theost.wavenote.adapters.ColorAdapter;
import com.theost.wavenote.models.Note;

import java.util.List;

public final class StyleBottomSheetDialog extends BottomSheetDialogBase {

    private ColorAdapter colorAdapter;
    public static final int NO_COLOR = -1;
    public static final String SERIF_MONOSPACE = "serif-monospace";
    public static final String SERIF = "serif";
    public static final String MONOSPACE = "monospace";
    public static final String MONOSPACE_SHORT = "mono";
    public static final String CURSIVE = "cursive";
    public static final String CASUAL = "casual";

    private CheckBox mBoldCheckbox;
    private CheckBox mItalicCheckbox;

    @Nullable
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) dismiss();
        return inflater.inflate(R.layout.bottom_sheet_style, container, false);
    }

    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
            public void onGlobalLayout() {
                view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                BottomSheetDialog dialog = (BottomSheetDialog) StyleBottomSheetDialog.this.getDialog();
                FrameLayout bottomSheet = dialog != null ? (FrameLayout) dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet) : null;
                BottomSheetBehavior behavior = BottomSheetBehavior.from((View) bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setPeekHeight(0);
                behavior.setBottomSheetCallback(new BottomSheetCallback() {
                    public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                    }

                    public void onStateChanged(@NonNull View bottomSheet, int newState) {
                        if (newState == BottomSheetBehavior.STATE_COLLAPSED) dismiss();
                    }
                });
            }
        });

        mBoldCheckbox = view.findViewById(R.id.text_bold);
        mItalicCheckbox = view.findViewById(R.id.text_italic);
        CheckBox mUpperCheckbox = view.findViewById(R.id.text_upper);
        CheckBox mStrokeCheckbox = view.findViewById(R.id.text_stroke);
        CheckBox mUnderlineCheckbox = view.findViewById(R.id.text_underline);
        CheckBox mStrikeCheckbox = view.findViewById(R.id.text_strikethrough);

        mBoldCheckbox.setChecked(Note.isIsTextStyleBold());
        mItalicCheckbox.setChecked(Note.isIsTextStyleItalic());
        mUpperCheckbox.setChecked(Note.isIsTextStyleUpper());
        mStrokeCheckbox.setChecked(Note.isIsTextStyleStroke());
        mUnderlineCheckbox.setChecked(Note.isIsTextStyleUnderline());
        mStrikeCheckbox.setChecked(Note.isIsTextStyleStrikethrough());

        mBoldCheckbox.setOnClickListener(it -> Note.setIsTextStyleBold(mBoldCheckbox.isChecked()));
        mItalicCheckbox.setOnClickListener(it -> Note.setIsTextStyleItalic(mItalicCheckbox.isChecked()));
        mUpperCheckbox.setOnClickListener(it -> Note.setIsTextStyleUpper(mUpperCheckbox.isChecked()));
        mStrokeCheckbox.setOnClickListener(it -> Note.setIsTextStyleStroke(mStrokeCheckbox.isChecked()));
        mUnderlineCheckbox.setOnClickListener(it -> Note.setIsTextStyleUnderline(mUnderlineCheckbox.isChecked()));
        mStrikeCheckbox.setOnClickListener(it -> Note.setIsTextStyleStrikethrough(mStrikeCheckbox.isChecked()));

        MaterialButtonToggleGroup mFontToggleGroup = view.findViewById(R.id.text_fonts);
        List<Integer> toggleList = Note.getTextFontToggles();
        if (toggleList != null) {
            for (Integer b : Note.getTextFontToggles()) {
                ((MaterialButton) mFontToggleGroup.findViewById(b)).setChecked(true);
            }
        }

        mFontToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!group.findViewById(checkedId).isPressed()) return;
            List<Integer> checkedIds = group.getCheckedButtonIds();
            String font = "";
            if (checkedIds.size() > 1) {
                if (checkedIds.size() == 2 && checkedIds.contains(R.id.toggle_serif_font) && checkedIds.contains(R.id.toggle_mono_font)) {
                    font = SERIF_MONOSPACE;
                } else {
                    for (int i = 0; i < checkedIds.size(); i++) {
                        int id = checkedIds.get(i);
                        if (id != checkedId) {
                            group.uncheck(id);
                            checkedIds.remove(i);
                            i -= 1;
                        }
                    }
                }
            }
            if (font.equals("")) {
                if (checkedIds.size() != 0) {
                    font = ((Button) group.findViewById(checkedId)).getText().toString().toLowerCase();
                    if (font.equals(MONOSPACE_SHORT)) font = MONOSPACE;
                    if (!isChecked) {
                        if (checkedId == R.id.toggle_mono_font) {
                            font = SERIF;
                        } else if (checkedId == R.id.toggle_serif_font) {
                            font = MONOSPACE;
                        }
                    }
                }
            }
            Note.setTextFontToggles(checkedIds);
            Note.setActiveTextFont(font);
        });

        if (colorAdapter != null) {
            RecyclerView mColorRecyclerView = view.findViewById(R.id.colorSheetList);
            mColorRecyclerView.setAdapter(colorAdapter);
        }

    }

    public void onDestroyView() {
        super.onDestroyView();
        colorAdapter = null;
    }

    public final void colorPicker(NoteEditorFragment fragment, @NonNull int[] colors,
                                  @Nullable Integer selectedColor, boolean noColorOption) {
        colorAdapter = new ColorAdapter(this, fragment, colors, selectedColor, noColorOption);
    }

    public final void show(@NonNull FragmentManager fragmentManager) {
        this.show(fragmentManager, "StyleSheet");
    }

}