package com.theost.wavenote.utils;

import android.content.Context;

import com.theost.wavenote.R;

import java.util.Arrays;
import java.util.List;

public class ChordUtils {

    public static String replaceChord(Context context, String originChord) {
        String chord = originChord;
        if ((originChord.length() > 1) && ((originChord.substring(1, 2).equals("#")) || (originChord.substring(1, 2).equals("b")))) {
            List<String> chordsReplacement = Arrays.asList(context.getResources().getStringArray(R.array.array_musical_chords_replace));
            String key = originChord.substring(0, 2);
            String end = "";
            if (chordsReplacement.indexOf(key) % 2 == 0) {
                if (chord.length() > 2) end = chord.substring(2);
                chord = chordsReplacement.get(chordsReplacement.indexOf(key) + 1) + end;
            }
        }
        return chord;
    }

}
