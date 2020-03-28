package com.theost.wavenote.widgets;

import android.text.style.ClickableSpan;
import android.view.View;

import androidx.annotation.NonNull;

public class CheckableSpan extends ClickableSpan {

    private boolean isChecked;

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }

    @Override
    public void onClick(@NonNull View view) {
        setChecked(!isChecked);
        if (view instanceof WavenoteEditText) {
            try {
                ((WavenoteEditText)view).toggleCheckbox(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
