package com.theost.wavenote;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import static android.app.Activity.RESULT_OK;

public class PhotoBottomSheetDialog extends BottomSheetDialogBase {
    private static final String TAG = InfoBottomSheetDialog.class.getSimpleName();

    private PhotosActivity mActivity;
    private TextView mCameraButton;
    private TextView mGalleryButton;

    public PhotoBottomSheetDialog(@NonNull PhotosActivity activity) {
        mActivity = activity;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        @SuppressLint("InflateParams") View photoView = inflater.inflate(R.layout.bottom_sheet_photo, null, false);

        View dialogSheetClose = photoView.findViewById(R.id.colorSheetClose);
        dialogSheetClose.setOnClickListener(v -> dismiss());

        mCameraButton = photoView.findViewById(R.id.add_photo_camera);
        mCameraButton.setOnClickListener(v -> addPhotoCamera());
        mGalleryButton = photoView.findViewById(R.id.add_photo_gallery);
        mGalleryButton.setOnClickListener(v -> addPhotoGallery());

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

    public void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
        String errorMessage = "";
        try {
            Uri selectedImage = imageReturnedIntent.getData();
            if (selectedImage == null) { errorMessage = this.getResources().getString(R.string.photo_error);
            } else { PhotosActivity.addPhoto(selectedImage); }
        } catch (Exception ex) {
            errorMessage = ex.toString();
        } finally {
            Toast toast = Toast.makeText(this.getContext(), errorMessage, Toast.LENGTH_SHORT);
            toast.show();
        }
        dismiss();
    }

    public void show(FragmentManager manager) {
        showNow(manager, TAG);
    }

    private void addPhotoCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, 0);
    }

    private void addPhotoGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, 1);
    }
}
