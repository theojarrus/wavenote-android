package com.theost.wavenote;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MenuCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.theost.wavenote.models.Note;
import com.theost.wavenote.models.Photo;
import com.theost.wavenote.utils.DatabaseHelper;
import com.theost.wavenote.utils.DateTimeUtils;
import com.theost.wavenote.utils.DrawableUtils;
import com.theost.wavenote.utils.PhotoAdapter;
import com.theost.wavenote.utils.SyntaxHighlighter;
import com.theost.wavenote.utils.ThemeUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class PhotosActivity extends ThemedAppCompatActivity {

    private final String[] SORT_TYPES = {"date", "name"};
    private PhotoBottomSheetDialog mPhotoBottomSheet;
    private RecyclerView mPhotoRecyclerView;
    private List<Photo> mPhotoList;
    private PhotoAdapter adapter;
    private LinearLayout emptyView;
    private DatabaseHelper localDatabase;
    private MenuItem mRemoveItem;
    private MenuItem mSortItem;
    private String noteId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        ThemeUtils.setTheme(this);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_photos);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        if (getIntent().getBooleanExtra("chordsBlockEnabled", false))
            findViewById(R.id.chords_block).setVisibility(View.VISIBLE);

        emptyView = findViewById(android.R.id.empty);
        ImageView mEmptyViewImage = emptyView.findViewById(R.id.image);
        TextView mEmptyViewText = emptyView.findViewById(R.id.text);
        mEmptyViewImage.setImageResource(R.drawable.m_insert_photo_black_24dp);
        mEmptyViewText.setText(R.string.empty_photos);

        noteId = getIntent().getStringExtra("noteId");
        if (noteId.equals("theory")) {
            setTitle(R.string.theory);
        } else {
            setTitle(R.string.photos);
        }

        mPhotoBottomSheet = new PhotoBottomSheetDialog(this);

        mPhotoRecyclerView = findViewById(R.id.photos_list);

        localDatabase = new DatabaseHelper(this);

        updateData();
        adapter = new PhotoAdapter(this, mPhotoList);
        mPhotoRecyclerView.setAdapter(adapter);

        sortPhotos(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.items_list, menu);
        mRemoveItem = menu.findItem(R.id.menu_remove);
        mSortItem = menu.findItem(R.id.menu_sort);
        menu.findItem(R.id.menu_restore).setVisible(false);
        MenuCompat.setGroupDividerEnabled(menu, true);
        checkEmptyView();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_add:
                showPhotoSheet();
                return true;
            case R.id.menu_sort:
                sortPhotos(true);
                return true;
            case R.id.menu_remove:
                removePhoto(null);
                return true;
            default:
                return false;
        }
    }

    public void checkEmptyView() {
        if (adapter.getItemCount() == 0) {
            mRemoveItem.setEnabled(false);
            mSortItem.setEnabled(false);
            DrawableUtils.setMenuItemAlpha(mSortItem, 0.3);
            if (noteId.equals("theory")) return;
            mPhotoRecyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            mPhotoRecyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
            mRemoveItem.setEnabled(true);
            mSortItem.setEnabled(true);
            DrawableUtils.setMenuItemAlpha(mSortItem, 1.0);
        }
    }

    private void showPhotoSheet() {
        mPhotoBottomSheet.show(getSupportFragmentManager());
    }

    private void showError(String error) {
        Toast toast = Toast.makeText(this, error, Toast.LENGTH_SHORT);
        toast.show();
    }

    private void updateData() {
        Cursor mImageData = localDatabase.getImageData(noteId);
        if (mImageData == null) return;
        mPhotoList = new ArrayList<>();
        while (mImageData.moveToNext()) {
            String id = mImageData.getString(0);
            String name = mImageData.getString(2);
            String image = mImageData.getString(3);
            String date = mImageData.getString(4);
            Photo photo = new Photo(id, name, image, date);
            if (photo.getBitmap(this) != null) {
                mPhotoList.add(photo);
            } else {
                localDatabase.removeImageData(id);
                showError(this.getResources().getString(R.string.file_error));
            }
        }
        if (adapter != null) adapter.updateData(mPhotoList);
    }

    public void insertPhoto(Uri selectedImage) {
        if (selectedImage == null) {
            showError(this.getResources().getString(R.string.photo_error));
            return;
        }

        String uri = selectedImage.toString();
        String date = DateTimeUtils.getDateTextString(this, Calendar.getInstance());
        boolean isInserted = localDatabase.insertImageData(noteId, "", uri, date);
        if (!isInserted) showError(this.getResources().getString(R.string.database_error));

        updateData();
        sortPhotos(false);
        checkEmptyView();
    }

    public void renamePhoto(String id, String name) {
        boolean isRenamed = localDatabase.renameImageData(id, name);
        if (!isRenamed) {
            showError(this.getResources().getString(R.string.database_error));
        }
    }

    public boolean removePhoto(String id) {
        if (adapter.getItemCount() == 0) return false;
        boolean isRemoved;
        if (id == null) {
            isRemoved = localDatabase.removeAllImageData(noteId);
            if (isRemoved) {
                adapter.clearData();
                checkEmptyView();
            }
        } else {
            isRemoved = localDatabase.removeImageData(id);
        }
        if (!isRemoved) {
            showError(this.getResources().getString(R.string.database_error));
            return false;
        }
        return true;
    }

    private void sortPhotos(boolean isModeChanged) {
        if (adapter.getItemCount() == 0) return;
        if (isModeChanged) {
            int index = Arrays.asList(SORT_TYPES).indexOf(Note.getPhotoSortMode()) + 1;
            if (index == SORT_TYPES.length) index = 0;
            Note.setPhotoSortMode(SORT_TYPES[index]);
        }
        if (Note.getPhotoSortMode().equals(SORT_TYPES[0])) {
            adapter.sortByDate();
        } else if (Note.getPhotoSortMode().equals(SORT_TYPES[1])) {
            adapter.sortByName();
        }
    }

    public void startChordsActivity(View view) {
        Intent intent = new Intent(this, ChordsActivity.class);
        intent.putExtra("isAllChords", true);
        intent.putExtra("chords", SyntaxHighlighter.getAllChords(this));
        intent.putExtra("activeInstrument", ((Button) view).getText().toString());
        startActivity(intent);
    }

}
