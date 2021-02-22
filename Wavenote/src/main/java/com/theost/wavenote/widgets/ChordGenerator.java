package com.theost.wavenote.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.theost.wavenote.R;
import com.theost.wavenote.utils.ChordUtils;
import com.theost.wavenote.utils.ResUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TreeMap;

public class ChordGenerator {

    private static final String FRET_OVERLAY = "%s_fret";
    private static final String BARRE = "string_barre_%d";
    private static final String STRING_EXTRA = "string_%s";
    private static final String FRET_NUMBER = "string_number_%s";
    private static final String STRING_BUBBLE = "string_bubble_%d";
    private static final String PIANO_BUBBLE = "piano_bubble_%s";

    private static final String SCALE_KEY_BEMOL = "b";
    private static final String SCALE_KEY_DIEZ = "#";

    private static final String REQUEST_DIVIDER = " ";
    private static final String REQUEST_KEY = "-b";
    private static final String REQUEST_FRET = "-f";
    private static final String REQUEST_STRING = "-s";
    private static final String STRING_CLOSED = "x";
    private static final String STRING_OPENED = "o";

    private static final String COORDINATE_X = "x";
    private static final String COORDINATE_Y = "y";
    private static final String KEY_LIGHT = "light";
    private static final String KEY_DARK = "dark";

    private static final String RESOURCE_ROOT_NAME = "root-fret";
    private static final String RESOURCE_MAJOR_NAME = "M";

    private static final String RESOURCE_SHAPE_DIVIDER = "\\|";
    private static final String RESOURCE_STRING_DIVIDER = "-";

    private static final String STRING_ORDER = "E00F01G03A05B07C08D10";
    private static final String PIANO_ORDER = "C01D03E05F06G08A10B12";

    private static final String DARK_PIANO_KEYS = "-2-4-7-9-11-14-16-19-21-23-";
    private static final String DARK_PIANO_KEYS_DIVIDER = "-";

    private static final int RESOURCE_INSTRUMENT_COUNT = 3;

    private static final int NOTES_REPEAT_INDEX = 12;
    private static final int PIANO_LAST_KEY = 24;

    private static final int STRING_FIRST_LINE_X = 26;
    private static final int STRING_SPACE_LINE_X = 34;
    private static final int STRING_FIRST_FRET_Y = 52;
    private static final int STRING_SPACE_FRET_Y = 48;

    private static final int STRING_EXTRA_Y = 0;
    private static final int FRET_NUMBER_X = 0;
    private static final int NECK_OVERLAY_X = 0;
    private static final int NECK_OVERLAY_Y = 0;

    private static final int PIANO_FIRST_LIGHT_KEY_X = 25;
    private static final int PIANO_FIRST_DARK_KEY_X = 38;
    private static final int PIANO_SPACE_KEY_X = 25;

    private static final int PIANO_LIGHT_KEY_Y = 211;
    private static final int PIANO_DARK_KEY_Y = 138;

    private final Context context;
    private ViewGroup viewGroup;
    private int maxPositionsCount;
    private ArrayList<LayeredImageView> imageViews;
    private boolean isGenerating;
    private generateChordsThread currentThread;

    public ChordGenerator(Context context, ViewGroup viewGroup) {
        this.context = context;
        this.viewGroup = viewGroup;
        this.maxPositionsCount = 100;
    }

    public void setView(ViewGroup viewGroup) {
        this.viewGroup = viewGroup;
    }

    public void setMaxPositionsCount(int count) {
        maxPositionsCount = count;
    }

    public void generateChord(String chord, int instrument) {
        if (isGenerating) {
            currentThread.interrupt();
            isGenerating = false;
        }
        currentThread = new generateChordsThread(chord, instrument);
        currentThread.start();
    }

    private class generateChordsThread extends Thread {
        private final String chord;
        private final int instrument;

        public generateChordsThread(String chord, int instrument) {
            this.chord = chord;
            this.instrument = instrument;
        }

        @Override
        public void run() {
            super.run();
            isGenerating = true;
            imageViews = new ArrayList<>();
            String[] chordData = getChordData(chord); // [note, key, type]
            if (instrument == R.string.guitar || instrument == R.string.ukulele) {
                generateStringChord(chordData, instrument);
            } else if (instrument == R.string.piano) {
                generatePianoChord(chordData, instrument);
            }
            if (!isInterrupted()) {
                Message message = new Message();
                message.obj = imageViews;
                mChordHandler.sendMessage(message);
            }
            isGenerating = false;
        }

    }

    private final Handler mChordHandler = new Handler(Looper.getMainLooper(), msg -> {
        ArrayList<LayeredImageView> imageViews = new ArrayList<>((ArrayList<LayeredImageView>) msg.obj);
        for (LayeredImageView imageView : imageViews) viewGroup.addView(imageView);
        return true;
    });

    private void generateChordBitmap(LinkedHashMap<String, LinkedHashMap<Integer, Integer>> overlayData, int instrument) {
        LayeredImageView imageView = createImageView(instrument);
        for (String resource : overlayData.keySet()) {
            LinkedHashMap<Integer, Integer> coordinates = overlayData.get(resource);
            Iterator<Integer> iteratorX = Objects.requireNonNull(coordinates).keySet().iterator();
            Iterator<Integer> iteratorY = coordinates.values().iterator();
            Bitmap resourceBitmap = ResUtils.getBitmap(context, ResUtils.getResId(resource, R.mipmap.class));
            while (iteratorX.hasNext() && iteratorY.hasNext()) {
                Matrix matrix = new Matrix();
                matrix.setScale(3.5f, 3.5f);
                matrix.preTranslate(iteratorX.next(), iteratorY.next());
                imageView.addLayer(resourceBitmap, matrix);
            }
        }
        imageViews.add(imageView);
    }

    @SuppressLint("NonConstantResourceId")
    private LayeredImageView createImageView(int instrument) {
        LayeredImageView imageView = new LayeredImageView(context);
        int templateId = 0;
        switch (instrument) {
            case R.string.guitar:
                templateId = R.mipmap.guitar_base;
                break;
            case R.string.ukulele:
                templateId = R.mipmap.ukulele_base;
                break;
            case R.string.piano:
                templateId = R.mipmap.piano_base;
                break;
        }

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(context.getResources().getInteger(R.integer.chords_margin), 0, 0, 0);
        imageView.setLayoutParams(layoutParams);
        imageView.setScaleType(ImageView.ScaleType.FIT_XY);
        imageView.setAdjustViewBounds(true);
        imageView.setImageResource(templateId);
        return imageView;
    }

    private void generatePianoChord(String[] chordData, int instrument) {
        String request = generatePianoChordRequest(chordData);
        generatePianoChord(request, instrument);
    }

    private void generatePianoChord(String request, int instrument) {
        LinkedHashMap<String, LinkedHashMap<Integer, Integer>> overlayData = new LinkedHashMap<>();
        LinkedHashMap<Integer, Integer> coordinates;
        List<String> requestData = new ArrayList<>(Arrays.asList(request.replaceAll("\\s+", REQUEST_DIVIDER).split(REQUEST_DIVIDER)));
        requestData.remove(REQUEST_KEY);
        // String chord = requestData.get(0);
        requestData.remove(0);

        for (String b : requestData) {
            String keyType;
            int y;
            if (DARK_PIANO_KEYS.contains(DARK_PIANO_KEYS_DIVIDER + b + DARK_PIANO_KEYS_DIVIDER)) {
                keyType = KEY_DARK;
                y = PIANO_DARK_KEY_Y;
            } else {
                keyType = KEY_LIGHT;
                y = PIANO_LIGHT_KEY_Y;
            }
            int x = getOverlayPosition(instrument, Integer.parseInt(b), keyType);
            String resource = String.format(PIANO_BUBBLE, keyType);
            if (overlayData.containsKey(resource)) {
                coordinates = overlayData.get(resource);
            } else {
                coordinates = new LinkedHashMap<>();
            }
            Objects.requireNonNull(coordinates).put(x, y);
            overlayData.put(resource, coordinates);
        }

        generateChordBitmap(overlayData, instrument);
    }

    private String generatePianoChordRequest(String[] chordData) {
        ArrayList<String> shape = getChordShapes(chordData, R.string.piano).get(0);
        StringBuilder request = new StringBuilder(chordData[0] + chordData[1] + chordData[2].replace(RESOURCE_MAJOR_NAME, "") + REQUEST_DIVIDER + REQUEST_KEY);
        for (String s : shape) request.append(REQUEST_DIVIDER).append(Integer.parseInt(s));
        return request.toString();
    }

    @SuppressLint("DefaultLocale")
    private void generateStringChord(String request, int instrument) {
        LinkedHashMap<String, LinkedHashMap<Integer, Integer>> overlayData = new LinkedHashMap<>();
        LinkedHashMap<Integer, Integer> coordinates;
        List<String> requestData = new ArrayList<>(Arrays.asList(request.replaceAll("\\s+", REQUEST_DIVIDER).split(REQUEST_DIVIDER)));
        // String chord = requestData.get(0);
        requestData.remove(0);

        int fretIndex = 0;
        while (fretIndex != -1) {
            fretIndex = requestData.indexOf(REQUEST_FRET);
            if (fretIndex != -1) {
                String number = requestData.get(fretIndex + 1);
                String resource = String.format(FRET_NUMBER, number);
                int y = getOverlayPosition(instrument, Integer.parseInt(requestData.get(fretIndex + 2)), COORDINATE_Y);
                if (overlayData.containsKey(resource)) {
                    coordinates = overlayData.get(resource);
                } else {
                    coordinates = new LinkedHashMap<>();
                }
                Objects.requireNonNull(coordinates).put(FRET_NUMBER_X, y);
                overlayData.put(resource, coordinates);
                if (number.equals("1")) {
                    coordinates = new LinkedHashMap<>();
                    coordinates.put(NECK_OVERLAY_X, NECK_OVERLAY_Y);
                    overlayData.put(String.format(FRET_OVERLAY, ResUtils.getStringLocale(context, Locale.US, instrument).toLowerCase()), coordinates);
                }
                for (int i = 0; i < 3; i++) requestData.remove(fretIndex);
            }
        }

        for (int finger = 0; finger < 5; finger++) {
            String requestShape = REQUEST_STRING + REQUEST_DIVIDER + finger;
            int first = request.indexOf(requestShape);
            int last = request.lastIndexOf(requestShape);
            if (last > first) {

                int barreFretIndex = first + requestShape.length() + REQUEST_DIVIDER.length();
                int barreStartIndex = first + requestShape.length() + REQUEST_DIVIDER.length() * 2 + 1;
                int barreEndIndex = last + requestShape.length() + REQUEST_DIVIDER.length() * 2 + 1;

                int fret = Integer.parseInt(request.substring(barreFretIndex, barreFretIndex + 1));
                int start = Integer.parseInt(request.substring(barreStartIndex, barreStartIndex + 1));
                int end = Integer.parseInt(request.substring(barreEndIndex, barreEndIndex + 1));

                @SuppressLint("DefaultLocale") String resource = String.format(BARRE, end - start + 1);
                int x = getOverlayPosition(instrument, start, COORDINATE_X);
                int y = getOverlayPosition(instrument, fret, COORDINATE_Y);
                if (overlayData.containsKey(resource)) {
                    coordinates = overlayData.get(resource);
                } else {
                    coordinates = new LinkedHashMap<>();
                }
                Objects.requireNonNull(coordinates).put(x, y);
                overlayData.put(resource, coordinates);
            }
        }

        int requestIndex = 0;
        while (requestIndex != -1) {
            requestIndex = requestData.indexOf(REQUEST_STRING);
            if (requestIndex != -1) {
                String resource;
                int x;
                int y;
                if (requestData.get(requestIndex + 1).equals(STRING_CLOSED) || requestData.get(requestIndex + 1).equals(STRING_OPENED)) {
                    resource = String.format(STRING_EXTRA, requestData.get(requestIndex + 1));
                    x = getOverlayPosition(instrument, Integer.parseInt(requestData.get(requestIndex + 2)), COORDINATE_X);
                    y = STRING_EXTRA_Y;
                } else {
                    x = getOverlayPosition(instrument, Integer.parseInt(requestData.get(requestIndex + 3)), COORDINATE_X);
                    y = getOverlayPosition(instrument, Integer.parseInt(requestData.get(requestIndex + 2)), COORDINATE_Y);
                    resource = String.format(STRING_BUBBLE, Integer.parseInt(requestData.get(requestIndex + 1)));
                }
                if (overlayData.containsKey(resource)) {
                    coordinates = overlayData.get(resource);
                } else {
                    coordinates = new LinkedHashMap<>();
                }
                Objects.requireNonNull(coordinates).put(x, y);
                overlayData.put(resource, coordinates);
                requestData.remove(requestIndex);
            }
        }

        generateChordBitmap(overlayData, instrument);
    }

    private void generateStringChord(String[] chordData, int instrument) {
        HashMap<Integer, ArrayList<String>> chordRequest = generateStringChordRequests(chordData, instrument);
        ArrayList<Integer> chordFrets = new ArrayList<>(chordRequest.keySet());
        Collections.sort(chordFrets);
        for (Integer fret : chordFrets) {
            for (String request : Objects.requireNonNull(chordRequest.get(fret))) {
                generateStringChord(request, instrument);
            }
        }
    }

    private HashMap<Integer, ArrayList<String>> generateStringChordRequests(String[] chordData, int instrument) {
        HashMap<Integer, ArrayList<String>> chordRequests = new HashMap<>();
        ArrayList<ArrayList<String>> shapes = getChordShapes(chordData, instrument);
        for (ArrayList<String> shape : shapes) {
            int fret = Integer.parseInt(shape.get(0));
            if (fret == 0) fret = 1;
            shape.remove(0);
            ArrayList<Integer> fingerings = getChordFingerings(shape);
            StringBuilder chordRequest = new StringBuilder(chordData[0] + chordData[1] + chordData[2].replace(RESOURCE_MAJOR_NAME, "") + REQUEST_DIVIDER + REQUEST_FRET + REQUEST_DIVIDER + fret + REQUEST_DIVIDER + 1);
            for (int string = 0; string < shape.size(); string++) {
                chordRequest.append(REQUEST_DIVIDER + REQUEST_STRING + REQUEST_DIVIDER);
                String position = shape.get(string);
                if (position.equals("0")) {
                    chordRequest.append(STRING_OPENED).append(REQUEST_DIVIDER).append(string + 1);
                } else if (position.equals("x")) {
                    chordRequest.append(STRING_CLOSED).append(REQUEST_DIVIDER).append(string + 1);
                } else {
                    chordRequest.append(fingerings.get(string)).append(REQUEST_DIVIDER).append(shape.get(string)).append(REQUEST_DIVIDER).append(string + 1);
                }
            }
            ArrayList<String> fretRequests = new ArrayList<>();
            if (chordRequests.containsKey(fret)) {
                fretRequests = chordRequests.get(fret);
            }
            Objects.requireNonNull(fretRequests).add(chordRequest.toString());
            chordRequests.put(fret, fretRequests);
        }
        return chordRequests;
    }

    private ArrayList<ArrayList<String>> getChordShapes(String[] chordData, int instrument) {
        List<String> shapes = Arrays.asList(context.getResources().getStringArray(R.array.array_chord_shapes));
        int start = shapes.indexOf(chordData[2]);
        String shapesLine = "";
        String rootLine = "";
        for (int i = 1; i <= RESOURCE_INSTRUMENT_COUNT + 1; i++) {
            String line = shapes.get(start + i);
            if (line.contains(ResUtils.getStringLocale(context, Locale.US, instrument).toLowerCase())) {
                shapesLine = ChordUtils.removeSpace(line);
            } else if (line.contains(RESOURCE_ROOT_NAME)) {
                rootLine = ChordUtils.removeSpace(line);
            }
            if (!shapesLine.equals("") && !rootLine.equals("")) {
                return generateChordShapes(chordData, instrument, shapesLine, rootLine);
            }
        }
        return new ArrayList<>();
    }

    private ArrayList<ArrayList<String>> generateChordShapes(String[] chordData, int instrument, String shapesLine, String rootLine) {
        ArrayList<ArrayList<String>> chordShapes = new ArrayList<>();
        int noteKeyOffset = 0;
        if (!chordData[1].equals("")) {
            if (chordData[1].equals(SCALE_KEY_DIEZ)) {
                noteKeyOffset += 1;
            } else if (chordData[1].equals(SCALE_KEY_BEMOL)) {
                noteKeyOffset -= 1;
            }
        }
        String[] shapesArray = shapesLine.split(RESOURCE_SHAPE_DIVIDER);
        for (int j = 1; j < shapesArray.length; j++) {
            if (j == maxPositionsCount + 1) break;
            ArrayList<String> shape = new ArrayList<>(Arrays.asList(shapesArray[j].split(RESOURCE_STRING_DIVIDER)));
            int noteOffset = noteKeyOffset;
            if (instrument == R.string.guitar || instrument == R.string.ukulele) {
                int noteIndex = STRING_ORDER.indexOf(chordData[0]);
                int noteRoot = Integer.parseInt(rootLine.split(RESOURCE_SHAPE_DIVIDER)[1]);
                noteOffset += Integer.parseInt(STRING_ORDER.substring(noteIndex + 1, noteIndex + 3)) - noteRoot;
                int minFret = ChordUtils.getMin(shape) + noteOffset;
                if (minFret < 0) {
                    minFret += NOTES_REPEAT_INDEX;
                    noteOffset += NOTES_REPEAT_INDEX;
                } else if (minFret >= NOTES_REPEAT_INDEX) {
                    minFret -= NOTES_REPEAT_INDEX;
                    noteOffset -= NOTES_REPEAT_INDEX;
                }
                int openFret = (minFret == 0) ? 0 : 1;
                noteOffset += openFret - minFret;
                shape.add(0, String.valueOf(minFret));
            } else if (instrument == R.string.piano) {
                int noteIndex = PIANO_ORDER.indexOf(chordData[0]);
                noteOffset += Integer.parseInt(PIANO_ORDER.substring(noteIndex + 1, noteIndex + 3)) - 1;
                if (ChordUtils.getMax(shape) + noteOffset > PIANO_LAST_KEY) {
                    noteOffset -= NOTES_REPEAT_INDEX;
                }
            }
            int begin = (instrument == R.string.guitar || instrument == R.string.ukulele) ? 1 : 0;
            for (int s = begin; s < shape.size(); s++) {
                if (!shape.get(s).equals(STRING_CLOSED)) {
                    shape.set(s, String.valueOf(Integer.parseInt(shape.get(s)) + noteOffset));
                }
            }
            chordShapes.add(shape);
        }
        return chordShapes;
    }

    private ArrayList<Integer> getChordFingerings(ArrayList<String> shape) {
        TreeMap<Integer, Integer> fingerings = new TreeMap<>(); // [index, value]
        TreeMap<Integer, ArrayList<Integer>> shapeTreeMap = ChordUtils.convertArrayTreeMap(shape);
        ArrayList<Integer> shapeFrets = new ArrayList<>(shapeTreeMap.keySet());
        Collections.sort(shapeFrets);
        int currentFinger = 0;
        boolean isFirstFretBarre = isFirstFretBarre(shapeTreeMap);
        ArrayList<Integer> barreStrings = getBarreData(shapeTreeMap, isFirstFretBarre);
        int firstChordFret = getFirstChordFret(shapeTreeMap);
        int[] fingerOffsetData = getFingerOffset(shapeTreeMap, barreStrings, firstChordFret, isFirstFretBarre);
        for (Integer fret : shapeFrets) {
            ArrayList<Integer> indexes = shapeTreeMap.get(fret);
            int startFinger = (fret <= 0 || (fret == 1 && isFirstFretBarre)) ? fret : 2;
            if (fret == 1 && startFinger <= 1) currentFinger += 1;
            if (fingerOffsetData[0] <= 5 && fret >= fingerOffsetData[0]) {
                currentFinger += fingerOffsetData[1];
                fingerOffsetData[0] = 6;
            }
            for (int string : Objects.requireNonNull(indexes)) {
                if (startFinger <= 1) {
                    fingerings.put(string, startFinger);
                } else {
                    if (!barreStrings.contains(string)) currentFinger += 1;
                    fingerings.put(string, currentFinger);
                }
            }
        }
        return new ArrayList<>(fingerings.values());
    }

    private ArrayList<Integer> getBarreData(TreeMap<Integer, ArrayList<Integer>> shapeHashMap, boolean isFirstFretBarre) {
        TreeMap<Integer, ArrayList<Integer>> shapeCopy = new TreeMap<>(shapeHashMap);
        ArrayList<Integer> barreStrings = new ArrayList<>();

        int lastFinger = 0;

        ArrayList<Integer> fifthFretStrings = shapeCopy.get(5);
        if (fifthFretStrings != null && fifthFretStrings.size() > 1) {
            barreStrings.addAll(getBarreStrings(fifthFretStrings));
            shapeCopy.remove(5);
            lastFinger += 1;
        }

        ArrayList<Integer> fourthFretStrings = shapeCopy.get(4);
        if (fourthFretStrings != null && fourthFretStrings.size() > 2) {
            barreStrings.addAll(getBarreStrings(fourthFretStrings));
            shapeCopy.remove(4);
            lastFinger += 1;
        }

        int maxCount = 0;
        int barreFret = 0;
        for (Integer fret : shapeCopy.keySet()) {
            if (fret <= 0) continue;
            if (fret == 1 && isFirstFretBarre) {
                lastFinger += 1;
            } else {
                int count = Objects.requireNonNull(shapeCopy.get(fret)).size();
                lastFinger += count;
                if (count >= maxCount) {
                    maxCount = count;
                    barreFret = fret;
                }
            }
        }

        if (lastFinger > 4) {
            ArrayList<Integer> barreFretStrings = shapeCopy.get(barreFret);
            barreStrings.addAll(getBarreStrings(Objects.requireNonNull(barreFretStrings)));
        }

        return barreStrings;
    }

    private ArrayList<Integer> getBarreStrings(ArrayList<Integer> fretStrings) {
        ArrayList<Integer> barreStrings = new ArrayList<>();
        for (int i = 1; i < fretStrings.size(); i++) {
            int currentFretString = fretStrings.get(i);
            int previousFretString = fretStrings.get(i - 1);
            if (currentFretString - 1 == previousFretString) {
                barreStrings.add(currentFretString);
            }
        }
        return barreStrings;
    }

    private boolean isFirstFretBarre(TreeMap<Integer, ArrayList<Integer>> shapeTreeMap) {
        ArrayList<Integer> shapeFrets = new ArrayList<>(shapeTreeMap.keySet());
        ArrayList<Integer> nullFretStrings = shapeTreeMap.get(0);
        ArrayList<Integer> firstFretStrings = shapeTreeMap.get(1);
        while (shapeFrets.size() > 0 && shapeFrets.get(0) <= 1) shapeFrets.remove(0);
        if (nullFretStrings != null && firstFretStrings != null) {
            return !(firstFretStrings.get(0) < nullFretStrings.get(nullFretStrings.size() - 1)
                    && firstFretStrings.get(firstFretStrings.size() - 1) > nullFretStrings.get(0));
        } else {
            return shapeFrets.size() > 0;
        }
    }

    private int[] getFingerOffset(TreeMap<Integer, ArrayList<Integer>> shapeTreeMap, ArrayList<Integer> barreStrings, int firstFret, boolean isFirstFretBarre) {
        if (firstFret > 0 && isNeedFingerOffset(shapeTreeMap, firstFret)) {
            ArrayList<Integer> stringsCounts = new ArrayList<>();
            int stringsSum = 0;
            for (int fret = firstFret; fret <= 5; fret++) {
                int currentCount = 0;
                if (shapeTreeMap.get(fret) != null) {
                    currentCount = Objects.requireNonNull(shapeTreeMap.get(fret)).size();
                    stringsSum += currentCount;
                }
                stringsCounts.add(currentCount);
            }
            if (isFirstFretBarre && shapeTreeMap.containsKey(1)) {
                stringsSum -= Objects.requireNonNull(shapeTreeMap.get(1)).size() - 1;
            }
            for (int i = 0; i < barreStrings.size(); i++) stringsSum -= 1;
            if (stringsSum < 4) {
                int spaceIndex = stringsCounts.indexOf(0);
                if (spaceIndex != -1) {
                    int fingerOffset = 1;
                    if (stringsSum < 3 && shapeTreeMap.get(3) == null) fingerOffset += 1;
                    return new int[]{spaceIndex + firstFret + 1, fingerOffset};
                }
            }
        }
        return new int[]{6, 0};
    }

    private int getFirstChordFret(TreeMap<Integer, ArrayList<Integer>> shapeTreeMap) {
        int first = 1;
        while (shapeTreeMap.get(first) == null && first < shapeTreeMap.keySet().size() - 1)
            first += 1;
        return first;
    }

    private boolean isNeedFingerOffset(TreeMap<Integer, ArrayList<Integer>> shapeTreeMap, int firstFret) {
        ArrayList<Integer> thirdFretCounts = shapeTreeMap.get(firstFret + 2);
        ArrayList<Integer> fourthFretCounts = shapeTreeMap.get(firstFret + 3);
        ArrayList<Integer> fifthFretCounts = shapeTreeMap.get(firstFret + 4);
        int totalCount = 0;
        if (fourthFretCounts != null) totalCount += fourthFretCounts.size();
        if (fifthFretCounts != null) totalCount += fifthFretCounts.size();
        return totalCount > 1 || (thirdFretCounts != null && thirdFretCounts.size() > 1);
    }

    private String[] getChordData(String chord) {
        int index = 1;
        if (chord.length() > 1) {
            String key = chord.substring(1, 2);
            if (key.equals(SCALE_KEY_DIEZ) || key.equals(SCALE_KEY_BEMOL)) {
                index += 1;
            }
        }
        String type = chord.substring(index);
        if (type.equals("")) type = RESOURCE_MAJOR_NAME;
        return new String[]{chord.substring(0, 1), chord.substring(1, index), type};
    }

    private int getOverlayPosition(int first, int space, int count) {
        return first + space * (count - 1);
    }

    private int getOverlayPosition(int instrument, int count, String extra) {
        int first = 0;
        int space = 0;
        if (instrument == R.string.guitar || instrument == R.string.ukulele) {
            if (extra.equals(COORDINATE_X)) {
                first = STRING_FIRST_LINE_X;
                space = STRING_SPACE_LINE_X;
            } else if (extra.equals(COORDINATE_Y)) {
                first = STRING_FIRST_FRET_Y;
                space = STRING_SPACE_FRET_Y;
            }
        } else if (instrument == R.string.piano) {
            space = PIANO_SPACE_KEY_X;
            if (extra.equals(KEY_LIGHT)) {
                first = PIANO_FIRST_LIGHT_KEY_X;
                count = getPianoLightCountOffset(count);
            } else if (extra.equals(KEY_DARK)) {
                first = PIANO_FIRST_DARK_KEY_X;
                count = getPianoDarkCountOffset(count);
            }
        }
        return getOverlayPosition(first, space, count);
    }

    private int getPianoLightCountOffset(int count) {
        if (count > 5) {
            if (count > 12) {
                if (count > 17) {
                    count += 1;
                }
                count += 1;
            }
            count += 1;
        }
        return count / 2 + count % 2;
    }

    private int getPianoDarkCountOffset(int count) {
        count = count / 2 + count % 2;
        if (count > 6) {
            count += 1;
        }
        return count;
    }

}