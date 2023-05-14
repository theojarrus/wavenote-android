package com.theost.wavenote;

import static com.theost.wavenote.NoteListFragment.TAG_PREFIX;
import static com.theost.wavenote.adapters.TagsAdapter.ALL_NOTES_ID;
import static com.theost.wavenote.adapters.TagsAdapter.DEFAULT_ITEM_POSITION;
import static com.theost.wavenote.adapters.TagsAdapter.METRONOME_ID;
import static com.theost.wavenote.adapters.TagsAdapter.SETTINGS_ID;
import static com.theost.wavenote.adapters.TagsAdapter.TAGS_ID;
import static com.theost.wavenote.adapters.TagsAdapter.THEORY_ID;
import static com.theost.wavenote.adapters.TagsAdapter.TRASH_ID;
import static com.theost.wavenote.adapters.TagsAdapter.UNTAGGED_NOTES_ID;
import static com.theost.wavenote.utils.DisplayUtils.disableScreenshotsIfLocked;
import static com.theost.wavenote.utils.FileUtils.NOTES_DIR;
import static com.theost.wavenote.utils.WidgetUtils.KEY_LIST_WIDGET_CLICK;
import static com.theost.wavenote.widgets.FeedbackDialog.DAYS_UNTIL_PROMPT;
import static com.theost.wavenote.widgets.FeedbackDialog.LAUNCHES_UNTIL_PROMPT;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.core.view.MenuCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.material.navigation.NavigationView;
import com.simperium.Simperium;
import com.simperium.client.Bucket;
import com.simperium.client.BucketObjectMissingException;
import com.simperium.client.BucketObjectNameInvalid;
import com.simperium.client.Query;
import com.simperium.client.User;
import com.theost.wavenote.adapters.TagsAdapter;
import com.theost.wavenote.models.Note;
import com.theost.wavenote.models.Tag;
import com.theost.wavenote.utils.AuthUtils;
import com.theost.wavenote.utils.DatabaseHelper;
import com.theost.wavenote.utils.DisplayUtils;
import com.theost.wavenote.utils.DrawableUtils;
import com.theost.wavenote.utils.FileUtils;
import com.theost.wavenote.utils.HtmlCompat;
import com.theost.wavenote.utils.PrefUtils;
import com.theost.wavenote.utils.ResUtils;
import com.theost.wavenote.utils.StrUtils;
import com.theost.wavenote.utils.ThemeUtils;
import com.theost.wavenote.utils.UndoBarController;
import com.theost.wavenote.widgets.FeedbackDialog;

import org.wordpress.passcodelock.AppLockManager;

import java.io.File;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NotesActivity extends ThemedAppCompatActivity implements NoteListFragment.Callbacks,
        User.StatusChangeListener, Simperium.OnUserCreatedListener, UndoBarController.UndoListener,
        Bucket.Listener<Note> {
    public static String TAG_NOTE_LIST = "noteList";
    public static String TAG_NOTE_EDITOR = "noteEditor";

    private static final String STATE_NOTE_LIST_WIDGET_BUTTON_TAPPED = "STATE_NOTE_LIST_WIDGET_BUTTON_TAPPED";

    protected Bucket<Note> mNotesBucket;
    protected Bucket<Tag> mTagsBucket;
    private boolean mHasTappedNoteListWidgetButton;
    private boolean mIsTheoryClicked;
    private boolean mIsMetronomeClicked;
    private boolean mIsSettingsClicked;
    private boolean mIsShowingMarkdown;
    private boolean mIsTabletFullscreen;
    private boolean mShouldSelectNewNote;

    private Menu mMenu;
    private String mTabletSearchQuery;
    private UndoBarController mUndoBarController;
    private View mFragmentsContainer;
    private SearchView mSearchView;
    private MenuItem mSearchMenuItem;
    private NoteListFragment mNoteListFragment;
    private NoteEditorFragment mNoteEditorFragment;
    private Note mCurrentNote;
    private MenuItem mEmptyTrashMenuItem;
    private final Handler mInvalidateOptionsMenuHandler = new Handler(Looper.getMainLooper());
    private final Runnable mInvalidateOptionsMenuRunnable = this::invalidateOptionsMenu;

    // Menu drawer
    private static final int GROUP_PRIMARY = 100;
    private static final int GROUP_SECONDARY = 101;
    private static final int GROUP_TERTIARY = 102;
    private DrawerLayout mDrawerLayout;
    private Menu mNavigationMenu;
    private ActionBarDrawerToggle mDrawerToggle;
    private TagsAdapter mTagsAdapter;
    private TagsAdapter.TagMenuItem mSelectedTag;
    // Tags bucket listener
    private final Bucket.Listener<Tag> mTagsMenuUpdater = new Bucket.Listener<Tag>() {
        @Override
        public void onSyncObject(Bucket<Tag> bucket, String key) {

        }

        @Override
        public void onLocalQueueChange(Bucket<Tag> bucket, Set<String> queuedObjects) {

        }

        void updateNavigationDrawer() {
            runOnUiThread(() -> updateNavigationDrawerItems());
        }

        @Override
        public void onSaveObject(Bucket<Tag> bucket, Tag tag) {
            updateNavigationDrawer();
        }

        @Override
        public void onDeleteObject(Bucket<Tag> bucket, Tag tag) {
            updateNavigationDrawer();
        }

        @Override
        public void onNetworkChange(Bucket<Tag> bucket, Bucket.ChangeType type, String key) {
            updateNavigationDrawer();
        }

        @Override
        public void onBeforeUpdateObject(Bucket<Tag> bucket, Tag object) {
            // noop
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.setTheme(this);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_notes);

        mFragmentsContainer = findViewById(R.id.note_fragment_container);

        Wavenote currentApp = (Wavenote) getApplication();
        if (mNotesBucket == null) {
            mNotesBucket = currentApp.getNotesBucket();
        }

        if (mTagsBucket == null) {
            mTagsBucket = currentApp.getTagsBucket();
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        configureNavigationDrawer(toolbar);

        ThemeUtils.updateTextTheme(this);

        if (savedInstanceState == null) {
            mNoteListFragment = new NoteListFragment();
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragmentTransaction.add(R.id.note_fragment_container, mNoteListFragment, TAG_NOTE_LIST);
            fragmentTransaction.commit();
        } else {
            mHasTappedNoteListWidgetButton = savedInstanceState.getBoolean(STATE_NOTE_LIST_WIDGET_BUTTON_TAPPED);
            mNoteListFragment = (NoteListFragment) getSupportFragmentManager().findFragmentByTag(TAG_NOTE_LIST);
        }

        mIsTabletFullscreen = mNoteListFragment.isHidden();

        if (DisplayUtils.isLargeScreen(this)) {
            if (getSupportFragmentManager().findFragmentByTag(TAG_NOTE_EDITOR) != null) {
                mNoteEditorFragment = (NoteEditorFragment) getSupportFragmentManager().findFragmentByTag(TAG_NOTE_EDITOR);
            } else if (DisplayUtils.isLandscape(this)) {
                addEditorFragment();
            }
        }

        // enable ActionBar app icon to behave as action to toggle nav drawer
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);

            // Add loading indicator to show when indexing
            @SuppressLint("InflateParams")
            ProgressBar progressBar = (ProgressBar) getLayoutInflater().inflate(R.layout.progressbar_toolbar, null);
            actionBar.setDisplayShowCustomEnabled(true);
            actionBar.setCustomView(progressBar);
            setToolbarProgressVisibility(false);
        }

        mUndoBarController = new UndoBarController(this);

        // Creates 'Welcome' note
        checkForFirstLaunch();

        checkForSharedContent();

        currentApp.getSimperium().setOnUserCreatedListener(this);
        currentApp.getSimperium().setUserStatusChangeListener(this);

        updateLaunchCount();
        checkForFeedback();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = getIntent();

        // Ensure user has valid authorization
        if (userAuthenticationIsInvalid()) {
            // startLoginActivity();
        }

        if ((!(intent.hasExtra(KEY_LIST_WIDGET_CLICK) && intent.getExtras() != null)) && !mHasTappedNoteListWidgetButton) {
            mHasTappedNoteListWidgetButton = true;
            intent.removeExtra(KEY_LIST_WIDGET_CLICK);
        }

        disableScreenshotsIfLocked(this);

        mNotesBucket.start();
        mTagsBucket.start();

        mNotesBucket.addOnNetworkChangeListener(this);
        mNotesBucket.addOnSaveObjectListener(this);
        mNotesBucket.addOnDeleteObjectListener(this);
        mTagsBucket.addListener(mTagsMenuUpdater);

        updateNavigationDrawerItems();

        // if the user is not authenticated and the tag doesn't exist revert to default drawer selection
        if (

                userIsUnauthorized()) {
            if (mTagsAdapter.getPosition(mSelectedTag) == -1) {
                mSelectedTag = null;
                mNavigationMenu.getItem(DEFAULT_ITEM_POSITION).setChecked(true);
            }
        }

        if (mSelectedTag != null) {
            filterListBySelectedTag();
        }

        if (mCurrentNote != null && mShouldSelectNewNote) {
            onNoteSelected(mCurrentNote.getSimperiumKey(), null, mCurrentNote.isMarkdownEnabled(), mCurrentNote.isPreviewEnabled());
            mShouldSelectNewNote = false;
        }

        if (mIsShowingMarkdown) {
            setMarkdownShowing(false);
        }

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (DisplayUtils.isLargeScreenLandscape(this)) {
            if (mIsTabletFullscreen) {
                ft.hide(mNoteListFragment);
            } else {
                ft.show(mNoteListFragment);
            }
        } else {
            ft.show(mNoteListFragment);
        }
        ft.commitNow();

        if (!PrefUtils.getBoolPref(this, PrefUtils.PREF_NEWS)) {
            startActivity(new Intent(this, NewsActivity.class));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();  // Always call the superclass method first
        mTagsBucket.removeListener(mTagsMenuUpdater);
        mTagsBucket.stop();

        mNotesBucket.removeOnNetworkChangeListener(this);
        mNotesBucket.removeOnSaveObjectListener(this);
        mNotesBucket.removeOnDeleteObjectListener(this);
        mNotesBucket.stop();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean(STATE_NOTE_LIST_WIDGET_BUTTON_TAPPED, mHasTappedNoteListWidgetButton);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void setTitle(CharSequence title) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout != null && mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onActionModeCreated() {
        mDrawerLayout.requestDisallowInterceptTouchEvent(true);
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
    }

    @Override
    public void onActionModeDestroyed() {
        if (mSearchMenuItem != null && !mSearchMenuItem.isActionViewExpanded()) {
            mDrawerLayout.requestDisallowInterceptTouchEvent(false);
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        }
    }

    private ColorStateList getIconSelector() {
        int[][] states = new int[][]{
                new int[]{android.R.attr.state_checked}, // checked
                new int[]{-android.R.attr.state_checked}  // unchecked
        };

        int[] colors = new int[]{
                ThemeUtils.getColorFromAttribute(NotesActivity.this, R.attr.colorAccent),
                ThemeUtils.getColorFromAttribute(NotesActivity.this, R.attr.toolbarIconColor)
        };

        return new ColorStateList(states, colors);
    }

    private ColorStateList getTextSelector() {
        int[][] states = new int[][]{
                new int[]{-android.R.attr.state_enabled}, // disabled
                new int[]{android.R.attr.state_checked}, // checked
                new int[]{-android.R.attr.state_checked}  // unchecked
        };

        int[] colors = new int[]{
                ContextCompat.getColor(this, R.color.text_title_disabled),
                ThemeUtils.getColorFromAttribute(NotesActivity.this, R.attr.colorAccent),
                ThemeUtils.getColorFromAttribute(NotesActivity.this, R.attr.noteTitleColor)
        };

        return new ColorStateList(states, colors);
    }

    private void configureNavigationDrawer(Toolbar toolbar) {
        ColorStateList iconSelector = getIconSelector();
        ColorStateList textSelector = getTextSelector();
        mDrawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.navigation_view);
        navigationView.getLayoutParams().width = ThemeUtils.getOptimalDrawerWidth(this);
        navigationView.setItemIconTintList(iconSelector);
        navigationView.setItemTextColor(textSelector);
        navigationView.setNavigationItemSelectedListener(
                item -> {
                    mDrawerLayout.closeDrawer(GravityCompat.START);

                    if (item.getItemId() == THEORY_ID) {
                        mIsTheoryClicked = true;
                        return false;
                    } else if (item.getItemId() == METRONOME_ID) {
                        mIsMetronomeClicked = true;
                        return false;
                    } else if (item.getItemId() == SETTINGS_ID) {
                        mIsSettingsClicked = true;
                        return false;
                    } else {
                        mSelectedTag = mTagsAdapter.getTagFromItem(item);
                        filterListBySelectedTag();
                        return true;
                    }
                }
        );

        mNavigationMenu = navigationView.getMenu();
        mNavigationMenu.add(GROUP_PRIMARY, ALL_NOTES_ID, Menu.NONE, getString(R.string.all_notes)).setIcon(R.drawable.ic_notes_24dp).setCheckable(true);
        mNavigationMenu.add(GROUP_PRIMARY, THEORY_ID, Menu.NONE, getString(R.string.theory)).setIcon(R.drawable.ic_theory_24dp).setCheckable(false);
        mNavigationMenu.add(GROUP_PRIMARY, METRONOME_ID, Menu.NONE, getString(R.string.metronome)).setIcon(R.drawable.ic_metronome_24dp).setCheckable(false);
        mNavigationMenu.add(GROUP_PRIMARY, TRASH_ID, Menu.NONE, getString(R.string.trash)).setIcon(R.drawable.ic_trash_24dp).setCheckable(true);
        mNavigationMenu.add(GROUP_PRIMARY, SETTINGS_ID, Menu.NONE, getString(R.string.settings)).setIcon(R.drawable.ic_settings_24dp).setCheckable(false);
        mTagsAdapter = new TagsAdapter(this, mNotesBucket);

        if (mSelectedTag == null)
            mSelectedTag = mTagsAdapter.getDefaultItem();

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar, R.string.open_drawer,
                R.string.close_drawer) {
            public void onDrawerClosed(View view) {
                supportInvalidateOptionsMenu();
                if (mIsTheoryClicked) {
                    Intent intent = new Intent(NotesActivity.this, PhotosActivity.class);
                    intent.putExtra(PhotosActivity.ARG_NOTE_ID, PhotosActivity.THEORY_PREFIX);
                    startActivity(intent);
                    mIsTheoryClicked = false;
                } else if (mIsMetronomeClicked) {
                    Intent intent = new Intent(NotesActivity.this, MetronomeActivity.class);
                    startActivity(intent);
                    mIsMetronomeClicked = false;
                } else if (mIsSettingsClicked) {
                    Intent intent = new Intent(NotesActivity.this, PreferencesActivity.class);
                    startActivityForResult(intent, Wavenote.INTENT_PREFERENCES);
                    mIsSettingsClicked = false;
                }
            }

            public void onDrawerOpened(View drawerView) {
                // noop
            }

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, 0f);
            }
        };

        mDrawerLayout.addDrawerListener(mDrawerToggle);
    }

    private void filterListBySelectedTag() {
        MenuItem selectedMenuItem = mNavigationMenu.findItem((int) mSelectedTag.id);

        if (selectedMenuItem != null) {
            mSelectedTag = mTagsAdapter.getTagFromItem(selectedMenuItem);
        } else {
            mSelectedTag = mTagsAdapter.getDefaultItem();
        }

        checkEmptyListText(mSearchMenuItem != null && mSearchMenuItem.isActionViewExpanded());

        // Show list/sidebar when it was hidden while in landscape orientation.
        if (mNoteListFragment.isHidden()) {
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragmentTransaction.show(mNoteListFragment);
            fragmentTransaction.commitNowAllowingStateLoss();
        }

        // Disable long press on notes when viewing Trash.
        getNoteListFragment().getListView().setLongClickable(mSelectedTag.id != TRASH_ID);

        getNoteListFragment().refreshListFromNavSelect();

        Map<String, String> properties = new HashMap<>(1);

        switch ((int) mSelectedTag.id) {
            case ALL_NOTES_ID:
                properties.put("tag", "all_notes");
                break;
            case TRASH_ID:
                properties.put("tag", "trash");
                break;
            case UNTAGGED_NOTES_ID:
                properties.put("tag", "untagged_notes");
                break;
        }
        setSelectedTagActive();
    }

    private void checkForFirstLaunch() {
        if (PrefUtils.getBoolPref(this, PrefUtils.PREF_FIRST_LAUNCH, true)) {
            // Create the welcome note
            try {
                Note welcomeNote = mNotesBucket.newObject("welcome-android");
                welcomeNote.setCreationDate(Calendar.getInstance());
                welcomeNote.setModificationDate(welcomeNote.getCreationDate());
                welcomeNote.setContent(new SpannableString(HtmlCompat.fromHtml(getString(R.string.welcome_note))));
                welcomeNote.getTitle();
                welcomeNote.save();
                Note songNote = mNotesBucket.newObject("welcomesong-android");
                songNote.setCreationDate(Calendar.getInstance());
                songNote.setModificationDate(welcomeNote.getCreationDate());
                songNote.setSyllableEnabled(true);
                songNote.setContent(new SpannableString(HtmlCompat.fromHtml(getString(R.string.welcome_song))));
                songNote.getTitle();
                songNote.save();
            } catch (BucketObjectNameInvalid e) {
                // this won't happen because welcome-android is a valid name
            }

            ResUtils.restoreDictionary(this);

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(PrefUtils.PREF_LAUNCH_TIME, String.valueOf(System.currentTimeMillis()));
            editor.putString(PrefUtils.PREF_LAUNCH_COUNT, String.valueOf(0));
            editor.putBoolean(PrefUtils.PREF_SHOW_FEEDBACK, true);
            editor.putBoolean(PrefUtils.PREF_WEB_SYLLABLE, true);
            editor.putBoolean(PrefUtils.PREF_DETECT_KEYWORDS, true);
            editor.putBoolean(PrefUtils.PREF_DETECT_CHORDS, true);
            editor.putBoolean(PrefUtils.PREF_DETECT_LINKS, true);
            editor.putBoolean(PrefUtils.PREF_FIRST_LAUNCH, false);
            editor.putBoolean(PrefUtils.PREF_ACCOUNT_REQUIRED, true);
            editor.putString(PrefUtils.PREF_EXPORT_DIR, FileUtils.getDefaultDir());
            editor.apply();
        }
    }

    private void checkForSharedContent() {
        if (getIntent().hasExtra(Intent.EXTRA_TEXT)) {
            // Check share action
            Intent intent = getIntent();
            String subject = intent.getStringExtra(Intent.EXTRA_SUBJECT);
            String text = intent.getStringExtra(Intent.EXTRA_TEXT);

            // Don't add the 'Note to self' subject or open the note if this was shared from a voice search
            String intentAction = StrUtils.notNullStr(intent.getAction());
            boolean isVoiceShare = intentAction.equals("com.google.android.gm.action.AUTO_SEND");
            if (!TextUtils.isEmpty(text)) {
                if (!TextUtils.isEmpty(subject) && !isVoiceShare) {
                    text = subject + "\n\n" + text;
                }
                Note note = mNotesBucket.newObject();
                note.setCreationDate(Calendar.getInstance());
                note.setModificationDate(note.getCreationDate());
                note.setContent(new SpannableString(text));
                note.save();
                setCurrentNote(note);
                mShouldSelectNewNote = true;

                if (!DisplayUtils.isLargeScreenLandscape(this)) {
                    // Disable the lock screen when sharing content and opening NoteEditorActivity
                    // Lock screen activities are enabled again in NoteEditorActivity.onPause()
                    if (AppLockManager.getInstance().isAppLockFeatureEnabled()) {
                        AppLockManager.getInstance().getAppLock().setExemptActivities(
                                new String[]{"com.theost.wavenote.NotesActivity",
                                        "com.theost.wavenote.NoteEditorActivity"});
                        AppLockManager.getInstance().getAppLock().setOneTimeTimeout(0);
                    }
                }
            }
        }
    }

    private void checkForFeedback() {
        if (PrefUtils.getBoolPref(this, PrefUtils.PREF_SHOW_FEEDBACK, true)) {
            if (System.currentTimeMillis() >= PrefUtils.getLongPref(this, PrefUtils.PREF_LAUNCH_TIME) + (DAYS_UNTIL_PROMPT * 24 * 60 * 60 * 1000)
                    && PrefUtils.getIntPref(this, PrefUtils.PREF_LAUNCH_COUNT) >= LAUNCHES_UNTIL_PROMPT) {
                resetLaunchCount();
                new FeedbackDialog(this).show();
            }
        }
    }

    private void resetLaunchCount() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(PrefUtils.PREF_LAUNCH_TIME, String.valueOf(System.currentTimeMillis()));
        editor.putString(PrefUtils.PREF_LAUNCH_COUNT, String.valueOf(0));
        editor.apply();
    }

    private void updateLaunchCount() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(PrefUtils.PREF_LAUNCH_COUNT, String.valueOf(PrefUtils.getIntPref(this, PrefUtils.PREF_LAUNCH_COUNT) + 1));
        editor.apply();
    }

    private void updateNavigationDrawerItems() {
        boolean isAlphaSort = PrefUtils.getBoolPref(this, PrefUtils.PREF_SORT_TAGS_ALPHA);
        Bucket.ObjectCursor<Tag> tagCursor;
        if (isAlphaSort) {
            tagCursor = Tag.allSortedAlphabetically(mTagsBucket).execute();
        } else {
            tagCursor = Tag.allWithName(mTagsBucket).execute();
        }

        mTagsAdapter.changeCursor(tagCursor);
        mNavigationMenu.removeGroup(GROUP_SECONDARY);
        mNavigationMenu.removeGroup(GROUP_TERTIARY);

        if (mTagsAdapter.getCountCustom() > 0) {
            mNavigationMenu.add(GROUP_SECONDARY, TAGS_ID, Menu.NONE, getString(R.string.tags)).setActionView(R.layout.drawer_action_edit).setEnabled(false);

            for (int i = 0; i < mTagsAdapter.getCount(); i++) {
                String name = mTagsAdapter.getItem(i).name;
                int id = (int) mTagsAdapter.getItem(i).id;

                if (id >= 0) { // Custom tags have a positive ID.
                    mNavigationMenu.add(GROUP_SECONDARY, id, Menu.NONE, name).setCheckable(true);
                }
            }

            mNavigationMenu.add(GROUP_TERTIARY, UNTAGGED_NOTES_ID, Menu.NONE, getString(R.string.untagged_notes)).setIcon(R.drawable.ic_untagged_24dp).setCheckable(true);
            setSelectedTagActive();
        }
    }

    public void launchEditTags(View view) {
        startActivity(new Intent(NotesActivity.this, TagsActivity.class));
    }

    private void setSelectedTagActive() {
        if (mSelectedTag == null) {
            mSelectedTag = mTagsAdapter.getDefaultItem();
        }

        MenuItem selectedMenuItem = mNavigationMenu.findItem((int) mSelectedTag.id);

        if (selectedMenuItem != null) {
            selectedMenuItem.setChecked(true);
        } else {
            mNavigationMenu.findItem(ALL_NOTES_ID).setChecked(true);
        }

        setTitle(mSelectedTag.name);
    }

    public TagsAdapter.TagMenuItem getSelectedTag() {
        if (mSelectedTag == null) {
            mSelectedTag = mTagsAdapter.getDefaultItem();
        }

        return mSelectedTag;
    }

    // Set trash action bar button enabled/disabled and icon based on deleted notes or not.
    public void updateTrashMenuItem() {
        if (mEmptyTrashMenuItem == null || mNotesBucket == null) {
            return;
        }

        // Disable trash icon if there are no trashed notes.
        Query<Note> query = Note.allDeleted(mNotesBucket);

        if (query.count() == 0) {
            DrawableUtils.setMenuItemAlpha(mEmptyTrashMenuItem, 0.5f);
            mEmptyTrashMenuItem.setEnabled(false);
        } else {
            DrawableUtils.setMenuItemAlpha(mEmptyTrashMenuItem, 1.0f);
            mEmptyTrashMenuItem.setEnabled(true);
        }
    }

    public void updateTrashMenuItem(boolean shouldWaitForAnimation) {
        if (shouldWaitForAnimation) {
            new Handler(Looper.getMainLooper()).postDelayed(
                    this::updateTrashMenuItem,
                    getResources().getInteger(R.integer.time_animation)
            );
        } else {
            updateTrashMenuItem();
        }
    }

    private void addEditorFragment() {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        mNoteEditorFragment = new NoteEditorFragment();
        ft.add(R.id.note_fragment_container, mNoteEditorFragment, TAG_NOTE_EDITOR);
        ft.commitAllowingStateLoss();
        fm.executePendingTransactions();
    }

    private boolean userAccountRequired() {
        return PrefUtils.getBoolPref(this, PrefUtils.PREF_ACCOUNT_REQUIRED, false);
    }

    /**
     * Checks for a previously valid user that is now not authenticated
     * Also checks if user account is required (added in version 1.5.6)
     *
     * @return true if user has invalid authorization
     */
    private boolean userAuthenticationIsInvalid() {
        Wavenote currentApp = (Wavenote) getApplication();
        Simperium simperium = currentApp.getSimperium();
        User user = simperium.getUser();
        boolean isNotAuthorized = user.getStatus().equals(User.Status.NOT_AUTHORIZED);
        return (user.hasAccessToken() && isNotAuthorized) ||
                (userAccountRequired() && isNotAuthorized);
    }

    public boolean userIsUnauthorized() {
        Wavenote currentApp = (Wavenote) getApplication();
        return currentApp.getSimperium().getUser().getStatus() == User.Status.NOT_AUTHORIZED;
    }

    public void setCurrentNote(Note note) {
        mCurrentNote = note;
    }

    public NoteListFragment getNoteListFragment() {
        return mNoteListFragment;
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.notes_list, menu);
        MenuCompat.setGroupDividerEnabled(menu, true);
        mMenu = menu;

        // restore the search query if on a landscape tablet
        String searchQuery = null;
        if (DisplayUtils.isLargeScreenLandscape(this) && mSearchView != null)
            searchQuery = mSearchView.getQuery().toString();

        mSearchMenuItem = menu.findItem(R.id.menu_search);
        mSearchView = (SearchView) mSearchMenuItem.getActionView();
        LinearLayout searchEditFrame = mSearchView.findViewById(R.id.search_edit_frame);
        ((LinearLayout.LayoutParams) searchEditFrame.getLayoutParams()).leftMargin = 0;

        if (!TextUtils.isEmpty(searchQuery)) {
            mSearchView.setQuery(searchQuery, false);
            mSearchMenuItem.expandActionView();
        } else {
            // Workaround for setting the search placeholder text color
            @SuppressWarnings("ResourceType")
            String hintHexColor = getString(R.color.text_title_disabled).replace("ff", "");
            mSearchView.setQueryHint(
                    HtmlCompat.fromHtml(
                            String.format(
                                    "<font color=\"%s\">%s</font>",
                                    hintHexColor,
                                    getString(R.string.search_hint)
                            )
                    )
            );
        }

        mSearchView.setMaxWidth(Integer.MAX_VALUE);
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String newText) {
                if (mSearchMenuItem.isActionViewExpanded()) {
                    getNoteListFragment().searchNotes(newText, false);
                }

                return true;
            }

            @Override
            public boolean onQueryTextSubmit(String queryText) {
                getNoteListFragment().searchNotes(queryText, true);
                getNoteListFragment().addSearchItem(queryText, 0);
                checkEmptyListText(true);
                return true;
            }
        });

        mSearchMenuItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem menuItem) {
                mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
                getNoteListFragment().searchNotes("", false);

                if (DisplayUtils.isLargeScreenLandscape(NotesActivity.this)) {
                    updateActionsForLargeLandscape(menu);
                }

                checkEmptyListText(true);

                // Hide floating action button and list bottom padding.
                if (mNoteListFragment != null) {
                    mNoteListFragment.setFloatingActionButtonVisible(false);
                    mNoteListFragment.showListPadding(false);
                }

                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem menuItem) {
                mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);

                if (DisplayUtils.isLargeScreenLandscape(NotesActivity.this)) {
                    updateActionsForLargeLandscape(menu);
                }

                // Show floating action button and list bottom padding.
                if (mNoteListFragment != null) {
                    mNoteListFragment.setFloatingActionButtonVisible(true);
                    mNoteListFragment.showListPadding(true);
                }

                mTabletSearchQuery = "";
                mSearchView.setQuery("", false);
                checkEmptyListText(false);
                getNoteListFragment().clearSearch();
                return true;
            }
        });

        mSearchMenuItem.setOnMenuItemClickListener(item -> {
            if (!mSearchMenuItem.isActionViewExpanded())
                showDetailPlaceholder();
            return false;
        });

        MenuItem trashItem = menu.findItem(R.id.menu_trash);

        if (mCurrentNote != null && mCurrentNote.isDeleted()) {
            trashItem.setTitle(R.string.restore);
        } else {
            trashItem.setTitle(R.string.trash);
        }

        if (DisplayUtils.isLargeScreenLandscape(NotesActivity.this)) {
            // Restore the search query on landscape tablets
            if (!TextUtils.isEmpty(mTabletSearchQuery)) {
                mSearchMenuItem.expandActionView();
                mSearchView.setQuery(mTabletSearchQuery, true);
                mSearchView.clearFocus();
            }

            updateActionsForLargeLandscape(menu);
        } else {
            menu.findItem(R.id.menu_search).setVisible(true);
            menu.findItem(R.id.menu_share).setVisible(false);
            menu.findItem(R.id.menu_info).setVisible(false);
            menu.findItem(R.id.menu_checklist).setVisible(false);
            menu.findItem(R.id.menu_markdown_preview).setVisible(false);
            menu.findItem(R.id.menu_sidebar).setVisible(false);
            trashItem.setVisible(false);
            menu.findItem(R.id.menu_empty_trash).setVisible(false);
            menu.setGroupVisible(R.id.group_organize_features, false);
            menu.setGroupVisible(R.id.group_publish_features, false);
            menu.setGroupVisible(R.id.group_ext_features, false);
            menu.setGroupVisible(R.id.group_info_features, false);
            menu.setGroupVisible(R.id.group_note_features, false);
            menu.setGroupVisible(R.id.group_share_features, false);
            menu.setGroupVisible(R.id.group_def_actions, false);
        }

        if (mSelectedTag != null && mSelectedTag.id == TRASH_ID) {
            mEmptyTrashMenuItem = menu.findItem(R.id.menu_empty_trash);
            mEmptyTrashMenuItem.setVisible(true);

            updateTrashMenuItem(false);

            menu.findItem(R.id.menu_search).setVisible(false);
            menu.findItem(R.id.menu_checklist).setVisible(false);
        }

        DrawableUtils.tintMenuItemWithAttribute(this, menu.findItem(R.id.menu_search), R.attr.toolbarIconColor);

        if (mDrawerLayout != null && mSearchMenuItem != null) {
            mDrawerLayout.setDrawerLockMode(mSearchMenuItem.isActionViewExpanded() ?
                    DrawerLayout.LOCK_MODE_LOCKED_CLOSED :
                    DrawerLayout.LOCK_MODE_UNLOCKED
            );
        }

        return true;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        switch (item.getItemId()) {
            case R.id.menu_sidebar:
                toggleSidebar(item);
                return true;
            case R.id.menu_markdown_preview:
                togglePreview(item);
                return true;
            case R.id.menu_trash:
                if (mNoteEditorFragment != null && mCurrentNote != null) {
                    mCurrentNote.setDeleted(!mCurrentNote.isDeleted());
                    mCurrentNote.setModificationDate(Calendar.getInstance());
                    mCurrentNote.save();
                    updateViewsAfterTrashAction(mCurrentNote);
                }

                return true;
            case R.id.menu_empty_trash:
                new MaterialDialog.Builder(this)
                        .title(R.string.empty_trash)
                        .content(R.string.confirm_empty_trash)
                        .positiveText(R.string.yes)
                        .negativeText(R.string.no)
                        .onPositive((dialog, which) -> {
                            new EmptyTrashTask(NotesActivity.this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                            DrawableUtils.startAnimatedVectorDrawable(item.getIcon());
                            setIconAfterAnimation(item, R.drawable.ic_trash_disabled_24dp, R.string.empty_trash);
                        }).show();
                return true;
            case android.R.id.home:
                invalidateOptionsMenu();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (DisplayUtils.isLargeScreenLandscape(NotesActivity.this)) {
            updateActionsForLargeLandscape(menu);
        }

        MenuItem pinItem = menu.findItem(R.id.menu_pin);
        MenuItem shareItem = menu.findItem(R.id.menu_share);
        MenuItem historyItem = menu.findItem(R.id.menu_history);
        MenuItem publishItem = menu.findItem(R.id.menu_publish);
        MenuItem copyLinkItem = menu.findItem(R.id.menu_copy);
        MenuItem markdownItem = menu.findItem(R.id.menu_markdown);
        MenuItem markdownPreviewItem = menu.findItem(R.id.menu_markdown_preview);

        if (mIsShowingMarkdown) {
            markdownPreviewItem.setIcon(R.drawable.ic_visibility_off_on_24dp);
            markdownPreviewItem.setTitle(R.string.markdown_hide);
        } else {
            markdownPreviewItem.setIcon(R.drawable.ic_visibility_on_off_24dp);
            markdownPreviewItem.setTitle(R.string.markdown_show);
        }

        if (mCurrentNote != null) {
            pinItem.setChecked(mCurrentNote.isPinned());
            publishItem.setChecked(mCurrentNote.isPublished());
            markdownItem.setChecked(mCurrentNote.isMarkdownEnabled());

            if (mCurrentNote.isDeleted()) {
                pinItem.setEnabled(false);
                shareItem.setEnabled(false);
                historyItem.setEnabled(false);
                publishItem.setEnabled(false);
                copyLinkItem.setEnabled(false);
                markdownItem.setEnabled(false);
            } else {
                pinItem.setEnabled(true);
                shareItem.setEnabled(true);
                historyItem.setEnabled(true);
                publishItem.setEnabled(true);
                copyLinkItem.setEnabled(mCurrentNote.isPublished());
                markdownItem.setEnabled(true);
            }
        }

        return super.onPrepareOptionsMenu(menu);
    }

    private void setIconAfterAnimation(final MenuItem item, final @DrawableRes int drawable, final @StringRes int string) {
        new Handler(Looper.getMainLooper()).postDelayed(
                () -> {
                    item.setIcon(drawable);
                    item.setTitle(string);

                    if (item == mEmptyTrashMenuItem) {
                        invalidateOptionsMenu();
                    }
                },
                getResources().getInteger(R.integer.time_animation)
        );
    }

    public void submitSearch(String selection) {
        if (mSearchView != null) {
            String query = mSearchView.getQuery().toString();

            if (query.endsWith(TAG_PREFIX)) {
                mSearchView.setQuery(query.substring(0, query.lastIndexOf(TAG_PREFIX)) + selection, true);
            } else {
                mSearchView.setQuery(selection, true);
            }
        }
    }

    private void updateActionsForLargeLandscape(Menu menu) {
        if (mCurrentNote != null) {
            menu.findItem(R.id.menu_checklist).setVisible(true);
            menu.findItem(R.id.menu_markdown_preview).setVisible(mCurrentNote.isMarkdownEnabled());
            menu.findItem(R.id.menu_sidebar).setVisible(true);
            menu.findItem(R.id.menu_info).setVisible(true);

            menu.setGroupVisible(R.id.group_organize_features, true);
            menu.setGroupVisible(R.id.group_publish_features, true);
            menu.setGroupVisible(R.id.group_ext_features, true);
            menu.setGroupVisible(R.id.group_info_features, true);
            menu.setGroupVisible(R.id.group_note_features, true);
            menu.setGroupVisible(R.id.group_share_features, true);
            menu.setGroupVisible(R.id.group_def_actions, true);
        } else {
            menu.findItem(R.id.menu_checklist).setVisible(false);
            menu.findItem(R.id.menu_markdown_preview).setVisible(false);
            menu.findItem(R.id.menu_sidebar).setVisible(false);
            menu.findItem(R.id.menu_info).setVisible(false);

            menu.setGroupVisible(R.id.group_organize_features, false);
            menu.setGroupVisible(R.id.group_publish_features, false);
            menu.setGroupVisible(R.id.group_ext_features, false);
            menu.setGroupVisible(R.id.group_info_features, false);
            menu.setGroupVisible(R.id.group_note_features, false);
            menu.setGroupVisible(R.id.group_share_features, false);
            menu.setGroupVisible(R.id.group_def_actions, false);
        }

        menu.findItem(R.id.menu_empty_trash).setVisible(mSelectedTag != null && mSelectedTag.id == TRASH_ID);
    }

    public void updateViewsAfterTrashAction(Note note) {
        if (note == null || isFinishing()) {
            return;
        }

        if (note.isDeleted()) {
            List<String> deletedNoteIds = new ArrayList<>();
            deletedNoteIds.add(note.getSimperiumKey());
            mUndoBarController.setDeletedNoteIds(deletedNoteIds);
            mUndoBarController.showUndoBar(getUndoView(), getString(R.string.note_deleted));
        }

        // If we just deleted/restored the active note, show the placeholder
        if (mCurrentNote != null && mCurrentNote.getSimperiumKey().equals(note.getSimperiumKey())) {
            showDetailPlaceholder();
        }

        NoteListFragment fragment = getNoteListFragment();
        if (fragment != null) {
            fragment.getPrefs();
            fragment.refreshList();
        }

        if (mInvalidateOptionsMenuHandler != null) {
            mInvalidateOptionsMenuHandler.removeCallbacks(mInvalidateOptionsMenuRunnable);
            mInvalidateOptionsMenuHandler.postDelayed(
                    mInvalidateOptionsMenuRunnable,
                    getResources().getInteger(android.R.integer.config_shortAnimTime)
            );
        }
    }

    public void setMarkdownShowing(boolean isMarkdownShowing) {
        mIsShowingMarkdown = isMarkdownShowing;

        if (mNoteEditorFragment != null) {
            if (isMarkdownShowing) {
                mNoteEditorFragment.showMarkdown();
            } else {
                mNoteEditorFragment.hideMarkdown();
            }
        }

        new Handler(Looper.getMainLooper()).postDelayed(
                this::invalidateOptionsMenu,
                getResources().getInteger(R.integer.time_animation)
        );
    }

    /**
     * Callback method from {@link NoteListFragment.Callbacks} indicating that
     * the item with the given ID was selected. Used for tablets only.
     */
    @Override
    public void onNoteSelected(String noteID, String matchOffsets, boolean isMarkdownEnabled, boolean isPreviewEnabled) {
        if (!DisplayUtils.isLargeScreenLandscape(this)) {
            // Launch the editor activity
            Bundle arguments = new Bundle();
            arguments.putString(NoteEditorFragment.ARG_ITEM_ID, noteID);
            arguments.putBoolean(NoteEditorFragment.ARG_MARKDOWN_ENABLED, isMarkdownEnabled);
            arguments.putBoolean(NoteEditorFragment.ARG_PREVIEW_ENABLED, isPreviewEnabled);

            if (matchOffsets != null) {
                arguments.putString(NoteEditorFragment.ARG_MATCH_OFFSETS, matchOffsets);
            }

            Intent editNoteIntent = new Intent(this, NoteEditorActivity.class);
            editNoteIntent.putExtras(arguments);

            if (mNoteListFragment.isHidden()) {
                editNoteIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            }

            startActivityForResult(editNoteIntent, Wavenote.INTENT_EDIT_NOTE);
        } else {
            mNoteEditorFragment.setNote(noteID, matchOffsets);
            getNoteListFragment().setNoteSelected(noteID);
            setMarkdownShowing(isPreviewEnabled && matchOffsets == null);

            if (mSearchView != null && mSearchView.getQuery() != null) {
                mTabletSearchQuery = mSearchView.getQuery().toString();
            }

            mNoteEditorFragment.clearMarkdown();

            if (!isMarkdownEnabled && mIsShowingMarkdown) {
                setMarkdownShowing(false);
            }

            invalidateOptionsMenu();
        }
    }

    @Override
    public void onUserCreated(User user) {
        // New account created
    }

    public void onUserStatusChange(User.Status status) {
        switch (status) {
            // successfully used access token to connect to simperium bucket
            case AUTHORIZED:
                runOnUiThread(() -> {
                    if (!mNotesBucket.hasChangeVersion()) {
                        setToolbarProgressVisibility(true);
                    }
                });
                break;

            // NOT_AUTHORIZED means we attempted to connect but the token was not valid
            case NOT_AUTHORIZED:
                runOnUiThread(this::startLoginActivity);
                break;

            // Default starting state of User, don't do anything we allow use of app while not signed in so don't do anything
            case UNKNOWN:
                break;
        }
    }

    private void setToolbarProgressVisibility(boolean isVisible) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowCustomEnabled(isVisible);
        }
    }

    public void startLoginActivity() {
        // Clear account-specific data
        AuthUtils.logOut((Wavenote) getApplication());

        Intent intent = new Intent(NotesActivity.this, WavenoteAuthenticationActivity.class);
        startActivityForResult(intent, Simperium.SIGNUP_SIGNIN_REQUEST);
    }

    @Override
    public void recreate() {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(NotesActivity.super::recreate);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case Wavenote.INTENT_PREFERENCES:
                // nbradbury - refresh note list when user returns from preferences (in case they changed anything)
                invalidateOptionsMenu();
                NoteListFragment fragment = getNoteListFragment();
                if (fragment != null) {
                    fragment.getPrefs();
                    fragment.refreshList();
                }

                break;
            case Wavenote.INTENT_EDIT_NOTE:
                if (resultCode == RESULT_OK && data != null) {
                    if (data.hasExtra(Wavenote.DELETED_NOTE_ID)) {
                        String noteId = data.getStringExtra(Wavenote.DELETED_NOTE_ID);
                        if (noteId != null) {
                            List<String> deletedNoteIds = new ArrayList<>();
                            deletedNoteIds.add(noteId);
                            mUndoBarController.setDeletedNoteIds(deletedNoteIds);
                            mUndoBarController.showUndoBar(getUndoView(), getString(R.string.note_deleted));
                        }
                    } else if (DisplayUtils.isLargeScreenLandscape(this) && data.hasExtra(Wavenote.SELECTED_NOTE_ID)) {
                        String selectedNoteId = data.getStringExtra(Wavenote.SELECTED_NOTE_ID);
                        mNoteListFragment.setNoteSelected(selectedNoteId);
                        if (mNoteEditorFragment != null) {
                            mNoteEditorFragment.setNote(selectedNoteId);
                        }

                        // Relaunch shortcut dialog if it was showing in editor (Chrome OS).
                        if (data.getBooleanExtra(ShortcutDialogFragment.DIALOG_VISIBLE, false)) {
                            ShortcutDialogFragment.showShortcuts(NotesActivity.this, false);
                        }
                    }
                }
                break;
            case Simperium.SIGNUP_SIGNIN_REQUEST:
                invalidateOptionsMenu();
                if (resultCode == Activity.RESULT_CANCELED && userAuthenticationIsInvalid()) {
                    finish();
                }
                break;
        }
    }

    @Override
    public void onUndo() {
        if (mUndoBarController == null) return;

        List<String> deletedNoteIds = mUndoBarController.getDeletedNoteIds();
        if (deletedNoteIds != null) {
            for (int i = 0; i < deletedNoteIds.size(); i++) {
                Note deletedNote;
                try {
                    deletedNote = mNotesBucket.get(deletedNoteIds.get(i));
                } catch (BucketObjectMissingException e) {
                    return;
                }
                if (deletedNote != null) {
                    deletedNote.setDeleted(false);
                    deletedNote.setModificationDate(Calendar.getInstance());
                    deletedNote.save();
                    NoteListFragment fragment = getNoteListFragment();
                    if (fragment != null) {
                        fragment.getPrefs();
                        fragment.refreshList();
                    }
                }
            }
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        mDrawerToggle.onConfigurationChanged(newConfig);

        // Relaunch shortcut dialog when window is maximized or restored (Chrome OS).
        if (getSupportFragmentManager().findFragmentByTag(ShortcutDialogFragment.DIALOG_TAG) != null) {
            ShortcutDialogFragment.showShortcuts(NotesActivity.this, false);
        }

        if (DisplayUtils.isLargeScreen(this)) {
            mIsShowingMarkdown = false;

            if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                // Add the editor fragment
                if (getSupportFragmentManager().findFragmentByTag(TAG_NOTE_EDITOR) != null) {
                    mNoteEditorFragment = (NoteEditorFragment) getSupportFragmentManager().findFragmentByTag(TAG_NOTE_EDITOR);
                } else if (DisplayUtils.isLandscape(this)) {
                    addEditorFragment();
                }

                if (mNoteListFragment != null) {
                    mNoteListFragment.setActivateOnItemClick(true);
                    mNoteListFragment.setDividerVisible(true);
                }

                // Select the current note on a tablet
                if (mCurrentNote != null) {
                    onNoteSelected(mCurrentNote.getSimperiumKey(), null, mCurrentNote.isMarkdownEnabled(), mCurrentNote.isPreviewEnabled());
                } else {
                    assert mNoteEditorFragment != null;
                    mNoteEditorFragment.setPlaceholderVisible(true);
                    mNoteListFragment.getListView().clearChoices();
                }

                invalidateOptionsMenu();
                // Go to NoteEditorActivity if note editing was fullscreen and orientation was switched to portrait
            } else if (mNoteListFragment.isHidden() && mCurrentNote != null) {
                onNoteSelected(mCurrentNote.getSimperiumKey(), null, mCurrentNote.isMarkdownEnabled(), mCurrentNote.isPreviewEnabled());
            }
        } else if (mNoteListFragment.isHidden()) {
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragmentTransaction.show(mNoteListFragment);
            fragmentTransaction.commitNowAllowingStateLoss();
            mIsTabletFullscreen = mNoteListFragment.isHidden();
        }

        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT && mNoteEditorFragment != null) {
            // Remove the editor fragment when rotating back to portrait
            mCurrentNote = null;
            if (mNoteListFragment != null) {
                mNoteListFragment.setActivateOnItemClick(false);
                mNoteListFragment.setDividerVisible(false);
                mNoteListFragment.setActivatedPosition(ListView.INVALID_POSITION);
                mNoteListFragment.refreshList();
            }
            FragmentManager fm = getSupportFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            ft.remove(mNoteEditorFragment);
            mNoteEditorFragment = null;
            ft.commitAllowingStateLoss();
            fm.executePendingTransactions();
            invalidateOptionsMenu();
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_C:
                if (event.isShiftPressed() && event.isCtrlPressed()) {
                    if (isLargeLandscapeAndNoteSelected()) {
                        if (mNoteEditorFragment != null) {
                            mNoteEditorFragment.insertChecklist();
                        }
                    } else {
                        Toast.makeText(NotesActivity.this, R.string.item_action_toggle_checklist_error, Toast.LENGTH_SHORT).show();
                    }

                    return true;
                } else {
                    return super.onKeyUp(keyCode, event);
                }
            case KeyEvent.KEYCODE_COMMA:
                if (event.isCtrlPressed()) {
                    ShortcutDialogFragment.showShortcuts(NotesActivity.this, false);
                    return true;
                } else {
                    return super.onKeyUp(keyCode, event);
                }
            case KeyEvent.KEYCODE_H:
                if (event.isCtrlPressed()) {
                    if (isLargeLandscapeAndNoteSelected()) {
                        if (mNoteEditorFragment != null) {
                            mNoteEditorFragment.showHistory();
                        }
                    } else {
                        Toast.makeText(NotesActivity.this, R.string.item_action_show_history_error, Toast.LENGTH_SHORT).show();
                    }

                    return true;
                } else {
                    return super.onKeyUp(keyCode, event);
                }
            case KeyEvent.KEYCODE_I:
                if (event.isShiftPressed() && event.isCtrlPressed()) {
                    getNoteListFragment().createNewNote("keyboard_shortcut");
                    return true;
                } else if (event.isCtrlPressed()) {
                    if (isLargeLandscapeAndNoteSelected()) {
                        if (mNoteEditorFragment != null) {
                            mNoteEditorFragment.showInfo();
                        }
                    } else {
                        Toast.makeText(NotesActivity.this, R.string.item_action_show_information_error, Toast.LENGTH_SHORT).show();
                    }

                    return true;
                } else {
                    return super.onKeyUp(keyCode, event);
                }
            case KeyEvent.KEYCODE_L:
                if (event.isShiftPressed() && event.isCtrlPressed()) {
                    if (isLargeLandscapeAndNoteSelected()) {
                        toggleSidebar(mMenu.findItem(R.id.menu_sidebar));
                    } else {
                        Toast.makeText(NotesActivity.this, R.string.item_action_toggle_list_error, Toast.LENGTH_SHORT).show();
                    }

                    return true;
                } else {
                    return super.onKeyUp(keyCode, event);
                }
            case KeyEvent.KEYCODE_P:
                if (event.isShiftPressed() && event.isCtrlPressed()) {
                    if (isLargeLandscapeAndNoteSelected()) {
                        if (mCurrentNote != null && mCurrentNote.isMarkdownEnabled()) {
                            togglePreview(mMenu.findItem(R.id.menu_markdown_preview));
                        } else {
                            Toast.makeText(NotesActivity.this, R.string.item_action_toggle_preview_enable_error, Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(NotesActivity.this, R.string.item_action_toggle_preview_error, Toast.LENGTH_SHORT).show();
                    }

                    return true;
                } else {
                    return super.onKeyUp(keyCode, event);
                }
            case KeyEvent.KEYCODE_S:
                if (event.isShiftPressed() && event.isCtrlPressed()) {
                    if (mSearchMenuItem != null && mSearchView != null) {
                        mSearchMenuItem.expandActionView();
                        mSearchView.requestFocus();
                    }

                    return true;
                } else if (event.isCtrlPressed()) {
                    if (isLargeLandscapeAndNoteSelected()) {
                        if (mNoteEditorFragment != null) {
                            mNoteEditorFragment.shareNote();
                        }
                    } else {
                        Toast.makeText(NotesActivity.this, R.string.item_action_show_share_error, Toast.LENGTH_SHORT).show();
                    }

                    return true;
                } else {
                    return super.onKeyUp(keyCode, event);
                }
            default:
                return super.onKeyUp(keyCode, event);
        }
    }

    private void togglePreview(MenuItem item) {
        if (mIsShowingMarkdown) {
            setIconAfterAnimation(item, R.drawable.ic_visibility_on_off_24dp, R.string.markdown_show);
            setMarkdownShowing(false);
            mCurrentNote.setPreviewEnabled(false);
        } else {
            setIconAfterAnimation(item, R.drawable.ic_visibility_off_on_24dp, R.string.markdown_hide);
            setMarkdownShowing(true);
            mCurrentNote.setPreviewEnabled(true);
        }

        mCurrentNote.save();
    }

    private void toggleSidebar(MenuItem item) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

        if (mNoteListFragment.isHidden()) {
            ft.show(mNoteListFragment);
            setIconAfterAnimation(item, R.drawable.ic_list_hide_show_24dp, R.string.list_hide);
        } else {
            ft.hide(mNoteListFragment);
            setIconAfterAnimation(item, R.drawable.ic_list_show_hide_24dp, R.string.list_show);
        }

        ft.commitNowAllowingStateLoss();
        mIsTabletFullscreen = mNoteListFragment.isHidden();
    }

    private boolean isLargeLandscapeAndNoteSelected() {
        return DisplayUtils.isLargeScreenLandscape(NotesActivity.this) && mNoteEditorFragment != null && !mNoteEditorFragment.isPlaceholderVisible();
    }

    public void checkEmptyListText(boolean isSearch) {
        if (isSearch) {
            if (DisplayUtils.isLandscape(this) && !DisplayUtils.isLargeScreen(this)) {
                getNoteListFragment().setEmptyListImage(-1);
            } else {
                getNoteListFragment().setEmptyListImage(R.drawable.ic_search_24dp);
            }

            getNoteListFragment().setEmptyListMessage(getString(R.string.empty_notes_search));
        } else if (mSelectedTag != null) {
            if (mSelectedTag.id == ALL_NOTES_ID) {
                getNoteListFragment().setEmptyListImage(R.drawable.ic_notes_list_24dp);
                getNoteListFragment().setEmptyListMessage(getString(R.string.empty_notes_all));
            } else if (mSelectedTag.id == TRASH_ID) {
                getNoteListFragment().setEmptyListImage(R.drawable.ic_trash_24dp);
                getNoteListFragment().setEmptyListMessage(getString(R.string.empty_notes_trash));
            } else if (mSelectedTag.id == UNTAGGED_NOTES_ID) {
                getNoteListFragment().setEmptyListImage(R.drawable.ic_untagged_24dp);
                getNoteListFragment().setEmptyListMessage(getString(R.string.empty_notes_untagged));
            } else {
                getNoteListFragment().setEmptyListImage(R.drawable.ic_tag_24dp);
                getNoteListFragment().setEmptyListMessage(getString(R.string.empty_notes_tag, mSelectedTag.name));
            }
        } else {
            getNoteListFragment().setEmptyListImage(R.drawable.ic_notes_list_24dp);
            getNoteListFragment().setEmptyListMessage(getString(R.string.empty_notes_all));
        }
    }

    public void showDetailPlaceholder() {
        if (DisplayUtils.isLargeScreenLandscape(this) && mNoteEditorFragment != null) {
            mCurrentNote = null;
            mNoteEditorFragment.setPlaceholderVisible(true);
            mNoteEditorFragment.clearMarkdown();
            mNoteEditorFragment.hideMarkdown();
            mIsShowingMarkdown = false;
        }
    }

    public void stopListeningToNotesBucket() {
        mNotesBucket.removeOnNetworkChangeListener(this);
        mNotesBucket.removeOnSaveObjectListener(this);
        mNotesBucket.removeOnDeleteObjectListener(this);
    }

    // Returns the appropriate view to show the undo bar within
    private View getUndoView() {
        View undoView = mFragmentsContainer;
        if (!DisplayUtils.isLargeScreenLandscape(this) &&
                getNoteListFragment() != null &&
                getNoteListFragment().getRootView() != null) {
            undoView = getNoteListFragment().getRootView();
        }

        return undoView;
    }

    public void showUndoBarWithNoteIds(List<String> noteIds) {
        if (mUndoBarController != null) {
            mUndoBarController.setDeletedNoteIds(noteIds);
            mUndoBarController.showUndoBar(
                    getUndoView(),
                    getResources().getQuantityString(R.plurals.trashed_notes, noteIds.size(), noteIds.size())
            );
        }
    }

    /* Simperium Bucket Listeners */
    // received a change from the network, refresh the list
    @Override
    public void onNetworkChange(Bucket<Note> bucket, final Bucket.ChangeType type, String key) {
        runOnUiThread(() -> {
            if (type == Bucket.ChangeType.INDEX) {
                setToolbarProgressVisibility(false);
            }
            mNoteListFragment.refreshList();
        });
    }

    @Override
    public void onSaveObject(Bucket<Note> bucket, Note note) {
        if (mNoteListFragment.isAdded()) runOnUiThread(() -> mNoteListFragment.refreshList());

        if (note.equals(mCurrentNote)) {
            mCurrentNote = note;

            new Handler(Looper.getMainLooper()).postDelayed(
                    this::invalidateOptionsMenu,
                    getResources().getInteger(R.integer.time_animation)
            );
        }
    }

    @Override
    public void onDeleteObject(Bucket<Note> bucket, Note object) {
        runOnUiThread(() -> mNoteListFragment.refreshList());
    }

    @Override
    public void onBeforeUpdateObject(Bucket<Note> bucket, Note note) {
        // noop, NoteEditorFragment will handle this
    }

    @Override
    public void onLocalQueueChange(Bucket<Note> bucket, Set<String> queuedObjects) {

    }

    @Override
    public void onSyncObject(Bucket<Note> bucket, String key) {

    }

    private static class EmptyTrashTask extends AsyncTask<Void, Void, Void> {
        private final SoftReference<NotesActivity> mNotesActivityReference;
        private final DatabaseHelper localDatabase;

        EmptyTrashTask(NotesActivity context) {
            mNotesActivityReference = new SoftReference<>(context);
            localDatabase = new DatabaseHelper(context);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            NotesActivity activity = mNotesActivityReference.get();

            if (activity.mNotesBucket == null) {
                return null;
            }

            Query<Note> query = Note.allDeleted(activity.mNotesBucket);
            Bucket.ObjectCursor cursor = query.execute();

            while (cursor.moveToNext()) {
                FileUtils.removeDirectory(new File(activity.getCacheDir() + NOTES_DIR + cursor.getSimperiumKey()));
                localDatabase.removeAllImageData(cursor.getSimperiumKey());
                cursor.getObject().delete();
            }
            if (localDatabase != null) localDatabase.close();
            return null;
        }

        @Override
        protected void onPostExecute(Void nada) {
            NotesActivity activity = mNotesActivityReference.get();

            if (activity != null) {
                activity.showDetailPlaceholder();
            }
        }
    }
}