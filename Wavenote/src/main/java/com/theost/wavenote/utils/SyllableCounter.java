package com.theost.wavenote.utils;

import android.content.Context;

import com.theost.wavenote.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.theost.wavenote.models.Note.NEW_LINE;
import static com.theost.wavenote.models.Note.SPACE;

public class SyllableCounter {

    private static final String CYRILLIC_ALPHABET = "абвгдеёжзийклмнопрстуфхцчшщъыьэюя";
    private static final String CYRILLIC_SYLLABLE = "абеёиоуыэюя";

    private static Map<String, Integer> exceptions;
    private static Set<String> twoVowelSounds;
    private static Set<String> vowels;
    private static Set<String> chords;

    public static void updateSyllableResources(Context context) {
        exceptions = new HashMap<>();
        vowels = new HashSet<>(Arrays.asList("a", "e", "i", "o", "u", "y"));
        twoVowelSounds = new HashSet<>(Arrays.asList("ae", "ee", "oa", "oo", "ou", "oi", "ow", "aw", "au"));
        chords = new HashSet<>(Arrays.asList(context.getResources().getStringArray(R.array.array_musical_chords)));
        String[] exceptionsArray = context.getResources().getStringArray(R.array.syllable_exceptions);
        for (String i : exceptionsArray) {
            String[] parseArray = i.split(SPACE);
            int count = Integer.parseInt(parseArray[0]);
            String word = parseArray[1];
            exceptions.put(word, count);
        }
    }

    public static String getSyllableContent(String content) {
        StringBuilder syllableBuilder = new StringBuilder();
        content = content.replaceAll("\\p{Punct}", "").replaceAll("\\u00a0", SPACE).replaceAll("[ ]{2,}", SPACE);
        String[] linesArray = content.split(NEW_LINE);
        for (String s : linesArray) syllableBuilder.append(getSyllableLine(s)).append(NEW_LINE);
        return SPACE + syllableBuilder.toString();
    }

    public static String getSyllableLine(String line) {
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

    public static int countSyllableCyrillic(final String word) {
        int count = 0;
        for (Character i : word.toLowerCase().toCharArray()) {
            if (CYRILLIC_SYLLABLE.indexOf(i) != -1) count++;
        }
        return count;
    }

    public static int countSyllableLatin(final String word) {
        if (word.length() == 0) {
            return 0;
        } else if (word.length() == 1) {
            if (vowels.contains(String.valueOf(word.toCharArray()[0]))) {
                return 1;
            } else {
                return 0;
            }
        }

        final String lowerCase = word.toLowerCase();
        if (exceptions.containsKey(lowerCase)) {
            return exceptions.get(lowerCase);
        }

        int count = countLatinVowels(lowerCase);
        int offset = getLatinRulesOffset(lowerCase, count);
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

    public static int getLatinRulesOffset(String word, int count) {
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

    public static int countLatinVowels(String word) {
        int count = 0;
        for (Character i : word.toCharArray())
            if (vowels.contains(i.toString())) count++;
        return count;
    }

}
