package com.theost.wavenote.models;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;

import com.theost.wavenote.R;
import com.simperium.client.Bucket;
import com.simperium.client.BucketObject;
import com.simperium.client.BucketSchema;
import com.simperium.client.Query;
import com.simperium.client.Query.ComparisonType;
import com.theost.wavenote.utils.HtmlCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class Note extends BucketObject {

    public static final String BUCKET_NAME = "note";
    public static final String MARKDOWN_TAG = "markdown";
    public static final String PINNED_TAG = "pinned";
    public static final String PREVIEW_TAG = "preview";
    public static final String PUBLISHED_TAG = "published";
    public static final String NEW_LINE = "\n";
    public static final String CONTENT_PROPERTY = "content";
    public static final String TAGS_PROPERTY = "tags";
    public static final String SYSTEM_TAGS_PROPERTY = "systemTags";
    public static final String CREATION_DATE_PROPERTY = "creationDate";
    public static final String MODIFICATION_DATE_PROPERTY = "modificationDate";
    public static final String SHARE_URL_PROPERTY = "shareURL";
    public static final String PUBLISH_URL_PROPERTY = "publishURL";
    public static final String DELETED_PROPERTY = "deleted";
    public static final String TITLE_INDEX_NAME = "title";
    public static final String CONTENT_PREVIEW_INDEX_NAME = "contentPreview";
    public static final String PINNED_INDEX_NAME = "pinned";
    public static final String MODIFIED_INDEX_NAME = "modified";
    public static final String CREATED_INDEX_NAME = "created";
    public static final String MATCHED_TITLE_INDEX_NAME = "matchedTitle";
    public static final String MATCHED_CONTENT_INDEX_NAME = "matchedContent";
    public static final String PUBLISH_URL = "http://simp.ly/publish/";
    public static Integer SELECTED_COLOR = -1;
    public static String TEXT_COLOR_AC = "#36393e";
    public static String TEXT_COLOR_DI = "#fafafa";
    public static String PHOTO_SORT_MODE = "date";
    public static String ACTIVE_INSTRUMENT;
    public static int ACTIVE_TAB_COLUMNS = 4;
    public static boolean TEXT_STYLE_BOLD = false;
    public static boolean TEXT_STYLE_ITALIC = false;
    public static boolean TEXT_STYLE_STROKE = false;
    public static boolean TEXT_STYLE_CODE = false;
    public static boolean TEXT_STYLE_UNDERLINE = false;
    public static boolean TEXT_STYLE_STRIKETHROUGH = false;
    public static boolean NEED_RESOURCES_UPDATE = true;
    private static final int MAX_PREVIEW_CHARS = 300;
    public static String ACTIVE_METRONOME_SOUND;
    public static final String[] FULL_TEXT_INDEXES = new String[]{Note.TITLE_INDEX_NAME, Note.CONTENT_PROPERTY};
    private static final Spannable BLANK_CONTENT = new SpannableString("");
    private static final String SPACE = " ";
    protected String mContentPreview = null;
    protected String mTitle = null;

    public Note(String key) {
        super(key, new JSONObject());
    }

    public Note(String key, JSONObject properties) {
        super(key, properties);
    }

    public static Query<Note> all(Bucket<Note> noteBucket) {
        return noteBucket.query()
                .where(DELETED_PROPERTY, ComparisonType.NOT_EQUAL_TO, true);
    }

    public static Query<Note> allDeleted(Bucket<Note> noteBucket) {
        return noteBucket.query()
                .where(DELETED_PROPERTY, ComparisonType.EQUAL_TO, true);
    }

    public static Query<Note> search(Bucket<Note> noteBucket, String searchString) {
        return noteBucket.query()
                .where(DELETED_PROPERTY, ComparisonType.NOT_EQUAL_TO, true)
                .where(CONTENT_PROPERTY, ComparisonType.LIKE, "%" + searchString + "%");
    }

    public static Query<Note> allInTag(Bucket<Note> noteBucket, String tag) {
        return noteBucket.query()
                .where(DELETED_PROPERTY, ComparisonType.NOT_EQUAL_TO, true)
                .where(TAGS_PROPERTY, ComparisonType.LIKE, tag);
    }

    public static Query<Note> allWithNoTag(Bucket<Note> noteBucket) {
        return noteBucket.query()
                .where(DELETED_PROPERTY, ComparisonType.NOT_EQUAL_TO, true)
                .where(TAGS_PROPERTY, ComparisonType.EQUAL_TO, null);
    }

    @SuppressWarnings("unused")
    public static String dateString(Number time, boolean useShortFormat, Context context) {
        Calendar c = numberToDate(time);
        return dateString(c, useShortFormat, context);
    }

    public static String dateString(Calendar c, boolean useShortFormat, Context context) {
        int year, month, day;

        String time, date, retVal;

        Calendar diff = Calendar.getInstance();
        diff.setTimeInMillis(diff.getTimeInMillis() - c.getTimeInMillis());

        year = diff.get(Calendar.YEAR);
        month = diff.get(Calendar.MONTH);
        day = diff.get(Calendar.DAY_OF_MONTH);

        diff.setTimeInMillis(0); // starting time
        time = DateFormat.getTimeInstance(DateFormat.SHORT).format(c.getTime());
        if ((year == diff.get(Calendar.YEAR)) && (month == diff.get(Calendar.MONTH)) && (day == diff.get(Calendar.DAY_OF_MONTH))) {
            date = context.getResources().getString(R.string.today);
            if (useShortFormat)
                retVal = time;
            else
                retVal = date + ", " + time;
        } else if ((year == diff.get(Calendar.YEAR)) && (month == diff.get(Calendar.MONTH)) && (day == 1)) {
            date = context.getResources().getString(R.string.yesterday);
            if (useShortFormat)
                retVal = date;
            else
                retVal = date + ", " + time;
        } else {
            date = new SimpleDateFormat("MMM dd", Locale.US).format(c.getTime());
            retVal = date + ", " + time;
        }

        return retVal;
    }

    public static Calendar numberToDate(Number time) {
        Calendar date = Calendar.getInstance();
        if (time != null) {
            // Flick Note uses millisecond resolution timestamps Wavenote expects seconds
            // since we only deal with create and modify timestamps, they should all have occurred
            // at the present time or in the past.
            float now = (float) date.getTimeInMillis() / 1000;
            float magnitude = time.floatValue() / now;
            if (magnitude >= 2.f) time = time.longValue() / 1000;
            date.setTimeInMillis(time.longValue() * 1000);
        }
        return date;
    }

    protected void updateTitleAndPreview() {
        // try to build a title and preview property out of content
        String content = getContent().toString().trim();
        if (content.length() > MAX_PREVIEW_CHARS) {
            content = content.substring(0, MAX_PREVIEW_CHARS - 1);
        }

        int firstNewLinePosition = content.indexOf(NEW_LINE);
        if (firstNewLinePosition > -1 && firstNewLinePosition < 200) {
            mTitle = content.substring(0, firstNewLinePosition).trim();

            if (firstNewLinePosition < content.length()) {
                mContentPreview = content.substring(firstNewLinePosition);
                mContentPreview = mContentPreview.replace(NEW_LINE, SPACE).replace(SPACE + SPACE, SPACE).trim();
            } else {
                mContentPreview = content;
            }
        } else {
            mTitle = content;
            mContentPreview = content;
        }
    }

    public static String getActiveInstrument() {
        return ACTIVE_INSTRUMENT;
    }

    public static void setActiveInstrument(String activeInstrument) {
        ACTIVE_INSTRUMENT = activeInstrument;
    }

    public static int getActiveTabColumns() {
        return ACTIVE_TAB_COLUMNS;
    }

    public static void setActiveTabColumns(int activeTabColumns) {
        ACTIVE_TAB_COLUMNS = activeTabColumns;
    }

    public static String getActiveMetronomeSound() {
        return ACTIVE_METRONOME_SOUND;
    }

    public static void setActiveMetronomeSound(String activeMetronomeSound) {
        ACTIVE_METRONOME_SOUND = activeMetronomeSound;
    }

    public static boolean isNeedResourcesUpdate() {
        return NEED_RESOURCES_UPDATE;
    }

    public static void setNeedResourcesUpdate(boolean update) {
        NEED_RESOURCES_UPDATE = update;
    }

    public static String getPhotoSortMode() {
        return PHOTO_SORT_MODE;
    }

    public static void setPhotoSortMode(String mode) {
        PHOTO_SORT_MODE = mode;
    }

    public static boolean isTextStyleBold() { return TEXT_STYLE_BOLD;}

    public static boolean isTextStyleItalic() {
        return TEXT_STYLE_ITALIC;
    }

    public static boolean isTextStyleStroke() {
        return TEXT_STYLE_STROKE;
    }

    public static boolean isTextStyleCode() {
        return TEXT_STYLE_CODE;
    }

    public static boolean isTextStyleUnderline() {
        return TEXT_STYLE_UNDERLINE;
    }

    public static boolean isTextStyleStrikethrough() {
        return TEXT_STYLE_STRIKETHROUGH;
    }

    public boolean[] getTextStyle() {
        return new boolean[]{TEXT_STYLE_BOLD, TEXT_STYLE_ITALIC, TEXT_STYLE_CODE, TEXT_STYLE_UNDERLINE,
        TEXT_STYLE_STRIKETHROUGH, TEXT_STYLE_STROKE};
    }

    public static void setTextStyleBold(boolean checked) {
        TEXT_STYLE_BOLD = checked;
    }

    public static void setTextStyleItalic(boolean checked) {
        TEXT_STYLE_ITALIC = checked;
    }

    public static void setTextStyleStroke(boolean checked) {
        TEXT_STYLE_STROKE = checked;
    }

    public static void setTextStyleCode(boolean checked) {
        TEXT_STYLE_CODE = checked;
    }

    public static void setTextStyleUnderline(boolean checked) {
        TEXT_STYLE_UNDERLINE = checked;
    }

    public static void setTextStyleStrikethrough(boolean checked) {
        TEXT_STYLE_STRIKETHROUGH = checked;
    }

    public Integer getSelectedColor() {
        return SELECTED_COLOR;
    }

    public void setSelectedColor(Integer selectedColor) {
        Note.SELECTED_COLOR = selectedColor;
    }

    public void setThemeText(String colorLight, String colorDark, boolean isLight) {
        if (isLight) {
            TEXT_COLOR_AC = colorLight;
            TEXT_COLOR_DI = colorDark;
        } else {
            TEXT_COLOR_AC = colorDark;
            TEXT_COLOR_DI = colorLight;
        }
    }

    public String getActiveColor() {
        return TEXT_COLOR_AC;
    }

    public String getTitle() {
        if (mTitle == null) {
            updateTitleAndPreview();
        }
        return mTitle;
    }

    public Spannable getContent() {
        Object obj = getProperty(CONTENT_PROPERTY);
        if (obj == null) {
            return BLANK_CONTENT;
        }
        String strContent = (String) obj;
        return (Spannable) HtmlCompat.fromHtml(strContent.replaceAll(TEXT_COLOR_DI, TEXT_COLOR_AC));
    }

    public void setContent(Spannable content) {
        mTitle = null;
        mContentPreview = null;
        String strContent = HtmlCompat.toHtml(content).replaceAll("<img .*?>","- [ ]");
        setProperty(CONTENT_PROPERTY, strContent);
    }

    public String getContentPreview() {
        if (mContentPreview == null) {
            updateTitleAndPreview();
        }
        return mContentPreview;
    }

    public Calendar getCreationDate() {
        return numberToDate((Number) getProperty(CREATION_DATE_PROPERTY));
    }

    public void setCreationDate(Calendar creationDate) {
        setProperty(CREATION_DATE_PROPERTY, creationDate.getTimeInMillis() / 1000);
    }

    public Calendar getModificationDate() {
        return numberToDate((Number) getProperty(MODIFICATION_DATE_PROPERTY));
    }

    public void setModificationDate(Calendar modificationDate) {
        setProperty(MODIFICATION_DATE_PROPERTY, modificationDate.getTimeInMillis() / 1000);
    }

    public String getPublishedUrl() {
        String urlCode = (String) getProperty(PUBLISH_URL_PROPERTY);
        if (TextUtils.isEmpty(urlCode)) {
            return "";
        }

        return PUBLISH_URL + urlCode;
    }

    public boolean hasTag(String tag) {
        List<String> tags = getTags();
        String tagLower = tag.toLowerCase();
        for (String tagName : tags) {
            if (tagLower.equals(tagName.toLowerCase())) return true;
        }
        return false;
    }

    public boolean hasTag(Tag tag) {
        return hasTag(tag.getSimperiumKey());
    }

    public List<String> getTags() {

        JSONArray tags = (JSONArray) getProperty(TAGS_PROPERTY);

        if (tags == null) {
            tags = new JSONArray();
            setProperty(TAGS_PROPERTY, "");
        }

        int length = tags.length();

        List<String> tagList = new ArrayList<>(length);

        if (length == 0) return tagList;

        for (int i = 0; i < length; i++) {
            String tag = tags.optString(i);
            if (!tag.equals(""))
                tagList.add(tag);
        }

        return tagList;
    }

    public void setTags(List<String> tags) {
        setProperty(TAGS_PROPERTY, new JSONArray(tags));
    }

    /**
     * String of tags delimited by a space
     */
    public CharSequence getTagString() {
        StringBuilder tagString = new StringBuilder();
        List<String> tags = getTags();
        for (String tag : tags) {
            if (tagString.length() > 0) {
                tagString.append(SPACE);
            }
            tagString.append(tag);
        }
        return tagString;
    }

    /**
     * Sets the note's tags by providing it with a {@link String} of space separated tags.
     * Filters out duplicate tags.
     *
     * @param tagString a space delimited list of tags
     */
    public void setTagString(String tagString) {
        List<String> tags = getTags();
        tags.clear();

        if (tagString == null) {
            setTags(tags);
            return;
        }

        // Make sure string has a trailing space
        if (tagString.length() > 1 && !tagString.substring(tagString.length() - 1).equals(SPACE))
            tagString = tagString + SPACE;
        // for comparing case-insensitive strings, would like to find a way to
        // do this without allocating a new list and strings
        List<String> tagsUpperCase = new ArrayList<>();
        // remove all current tags
        int start = 0;
        int next;
        String possible;
        String possibleUpperCase;
        // search tag string for space characters and pull out individual tags
        do {
            next = tagString.indexOf(SPACE, start);
            if (next > start) {
                possible = tagString.substring(start, next);
                possibleUpperCase = possible.toUpperCase();
                if (!possible.equals(SPACE) && !tagsUpperCase.contains(possibleUpperCase)) {
                    tagsUpperCase.add(possibleUpperCase);
                    tags.add(possible);
                }
            }
            start = next + 1;
        } while (next > -1);
        setTags(tags);
    }

    public JSONArray getSystemTags() {
        JSONArray tags = (JSONArray) getProperty(SYSTEM_TAGS_PROPERTY);
        if (tags == null) {
            tags = new JSONArray();
            setProperty(SYSTEM_TAGS_PROPERTY, tags);
        }
        return tags;
    }

    public Boolean isDeleted() {
        Object deleted = getProperty(DELETED_PROPERTY);
        if (deleted == null) {
            return false;
        }
        if (deleted instanceof Boolean) {
            return (Boolean) deleted;
        } else {
            // Simperium-iOS sets booleans as integer values (0 or 1)
            return deleted instanceof Number && ((Number) deleted).intValue() != 0;
        }
    }

    public void setDeleted(boolean deleted) {
        setProperty(DELETED_PROPERTY, deleted);
    }

    public boolean isMarkdownEnabled() {
        return hasSystemTag(MARKDOWN_TAG);
    }

    public void setMarkdownEnabled(boolean isMarkdownEnabled) {
        if (isMarkdownEnabled) {
            addSystemTag(MARKDOWN_TAG);
        } else {
            removeSystemTag(MARKDOWN_TAG);
        }
    }

    public boolean isPinned() {
        return hasSystemTag(PINNED_TAG);
    }

    public void setPinned(boolean isPinned) {
        if (isPinned) {
            addSystemTag(PINNED_TAG);
        } else {
            removeSystemTag(PINNED_TAG);
        }
    }

    public boolean isPreviewEnabled() {
        return hasSystemTag(PREVIEW_TAG);
    }

    public void setPreviewEnabled(boolean isPreviewEnabled) {
        if (isPreviewEnabled) {
            addSystemTag(PREVIEW_TAG);
        } else {
            removeSystemTag(PREVIEW_TAG);
        }
    }

    public boolean isPublished() {
        return hasSystemTag(PUBLISHED_TAG) && !TextUtils.isEmpty(getPublishedUrl());
    }

    public void setPublished(boolean isPublished) {
        if (isPublished) {
            addSystemTag(PUBLISHED_TAG);
        } else {
            removeSystemTag(PUBLISHED_TAG);
        }
    }

    private boolean hasSystemTag(String tag) {
        if (TextUtils.isEmpty(tag))
            return false;

        JSONArray tags = getSystemTags();
        int length = tags.length();
        for (int i = 0; i < length; i++) {
            if (tags.optString(i).equals(tag)) {
                return true;
            }
        }

        return false;
    }

    private void addSystemTag(String tag) {
        if (TextUtils.isEmpty(tag)) {
            return;
        }

        // Ensure we don't add the same tag again
        if (!hasSystemTag(tag)) {
            getSystemTags().put(tag);
        }
    }

    private void removeSystemTag(String tag) {
        if (!hasSystemTag(tag)) {
            return;
        }

        JSONArray tags = getSystemTags();
        JSONArray newTags = new JSONArray();
        int length = tags.length();
        try {
            for (int i = 0; i < length; i++) {
                Object val = tags.get(i);
                if (!val.equals(tag))
                    newTags.put(val);
            }
        } catch (JSONException e) {
            // could not update pinned setting
        }

        setProperty(SYSTEM_TAGS_PROPERTY, newTags);
    }

    /**
     * Check if the note has any changes
     *
     * @param content           the new note content
     * @param tagString         space separated tags
     * @param isPinned          note is pinned
     * @param isMarkdownEnabled note has markdown enabled
     * @param isPreviewEnabled  note has preview enabled
     * @return true if note has changes, false if it is unchanged.
     */
    public boolean hasChanges(Spannable content, String tagString, boolean isPinned, boolean isMarkdownEnabled, boolean isPreviewEnabled) {
        return !content.equals(this.getContent())
                || !tagString.equals(this.getTagString().toString())
                || this.isPinned() != isPinned
                || this.isMarkdownEnabled() != isMarkdownEnabled
                || this.isPreviewEnabled() != isPreviewEnabled;
    }

    public static class Schema extends BucketSchema<Note> {

        protected static NoteIndexer sNoteIndexer = new NoteIndexer();
        protected static NoteFullTextIndexer sFullTextIndexer = new NoteFullTextIndexer();

        public Schema() {
            autoIndex();
            addIndex(sNoteIndexer);
            setupFullTextIndex(sFullTextIndexer, NoteFullTextIndexer.INDEXES);
            setDefault(CONTENT_PROPERTY, "");
            setDefault(SYSTEM_TAGS_PROPERTY, new JSONArray());
            setDefault(TAGS_PROPERTY, new JSONArray());
            setDefault(DELETED_PROPERTY, false);
            setDefault(SHARE_URL_PROPERTY, "");
            setDefault(PUBLISH_URL_PROPERTY, "");
        }

        public String getRemoteName() {
            return Note.BUCKET_NAME;
        }

        public Note build(String key, JSONObject properties) {
            return new Note(key, properties);
        }

        public void update(Note note, JSONObject properties) {
            note.setProperties(properties);
            note.mTitle = null;
            note.mContentPreview = null;
        }
    }
}
