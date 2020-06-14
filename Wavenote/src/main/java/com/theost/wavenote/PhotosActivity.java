package com.theost.wavenote;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MenuCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.theost.wavenote.models.Note;
import com.theost.wavenote.models.Photo;
import com.theost.wavenote.utils.DatabaseHelper;
import com.theost.wavenote.utils.DateTimeUtils;
import com.theost.wavenote.utils.DisplayUtils;
import com.theost.wavenote.utils.FileUtils;
import com.theost.wavenote.utils.PermissionUtils;
import com.theost.wavenote.utils.PhotoAdapter;
import com.theost.wavenote.utils.SyntaxHighlighter;
import com.theost.wavenote.utils.ThemeUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static com.theost.wavenote.utils.FileUtils.NOTES_DIR;

public class PhotosActivity extends ThemedAppCompatActivity {

    public static final String THEORY_PREFIX = "theory";
    public static final String ARG_NOTE_ID = "note_id";
    public static final String ARG_UPDATE = "need_update";

    private ImportPhotosThread importPhotosThread;

    private boolean isImporting;

    private PhotoBottomSheetDialog mPhotoBottomSheet;
    private RecyclerView mPhotoRecyclerView;
    private List<Photo> mPhotoList;
    private PhotoAdapter adapter;
    private LinearLayout emptyView;
    private LinearLayout mMaterialTitle;
    private RelativeLayout mSortLayoutContent;
    private ObjectAnimator mSortDirectionAnimation;

    private ImageView mSortDirection;
    private TextView mSortOrder;

    private MaterialDialog loadingDialog;

    private String importImagePath;
    private String noteId;

    private boolean mIsSortDown;
    private boolean mIsSortReverse;

    private DatabaseHelper localDatabase;
    private MenuItem mRemoveItem;

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

        emptyView = findViewById(android.R.id.empty);
        ImageView mEmptyViewImage = emptyView.findViewById(R.id.image);
        TextView mEmptyViewText = emptyView.findViewById(R.id.text);
        mEmptyViewImage.setImageResource(R.drawable.m_insert_photo_black_24dp);
        mEmptyViewText.setText(R.string.empty_photos);

        if (noteId == null) noteId = getIntent().getStringExtra(ARG_NOTE_ID);
        if (noteId.equals(THEORY_PREFIX)) {
            setTitle(R.string.theory);
            findViewById(R.id.chords_block).setVisibility(View.VISIBLE);
        } else {
            setTitle(R.string.photos);
        }

        mPhotoBottomSheet = new PhotoBottomSheetDialog(this);

        if (Note.getPhotoActiveSortMode() == 0)
            Note.setPhotoActiveSortMode(R.id.sort_by_date);

        mPhotoRecyclerView = findViewById(R.id.photos_list);
        mMaterialTitle = findViewById(R.id.materials_title);

        localDatabase = new DatabaseHelper(this);

        updateData();
        adapter = new PhotoAdapter(this, mPhotoList);
        mPhotoRecyclerView.setAdapter(adapter);

        mSortLayoutContent = findViewById(R.id.sort_content);
        mSortOrder = findViewById(R.id.sort_order);

        PopupMenu popup = new PopupMenu(mSortOrder.getContext(), mSortOrder, Gravity.START);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.items_sort, popup.getMenu());
        popup.getMenu().findItem(R.id.sort_by_type).setVisible(false);

        mSortOrder.setText((popup.getMenu().findItem(Note.getPhotoActiveSortMode())).getTitle());

        mSortLayoutContent.setOnClickListener(v -> {
            popup.setOnMenuItemClickListener(item -> {
                // Do nothing when same sort is selected.
                if (mSortOrder.getText().equals(item.getTitle()))
                    return false;
                mSortOrder.setText(item.getTitle());
                Note.setPhotoActiveSortMode(item.getItemId());
                sortItems();
                return true;
            });
            popup.show();
        });

        mIsSortReverse = Note.isPhotoSortDirRev();

        mSortDirection = findViewById(R.id.sort_direction);
        ImageView sortDirectionSwitch = findViewById(R.id.sort_direction_switch);
        sortDirectionSwitch.setImageResource(R.drawable.ic_sort_order_24dp);
        sortDirectionSwitch.setOnClickListener(v -> {
            if (Note.isPhotoSortDirRev()) {
                Note.setPhotoSortDirRev(false);
                if (mIsSortReverse) {
                    mSortDirectionAnimation.start();
                } else {
                    mSortDirectionAnimation.reverse();
                }
            } else {
                Note.setPhotoSortDirRev(true);
                if (!mIsSortReverse) {
                    mSortDirectionAnimation.start();
                } else {
                    mSortDirectionAnimation.reverse();
                }
            }
            sortItems();
        });

        sortDirectionSwitch.setOnLongClickListener(v -> {
            if (v.isHapticFeedbackEnabled()) {
                v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            }
            Toast.makeText(this, this.getString(R.string.sort_search_reverse_order), Toast.LENGTH_SHORT).show();
            return true;
        });

        setSortDirection();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.items_list, menu);
        mRemoveItem = menu.findItem(R.id.menu_remove);
        menu.findItem(R.id.menu_restore).setVisible(false);
        MenuCompat.setGroupDividerEnabled(menu, true);
        if (checkEmptyView()) sortItems();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_add:
                if (PermissionUtils.requestPermissions(this))
                    showPhotoSheet();
                return true;
            case R.id.menu_remove:
                showRemoveDialog();
                return true;
            case android.R.id.home:
                invalidateOptionsMenu();
                return true;
            default:
                return false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (data.getBooleanExtra(ARG_UPDATE, false)) {
                mPhotoRecyclerView.setAdapter(adapter);
            }
        }
    }

    public boolean checkEmptyView() {
        if (adapter.getItemCount() == 0) {
            mRemoveItem.setEnabled(false);
            mSortLayoutContent.setVisibility(View.GONE);
            if (noteId.equals(THEORY_PREFIX)) {
                mMaterialTitle.setVisibility(View.GONE);
                return false;
            }
            mPhotoRecyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
            return false;
        } else {
            mPhotoRecyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
            mRemoveItem.setEnabled(true);
            mSortLayoutContent.setVisibility(View.VISIBLE);
            if (noteId.equals(THEORY_PREFIX))
                mMaterialTitle.setVisibility(View.VISIBLE);
            return true;
        }
    }

    public String getNoteId() {
        return noteId;
    }

    private void showPhotoSheet() {
        mPhotoBottomSheet.show(getSupportFragmentManager());
    }

    private void showRemoveDialog() {
        new MaterialDialog.Builder(this)
                .title(R.string.remove_all)
                .content(R.string.confirm_delete_all)
                .positiveText(R.string.yes)
                .onPositive((dialog, which) -> removePhoto(null))
                .negativeText(R.string.no).show();
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
        if (checkEmptyView()) sortItems();
    }

    public void renamePhoto(String id, String name) {
        boolean isRenamed = localDatabase.renameImageData(id, name);
        if (!isRenamed) {
            DisplayUtils.showToast(this, getResources().getString(R.string.database_error));
        } else {
            sortItems();
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

    private void setSortDirection() {

        if (Note.isPhotoSortDirRev()) {
            mSortDirection.setContentDescription(getString(R.string.description_up));
            mSortDirection.setImageResource(R.drawable.ic_arrow_up_16dp);
            mIsSortDown = false;
        } else {
            mSortDirection.setContentDescription(getString(R.string.description_down));
            mSortDirection.setImageResource(R.drawable.ic_arrow_down_16dp);
            mIsSortDown = true;
        }

        mSortDirectionAnimation = ObjectAnimator.ofFloat(
                mSortDirection,
                View.ROTATION,
                0f,
                mIsSortDown ? -180f : 180f
        ).setDuration(getResources().getInteger(android.R.integer.config_shortAnimTime));
    }

    private void sortItems() {
        switch (Note.getPhotoActiveSortMode()) {
            case R.id.sort_by_date:
                adapter.sortByDate(Note.isPhotoSortDirRev());
                break;
            case R.id.sort_by_name:
                adapter.sortByName(Note.isPhotoSortDirRev());
                break;
        }
    }

    public void startChordsActivity(View view) {
        Intent intent = new Intent(this, ChordsActivity.class);
        intent.putExtra(ChordsActivity.ARG_ALL_CHORDS, true);
        intent.putExtra(ChordsActivity.ARG_CHORDS, SyntaxHighlighter.getAllChords(this));
        intent.putExtra(ChordsActivity.ARG_INSTRUMENT, ((Button) view).getText().toString());
        startActivity(intent);
    }

    public void startSliderActivity(int position) {
        Intent intent = new Intent(this, SliderActivity.class);
        intent.putParcelableArrayListExtra(SliderActivity.ARG_PHOTOS, (ArrayList) mPhotoList);
        intent.putExtra(SliderActivity.ARG_POSITION, position);
        intent.putExtra(ARG_NOTE_ID, noteId);
        startActivityForResult(intent, 0);
    }

    public void importPhoto(File imageFile, Bitmap imageBitmap, String imageLink) {
        importImagePath = imageFile.getPath();
        importPhotosThread = new ImportPhotosThread(imageFile, imageBitmap, imageLink);
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
        } else if (msg.what == 1) {
            DisplayUtils.showToast(this, getResources().getString(R.string.file_error));
        } else if (msg.what == 2) {
            DisplayUtils.showToast(this, getResources().getString(R.string.link_error));
        }
        loadingDialog.dismiss();
        return true;
    });

    private class ImportPhotosThread extends Thread {
        File imageFile;
        Bitmap imageBitmap;
        String imageLink;

        private ImportPhotosThread(File imageFile, Bitmap imageBitmap, String imageLink) {
            this.imageFile = imageFile;
            this.imageBitmap = imageBitmap;
            this.imageLink = imageLink;
        }

        public void run() {
            isImporting = true;
            if (imageLink != null) {
                try {
                    if (URLUtil.isValidUrl(imageLink)) {
                        URL imageUrl = new URL(imageLink);
                        imageBitmap = BitmapFactory.decodeStream(imageUrl.openStream());
                    } else {
                        mImportHandler.sendEmptyMessage(2);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    mImportHandler.sendEmptyMessage(2);
                }
            }
            if (imageBitmap != null) {
                try {
                    FileUtils.createPhotoFile(imageBitmap, imageFile);
                    mImportHandler.sendEmptyMessage(0);
                } catch (IOException e) {
                    e.printStackTrace();
                    mImportHandler.sendEmptyMessage(1);
                }
            }
            isImporting = false;
        }
    }

}
