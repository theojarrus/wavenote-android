package com.theost.wavenote;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.theost.wavenote.models.Note;
import com.theost.wavenote.utils.DisplayUtils;
import com.theost.wavenote.utils.NetworkUtils;
import com.theost.wavenote.utils.StrUtils;
import com.theost.wavenote.utils.ViewUtils;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import static com.theost.wavenote.models.Note.NEW_LINE;
import static com.theost.wavenote.models.Note.SPACE;
import static com.theost.wavenote.utils.ImportUtils.LANGUAGE_ERROR;
import static com.theost.wavenote.utils.ImportUtils.NETWORK_ERROR;
import static com.theost.wavenote.utils.ImportUtils.RESULT_OK;

public class LookupBottomSheetDialog extends BottomSheetDialogBase {

    private static final String TAG = LookupBottomSheetDialog.class.getSimpleName();

    public static final String API_URL = "http://api.datamuse.com/words?";

    private static final String API_REQ_WORD = "rel_%s=%s"; // (code, word)
    private static final String API_REQ_DEFINITION = "sp=%s&md=d";
    public static final String API_REQ_SYLLABLE = "sp=%s&md=s&max=1";
    private static final String API_REQ_CUT = "&sp=%s";
    private static final String API_REQ_SOUNDS = "&sl=%s";
    private static final String API_REQ_MEANS = "&ml=%s";

    private static final String API_CODE_EXACT_RHYMES = "rhy";
    private static final String API_CODE_NEAR_RHYMES = "nry";
    private static final String API_CODE_SYNONYM = "syn";
    private static final String API_CODE_ANTONYM = "ant";
    private static final String API_CODE_ASSOCIATION = "trg";
    private static final String API_CODE_ADJECTIVES = "jjb";
    private static final String API_CODE_NOUNS = "jja";
    private static final String API_CODE_HOMOPHONES = "hom";
    private static final String API_CODE_HYPERNYMS = "spc";
    private static final String API_CODE_HYPONYMS = "gen";
    private static final String API_CODE_HOLONYMS = "com";
    private static final String API_CODE_MERONYM = "par";
    private static final String API_CODE_CONSONANT_MATCH = "cns";

    private static final String API_EXT_SELECTOR = "*";
    private static final String API_EXT_SUMMER = "+";
    private static final String API_EXT_SPLITTER = "\t";
    private static final String DISPLAY_STRING_LIST = "— ";
    private static final String DISPLAY_STRING_DOT = ".";
    private static final String DISPLAY_STRING_COMMA = ",";

    private static final String API_FIELD_DEFINITIONS = "defs";
    private static final String API_FIELD_WORD = "word";

    private static final String ADVANCED_VIEW_UNAVAILABLE = "Unavailable";

    private static final int ADVANCED_VIEW_CODE_ENABLE_ALL = 0;
    private static final int ADVANCED_VIEW_CODE_DISABLE_ALL = 1;
    private static final int ADVANCED_VIEW_CODE_DISABLE_MEANS = 2;
    private static final int ADVANCED_VIEW_CODE_DISABLE_SOUNDS = 3;

    private int mAdvancedViewStatus;

    private String[] mSearchTypes;
    private String[] mCutTypes;
    private List<String> mSearchTypesList;
    private List<String> mCutTypesList;

    private LinearLayout mLayoutAdvanced;
    private LinearLayout mLayoutRoot;
    private LinearLayout mTypeLayout;
    private LinearLayout mCutTypeLayout;
    private LinearLayout mResultLayout;

    private EditText mLookupWordEditText;
    private EditText mCutEditText;
    private EditText mMeansEditText;
    private EditText mSoundsEditText;
    private TextView mResultTextView;
    private AutoCompleteTextView mTypeTextView;
    private AutoCompleteTextView mCutTypeTextView;
    private ProgressBar mProgressLoading;

    private BottomSheetBehavior behavior;

    private boolean isSearchActive;
    private boolean isSearching;

    private MaterialButton mSearchButton;
    private MaterialCheckBox mAdvancedCheckbox;

    private final Activity mActivity;

    private String mSelectedWord;
    private String mSearchedWord;

    private View mLookupView;

    private String mSearchType;

    public LookupBottomSheetDialog(Activity activity) {
        mActivity = activity;
    }

    @SuppressLint({"InflateParams", "ResourceType"})
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mLookupView = inflater.inflate(R.layout.bottom_sheet_lookup, null, false);

        if (getDialog() != null) {
            // Set peek height to full height of view (i.e. set STATE_EXPANDED) to avoid buttons
            // being off screen when bottom sheet is shown.
            getDialog().setOnShowListener(dialogInterface -> {
                BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialogInterface;
                FrameLayout bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
                if (bottomSheet != null) {
                    int peekHeight = mLookupView.findViewById(R.id.title_layout).getHeight() + mLookupView.findViewById(R.id.peek_layout).getHeight();
                    behavior = BottomSheetBehavior.from(bottomSheet);
                    behavior.setPeekHeight(peekHeight);
                    behavior.setDraggable(false);
                    if (Note.isAdvancedSearch()) {
                        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                    } else {
                        behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    }
                }
            });
            getDialog().setContentView(mLookupView);
        }

        mLayoutRoot = mLookupView.findViewById(R.id.search_root_layout);

        mTypeTextView = mLookupView.findViewById(R.id.lookup_type);
        mLookupWordEditText = mLookupView.findViewById(R.id.lookup_word);
        mSearchButton = mLookupView.findViewById(R.id.lookup_button);
        mAdvancedCheckbox = mLookupView.findViewById(R.id.lookup_advanced);
        mLayoutAdvanced = mLookupView.findViewById(R.id.lookup_layout_advanced);
        mTypeLayout = mLookupView.findViewById(R.id.lookup_type_layout);
        mCutTypeLayout = mLookupView.findViewById(R.id.lookup_cut_layout);

        mCutTypeTextView = mLookupView.findViewById(R.id.lookup_cut_type);
        mCutEditText = mLookupView.findViewById(R.id.lookup_cut);
        mMeansEditText = mLookupView.findViewById(R.id.lookup_means);
        mSoundsEditText = mLookupView.findViewById(R.id.lookup_sounds);
        mResultLayout = mLookupView.findViewById(R.id.lookup_result_layout);
        mResultTextView = mLookupView.findViewById(R.id.lookup_result);

        mProgressLoading = mLookupView.findViewById(R.id.search_progress_bar);

        mSearchTypes = getResources().getStringArray(R.array.array_lookup_modes);
        mCutTypes = getResources().getStringArray(R.array.array_lookup_cut_modes);
        mSearchTypesList = Arrays.asList(mSearchTypes);
        mCutTypesList = Arrays.asList(mCutTypes);

        mLookupWordEditText.setText(mSelectedWord);

        mTypeTextView.setText(mSearchTypes[Note.getSearchType()]);
        mCutTypeTextView.setText(mCutTypes[Note.getSearchCutType()]);

        ViewUtils.disbaleInput(mTypeTextView);
        ViewUtils.disbaleInput(mCutTypeTextView);
        ViewUtils.removeFocus(mTypeTextView);
        ViewUtils.removeFocus(mCutTypeTextView);
        ViewUtils.updateDropdown(mActivity, mTypeTextView, mSearchTypes);
        ViewUtils.updateDropdown(mActivity, mCutTypeTextView, mCutTypes);
        ViewUtils.restoreFocus(mTypeTextView);
        ViewUtils.restoreFocus(mCutTypeTextView);

        updateAdvancedStatus();
        updateAdvancedViews();

        mAdvancedCheckbox.setChecked(Note.isAdvancedSearch());
        updateAdvancedLayout();

        mAdvancedCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            clearFocus();
            Note.setAdvancedSearch(isChecked);
            updateAdvancedLayout();
        });

        mSearchButton.setOnClickListener(v -> onSearchClicked());

        mTypeTextView.setOnFocusChangeListener((v, hasFocus) -> DisplayUtils.hideKeyboard(mTypeTextView));

        mTypeTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable arg0) {
                Note.setSearchType(mSearchTypesList.indexOf(mTypeTextView.getText().toString()));
                updateAdvancedStatus();
                updateAdvancedViews();
                mTypeTextView.clearFocus();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                System.out.println();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        mCutTypeTextView.setOnFocusChangeListener((v, hasFocus) -> DisplayUtils.hideKeyboard(mCutTypeTextView));
        mCutTypeTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable arg0) {
                Note.setSearchCutType(mCutTypesList.indexOf(mCutTypeTextView.getText().toString()));
                mCutTypeTextView.clearFocus();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    public void show(@NonNull FragmentManager manager, String word) {
        mSelectedWord = word;
        showNow(manager, TAG);
    }

    private void clearFocus() {
        mLookupWordEditText.requestFocus();
        DisplayUtils.hideKeyboard(mLookupWordEditText);
        mLookupWordEditText.clearFocus();
    }

    private void onSearchClicked() {
        clearFocus();
        if (!isSearchActive) {
            mSearchedWord = mLookupWordEditText.getText().toString().trim();
            if (!mSearchedWord.equals("")) {
                isSearchActive = true;
                updateSearchViews();
                searchWord();
            }
        } else {
            isSearchActive = false;
            updateSearchViews();
        }
    }

    private void updateSearchViews() {
        if (isSearchActive) {
            mSearchButton.setIcon(ContextCompat.getDrawable(mActivity, R.drawable.ic_close_24dp));
            mTypeLayout.setEnabled(false);
            mLookupWordEditText.setEnabled(false);
            mTypeLayout.setAlpha(0.3f);
            mLookupWordEditText.setAlpha(0.3f);
            mResultTextView.setVisibility(View.GONE);
            mProgressLoading.setVisibility(View.VISIBLE);
            mResultLayout.setMinimumHeight(mAdvancedCheckbox.getHeight() + mLayoutAdvanced.getHeight());
            mResultLayout.setVisibility(View.VISIBLE);
            mAdvancedCheckbox.setVisibility(View.GONE);
            mLayoutAdvanced.setVisibility(View.GONE);
            if (!Note.isAdvancedSearch()) behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        } else {
            mSearchButton.setIcon(ContextCompat.getDrawable(mActivity, R.drawable.ic_search_24dp));
            mTypeLayout.setEnabled(true);
            mLookupWordEditText.setEnabled(true);
            mTypeLayout.setAlpha(1.0f);
            mLookupWordEditText.setAlpha(1.0f);
            mResultLayout.setVisibility(View.GONE);
            mAdvancedCheckbox.setVisibility(View.VISIBLE);
            if (!Note.isAdvancedSearch()) {
                mLayoutAdvanced.setVisibility(View.INVISIBLE);
                behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            } else {
                mLayoutAdvanced.setVisibility(View.VISIBLE);
            }
        }
    }

    private void updateAdvancedStatus() {
        String mode = mSearchTypes[Note.getSearchType()];
        if (mode.equals(getString(R.string.definitions))) {
            mAdvancedViewStatus = ADVANCED_VIEW_CODE_DISABLE_ALL;
        } else if (mode.equals(getString(R.string.means_like))) {
            mAdvancedViewStatus = ADVANCED_VIEW_CODE_DISABLE_MEANS;
        } else if (mode.equals(getString(R.string.sounds_like))) {
            mAdvancedViewStatus = ADVANCED_VIEW_CODE_DISABLE_SOUNDS;
        } else {
            mAdvancedViewStatus = ADVANCED_VIEW_CODE_ENABLE_ALL;
        }
    }

    private void updateAdvancedViews() {
        if (mAdvancedViewStatus != ADVANCED_VIEW_CODE_ENABLE_ALL) {
            if (mAdvancedViewStatus == ADVANCED_VIEW_CODE_DISABLE_MEANS) {
                mAdvancedViewStatus = ADVANCED_VIEW_CODE_ENABLE_ALL;
                updateAdvancedViews();
                mMeansEditText.setEnabled(false);
                mMeansEditText.setAlpha(0.3f);
                mMeansEditText.setText(ADVANCED_VIEW_UNAVAILABLE);
            }
            if (mAdvancedViewStatus == ADVANCED_VIEW_CODE_DISABLE_SOUNDS) {
                mAdvancedViewStatus = ADVANCED_VIEW_CODE_ENABLE_ALL;
                updateAdvancedViews();
                mSoundsEditText.setEnabled(false);
                mSoundsEditText.setAlpha(0.3f);
                mSoundsEditText.setText(ADVANCED_VIEW_UNAVAILABLE);
                mAdvancedViewStatus = ADVANCED_VIEW_CODE_DISABLE_MEANS;
            }
            if (mAdvancedViewStatus == ADVANCED_VIEW_CODE_DISABLE_ALL) {
                mCutTypeLayout.setEnabled(false);
                mCutTypeTextView.setEnabled(false);
                mCutEditText.setEnabled(false);
                mSoundsEditText.setEnabled(false);
                mMeansEditText.setEnabled(false);
                mCutTypeTextView.setAlpha(0.3f);
                mCutEditText.setAlpha(0.3f);
                mSoundsEditText.setAlpha(0.3f);
                mMeansEditText.setAlpha(0.3f);
                mCutEditText.setText(ADVANCED_VIEW_UNAVAILABLE);
                mSoundsEditText.setText(ADVANCED_VIEW_UNAVAILABLE);
                mMeansEditText.setText(ADVANCED_VIEW_UNAVAILABLE);
            }
        } else {
            mCutTypeLayout.setEnabled(true);
            mCutTypeTextView.setEnabled(true);
            mCutEditText.setEnabled(true);
            mMeansEditText.setEnabled(true);
            mSoundsEditText.setEnabled(true);
            mCutTypeTextView.setAlpha(1.0f);
            mCutEditText.setAlpha(1.0f);
            mMeansEditText.setAlpha(1.0f);
            mSoundsEditText.setAlpha(1.0f);
            if (mMeansEditText.getText().toString().trim()
                    .equals(ADVANCED_VIEW_UNAVAILABLE)) {
                mMeansEditText.setText("");
            }
            if (mSoundsEditText.getText().toString().trim()
                    .equals(ADVANCED_VIEW_UNAVAILABLE)) {
                mSoundsEditText.setText("");
            }
            if (mCutEditText.getText().toString().trim()
                    .equals(ADVANCED_VIEW_UNAVAILABLE)) {
                mCutEditText.setText("");
            }
        }
    }

    private void searchWord() {
        if (!isSearching) {
            new RequestThread().start();
        } else {
            DisplayUtils.showToast(mActivity, getString(R.string.searching_error));
        }
    }

    private final Handler mRequestHandler = new Handler(Looper.getMainLooper(), msg -> {
        mProgressLoading.setVisibility(View.GONE);
        mResultTextView.setVisibility(View.VISIBLE);
        if (msg.what == RESULT_OK) {
            mResultTextView.setText((String) msg.obj);
        } else {
            if (msg.what == NETWORK_ERROR) {
                mResultTextView.setText(getString(R.string.network_error_message));
            } else if (msg.what == LANGUAGE_ERROR) {
                mResultTextView.setText(getString(R.string.search_language_error));
            }
        }
        return true;
    });

    private class RequestThread extends Thread {

        @Override
        public void run() {
            isSearching = true;
            if (!StrUtils.isStringEnglish(mSearchedWord.replaceAll("\\s+", ""))) {
                mRequestHandler.sendEmptyMessage(LANGUAGE_ERROR);
            } else if (!NetworkUtils.isNetworkAvailable(mActivity)) {
                mRequestHandler.sendEmptyMessage(NETWORK_ERROR);
            } else {
                try {
                    String request = generateRequest();
                    String response = getApiResponse(request);
                    String result = generateResult(response);
                    Message msg = new Message();
                    msg.obj = result;
                    msg.what = RESULT_OK;
                    mRequestHandler.sendMessage(msg);
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                    mRequestHandler.sendEmptyMessage(NETWORK_ERROR);
                }
            }
            isSearching = false;
        }

    }

    private String generateResult(String response) throws JSONException {
        JSONArray responseArray = new JSONArray(response);
        if (responseArray.length() == 0) {
            return getString(R.string.search_results_error);
        } else {
            StringBuilder result = new StringBuilder();
            if (mSearchType.equals(getString(R.string.definitions))) {
                if (!responseArray.getJSONObject(0).has(API_FIELD_DEFINITIONS))
                    return getString(R.string.search_results_error);
                JSONArray sourceArray = responseArray.getJSONObject(0).getJSONArray(API_FIELD_DEFINITIONS);
                for (int i = 0; i < sourceArray.length(); i++) {
                    int index = 0;
                    String[] lines = sourceArray.getString(i).split(API_EXT_SPLITTER);
                    if (lines.length > 1) index += 1;
                    String line = lines[index];
                    if (i > 0) result.append(NEW_LINE);
                    result.append(DISPLAY_STRING_LIST).append(StrUtils.firstToUppercase(line)).append(DISPLAY_STRING_DOT);
                }
            } else {
                for (int j = 0; j < responseArray.length(); j++) {
                    String line = responseArray.getJSONObject(j).getString(API_FIELD_WORD);
                    if (j > 0) {
                        result.append(DISPLAY_STRING_COMMA + SPACE);
                    } else {
                        line = StrUtils.firstToUppercase(line);
                    }
                    result.append(line);
                    if (j == responseArray.length() - 1) result.append(DISPLAY_STRING_DOT);
                }
            }
            return result.toString();
        }
    }

    private String getApiResponse(String url) throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();
        try (Response response = client.newCall(request).execute()) {
            ResponseBody responseBody = response.body();
            if (responseBody != null) {
                return responseBody.string();
            } else {
                return "";
            }
        }
    }

    @SuppressLint("NonConstantResourceId")
    private String generateRequest() {
        mSearchedWord = StrUtils.convertToRequestFormat(mSearchedWord, API_EXT_SUMMER);
        mSearchType = mSearchTypes[Note.getSearchType()];
        if (mSearchType.equals(getString(R.string.definitions))) {
            return API_URL + String.format(API_REQ_DEFINITION, mSearchedWord);
        } else {
            String code = "";
            switch (Note.getSearchType()) {
                case 1:
                    code = API_CODE_EXACT_RHYMES;
                    break;
                case 2:
                    code = API_CODE_NEAR_RHYMES;
                    break;
                case 3:
                    code = API_CODE_SYNONYM;
                    break;
                case 4:
                    code = API_CODE_ANTONYM;
                    break;
                case 5:
                    code = API_CODE_ASSOCIATION;
                    break;
                case 6:
                    code = API_CODE_ADJECTIVES;
                    break;
                case 7:
                    code = API_CODE_NOUNS;
                    break;
                case 8:
                    code = API_CODE_HOMOPHONES;
                    break;
                case 9:
                    code = API_CODE_HYPERNYMS;
                    break;
                case 10:
                    code = API_CODE_HYPONYMS;
                    break;
                case 11:
                    code = API_CODE_HOLONYMS;
                    break;
                case 12:
                    code = API_CODE_MERONYM;
                    break;
                case 13:
                    code = API_CODE_CONSONANT_MATCH;
                    break;
                case 14:
                    code = API_REQ_MEANS;
                    break;
                case 15:
                    code = API_REQ_SOUNDS;
                    break;
            }
            String request;
            if (Note.getSearchType() < 14) {
                request = String.format(API_REQ_WORD, code, mSearchedWord);
            } else {
                request = String.format(code, mSearchedWord);
            }
            if (Note.isAdvancedSearch()) {
                String cut = StrUtils.convertToRequestFormat(mCutEditText.getText().toString(), API_EXT_SUMMER);
                if (!cut.equals("")) {
                    String cutValue = API_EXT_SELECTOR;
                    switch (Note.getSearchCutType()) {
                        case 0:
                            cutValue = cut + cutValue;
                            break;
                        case 1:
                            cutValue = cutValue + cut;
                            break;
                    }
                    request += String.format(API_REQ_CUT, cutValue);
                }
                String means = StrUtils.convertToRequestFormat(mMeansEditText.getText().toString(), API_EXT_SUMMER);
                if (!means.equals("")) request += String.format(API_REQ_MEANS, means);
                String sounds = StrUtils.convertToRequestFormat(mSoundsEditText.getText().toString(), API_EXT_SUMMER);
                if (!sounds.equals("")) request += String.format(API_REQ_SOUNDS, sounds);
            }
            return API_URL + request;
        }
    }

    private void updateAdvancedLayout() {
        if (behavior != null) {
            TransitionManager.beginDelayedTransition(mLayoutRoot);
            if (Note.isAdvancedSearch()) {
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                mLayoutAdvanced.setVisibility(View.VISIBLE);
            } else {
                behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                mLayoutAdvanced.setVisibility(View.INVISIBLE);
            }
        }
    }

}
