package com.theost.wavenote;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
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
import com.theost.wavenote.utils.ImportUtils;
import com.theost.wavenote.utils.PermissionUtils;
import com.theost.wavenote.adapters.PhotoAdapter;
import com.theost.wavenote.utils.SyntaxHighlighter;
import com.theost.wavenote.utils.ThemeUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static com.theost.wavenote.utils.FileUtils.NOTES_DIR;

public class PhotosActivity extends ThemedAppCompatActivity {

    public static final String THEORY_PREFIX = "theory";
    public static final String ARG_NOTE_ID = "note_id";
    public static final String ARG_UPDATE = "need_update";

    private boolean isImporting;

    private PhotoBottomSheetDialog mPhotoBottomSheet;
    private RecyclerView mPhotoRecyclerView;
    private List<Photo> mPhotoList;
    private PhotoAdapter adapter;
    private LinearLayout emptyView;
    private LinearLayout mMaterialTitle;
    private LinearLayout mSortLayout;
    private RelativeLayout mSortLayoutContent;
    private ObjectAnimator mSortDirectionAnimation;

    private ImageView mSortDirection;
    private TextView mSortOrder;

    private MaterialDialog loadingDialog;

    private String imageImportPath;
    private String noteId;

    private boolean mIsSortDown;
    private boolean mIsSortReverse;

    private DatabaseHelper localDatabase;
    private MenuItem mRemoveItem;

    private File imageImportFile;
    private Bitmap imageImportBitmap;
    private String imageImportLink;

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

        mMaterialTitle = findViewById(R.id.materials_title);

        if (noteId == null) noteId = getIntent().getStringExtra(ARG_NOTE_ID);
        if (noteId.equals(THEORY_PREFIX)) {
            setTitle(R.string.theory);
            findViewById(R.id.chords_block).setVisibility(View.VISIBLE);
        } else {
            setTitle(R.string.photo);
            mMaterialTitle.setVisibility(View.GONE);
        }

        mPhotoBottomSheet = new PhotoBottomSheetDialog(this);

        if (Note.getPhotoActiveSortMode() == 0)
            Note.setPhotoActiveSortMode(R.id.sort_by_date);

        mPhotoRecyclerView = findViewById(R.id.photos_list);

        localDatabase = new DatabaseHelper(this);

        mPhotoList = new ArrayList<>();

        adapter = new PhotoAdapter(this, mPhotoList);
        mPhotoRecyclerView.setAdapter(adapter);

        mSortLayout = findViewById(R.id.sort_layout);
        mSortOrder = findViewById(R.id.sort_order);

        PopupMenu popup = new PopupMenu(mSortOrder.getContext(), mSortOrder, Gravity.START);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.items_sort, popup.getMenu());
        popup.getMenu().findItem(R.id.sort_by_type).setVisible(false);

        mSortOrder.setText((popup.getMenu().findItem(Note.getPhotoActiveSortMode())).getTitle());

        mSortLayoutContent = findViewById(R.id.sort_content);
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
        updateData();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_add:
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

    public boolean updateEmptyView() {
        if (adapter == null || adapter.getItemCount() == 0) {
            mRemoveItem.setEnabled(false);
            mSortLayout.setVisibility(View.INVISIBLE);
            if (noteId.equals(THEORY_PREFIX)) {
                mMaterialTitle.setVisibility(View.INVISIBLE);
                return false;
            }
            mPhotoRecyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
            return false;
        } else {
            mPhotoRecyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
            mRemoveItem.setEnabled(true);
            mSortLayout.setVisibility(View.VISIBLE);
            if (noteId.equals(THEORY_PREFIX))
                mMaterialTitle.setVisibility(View.VISIBLE);
            return true;
        }
    }

    public String getNoteId() {
        return noteId;
    }

    private boolean checkPermission() {
        boolean isPermissionDenied = false;
        if (!PermissionUtils.requestFilePermissions(this)) isPermissionDenied = true;
        if (isPermissionDenied) DisplayUtils.showToast(this, getResources().getString(R.string.error_permission));
        return isPermissionDenied;
    }

    private void showPhotoSheet() {
        if (checkPermission()) return;
        if (!mPhotoBottomSheet.isAdded()) mPhotoBottomSheet.show(getSupportFragmentManager());
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
        new UpdateDataThread().start();
    }

    private void updateAdapter() {
        adapter.updateData(mPhotoList);
        if (updateEmptyView()) sortItems();
    }

    private Handler mUpdateHandler = new Handler(msg -> {
        if (msg.what == ImportUtils.RESULT_OK) {
            if (mPhotoList == null || mPhotoList.size() == 0)
                mSortLayout.setVisibility(View.INVISIBLE);
            updateAdapter();
        } else if (msg.what == ImportUtils.DATABASE_ERROR) {
            DisplayUtils.showToast(this, getResources().getString(R.string.database_error));
        }
        return true;
    });

    private class UpdateDataThread extends Thread {
        public void run() {
            Cursor mImageData = localDatabase.getImageData(noteId);
            updatePhotos(mImageData);
            mUpdateHandler.sendEmptyMessage(ImportUtils.RESULT_OK);
        }
    }

    private void updatePhotos(Cursor mImageData) {
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
                File imageFile = new File(photo.getUri());
                if (imageFile.exists()) imageFile.delete();
                localDatabase.removeImageData(id);
                mUpdateHandler.sendEmptyMessage(ImportUtils.DATABASE_ERROR);
            }
        }
    }

    private int insertPhoto() {
        if (imageImportPath == null)
            return ImportUtils.URI_ERROR;

        String date = DateTimeUtils.getDateTextString(this, Calendar.getInstance());
        int photoId = localDatabase.insertImageData(noteId, "", imageImportPath, date);
        if (photoId == -1) {
            if (imageImportFile.exists()) imageImportFile.delete();
            return ImportUtils.DATABASE_ERROR;
        }
        mPhotoList.add(new Photo(String.valueOf(photoId), "", imageImportPath, date));
        return ImportUtils.RESULT_OK;
    }

    public void renamePhoto(String id, String name) {
        boolean isRenamed = localDatabase.renameImageData(id, name);
        if (!isRenamed) {
            DisplayUtils.showToast(this, getResources().getString(R.string.database_error));
        } else {
            sortItems();
        }
    }

    public void removePhoto(String id) {
        if (id == null) {
            adapter.clearData();
            updateEmptyView();
        }
        File cacheDir = getCacheDir();
        new Thread() {
            @Override
            public void run() {
                if (id == null) {
                    FileUtils.removeDirectory(new File(cacheDir + NOTES_DIR + noteId));
                    localDatabase.removeAllImageData(noteId);
                } else {
                    String path = localDatabase.getImageUri(id);
                    File file = new File(path);
                    if (file.exists()) file.delete();
                    localDatabase.removeImageData(id);
                }
            }
        }.start();
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
        imageImportFile = imageFile;
        imageImportBitmap = imageBitmap;
        imageImportLink = imageLink;
        imageImportPath = imageFile.getPath();
        new ImportPhotosThread().start();
        showLoadingDialog();
    }

    private void clearImportData() {
        imageImportPath = null;
        imageImportFile = null;
        imageImportBitmap = null;
        imageImportLink = null;
    }

    public void showShareBottomSheet(int position) {
        DisplayUtils.showImageShareBottomSheet(this, mPhotoList.get(position));
    }

    private void showLoadingDialog() {
        PhotosActivity context = this;
        new CountDownTimer(400, 400) {
            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                if (isImporting) loadingDialog = DisplayUtils.showLoadingDialog(context, R.string.import_text, R.string.importing);
            }
        }.start();
    }

    private Handler mImportHandler = new Handler(msg -> {
        if (msg.what == ImportUtils.RESULT_OK) {
            updateAdapter();
        } else if (msg.what == ImportUtils.FILE_ERROR) {
            DisplayUtils.showToast(this, getResources().getString(R.string.file_error));
        } else if (msg.what == ImportUtils.LINK_ERROR) {
            DisplayUtils.showToast(this, getResources().getString(R.string.link_error));
        } else if (msg.what == ImportUtils.DATABASE_ERROR) {
            DisplayUtils.showToast(this, getResources().getString(R.string.database_error));
        } else if (msg.what == ImportUtils.URI_ERROR) {
            DisplayUtils.showToast(this, getResources().getString(R.string.photo_error));
        }
        if (loadingDialog != null) loadingDialog.dismiss();
        clearImportData();
        return true;
    });

    private class ImportPhotosThread extends Thread {

        public void run() {
            isImporting = true;

            if (imageImportBitmap == null) imageImportBitmap = ImportUtils.getLinkImage(imageImportLink);

            if (imageImportBitmap == null) {
                mImportHandler.sendEmptyMessage(ImportUtils.LINK_ERROR);
                isImporting = false;
                return;
            }

            int fileResultCode = ImportUtils.importPhoto(imageImportFile, imageImportBitmap);
            if (fileResultCode != ImportUtils.RESULT_OK) {
                mImportHandler.sendEmptyMessage(fileResultCode);
                isImporting = false;
                return;
            }

            int dataResultCode = insertPhoto();
            mImportHandler.sendEmptyMessage(dataResultCode);
            isImporting = false;
        }

    }

}
