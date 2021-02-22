package com.theost.wavenote.utils;

import android.content.Context;

import com.theost.wavenote.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import static com.theost.wavenote.models.Note.NEW_LINE;
import static com.theost.wavenote.models.Note.SPACE;

public class ChordUtils {

    public static Map<ArrayList<String>, HashMap<Integer, String>> getChordsData(Context context, String content) {
        String[] resourceKeys = context.getResources().getStringArray(R.array.array_musical_keys);
        String[] resourceChords = context.getResources().getStringArray(R.array.array_musical_chords);
        content = content.replaceAll("\\u00a0", SPACE).replaceAll("[ ]{2,}", SPACE).replaceAll("\\n+", "\n");
        String contentInline = content.replaceAll("\\n+", SPACE);

        Map<Integer, String> chordsSortedMap = new TreeMap<>();
        Map<Integer, Integer> chordsIndexesMap = new TreeMap<>();
        ArrayList<Integer> wordsInsertIndexes = new ArrayList<>();

        // Parsing chords and indexes
        for (String key : resourceKeys) {
            if (contentInline.contains(SPACE + key)) {
                for (String chord : resourceChords) {
                    chord = key + chord;
                    String splitter = " " + chord + " ";
                    int index = 0;
                    while (index != -1) {
                        index = contentInline.indexOf(splitter, index);
                        if (index != -1) {
                            chordsSortedMap.put(index, chord);
                            chordsIndexesMap.put(index, index + splitter.length());
                            index++;
                        }
                    }
                }
            }
        }

        // Don't waste time if there's no chords
        if (chordsSortedMap.size() == 0) return new HashMap<>();

        // Checking indexes for repeat and getting strings insert indexes
        int count = 0;
        if (!(chordsIndexesMap.containsKey(0))) {
            wordsInsertIndexes.add(0, 0);
            count++;
        }

        for (int i = 0; i < chordsIndexesMap.size() - 1; i++) {
            ArrayList<Integer> startIndexes = new ArrayList<>(chordsIndexesMap.keySet());
            ArrayList<Integer> endIndexes = new ArrayList<>(chordsIndexesMap.values());
            int start = startIndexes.get(i);
            int end = endIndexes.get(i);
            int nextStart = startIndexes.get(i + 1);
            int nextEnd = endIndexes.get(i + 1);
            if (end >= nextStart) {
                chordsIndexesMap.remove(start);
                chordsIndexesMap.remove(nextStart);
                chordsIndexesMap.put(start, nextEnd);
                count++;
                i--;
            } else {
                int arraySize = getArraySize(wordsInsertIndexes);
                wordsInsertIndexes.add(arraySize + 1 + count);
                count = 1;
            }
        }

        if (!(chordsIndexesMap.containsValue(content.length()))) {
            int arraySize = getArraySize(wordsInsertIndexes);
            wordsInsertIndexes.add(arraySize + 1 + count);
        }

        // Generating result
        ArrayList<String> chordsList = new ArrayList<>(chordsSortedMap.values());
        HashMap<Integer, String> wordsMap = new HashMap<>();

        ArrayList<String> wordsList = new ArrayList<>();

        int index = 0;
        if (chordsIndexesMap.containsKey(0)) index = chordsIndexesMap.get(0);
        for (Integer j : chordsIndexesMap.keySet()) {
            if (index < j) {
                wordsList.add(content.substring(index, j));
                index = chordsIndexesMap.get(j);
            }
        }
        if (index < content.length()) wordsList.add(content.substring(index));

        int offset = 0;
        for (int k = 0; k < wordsList.size(); k++) {
            String word = wordsList.get(k);
            word = word.trim();
            if (word.equals("") || word.equals(NEW_LINE)) {
                offset++;
                continue;
            }
            wordsMap.put(wordsInsertIndexes.get(k) - offset, word);
        }

        Map<ArrayList<String>, HashMap<Integer, String>> chordsData = new HashMap<>();
        chordsData.put(chordsList, wordsMap);

        return chordsData;
    }

    private static int getArraySize(ArrayList<Integer> arrayList) {
        if (arrayList.size() > 0) {
            return arrayList.get(arrayList.size() - 1);
        } else {
            return 0;
        }
    }

    public static List<String> getChordsSet(List<String> chordsList) {
        return new ArrayList<>(new LinkedHashSet<>(chordsList));
    }

    public static ArrayList<String> getAllChords(Context context) {
        String[] resourceKeys = context.getResources().getStringArray(R.array.array_musical_keys);
        String[] resourceChords = context.getResources().getStringArray(R.array.array_musical_chords);
        ArrayList<String> chordsList = new ArrayList<>();
        for (String key : resourceKeys) {
            for (String chord : resourceChords) {
                chordsList.add(key + chord);
            }
        }
        return chordsList;
    }

    public static TreeMap<Integer, ArrayList<Integer>> convertArrayTreeMap(ArrayList<String> list) {
        TreeMap<Integer, ArrayList<Integer>> copy = new TreeMap<>();
        int element;
        for (int i = 0; i < list.size(); i++) {
            try {
                element = Integer.parseInt(list.get(i));
            } catch (Exception e) {
                element = -1;
            }
            ArrayList<Integer> values = new ArrayList<>();
            if (copy.containsKey(element)) values.addAll(Objects.requireNonNull(copy.get(element)));
            values.add(i);
            copy.put(element, values);
        }
        return copy;
    }

    public static ArrayList<Integer> convertArrayDigit(ArrayList<String> list) {
        ArrayList<Integer> copy = new ArrayList<>();
        for (String s : list) {
            try {
                copy.add(Integer.parseInt(s));
            } catch (Exception ignored) {
                // skip non-digit strings
            }
        }
        return copy;
    }

    public static int getMin(ArrayList<String> shape) {
        ArrayList<Integer> copy = convertArrayDigit(shape);
        for (Integer i : copy) if (i < 0) copy.remove(i);
        if (copy.size() == 0) return 0;
        return Collections.min(copy);
    }

    public static int getMax(ArrayList<String> shape) {
        ArrayList<Integer> copy = convertArrayDigit(shape);
        if (copy.size() == 0) return 0;
        return Collections.max(copy);
    }

    public static String removeSpace(String line) {
        return line.replaceAll("\\s+", "");
    }

}
