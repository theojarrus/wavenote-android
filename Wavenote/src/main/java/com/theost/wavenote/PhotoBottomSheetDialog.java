package com.theost.wavenote;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.internal.MDButton;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputLayout;
import com.theost.wavenote.utils.DictionaryUtils;
import com.theost.wavenote.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import static android.app.Activity.RESULT_OK;

public class PhotoBottomSheetDialog extends BottomSheetDialogBase {

    private static final String TAG = InfoBottomSheetDialog.class.getSimpleName();
    private static final int CAMERA_REQUEST = 0;
    private static final int FILE_REQUEST = 1;
    private static final int LINK_REQUEST = 2;

    private EditText mAddLinkEditText;

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

        TextView mLinkButton = photoView.findViewById(R.id.add_photo_link);
        mLinkButton.setOnClickListener(v -> showLinkDialog());
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
            Bitmap imageBitmap = null;
            String imageLink = null;
            File imageFile = FileUtils.createNoteFile(mActivity, noteId, "photo");
            if (imageFile == null) {
                dismiss();
                return;
            }
            if (requestCode == CAMERA_REQUEST) {
                Bundle extras = data.getExtras();
                imageBitmap = (Bitmap) extras.get("data");
            } else if (requestCode == FILE_REQUEST) {
                Uri imageUri = data.getData();
                imageBitmap = getBitmap(imageUri);
            } else if (requestCode == LINK_REQUEST) {
                imageLink = data.getStringExtra("imageLink");
            }
            if ((imageBitmap != null && imageLink == null) || (imageBitmap == null && imageLink != null))
                mActivity.importPhoto(imageFile, imageBitmap, imageLink);
        }
        dismiss();
    }

    public void show(FragmentManager manager) {
        showNow(manager, TAG);
    }

    private void showLinkDialog() {
        dismiss();
        int[] dialogColors = DictionaryUtils.getDialogColors(getContext());
        MaterialDialog linkDialog = new MaterialDialog.Builder(getContext())
                .customView(R.layout.add_dialog, false)
                .title(R.string.link)
                .positiveText(R.string.get)
                .positiveColor(dialogColors[0])
                .onPositive((dialog, which) -> addPhotoLink(mAddLinkEditText.getText().toString().trim()))
                .negativeText(R.string.cancel)
                .build();
        MDButton addButton = linkDialog.getActionButton(DialogAction.POSITIVE);
        TextInputLayout mAddLinkLayout = linkDialog.getCustomView().findViewById(R.id.dialog_layout);
        mAddLinkLayout.setCounterEnabled(false);
        mAddLinkEditText = linkDialog.getCustomView().findViewById(R.id.dialog_input);
        mAddLinkEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(100)});
        mAddLinkEditText.setText("");
        mAddLinkEditText.requestFocus();
        mAddLinkEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable arg0) {
                if (mAddLinkEditText.getText().length() == 0) {
                    addButton.setTextColor(dialogColors[0]);
                } else {
                    addButton.setTextColor(dialogColors[1]);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
        linkDialog.show();
    }

    private void addPhotoLink(String imageLink) {
        if (imageLink.equals("")) return;
        Intent intent = new Intent();
        intent.putExtra("imageLink", imageLink);
        onActivityResult(LINK_REQUEST, RESULT_OK, intent);
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

    private Bitmap getBitmap(Uri imageUri) {
        try {
            return MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), imageUri);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}