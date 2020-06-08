package com.theost.wavenote.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ablanco.zoomy.Zoomy;
import com.theost.wavenote.PhotosActivity;
import com.theost.wavenote.R;
import com.theost.wavenote.models.Photo;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.ViewHolder> {

    private List<Photo> mData;
    private PhotosActivity mActivity;
    private LayoutInflater mInflater;

    public PhotoAdapter(PhotosActivity activity, List<Photo> data) {
        this.mInflater = LayoutInflater.from(activity);
        this.mActivity = activity;
        this.mData = data;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.photo_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.mNameEditText.setText(mData.get(position).getName());
        holder.mPhotoView.setImageBitmap(mData.get(position).getBitmap(mActivity));
        holder.mDateTextView.setText(mData.get(position).getDate());

        InputMethodManager keyboard = (InputMethodManager) mActivity.getSystemService(Context.INPUT_METHOD_SERVICE);

        holder.mNameEditText.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                holder.photoName = holder.mNameEditText.getText().toString();
                holder.mSaveButton.setVisibility(View.VISIBLE);
                holder.mCancelButton.setVisibility(View.VISIBLE);
                holder.mFullscreenButton.setVisibility(View.INVISIBLE);
                holder.mTrashButton.setVisibility(View.INVISIBLE);
            } else {
                holder.mNameEditText.setText(holder.photoName);
                holder.mSaveButton.setVisibility(View.INVISIBLE);
                holder.mCancelButton.setVisibility(View.INVISIBLE);
                holder.mFullscreenButton.setVisibility(View.VISIBLE);
                holder.mTrashButton.setVisibility(View.VISIBLE);
            }
        });

        Zoomy.Builder builder = new Zoomy.Builder(mActivity)
                .target(holder.mPhotoView)
                .interpolator(new OvershootInterpolator())
                .tapListener(v -> mActivity.startSliderActivity(position))
                .doubleTapListener(v -> mActivity.startSliderActivity(position));

        builder.register();

        holder.mSaveButton.setOnClickListener(view -> {
            holder.photoName = holder.mNameEditText.getText().toString();
            holder.mNameEditText.clearFocus();
            keyboard.hideSoftInputFromWindow(holder.itemView.getWindowToken(), 0);
            mData.get(position).setName(holder.photoName);
            mActivity.renamePhoto(mData.get(position).getId(), holder.photoName);
        });

        holder.mCancelButton.setOnClickListener(view -> {
            holder.mNameEditText.clearFocus();
            keyboard.hideSoftInputFromWindow(holder.itemView.getWindowToken(), 0);
        });

        holder.mFullscreenButton.setOnClickListener(view ->
                mActivity.startSliderActivity(position));

        holder.mTrashButton.setOnClickListener(view -> {
            if (mActivity.removePhoto(mData.get(position).getId())) {
                mData.remove(position);
                notifyItemRemoved(position);
                notifyItemRangeChanged(position, getItemCount());
                mActivity.checkEmptyView();
            }
        });
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        EditText mNameEditText;
        ImageView mPhotoView;
        TextView mDateTextView;
        ImageButton mSaveButton;
        ImageButton mCancelButton;
        ImageButton mFullscreenButton;
        ImageButton mTrashButton;
        String photoName;

        ViewHolder(View itemView) {
            super(itemView);
            mNameEditText = itemView.findViewById(R.id.photo_name);
            mPhotoView = itemView.findViewById(R.id.photo_image);
            mDateTextView = itemView.findViewById(R.id.date_image_created);
            mSaveButton = itemView.findViewById(R.id.photo_name_save);
            mCancelButton = itemView.findViewById(R.id.photo_name_cancel);
            mFullscreenButton = itemView.findViewById(R.id.photo_fullscreen);
            mTrashButton = itemView.findViewById(R.id.photo_trash);
        }
    }

    public void updateData(List<Photo> data) {
        this.mData = data;
        notifyItemRangeChanged(0, mData.size());
    }

    public void clearData() {
        mData.clear();
        notifyItemRangeChanged(0, mData.size());
    }

    public void sortByDate(boolean isSortReversed) {
        Comparator<Photo> comparator;
        if (!isSortReversed) {
            comparator = (k1, k2) -> (Integer.parseInt(k2.getId()) - Integer.parseInt(k1.getId()));
        } else {
            comparator = (k1, k2) -> Integer.parseInt(k1.getId()) - (Integer.parseInt(k2.getId()));
        }
        Collections.sort(mData, comparator);
        notifyItemRangeChanged(0, mData.size());
    }

    public void sortByName(boolean isSortReversed) {
        Comparator<Photo> comparator;
        if (!isSortReversed) {
            comparator = (p1, p2) -> p1.getName().compareTo(p2.getName());
        } else {
            comparator = (p1, p2) -> p2.getName().compareTo(p1.getName());
        }
        Collections.sort(mData, comparator);
        notifyItemRangeChanged(0, mData.size());
    }

}