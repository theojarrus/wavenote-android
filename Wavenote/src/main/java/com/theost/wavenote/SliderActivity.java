package com.theost.wavenote;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Bundle;
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

import com.theost.wavenote.models.Photo;
import com.theost.wavenote.utils.DisplayUtils;
import com.theost.wavenote.utils.FileUtils;
import com.theost.wavenote.utils.SliderAdapter;
import com.theost.wavenote.widgets.MultiTouchViewPager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SliderActivity extends ThemedAppCompatActivity {

    private static final int ROTATE_LEFT = -90;
    private static final int ROTATE_RIGHT = 90;

    private boolean isToolbarEnabled = true;
    private boolean isModified = false;

    private LinearLayout mSliderData;
    private TextView mNameTextView;
    private TextView mDateTextView;
    Toolbar toolbar;

    MultiTouchViewPager viewPager;
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

        mPhotoList = (ArrayList) getIntent().getParcelableArrayListExtra("photoList");
        int position = getIntent().getIntExtra("position", 0);
        noteId = getIntent().getStringExtra("noteId");

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
            public void onPageScrollStateChanged(int state) {}
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
        intent.putExtra("isNeedUpdate", isModified);
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
        toolbar.setPadding(0,DisplayUtils.dpToPx(this,
                getResources().getInteger(R.integer.status_bar_height)), toolbarPadding,0);
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
        Matrix matrix = new Matrix();
        matrix.postRotate(direction);
        Photo photo = mPhotoList.get(viewPager.getCurrentItem());
        Bitmap imageBitmap = photo.getBitmap(this);
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(imageBitmap, imageBitmap.getWidth(), imageBitmap.getHeight(), true);
        Bitmap rotatedBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);
        updateBitmap(rotatedBitmap, new File(photo.getUri()));
    }

    private void flipBitmap(int x, int y) {
        Matrix matrix = new Matrix();
        matrix.preScale(x * 1.0f, y * 1.0f);
        Photo photo = mPhotoList.get(viewPager.getCurrentItem());
        Bitmap imageBitmap = photo.getBitmap(this);
        Bitmap flipedBitmap = Bitmap.createBitmap(imageBitmap, 0, 0, imageBitmap.getWidth(), imageBitmap.getHeight(), matrix, true);
        updateBitmap(flipedBitmap, new File(photo.getUri()));
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

}
