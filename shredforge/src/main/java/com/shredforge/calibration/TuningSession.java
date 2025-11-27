package com.shredforge.calibration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a tuning session with the strings to tune and current progress.
 * Guides the user through tuning each string sequentially.
 */
public final class TuningSession {

    private final List<TuningString> strings;
    private final String tuningName;
    private int currentStringIndex;
    private final List<Boolean> stringTuned;
    private volatile double lastDetectedFrequency;
    private volatile double lastDetectedCents;

    /**
     * Creates a tuning session from a list of strings.
     * Strings should be ordered from lowest to highest (string 6 = low E first).
     */
    public TuningSession(String tuningName, List<TuningString> strings) {
        Objects.requireNonNull(tuningName, "tuningName");
        Objects.requireNonNull(strings, "strings");
        if (strings.isEmpty()) {
            throw new IllegalArgumentException("strings cannot be empty");
        }
        this.tuningName = tuningName;
        this.strings = List.copyOf(strings);
        this.currentStringIndex = 0;
        this.stringTuned = new ArrayList<>(Collections.nCopies(strings.size(), false));
        this.lastDetectedFrequency = 0;
        this.lastDetectedCents = 0;
    }

    /**
     * Creates a standard tuning session (EADGBE).
     */
    public static TuningSession standardTuning() {
        return fromPreset(TuningLibrary.commonTunings().get(0));
    }

    /**
     * Creates a tuning session from a TuningPreset.
     */
    public static TuningSession fromPreset(TuningPreset preset) {
        Objects.requireNonNull(preset, "preset");
        List<TuningString> strings = new ArrayList<>();
        
        // Convert preset map to ordered list (low to high for tuning order)
        // Standard guitar: E2, A2, D3, G3, B3, E4
        var freqMap = preset.stringFrequenciesHz();
        
        // Sort by frequency ascending (low strings first - this is how guitarists typically tune)
        List<String> sortedNotes = new ArrayList<>(freqMap.keySet());
        sortedNotes.sort((a, b) -> Double.compare(freqMap.get(a), freqMap.get(b)));
        
        // String numbers: 6 = low E, 5 = A, 4 = D, 3 = G, 2 = B, 1 = high E
        int stringNum = sortedNotes.size();
        for (String noteName : sortedNotes) {
            double freq = freqMap.get(noteName);
            strings.add(TuningString.of(stringNum--, noteName, freq));
        }
        
        return new TuningSession(preset.name(), strings);
    }

    /**
     * Creates a tuning session from MIDI note numbers (as extracted from AlphaTab).
     * AlphaTab provides tuning from high string to low string (index 0 = string 1 = high E).
     * We reverse this so tuning starts with the low string (more natural for guitarists).
     * @param tuningName display name for the tuning
     * @param midiNotes array of MIDI note numbers, one per string (high to low from AlphaTab)
     */
    public static TuningSession fromMidiNotes(String tuningName, int[] midiNotes) {
        Objects.requireNonNull(tuningName, "tuningName");
        Objects.requireNonNull(midiNotes, "midiNotes");
        if (midiNotes.length == 0) {
            throw new IllegalArgumentException("midiNotes cannot be empty");
        }
        
        List<TuningString> strings = new ArrayList<>();
        int numStrings = midiNotes.length;
        
        // Reverse order: start from the last element (lowest string) and work up
        // String numbering: 6 = low E, 5 = A, 4 = D, 3 = G, 2 = B, 1 = high E
        for (int i = numStrings - 1; i >= 0; i--) {
            int midi = midiNotes[i];
            double freq = midiToFrequency(midi);
            String noteName = midiToNoteName(midi);
            int stringNumber = i + 1; // AlphaTab index + 1 = string number (1 = high, 6 = low)
            strings.add(new TuningString(stringNumber, noteName, freq, midi));
        }
        
        return new TuningSession(tuningName, strings);
    }

    public String tuningName() {
        return tuningName;
    }

    public List<TuningString> strings() {
        return strings;
    }

    public int currentStringIndex() {
        return currentStringIndex;
    }

    public TuningString currentString() {
        return strings.get(currentStringIndex);
    }

    public int totalStrings() {
        return strings.size();
    }

    public boolean isComplete() {
        return stringTuned.stream().allMatch(b -> b);
    }

    public double lastDetectedFrequency() {
        return lastDetectedFrequency;
    }

    public double lastDetectedCents() {
        return lastDetectedCents;
    }

    /**
     * Updates the detected pitch for the current string.
     * @return true if the string is now considered in tune
     */
    public boolean updateDetectedPitch(double frequencyHz, double centsTolerance) {
        this.lastDetectedFrequency = frequencyHz;
        TuningString current = currentString();
        this.lastDetectedCents = current.centsFromTarget(frequencyHz);
        return current.isInTune(frequencyHz, centsTolerance);
    }

    /**
     * Marks the current string as tuned and advances to the next.
     * @return true if there are more strings to tune
     */
    public boolean markCurrentTunedAndAdvance() {
        stringTuned.set(currentStringIndex, true);
        if (currentStringIndex < strings.size() - 1) {
            currentStringIndex++;
            lastDetectedFrequency = 0;
            lastDetectedCents = 0;
            return true;
        }
        return false;
    }

    /**
     * Moves to the previous string.
     * @return true if moved successfully
     */
    public boolean goToPreviousString() {
        if (currentStringIndex > 0) {
            currentStringIndex--;
            lastDetectedFrequency = 0;
            lastDetectedCents = 0;
            return true;
        }
        return false;
    }

    /**
     * Moves to the next string without marking current as tuned.
     * @return true if moved successfully
     */
    public boolean goToNextString() {
        if (currentStringIndex < strings.size() - 1) {
            currentStringIndex++;
            lastDetectedFrequency = 0;
            lastDetectedCents = 0;
            return true;
        }
        return false;
    }

    /**
     * Resets the session to start from the first string.
     */
    public void reset() {
        currentStringIndex = 0;
        Collections.fill(stringTuned, false);
        lastDetectedFrequency = 0;
        lastDetectedCents = 0;
    }

    /**
     * Returns the number of strings that have been tuned.
     */
    public int stringsTuned() {
        return (int) stringTuned.stream().filter(b -> b).count();
    }

    /**
     * Returns true if the given string index has been tuned.
     */
    public boolean isStringTuned(int index) {
        return index >= 0 && index < stringTuned.size() && stringTuned.get(index);
    }

    private static double midiToFrequency(int midi) {
        return 440.0 * Math.pow(2.0, (midi - 69) / 12.0);
    }

    private static String midiToNoteName(int midi) {
        String[] names = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
        int noteIndex = Math.floorMod(midi, 12);
        int octave = (midi / 12) - 1;
        return names[noteIndex] + octave;
    }
}
