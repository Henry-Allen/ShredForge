package com.shredforge.model;

/**
 * Represents a musical note with pitch, timing, and position information.
 * Used throughout ShredForge for both expected notes (from tabs) and detected notes (from audio).
 */
public class Note {
    private final String noteName;  // e.g., "E", "F#", "G"
    private final int octave;        // e.g., 2, 3, 4
    private final int string;        // Guitar string (1-6, where 1 is high E)
    private final int fret;          // Fret position (0-24)
    private final long timestamp;    // Timing in milliseconds

    public Note(String noteName, int octave, int string, int fret, long timestamp) {
        this.noteName = noteName;
        this.octave = octave;
        this.string = string;
        this.fret = fret;
        this.timestamp = timestamp;
    }

    /**
     * Creates a Note from frequency and guitar position
     */
    public static Note fromFrequency(float frequency, int string, int fret, long timestamp) {
        String[] noteNames = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};

        // Calculate semitones from A4 (440 Hz)
        double semitones = 12 * Math.log(frequency / 440.0) / Math.log(2);
        int semitonesFromA4 = (int) Math.round(semitones);

        // A4 is the 9th note (index 9) in octave 4
        int noteIndex = (9 + semitonesFromA4) % 12;
        if (noteIndex < 0) noteIndex += 12;

        int octave = 4 + (9 + semitonesFromA4) / 12;

        return new Note(noteNames[noteIndex], octave, string, fret, timestamp);
    }

    /**
     * Checks if this note matches another note (ignoring timestamp)
     */
    public boolean matches(Note other) {
        return this.noteName.equals(other.noteName) &&
               this.octave == other.octave;
    }

    /**
     * Calculate timing difference with another note in milliseconds
     */
    public float getTimingDiff(Note expected) {
        return Math.abs(this.timestamp - expected.timestamp);
    }

    /**
     * Get the expected frequency for this note in Hz
     */
    public float getFrequency() {
        String[] noteNames = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
        int noteIndex = -1;
        for (int i = 0; i < noteNames.length; i++) {
            if (noteNames[i].equals(noteName)) {
                noteIndex = i;
                break;
            }
        }

        // Calculate semitones from A4
        int semitonesFromA4 = (octave - 4) * 12 + (noteIndex - 9);

        // Calculate frequency using A4 = 440 Hz as reference
        return (float) (440.0 * Math.pow(2.0, semitonesFromA4 / 12.0));
    }

    // Getters
    public String getNoteName() { return noteName; }
    public int getOctave() { return octave; }
    public int getString() { return string; }
    public int getFret() { return fret; }
    public long getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return noteName + octave + " (String:" + string + " Fret:" + fret + " @" + timestamp + "ms)";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Note)) return false;
        Note note = (Note) o;
        return octave == note.octave &&
               string == note.string &&
               fret == note.fret &&
               noteName.equals(note.noteName);
    }

    @Override
    public int hashCode() {
        int result = noteName.hashCode();
        result = 31 * result + octave;
        result = 31 * result + string;
        result = 31 * result + fret;
        return result;
    }
}
