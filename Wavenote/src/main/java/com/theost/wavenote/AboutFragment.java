package com.theost.wavenote;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.util.Calendar;
import java.util.Locale;

@Keep
public class AboutFragment extends Fragment {

    private static final String WAVENOTE_GITHUB_URL = "https://github.com/theo-jedi/wavenote-android";
    private static final String DEVELOPER_INSTAGRAM_URL = "https://instagram.com/theo.jedi";
    private static final String DEVELOPER_WEBSITE_URL = "http://theost.tech";
    public static final String DEVELOPER_SUPPORT_URL = "https://patreon.com/theojedi";
    public static final String PLAY_STORE_URI = "https://play.google.com/store/apps/details?id=com.theost.wavenote";

    private TextView version;
    private TextView copyright;
    private ImageView logoImageView;
    private View instagram;
    private View website;
    private View playStore;
    private View donate;
    private View github;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_about, container, false);

        version = view.findViewById(R.id.about_version);
        copyright = view.findViewById(R.id.about_copyright);
        logoImageView = view.findViewById(R.id.about_logo);
        instagram = view.findViewById(R.id.about_instagram);
        website = view.findViewById(R.id.about_website);
        playStore = view.findViewById(R.id.about_play_store);
        donate = view.findViewById(R.id.about_donate);
        github = view.findViewById(R.id.about_github);

        version.setText(String.format("%s %s", getString(R.string.version), BuildConfig.VERSION_NAME));

        int thisYear = Calendar.getInstance().get(Calendar.YEAR);
        copyright.setText(String.format(Locale.getDefault(), "© %1d Theo St.", thisYear));

        logoImageView.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_wavenote_24dp));

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        instagram.setOnClickListener(v -> openLink(DEVELOPER_INSTAGRAM_URL));
        website.setOnClickListener(v -> openLink(DEVELOPER_WEBSITE_URL));
        playStore.setOnClickListener(v -> openLink(PLAY_STORE_URI));
        donate.setOnClickListener(v -> openLink(DEVELOPER_SUPPORT_URL));
        github.setOnClickListener(v -> openLink(WAVENOTE_GITHUB_URL));
    }

    private void openLink(String link) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link)));
        } catch (Exception e) {
            Toast.makeText(getActivity(), R.string.no_browser_available, Toast.LENGTH_LONG).show();
        }
    }

}
