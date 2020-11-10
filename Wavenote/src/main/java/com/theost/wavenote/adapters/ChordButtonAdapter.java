package com.theost.wavenote.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.theost.wavenote.ChordsActivity;
import com.theost.wavenote.R;
import com.theost.wavenote.models.Note;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ChordButtonAdapter extends RecyclerView.Adapter<ChordButtonAdapter.ViewHolder> {

    private ChordsActivity mActivity;
    private List<String> mData;
    private Map<Integer, String> mWordsData;
    private LayoutInflater mInflater;
    private int mItemSize;
    private int mWordsSize;
    private int[] mColors;

    public ChordButtonAdapter(ChordsActivity activity, int itemSize, int wordsSize, int[] colors) {
        this.mActivity = activity;
        this.mInflater = LayoutInflater.from(activity);
        this.mItemSize = itemSize;
        this.mWordsSize = wordsSize;
        this.mColors = colors;
        this.mData = new ArrayList<>();
        this.mWordsData = new TreeMap<>();
    }

    @NonNull
    @Override
    public ChordButtonAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.chord_button, parent, false);
        return new ChordButtonAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ChordButtonAdapter.ViewHolder holder, int position) {
        holder.mChordButton.setAlpha(0.0f);
        holder.mChordButton.setText(mData.get(position));
        holder.mChordButton.animate().alpha(1.0f).setDuration(500);
        if ((mWordsData.containsKey(position)) && (Note.isChordGridEnabled())) {
            holder.mChordButton.setTextColor(mColors[2]);
            holder.mChordButton.setBackgroundColor(mColors[3]);
            holder.mChordButton.setEnabled(false);
            holder.mChordButton.setLayoutParams(new LinearLayout.LayoutParams(mWordsSize, LinearLayout.LayoutParams.WRAP_CONTENT));
            holder.mChordButton.setOnClickListener(null);
        } else {
            holder.mChordButton.setTextColor(mColors[0]);
            holder.mChordButton.setBackgroundColor(mColors[1]);
            holder.mChordButton.setEnabled(true);
            holder.mChordButton.setLayoutParams(new LinearLayout.LayoutParams(mItemSize, mItemSize));
            holder.mChordButton.setOnClickListener(v -> mActivity.showChords(mData.get(position)));
        }
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        Button mChordButton;

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

    public void updateItemSize(int itemSize, int wordSize) {
        mItemSize = itemSize;
        mWordsSize = wordSize;
        notifyDataSetChanged();
    }

    public void updateItemDrawable(List<String> data) {
        mData = new ArrayList<>(data);
        notifyDataSetChanged();
    }

    public void showWordsData() {
        for (Integer i : mWordsData.keySet()) mData.add(i, mWordsData.get(i));
        notifyDataSetChanged();
    }

}
