package com.theost.wavenote.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import com.theost.wavenote.NoteEditorFragment;
import com.theost.wavenote.R;
import com.theost.wavenote.StyleBottomSheetDialog;
import com.theost.wavenote.utils.ThemeUtils;

import static com.theost.wavenote.StyleBottomSheetDialog.NO_COLOR;

public final class ColorAdapter extends Adapter {
    private final StyleBottomSheetDialog dialog;
    private final NoteEditorFragment fragment;
    private final Integer selectedColor;
    private final boolean noColorOption;
    private int[] colors;

    @NonNull
    public ColorAdapter.ColorItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View color = inflater.inflate(R.layout.color_item, parent, false);
        return new ColorAdapter.ColorItemViewHolder(color);
    }

    public int getItemCount() {
        return noColorOption ? colors.length + 1 : colors.length;
    }

    public void onBindViewHolder(@NonNull ColorAdapter.ColorItemViewHolder holder, int position) {
        holder.bindView();
    }

    public void onBindViewHolder(@NonNull ViewHolder var1, int var2) {
        this.onBindViewHolder((ColorAdapter.ColorItemViewHolder) var1, var2);
    }

    public ColorAdapter(@Nullable StyleBottomSheetDialog dialog, NoteEditorFragment fragment, @NonNull int[] colors, @Nullable Integer selectedColor, boolean noColorOption) {
        this.dialog = dialog;
        this.fragment = fragment;
        this.colors = colors;
        this.selectedColor = selectedColor;
        this.noColorOption = noColorOption;
    }

    public void setColors(int[] colors) {
        this.colors = colors;
    }

    public final class ColorItemViewHolder extends ViewHolder implements View.OnClickListener {
        ImageView mSelectedImageView;
        ImageView mSelectedCircleImageView;

        public final void bindView() {
            int position = getAdapterPosition();
            int color;
            if (noColorOption) {
                if (position != 0) {
                    color = colors[position - 1];
                    bindColorView(color);
                } else {
                    bindNoColorView();
                }
            } else {
                color = colors[position];
                bindColorView(color);
            }
        }

        private void bindColorView(@ColorInt int color) {
            mSelectedImageView.setVisibility((selectedColor != null && selectedColor == color) ? View.VISIBLE : View.GONE);
            mSelectedImageView.setImageResource(R.drawable.ic_check);
            if (ThemeUtils.isColorDark(color)) {
                mSelectedImageView.setColorFilter(Color.WHITE);
            } else {
                mSelectedImageView.setColorFilter(Color.BLACK);
            }
            mSelectedCircleImageView.setColorFilter(color);
        }

        private void bindNoColorView() {
            mSelectedImageView.setVisibility(View.VISIBLE);
            if (selectedColor != null && selectedColor == NO_COLOR) {
                mSelectedImageView.setImageResource(R.drawable.ic_check);
            } else {
                mSelectedImageView.setImageResource(R.drawable.ic_no_color);
            }
            mSelectedImageView.setColorFilter(Color.WHITE);
            mSelectedCircleImageView.setColorFilter(Color.BLACK);
        }

        public void onClick(@Nullable View v) {
            int position = getAdapterPosition();
            int selected;
            if (noColorOption) {
                if (position == 0) {
                    selected = NO_COLOR;
                } else {
                    selected = colors[position - 1];
                }
            } else {
                selected = colors[position];
            }
            fragment.updateTextColor(selected);
            dialog.dismiss();
        }

        public ColorItemViewHolder(@NonNull final View itemView) {
            super(itemView);
            mSelectedImageView = itemView.findViewById(R.id.colorSelected);
            mSelectedCircleImageView = itemView.findViewById(R.id.colorSelectedCircle);
            itemView.setOnClickListener(this);
        }
    }
}
