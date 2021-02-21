package com.theost.wavenote.adapters;

import android.content.res.ColorStateList;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.theost.wavenote.ChordsActivity;
import com.theost.wavenote.R;
import com.theost.wavenote.models.Note;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ChordButtonAdapter extends RecyclerView.Adapter<ChordButtonAdapter.ViewHolder> {

    private final ChordsActivity mActivity;
    private List<String> mData;
    private Map<Integer, String> mWordsData;
    private final LayoutInflater mInflater;
    private int mItemSize;
    private int mWordsSize;
    private int mTextSize;
    private final int[] mColors;
    private final boolean isAllChords;

    public ChordButtonAdapter(ChordsActivity activity, int itemSize, int wordsSize, int textSize, int[] colors, boolean isAllChords) {
        this.mActivity = activity;
        this.mInflater = LayoutInflater.from(activity);
        this.mItemSize = itemSize;
        this.mWordsSize = wordsSize;
        this.mTextSize = textSize;
        this.mColors = colors;
        this.mData = new ArrayList<>();
        this.mWordsData = new TreeMap<>();
        this.isAllChords = isAllChords;
    }

    @NonNull
    @Override
    public ChordButtonAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.chord_button, parent, false);
        return new ChordButtonAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.mChordButton.setAlpha(0.0f);
        if (!isAllChords && mWordsData.containsKey(position) && Note.isChordGridEnabled()) {
            holder.mChordButton.setTextColor(mColors[2]);
            holder.mChordButton.setEnabled(false);
            holder.mChordButton.setStrokeColor(ColorStateList.valueOf(mColors[3]));
            holder.mChordButton.setLayoutParams(new LinearLayout.LayoutParams(mWordsSize, LinearLayout.LayoutParams.WRAP_CONTENT));
            holder.mChordButton.setOnClickListener(null);
            holder.mChordButton.setBackgroundColor(mColors[3]);
        } else {
            holder.mChordButton.setTextColor(mColors[0]);
            holder.mChordButton.setEnabled(true);
            holder.mChordButton.setStrokeColor(ColorStateList.valueOf(mColors[1]));
            holder.mChordButton.setLayoutParams(new LinearLayout.LayoutParams(mItemSize, mItemSize));
            holder.mChordButton.setOnClickListener(v -> mActivity.showChords(mData.get(position), false));
            if (!isAllChords) {
                holder.mChordButton.setBackgroundColor(mColors[1]);
            } else {
                holder.mChordButton.setTextColor(mColors[1]);
                holder.mChordButton.setBackgroundColor(mColors[0]);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    holder.mChordButton.setForeground(null);
                }
            }
        }
        holder.mChordButton.setTextSize(mTextSize);
        holder.mChordButton.setText(mData.get(position));
        holder.mChordButton.animate().alpha(1.0f).setDuration(300);
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialButton mChordButton;

        ViewHolder(View itemView) {
            super(itemView);
            mChordButton = itemView.findViewById(R.id.chord_button);
        }
    }

    public void updateChordData(List<String> data) {
        this.mData = new ArrayList<>(data);
        notifyDataSetChanged();
    }

    public void updateWordsData(Map<Integer, String> words) {
        this.mWordsData = new TreeMap<>(words);
    }

    public void updateItemSize(int itemSize, int wordSize, int textSize) {
        mItemSize = itemSize;
        mWordsSize = wordSize;
        mTextSize = textSize;
        notifyDataSetChanged();
    }

    public void showWordsData() {
        for (Integer i : mWordsData.keySet()) mData.add(i, mWordsData.get(i));
        notifyDataSetChanged();
    }

}
