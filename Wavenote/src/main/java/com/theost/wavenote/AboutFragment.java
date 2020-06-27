package com.theost.wavenote;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.util.Calendar;
import java.util.Locale;

public class AboutFragment extends Fragment {

    private static final String DEVELOPER_INSTAGRAM_URL = "http://instagram.com/theo.jedi";
    private static final String DEVELOPER_SUPPORT_URL = "http://buymeacoffee.com/theojedi";
    private static final String WAVENOTE_GITHUB_URL = "http://github.com/fedor-jedi/wavenote-android";
    private static final String PLAY_STORE_URL = "http://play.google.com/store/apps/details?id=";
    private static final String PLAY_STORE_URI = "market://details?id=";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_about, container, false);

        TextView version = view.findViewById(R.id.about_version);
        TextView copyright = view.findViewById(R.id.about_copyright);
        ImageView logoImageView = view.findViewById(R.id.about_logo);
        View instagram = view.findViewById(R.id.about_instagram);
        View playStore = view.findViewById(R.id.about_play_store);
        View donate = view.findViewById(R.id.about_donate);
        View github = view.findViewById(R.id.about_github);

        version.setText(String.format("%s %s", getString(R.string.version), BuildConfig.VERSION_NAME));

        int thisYear = Calendar.getInstance().get(Calendar.YEAR);
        copyright.setText(String.format(Locale.getDefault(), "© %1d Theo St.", thisYear));

        logoImageView.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_wavenote_24dp));

        instagram.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(DEVELOPER_INSTAGRAM_URL)));
            } catch (Exception e) {
                Toast.makeText(getActivity(), R.string.no_browser_available, Toast.LENGTH_LONG).show();
            }
        });

        playStore.setOnClickListener(v -> {
            Uri uri = Uri.parse(PLAY_STORE_URI + requireActivity().getPackageName());
            Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
            goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                    Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            try {
                startActivity(goToMarket);
            } catch (ActivityNotFoundException e) {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse(PLAY_STORE_URL + requireActivity().getPackageName())));
            }
        });

        donate.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(DEVELOPER_SUPPORT_URL)));
            } catch (Exception e) {
                Toast.makeText(getActivity(), R.string.no_browser_available, Toast.LENGTH_LONG).show();
            }
        });

        github.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(WAVENOTE_GITHUB_URL)));
            } catch (Exception e) {
                Toast.makeText(getActivity(), R.string.no_browser_available, Toast.LENGTH_LONG).show();
            }
        });

        return view;
    }
}
