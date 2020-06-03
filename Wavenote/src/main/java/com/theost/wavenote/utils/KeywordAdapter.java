package com.theost.wavenote.utils;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.theost.wavenote.DictionaryActivity;
import com.theost.wavenote.R;
import com.theost.wavenote.models.Keyword;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class KeywordAdapter extends RecyclerView.Adapter<KeywordAdapter.ViewHolder> {

    private List<Keyword> mData;
    private Context context;
    private LayoutInflater mInflater;

    public KeywordAdapter(Context context, List<Keyword> data) {
        this.mInflater = LayoutInflater.from(context);
        this.context = context;
        this.mData = data;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.dictionary_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.mWordEditText.setText(mData.get(position).getWord());
        holder.mTypeTextView.setText(mData.get(position).getType());

        holder.mTypeTextView.setOnItemClickListener((parent, view, position1, id) -> {
            String type = holder.mTypeTextView.getText().toString();
            holder.mTypeTextView.clearFocus();
            if (((DictionaryActivity) context).renameKeyword(mData.get(position).getId(), type)) {
                mData.get(position).setType(type);
                notifyDataSetChanged();
            }
        });

        holder.mTrashButton.setOnClickListener(view -> {
            Drawable drawable = holder.mTrashButton.getDrawable();
            DrawableUtils.startAnimatedVectorDrawable(drawable);
            if (((DictionaryActivity) context).removeKeyword(mData.get(position).getId())) {
                mData.remove(position);
                notifyDataSetChanged();
                ((DictionaryActivity) context).checkEmptyView();
            }
        });
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        EditText mWordEditText;
        AutoCompleteTextView mTypeTextView;
        ImageButton mTrashButton;

        ViewHolder(View itemView) {
            super(itemView);
            mWordEditText = itemView.findViewById(R.id.keyword_name);
            mTypeTextView = itemView.findViewById(R.id.keyword_type);
            mTrashButton = itemView.findViewById(R.id.keyword_trash);
            ((DictionaryActivity) context).disableDictionaryInputs(mWordEditText, mTypeTextView);
        }
    }

    public void updateData(List<Keyword> data) {
        this.mData = data;
        notifyDataSetChanged();
    }

    public void clearData() {
        mData.clear();
        notifyDataSetChanged();
    }

    public void sortByDate() {
        Comparator<Keyword> comparator = (k1, k2) -> (Integer.parseInt(k2.getId()) - Integer.parseInt(k1.getId()));
        Collections.sort(mData, comparator);
        notifyDataSetChanged();
    }

    public void sortByName() {
        Comparator<Keyword> comparator = (k1, k2) -> k1.getWord().compareTo(k2.getWord());
        Collections.sort(mData, comparator);
        notifyDataSetChanged();
    }

    public void sortByType() {
        Comparator<Keyword> comparator = (k1, k2) -> k1.getType().compareTo(k2.getType());
        Collections.sort(mData, comparator);
        notifyDataSetChanged();
    }

}
