package com.theost.wavenote.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.net.Uri;
import android.text.Spannable;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.afollestad.materialdialogs.MaterialDialog;
import com.theost.wavenote.R;
import com.theost.wavenote.models.Photo;

import org.wordpress.passcodelock.AppLockManager;

import java.io.File;
import java.io.FileNotFoundException;

public class DisplayUtils {
    private DisplayUtils() {
        throw new AssertionError();
    }

    public static boolean isLandscape(Context context) {
        return context != null && context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    public static Point getDisplayPixelSize(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size;
    }

    public static int dpToPx(Context context, int dp) {
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                context.getResources().getDisplayMetrics());
        return (int) px;
    }

    public static boolean isLarge(Context context) {
        return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK)
                == Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    public static boolean isXLarge(Context context) {
        return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK)
                == Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    public static boolean isLargeScreen(Context context) {
        return isLarge(context) || isXLarge(context);
    }

    public static boolean isLargeScreenLandscape(Context context) {
        return isLargeScreen(context) && isLandscape(context);
    }

    /**
     * returns the height of the ActionBar if one is enabled - supports both the native ActionBar
     * and ActionBarSherlock - http://stackoverflow.com/a/15476793/1673548
     */
    public static int getActionBarHeight(Context context) {
        if (context == null) {
            return 0;
        }
        TypedValue tv = new TypedValue();
        if (context.getTheme() != null
                && context.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            return TypedValue.complexToDimensionPixelSize(tv.data, context.getResources().getDisplayMetrics());
        }

        // if we get this far, it's because the device doesn't support an ActionBar,
        // so return the standard ActionBar height (48dp)
        return dpToPx(context, 48);
    }

    public static int getScreenWidth() {
        return Resources.getSystem().getDisplayMetrics().widthPixels;
    }

    public static int getScreenHeight() {
        return Resources.getSystem().getDisplayMetrics().heightPixels;
    }

    // Returns the proper size for a checklist drawable (font size + a bit)
    public static int getChecklistIconSize(Context context) {
        if (context == null) {
            return 18;
        }
        return DisplayUtils.dpToPx(context, PrefUtils.getFontSize(context)) + DisplayUtils.dpToPx(context, 6);
    }

    // Disable screenshots if app PIN lock is on
    public static void disableScreenshotsIfLocked(Activity activity) {
        if (AppLockManager.getInstance().getAppLock().isPasswordLocked()) {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        } else {
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
    }

    public static void hideSystemUI(Activity activity) {
        activity.getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);
    }

    public static void showSystemUI(Activity activity) {
        activity.getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    /**
     * Hides the keyboard for the given {@link View}.  Since no {@link InputMethodManager} flag is
     * used, the keyboard is forcibly hidden regardless of the circumstances.
     */
    public static void showKeyboard(Context context, View view) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
    }

    public static void hideKeyboard(@Nullable final View view) {
        if (view == null) {
            return;
        }

        InputMethodManager inputMethodManager = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

        if (inputMethodManager != null) {
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public static void showToast(Context context, String message) {
        Toast toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
        TextView v = toast.getView().findViewById(android.R.id.message);
        if (v != null) v.setGravity(Gravity.CENTER);
        toast.show();
    }

    public static MaterialDialog showLoadingDialog(Context context, int titleId, int contentId) {
        MaterialDialog loadingDialog = new MaterialDialog.Builder(context)
                .title(titleId)
                .content(contentId)
                .cancelable(false)
                .canceledOnTouchOutside(false)
                .progress(true, 0)
                .progressIndeterminateStyle(true).build();
        loadingDialog.show();
        return loadingDialog;
    }

    public static void showImageShareBottomSheet(Context context, Photo photo) {
        Uri uri;
        try {
            uri = Uri.parse(android.provider.MediaStore.Images.Media.insertImage(context.getContentResolver(),
                    new File(photo.getUri()).getAbsolutePath(), photo.getName(), null));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            DisplayUtils.showToast(context, context.getResources().getString(R.string.file_error));
            return;
        }
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.setType("image/*");
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(Intent.createChooser(shareIntent, context.getResources().getText(R.string.share)));
    }

    public static void showTextShareBottomSheet(Context context, Spannable content) {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, content);
        sendIntent.setType("text/*");
        Intent shareIntent = Intent.createChooser(sendIntent, context.getResources().getText(R.string.share));
        context.startActivity(shareIntent);
    }
}
