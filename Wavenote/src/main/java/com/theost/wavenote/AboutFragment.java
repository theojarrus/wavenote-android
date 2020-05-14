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

    private static final String DEVELOPER_TELEGRAM_URL = "https://t.me/fedorjedi";
    private static final String DEVELOPER_INSTAGRAM_URL = "https://instagram.com/fedor.jedi";
    private static final String DEVELOPER_PATREON_URL = "https://patreon.com/theojedi";
    private static final String WAVENOTE_GITHUB_URL = "https://github.com/fedor-jedi/wavenote-android";
    private static final String PLAY_STORE_URL = "http://play.google.com/store/apps/details?id=";
    private static final String PLAY_STORE_URI = "market://details?id=";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_about, container, false);

        TextView version = view.findViewById(R.id.about_version);
        TextView copyright = view.findViewById(R.id.about_copyright);
        ImageView logoImageView = view.findViewById(R.id.about_logo);
        View telegram = view.findViewById(R.id.about_telegram);
        View instagram = view.findViewById(R.id.about_instagram);
        View playStore = view.findViewById(R.id.about_play_store);
        View patreon = view.findViewById(R.id.about_donate);
        View github = view.findViewById(R.id.about_github);

        version.setText(String.format("%s %s", getString(R.string.version), BuildConfig.VERSION_NAME));

        int thisYear = Calendar.getInstance().get(Calendar.YEAR);
        copyright.setText(String.format(Locale.getDefault(), "© %1d Theo St.", thisYear));

        logoImageView.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_wavenote_24dp));

        telegram.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(DEVELOPER_TELEGRAM_URL)));
            } catch (Exception e) {
                Toast.makeText(getActivity(), R.string.no_browser_available, Toast.LENGTH_LONG).show();
            }
        });

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

        patreon.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(DEVELOPER_PATREON_URL)));
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
