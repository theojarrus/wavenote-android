package com.theost.wavenote;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static android.app.Activity.RESULT_OK;

public class PhotoBottomSheetDialog extends BottomSheetDialogBase {

    private static final String TAG = InfoBottomSheetDialog.class.getSimpleName();
    private static final String CACHE_NOTES_DIR = "/Notes/";
    private static final String CACHE_PHOTOS_DIR = "/Photos/";
    private static final String FILENAME_PATERN = "/photo_%d.jpg";
    private static final int CAMERA_REQUEST = 0;
    private static final int FILE_REQUEST = 1;

    private PhotosActivity mActivity;
    private String noteId;

    public PhotoBottomSheetDialog(@NonNull PhotosActivity activity) {
        mActivity = activity;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        @SuppressLint("InflateParams") View photoView = inflater.inflate(R.layout.bottom_sheet_photo, null, false);

        View dialogSheetClose = photoView.findViewById(R.id.colorSheetClose);
        dialogSheetClose.setOnClickListener(v -> dismiss());

        TextView mCameraButton = photoView.findViewById(R.id.add_photo_camera);
        mCameraButton.setOnClickListener(v -> addPhotoCamera());
        TextView mGalleryButton = photoView.findViewById(R.id.add_photo_gallery);
        mGalleryButton.setOnClickListener(v -> addPhotoGallery());
        TextView mStorageButton = photoView.findViewById(R.id.add_photo_storage);
        mStorageButton.setOnClickListener(v -> addPhotoStorage());

        noteId = mActivity.getNoteId();

        if (getDialog() != null) {
            // Set peek height to full height of view (i.e. set STATE_EXPANDED) to avoid buttons
            // being off screen when bottom sheet is shown.
            getDialog().setOnShowListener(dialogInterface -> {
                BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialogInterface;
                FrameLayout bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
                if (bottomSheet != null) {
                    BottomSheetBehavior behavior = BottomSheetBehavior.from(bottomSheet);
                    behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                    behavior.setSkipCollapsed(true);
                }
            });
            getDialog().setContentView(photoView);
        }

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @SuppressLint("DefaultLocale")
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            Bitmap imageBitmap;
            File file = getFile();
            if (requestCode == 0) {
                Bundle extras = data.getExtras();
                imageBitmap = (Bitmap) extras.get("data");
            } else {
                Uri imageUri = data.getData();
                imageBitmap = getBitmap(imageUri);
            }
            createPhotoFile(imageBitmap, file);
            mActivity.insertPhoto(file.getPath());
        }
        dismiss();
    }

    public void show(FragmentManager manager) {
        showNow(manager, TAG);
    }

    private void addPhotoCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getContext().getPackageManager()) != null) {
            startActivityForResult(intent, CAMERA_REQUEST);
        }
    }

    private void addPhotoGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, FILE_REQUEST);
    }

    private void addPhotoStorage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, FILE_REQUEST);
    }

    private void createPhotoFile(Bitmap imageBitmap, File file) {
        try {
            FileOutputStream fileOut = new FileOutputStream(file);
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOut);
            fileOut.flush();
            fileOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Bitmap getBitmap(Uri imageUri) {
        try {
            return MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), imageUri);
        } catch (IOException e) {
            return null;
        }
    }

    @SuppressLint("DefaultLocale")
    private File getFile() {
        File file;
        int fileId = 0;
        File directory = new File(getContext().getCacheDir() + CACHE_NOTES_DIR + noteId + CACHE_PHOTOS_DIR);
        if (!directory.exists()) directory.mkdirs();
        while (true) {
            fileId += 1;
            file = new File(directory.getPath(), String.format(FILENAME_PATERN, fileId));
            if (!file.exists()) {
                return file;
            }
        }
    }

}
