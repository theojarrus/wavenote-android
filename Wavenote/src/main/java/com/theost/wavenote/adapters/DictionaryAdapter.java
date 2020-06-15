package com.theost.wavenote.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

public class DictionaryAdapter extends RecyclerView.Adapter<DictionaryAdapter.ViewHolder> {

    private final static int FADE_DURATION = 1500;

    private List<Keyword> mData;
    private DictionaryActivity mActivity;
    private LayoutInflater mInflater;

    public DictionaryAdapter(DictionaryActivity activity, List<Keyword> data) {
        this.mInflater = LayoutInflater.from(activity);
        this.mActivity = activity;
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
            if (mActivity.renameKeyword(mData.get(position).getId(), type)) {
                mData.get(position).setType(type);
            }
        });

        holder.mTrashButton.setOnClickListener(view -> {
            if (mActivity.removeKeyword(mData.get(position).getId(), mData.get(position).getWord())) {
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

    public class ViewHolder extends RecyclerView.ViewHolder {
        EditText mWordEditText;
        AutoCompleteTextView mTypeTextView;
        ImageButton mTrashButton;

        ViewHolder(View itemView) {
            super(itemView);
            mWordEditText = itemView.findViewById(R.id.keyword_name);
            mTypeTextView = itemView.findViewById(R.id.keyword_type);
            mTrashButton = itemView.findViewById(R.id.keyword_trash);
            mActivity.disableDictionaryInputs(mWordEditText, mTypeTextView);
        }
    }

    public void updateData(List<Keyword> data) {
        this.mData = data;
        notifyItemRangeChanged(0, mData.size());
    }

    public void clearData() {
        mData.clear();
        notifyItemRangeChanged(0, mData.size());
    }

    public void sortByDate(boolean isSortReversed) {
        Comparator<Keyword> comparator;
        if (!isSortReversed) {
            comparator = (k1, k2) -> (Integer.parseInt(k2.getId()) - Integer.parseInt(k1.getId()));
        } else {
            comparator = (k1, k2) -> Integer.parseInt(k1.getId()) - (Integer.parseInt(k2.getId()));
        }
        Collections.sort(mData, comparator);
        notifyItemRangeChanged(0, mData.size());
    }

    public void sortByName(boolean isSortReversed) {
        Comparator<Keyword> comparator;
        if (!isSortReversed) {
            comparator = (k1, k2) -> k1.getWord().toLowerCase().compareTo(k2.getWord().toLowerCase());
        } else {
            comparator = (k1, k2) -> k2.getWord().toLowerCase().compareTo(k1.getWord().toLowerCase());
        }
        Collections.sort(mData, comparator);
        notifyItemRangeChanged(0, mData.size());
    }

    public void sortByType(boolean isSortReversed) {
        Comparator<Keyword> comparator;
        if (!isSortReversed) {
            comparator = (k1, k2) -> k1.getType().compareTo(k2.getType());
        } else {
            comparator = (k1, k2) -> k2.getType().compareTo(k1.getType());
        }
        Collections.sort(mData, comparator);
        notifyItemRangeChanged(0, mData.size());
    }

}
