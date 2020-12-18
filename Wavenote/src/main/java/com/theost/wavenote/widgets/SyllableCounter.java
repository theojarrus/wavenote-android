package com.theost.wavenote.widgets;

import android.content.Context;

import com.theost.wavenote.R;
import com.theost.wavenote.utils.NetworkUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import static com.theost.wavenote.LookupBottomSheetDialog.API_REQ_SYLLABLE;
import static com.theost.wavenote.LookupBottomSheetDialog.API_URL;
import static com.theost.wavenote.models.Note.NEW_LINE;
import static com.theost.wavenote.models.Note.SPACE;

public class SyllableCounter {

    private static final String CYRILLIC_ALPHABET = "абвгдеёжзийклмнопрстуфхцчшщъыьэюя";
    private static final String CYRILLIC_SYLLABLE = "абеёиоуыэюя";

    private final String API_SYLLABLE_DIVIDER_START = "\"numSyllables\":";
    private final String API_SYLLABLE_DIVIDER_END = "}";

    private Map<String, Integer> exceptions;
    private Set<String> twoVowelSounds;
    private Set<String> vowels;
    private Set<String> chords;
    private ArrayList<String> tempLinesList;
    private ArrayList<String> tempIndexesList;
    private Context context;

    private boolean isWebEnabled;

    public SyllableCounter(Context context, boolean isWebEnabled) {
        this.context = context;
        this.isWebEnabled = isWebEnabled;
        this.exceptions = new HashMap<>();
        this.vowels = new HashSet<>(Arrays.asList("a", "e", "i", "o", "u", "y"));
        this.twoVowelSounds = new HashSet<>(Arrays.asList("ae", "ee", "oa", "oo", "ou", "oi", "ow", "aw", "au"));
        this.chords = new HashSet<>(Arrays.asList(context.getResources().getStringArray(R.array.array_musical_chords)));
        String[] exceptionsArray = context.getResources().getStringArray(R.array.array_syllable_exceptions);
        for (String i : exceptionsArray) {
            String[] parseArray = i.split(SPACE);
            int count = Integer.parseInt(parseArray[0]);
            String word = parseArray[1];
            this.exceptions.put(word, count);
        }
        tempLinesList = new ArrayList<>();
        tempIndexesList = new ArrayList<>();
    }

    public String getSyllableContent(String content) {
        StringBuilder syllableBuilder = new StringBuilder();
        content = content.replaceAll("\\p{Punct}", "").replaceAll("\\u00a0", SPACE).replaceAll("[ ]{2,}", SPACE);
        String[] linesArray = content.split(NEW_LINE);
        for (int i = 0; i < linesArray.length; i++) {
            String line = linesArray[i].trim();
            String syllableLine;
            if (i < tempLinesList.size() && tempLinesList.get(i).equals(line)) {
                syllableLine = tempIndexesList.get(i);
            } else {
                syllableLine = getSyllableLine(line);
                if (i < tempLinesList.size()) {
                    tempLinesList.remove(i);
                    tempIndexesList.remove(i);
                    tempLinesList.add(i, line);
                    tempIndexesList.add(i, syllableLine);
                } else {
                    tempLinesList.add(line);
                    tempIndexesList.add(syllableLine);
                }
            }
            syllableBuilder.append(syllableLine).append(NEW_LINE);
        }
        if (tempLinesList.size() > linesArray.length) {
            tempLinesList.subList(linesArray.length, tempLinesList.size()).clear();
            tempIndexesList.subList(linesArray.length, tempIndexesList.size()).clear();
        }
        return SPACE + syllableBuilder.toString();
    }

    public String getSyllableLine(String line) {
        String[] words = line.split(SPACE);
        int count = 0;
        for (String w : words) {
            if (!w.equals("") && !chords.contains(w)) {
                w = w.toLowerCase().trim();
                if (CYRILLIC_ALPHABET.indexOf(w.toCharArray()[0]) == -1) {
                    count += countSyllableLatin(w);
                } else {
                    count += countSyllableCyrillic(w);
                }
            }
        }
        if (count != 0) {
            return String.valueOf(count);
        } else {
            return "";
        }
    }

    public int countSyllableCyrillic(final String word) {
        int count = 0;
        for (Character i : word.toLowerCase().toCharArray()) {
            if (CYRILLIC_SYLLABLE.indexOf(i) != -1) count++;
        }
        return count;
    }

    public int countSyllableLatin(String word) {
        boolean isVowels = false;
        for (Character c : word.toCharArray()) {
            if (vowels.contains(c.toString())) {
                isVowels = true;
            }
        }
        int count;
        if (isVowels) {
            if (isWebEnabled && NetworkUtils.isNetworkAvailable(context)) {
                count = countSyllableLatinOnline(word);
            } else {
                count = countSyllableLatinOffline(word);
            }
        } else {
            count = 0;
        }
        return count;
    }

    public int countSyllableLatinOnline(String word) {
        String response = getApiSyllable(word);
        int count;
        if (!response.equals("")) {
            count = Integer.parseInt(response);
        } else {
            count = countSyllableLatinOffline(word);
        }
        return count;
    }

    private String getApiSyllable(String word) {
        String requestUrl = API_URL + String.format(API_REQ_SYLLABLE, word);
        String responseResult = "";
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(requestUrl).build();
        try (Response response = client.newCall(request).execute()) {
            ResponseBody responseBody = response.body();
            if (responseBody != null) {
                String responseStr = responseBody.string();
                int start = responseStr.indexOf(API_SYLLABLE_DIVIDER_START);
                if (start != -1) {
                    responseStr = responseStr.substring(start + API_SYLLABLE_DIVIDER_START.length());
                    responseResult = responseStr.substring(0, responseStr.indexOf(API_SYLLABLE_DIVIDER_END));
                }
            }
        } catch (IOException e) {
            // e.printStackTrace();
        }
        return responseResult;
    }

    public int countSyllableLatinOffline(String word) {
        if (word.length() == 1) {
            if (vowels.contains(String.valueOf(word.toCharArray()[0]))) {
                return 1;
            } else {
                return 0;
            }
        }

        final String lowerCase = word.toLowerCase();
        if (exceptions != null && exceptions.containsKey(lowerCase)) {
            return exceptions.get(lowerCase);
        }

        int count = countLatinVowels(lowerCase);
        int offset = getLatinRulesOffset(lowerCase);
        if (count == offset) {
            if (count > 0) {
                count = 1;
            } else {
                count = 0;
            }
        } else {
            count -= offset;
        }

        return count;
    }

    public int getLatinRulesOffset(String word) {
        ArrayList<String> letterList = new ArrayList<>();
        char[] letters = word.toCharArray();
        for (Character c : letters) letterList.add(c.toString());
        int offset = 0;

        // First rule
        int index = letterList.indexOf("q");
        if ((index != -1) && ((index + 1 < letterList.size()) && (letterList.get(index + 1).equals("u"))))
            offset++;

        // Second rule
        index = letterList.indexOf("y");
        if (index != -1) {
            if (index == 0) {
                for (int i = 1; i < 3; i++) {
                    if ((i < letterList.size()) && (vowels.contains(letterList.get(i)))) offset++;
                }
            } else if (index == letterList.size() - 1) {
                if (vowels.contains(letterList.get(index - 1))) offset++;
            } else {
                if ((vowels.contains(letterList.get(index - 1))) || vowels.contains(letterList.get(index + 1)))
                    offset++;
            }
        }

        //Third rule
        index = letterList.lastIndexOf("e");
        if (index > 1) {
            if ((index == letterList.size() - 1) && (vowels.contains(letterList.get(index - 2)))) {
                offset++;
            }
        }

        // Fourth rule
        index = word.lastIndexOf("es");
        if (index != -1) {
            if (index == word.length() - 2) {
                for (int i = index - 1; i > index - 4; i--) {
                    if (vowels.contains(word.substring(i, i + 1))) offset++;
                }
            }
        }

        // Fifth rule
        for (String s : twoVowelSounds) if (word.contains(s)) offset++;

        return offset;
    }

    public int countLatinVowels(String word) {
        int count = 0;
        for (Character i : word.toCharArray())
            if (vowels.contains(i.toString())) count++;
        return count;
    }

}
