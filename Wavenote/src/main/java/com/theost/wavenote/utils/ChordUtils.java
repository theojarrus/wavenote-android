package com.theost.wavenote.utils;

import android.content.Context;

import com.theost.wavenote.R;

import java.util.Arrays;
import java.util.List;

public class ChordUtils {

    public static List<String> replaceChords(Context context, List<String> chordsList, boolean isAllChords) {
        List<String> chordsReplacement = Arrays.asList(context.getResources().getStringArray(R.array.array_musical_chords_replace));
        for (int i = 0; i < chordsList.size(); i++) {
            String chord = chordsList.get(i);
            if ((chord.length() > 1) && ((chord.substring(1, 2).equals("#")) || (chord.substring(1, 2).equals("b")))) {
                String key = chord.substring(0, 2);
                String end = "";
                if (chordsReplacement.indexOf(key) % 2 == 0) {
                    if (isAllChords) {
                        chordsList.remove(i);
                        i -= 1;
                        continue;
                    }
                    if (chord.length() > 2) end = chord.substring(2);
                    chord = chordsReplacement.get(chordsReplacement.indexOf(key) + 1) + end;
                }
                chordsList.set(i, chord);
            }
        }
        return chordsList;
    }

}
