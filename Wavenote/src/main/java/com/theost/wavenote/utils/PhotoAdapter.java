package com.theost.wavenote.utils;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.theost.wavenote.PhotosActivity;
import com.theost.wavenote.R;
import com.theost.wavenote.models.Photo;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.ViewHolder> {

    private List<Photo> mData;
    private Context context;
    private LayoutInflater mInflater;

    public PhotoAdapter(Context context, List<Photo> data) {
        this.mInflater = LayoutInflater.from(context);
        this.context = context;
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
        holder.mPhotoView.setImageBitmap(mData.get(position).getBitmap(context));
        holder.mDateTextView.setText(mData.get(position).getDate());

        InputMethodManager keyboard = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);

        holder.mNameEditText.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                holder.photoName = holder.mNameEditText.getText().toString();
                holder.mSaveButton.setVisibility(View.VISIBLE);
                holder.mCancelButton.setVisibility(View.VISIBLE);
                holder.mTrashButton.setVisibility(View.INVISIBLE);
            } else {
                holder.mNameEditText.setText(holder.photoName);
                holder.mSaveButton.setVisibility(View.INVISIBLE);
                holder.mCancelButton.setVisibility(View.INVISIBLE);
                holder.mTrashButton.setVisibility(View.VISIBLE);
            }
        });

        holder.mSaveButton.setOnClickListener(view -> {
            holder.photoName = holder.mNameEditText.getText().toString();
            holder.mNameEditText.clearFocus();
            keyboard.hideSoftInputFromWindow(holder.itemView.getWindowToken(), 0);
            mData.get(position).setName(holder.photoName);
            ((PhotosActivity) context).renamePhoto(mData.get(position).getId(), holder.photoName);
        });

        holder.mCancelButton.setOnClickListener(view -> {
            holder.mNameEditText.clearFocus();
            keyboard.hideSoftInputFromWindow(holder.itemView.getWindowToken(), 0);
        });

        holder.mTrashButton.setOnClickListener(view -> {
            Drawable drawable = holder.mTrashButton.getDrawable();
            DrawableUtils.startAnimatedVectorDrawable(drawable);
            if (((PhotosActivity) context).removePhoto(mData.get(position).getId())) {
                mData.remove(position);
                notifyDataSetChanged();
                ((PhotosActivity) context).checkEmptyView();
            }
        });
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        EditText mNameEditText;
        ImageView mPhotoView;
        TextView mDateTextView;
        ImageButton mSaveButton;
        ImageButton mCancelButton;
        ImageButton mTrashButton;
        String photoName;

        ViewHolder(View itemView) {
            super(itemView);
            mNameEditText = itemView.findViewById(R.id.photo_name);
            mPhotoView = itemView.findViewById(R.id.photo_image);
            mDateTextView = itemView.findViewById(R.id.date_image_created);
            mSaveButton = itemView.findViewById(R.id.photo_name_save);
            mCancelButton = itemView.findViewById(R.id.photo_name_cancel);
            mTrashButton = itemView.findViewById(R.id.photo_trash);
        }
    }

    public void updateData(List<Photo> data) {
        this.mData = data;
        notifyDataSetChanged();
    }

    public void clearData() {
        mData.clear();
        notifyDataSetChanged();
    }

    public void sortByDate() {
        Comparator<Photo> comparator = (k1, k2) -> (Integer.parseInt(k2.getId()) - Integer.parseInt(k1.getId()));
            Collections.sort(mData, comparator);
        notifyDataSetChanged();
    }

    public void sortByName() {
        Comparator<Photo> comparator = (p1, p2) -> p1.getName().compareTo(p2.getName());
        Collections.sort(mData, comparator);
        notifyDataSetChanged();
    }

}