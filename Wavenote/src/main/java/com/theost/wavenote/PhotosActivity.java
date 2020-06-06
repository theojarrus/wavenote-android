package com.theost.wavenote;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MenuCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.theost.wavenote.models.Note;
import com.theost.wavenote.models.Photo;
import com.theost.wavenote.utils.DatabaseHelper;
import com.theost.wavenote.utils.DateTimeUtils;
import com.theost.wavenote.utils.DisplayUtils;
import com.theost.wavenote.utils.DrawableUtils;
import com.theost.wavenote.utils.FileUtils;
import com.theost.wavenote.utils.PermissionUtils;
import com.theost.wavenote.utils.PhotoAdapter;
import com.theost.wavenote.utils.SyntaxHighlighter;
import com.theost.wavenote.utils.ThemeUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import static com.theost.wavenote.utils.FileUtils.NOTES_DIR;

public class PhotosActivity extends ThemedAppCompatActivity {

    private ImportPhotosThread importPhotosThread;

    private boolean isImporting;

    private PhotoBottomSheetDialog mPhotoBottomSheet;
    private RecyclerView mPhotoRecyclerView;
    private List<Photo> mPhotoList;
    private PhotoAdapter adapter;
    private LinearLayout emptyView;
    private LinearLayout mMaterialTitle;

    private MaterialDialog loadingDialog;

    private String[] sortModes;

    private String importImagePath;
    private String noteId;

    private DatabaseHelper localDatabase;
    private MenuItem mRemoveItem;
    private MenuItem mSortItem;

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

        sortModes = getResources().getStringArray(R.array.array_sort_photos);

        mPhotoRecyclerView = findViewById(R.id.photos_list);
        mMaterialTitle = findViewById(R.id.materials_title);

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
                if (PermissionUtils.requestPermissions(this))
                    showPhotoSheet();
                return true;
            case R.id.menu_sort:
                sortPhotos(true);
                return true;
            case R.id.menu_remove:
                removePhoto(null);
                return true;
            case android.R.id.home:
                invalidateOptionsMenu();
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
            if (noteId.equals("theory")) {
                mMaterialTitle.setVisibility(View.GONE);
                return;
            }
            mPhotoRecyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            mPhotoRecyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
            mRemoveItem.setEnabled(true);
            mSortItem.setEnabled(true);
            DrawableUtils.setMenuItemAlpha(mSortItem, 1.0);
            if (noteId.equals("theory"))
                mMaterialTitle.setVisibility(View.VISIBLE);
        }
    }

    public String getNoteId() {
        return noteId;
    }

    private void showPhotoSheet() {
        mPhotoBottomSheet.show(getSupportFragmentManager());
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
                DisplayUtils.showToast(this, getResources().getString(R.string.file_error));
                boolean isRemoved = localDatabase.removeImageData(id);
                if (!isRemoved) {
                    DisplayUtils.showToast(this, getResources().getString(R.string.database_error));
                }
            }
        }
        if (adapter != null) adapter.updateData(mPhotoList);
    }

    public void insertPhoto(String uri) {
        if (uri == null) {
            DisplayUtils.showToast(this, getResources().getString(R.string.photo_error));
            return;
        }

        String date = DateTimeUtils.getDateTextString(this, Calendar.getInstance());
        boolean isInserted = localDatabase.insertImageData(noteId, "", uri, date);
        if (!isInserted) {
            DisplayUtils.showToast(this, getResources().getString(R.string.database_error));
            File imageFile = new File(uri);
            if (imageFile.exists()) imageFile.delete();
        }

        updateData();
        sortPhotos(false);
        checkEmptyView();
    }

    public void renamePhoto(String id, String name) {
        boolean isRenamed = localDatabase.renameImageData(id, name);
        if (!isRenamed) {
            DisplayUtils.showToast(this, getResources().getString(R.string.database_error));
        }
    }

    public boolean removePhoto(String id) {
        if (adapter.getItemCount() == 0) return false;
        boolean isRemovedFile = false;
        boolean isRemoved = false;
        if (id == null) {
            isRemovedFile = FileUtils.removeFiles(new File(this.getCacheDir() + NOTES_DIR + noteId));
            if (isRemovedFile) {
                isRemoved = localDatabase.removeAllImageData(noteId);
                if (isRemoved) {
                    adapter.clearData();
                    checkEmptyView();
                }
            }
        } else {
            String path = localDatabase.getImageUri(id);
            if (path != null)
                isRemovedFile = FileUtils.removeFile(path);
            if (isRemovedFile) {
                isRemoved = localDatabase.removeImageData(id);
            }
        }
        if (!isRemovedFile) {
            DisplayUtils.showToast(this, getResources().getString(R.string.file_error));
            return false;
        } else if (!isRemoved) {
            DisplayUtils.showToast(this, getResources().getString(R.string.database_error));
            return false;
        }
        return true;
    }

    private void sortPhotos(boolean isModeChanged) {
        if (adapter.getItemCount() == 0) return;
        if (isModeChanged) {
            int index = Arrays.asList(sortModes).indexOf(Note.getNotePhotosSort()) + 1;
            if (index == sortModes.length) index = 0;
            Note.setNotePhotosSort(sortModes[index]);
        }
        if (Note.getNotePhotosSort().equals(sortModes[0])) {
            adapter.sortByDate();
        } else if (Note.getNotePhotosSort().equals(sortModes[1])) {
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

    public void importPhoto(File imageFile, Bitmap imageBitmap) {
        importImagePath = imageFile.getPath();
        importPhotosThread = new ImportPhotosThread(imageFile, imageBitmap);
        importPhotosThread.start();
        showLoadingDialog();
    }

    private void showLoadingDialog() {
        loadingDialog = DisplayUtils.showLoadingDialog(this, R.string.import_note, R.string.importing);
    }

    private Handler mImportHandler = new Handler(msg -> {
        if (msg.what == 0) {
            insertPhoto(importImagePath);
            importImagePath = null;
        } else {
            DisplayUtils.showToast(this, getResources().getString(R.string.file_error));
        }
        loadingDialog.dismiss();
        return true;
    });

    private class ImportPhotosThread extends Thread {
        File imageFile;
        Bitmap imageBitmap;

        private ImportPhotosThread(File imageFile, Bitmap imageBitmap) {
            this.imageFile = imageFile;
            this.imageBitmap = imageBitmap;
        }

        public void run() {
            isImporting = true;
            try {
                FileUtils.createPhotoFile(imageBitmap, imageFile);
                mImportHandler.sendEmptyMessage(0);
            } catch (IOException e) {
                e.printStackTrace();
                mImportHandler.sendEmptyMessage(-1);
            }
            isImporting = false;
        }
    }

}
