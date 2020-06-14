package com.theost.wavenote;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MenuCompat;
import androidx.viewpager.widget.ViewPager;

import com.afollestad.materialdialogs.MaterialDialog;
import com.theost.wavenote.models.Photo;
import com.theost.wavenote.utils.DisplayUtils;
import com.theost.wavenote.utils.FileUtils;
import com.theost.wavenote.utils.SliderAdapter;
import com.theost.wavenote.widgets.MultiTouchViewPager;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class SliderActivity extends ThemedAppCompatActivity {

    public static final String ARG_PHOTOS = "photos_data";
    public static final String ARG_POSITION = "position";

    private static final String ROTATE_MODE = "rotate";
    private static final String FLIP_MODE = "flip";

    private static final int ROTATE_ANGLE = 90;
    private static final int ROTATE_LEFT = -1;
    private static final int ROTATE_RIGHT = 1;

    private boolean isToolbarEnabled = true;
    private boolean isModified = false;

    private boolean isEditing = false;

    MaterialDialog loadingDialog;

    private LinearLayout mSliderData;
    private TextView mNameTextView;
    private TextView mDateTextView;
    private Toolbar toolbar;

    private MultiTouchViewPager viewPager;
    private SliderAdapter adapter;

    private List<Photo> mPhotoList;
    private String noteId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme(R.style.Slider_Theme_Wavestyle);
        setContentView(R.layout.activity_slider);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        updateOrientation(getResources().getConfiguration().orientation);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mPhotoList = (ArrayList) getIntent().getParcelableArrayListExtra(ARG_PHOTOS);
        int position = getIntent().getIntExtra(ARG_POSITION, 0);
        noteId = getIntent().getStringExtra(PhotosActivity.ARG_NOTE_ID);

        viewPager = findViewById(R.id.view_pager);
        adapter = new SliderAdapter(this, mPhotoList);

        mSliderData = findViewById(R.id.slider_data);

        mNameTextView = findViewById(R.id.slide_name);
        mDateTextView = findViewById(R.id.slide_date);

        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                setPageData(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        setPage(position);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.slider_list, menu);
        MenuCompat.setGroupDividerEnabled(menu, true);
        return true;
    }

    @Override
    public void onBackPressed() {
        Intent intent = getIntent();
        intent.putExtra(PhotosActivity.ARG_UPDATE, isModified);
        if (getParent() == null) {
            setResult(Activity.RESULT_OK, intent);
        } else {
            getParent().setResult(Activity.RESULT_OK, intent);
        }
        finish();
        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_flip_horizontal:
                flipBitmap(-1, 1);
                return true;
            case R.id.menu_flip_vertical:
                flipBitmap(1, -1);
                return true;
            case R.id.menu_rotate_left:
                rotateBitmap(ROTATE_LEFT);
                return true;
            case R.id.menu_rotate_right:
                rotateBitmap(ROTATE_RIGHT);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateOrientation(newConfig.orientation);
    }

    private void updateOrientation(int orientation) {
        int dataPadding = DisplayUtils.dpToPx(this,
                getResources().getInteger(R.integer.padding_medium));
        int navHeight = DisplayUtils.dpToPx(this,
                getResources().getInteger(R.integer.navigation_bar_height));
        int rightPadding = 0;
        int bottomPadding = 0;
        int toolbarPadding = 0;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            rightPadding = navHeight + dataPadding;
            bottomPadding = dataPadding;
            toolbarPadding = navHeight;
        } else if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            rightPadding = dataPadding;
            bottomPadding = navHeight + dataPadding;
        }
        findViewById(R.id.slider_data).setPadding(dataPadding, dataPadding, rightPadding, bottomPadding);
        toolbar.setPadding(0, DisplayUtils.dpToPx(this,
                getResources().getInteger(R.integer.status_bar_height)), toolbarPadding, 0);
    }

    public void updateToolbar() {
        isToolbarEnabled = !isToolbarEnabled;
        if (isToolbarEnabled) {
            DisplayUtils.showSystemUI(this);
            toolbar.animate().alpha(1.0f);
            mSliderData.animate().alpha(1.0f);
            toolbar.setVisibility(View.VISIBLE);
        } else {
            DisplayUtils.hideSystemUI(this);
            toolbar.animate().alpha(0.0f);
            mSliderData.animate().alpha(0.0f);
            toolbar.setVisibility(View.INVISIBLE);
        }
    }

    private void setPage(int position) {
        setPageData(position);
        viewPager.setCurrentItem(position);
    }

    private void setPageData(int position) {
        setTitle(position + 1 + " " + getResources().getString(R.string.of) + " " + adapter.getCount());
        mNameTextView.setText(mPhotoList.get(position).getName());
        mDateTextView.setText(mPhotoList.get(position).getDate());
    }

    private void rotateBitmap(int direction) {
        new TransformImageTask(this, ROTATE_MODE, ROTATE_ANGLE, direction).execute();
    }

    private void flipBitmap(int x, int y) {
        new TransformImageTask(this, FLIP_MODE, x, y).execute();
    }

    private void updateBitmap(Bitmap bitmap, File file) {
        try {
            FileUtils.createPhotoFile(bitmap, file);
            isModified = true;
            int position = viewPager.getCurrentItem();
            viewPager.setAdapter(adapter);
            setPage(position);
        } catch (IOException e) {
            e.printStackTrace();
            DisplayUtils.showToast(this, getResources().getString(R.string.file_error));
        }
    }

    private static class TransformImageTask extends AsyncTask<Void, Void, Boolean> {

        private WeakReference<SliderActivity> activityReference;

        private String mode;
        private int param1;
        private int param2;

        private Bitmap editedBitmap;
        private Photo photo;

        public TransformImageTask(SliderActivity activity, String mode, int param1, int param2) {
            activityReference = new WeakReference<>(activity);
            this.mode = mode;
            this.param1 = param1;
            this.param2 = param2;
        }

        @Override
        protected void onPreExecute() {
            SliderActivity mActivity = activityReference.get();
            photo = mActivity.mPhotoList.get(mActivity.viewPager.getCurrentItem());
            new CountDownTimer(200, 1000) {
                public void onTick(long millisUntilFinished) {
                }

                public void onFinish() {
                    if (mActivity.isEditing)
                        mActivity.loadingDialog = DisplayUtils.showLoadingDialog(
                                mActivity, R.string.edit, R.string.editing);
                }
            }.start();
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            SliderActivity mActivity = activityReference.get();
            mActivity.isEditing = true;
            Bitmap imageBitmap = photo.getBitmap(mActivity);
            Matrix matrix = new Matrix();
            switch (mode) {
                case ROTATE_MODE:
                    matrix.postRotate(param1 * param2);
                    Bitmap scaledBitmap = Bitmap.createScaledBitmap(imageBitmap, imageBitmap.getWidth(), imageBitmap.getHeight(), true);
                    editedBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);
                    break;
                case FLIP_MODE:
                    matrix.preScale(param1 * 1.0f, param2 * 1.0f);
                    editedBitmap = Bitmap.createBitmap(imageBitmap, 0, 0, imageBitmap.getWidth(), imageBitmap.getHeight(), matrix, true);
                    break;
            }
            mActivity.isEditing = false;
            return true;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            SliderActivity mActivity = activityReference.get();
            if (mActivity.loadingDialog != null) mActivity.loadingDialog.dismiss();
            mActivity.updateBitmap(editedBitmap, new File(photo.getUri()));
            super.onPostExecute(aBoolean);
        }

    }

}
