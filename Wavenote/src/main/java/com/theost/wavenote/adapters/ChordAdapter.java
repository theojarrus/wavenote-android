package com.theost.wavenote.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.theost.wavenote.R;

import java.util.List;

public class ChordAdapter extends RecyclerView.Adapter<ChordAdapter.ViewHolder> {

    private List<Drawable> mData;
    private LayoutInflater mInflater;
    private int mItemSize;

    public ChordAdapter(Context context, List<Drawable> data, int size) {
        this.mInflater = LayoutInflater.from(context);
        this.mData = data;
        this.mItemSize = size;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.chord_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Drawable drawable = mData.get(position);
        holder.mChordImageView.setImageDrawable(drawable);
        holder.mChordImageView.getLayoutParams().width = mItemSize;
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView mChordImageView;

        ViewHolder(View itemView) {
            super(itemView);
            mChordImageView = itemView.findViewById(R.id.chord_photo);
        }
    }

    public void updateItemSize(int size) {
        mItemSize = size;
        notifyDataSetChanged();
    }

    public void updateItemDrawable(List<Drawable> data) {
        mData = data;
        notifyDataSetChanged();
    }

}