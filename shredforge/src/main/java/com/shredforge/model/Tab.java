package com.shredforge.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a guitar tablature with metadata and note information.
 * Tabs are retrieved from Songsterr API and stored locally.
 * Uses Jackson for JSON serialization/deserialization.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Tab {
    @JsonProperty("id")
    private String id;

    @JsonProperty("title")
    private String title;

    @JsonProperty("artist")
    private String artist;

    @JsonProperty("difficulty")
    private String difficulty;  // "Easy", "Medium", "Hard", "Expert"

    @JsonProperty("rating")
    private float rating;

    @JsonProperty("notes")
    private List<Note> notes;

    @JsonProperty("duration")
    private long duration;  // Duration in milliseconds

    @JsonProperty("tempo")
    private int tempo;  // BPM (beats per minute)

    private boolean downloaded;
    private String localPath;

    public Tab() {
        this.notes = new ArrayList<>();
    }

    public Tab(String id, String title, String artist) {
        this();
        this.id = id;
        this.title = title;
        this.artist = artist;
    }

    /**
     * Get total number of notes in the tab
     */
    public int getTotalNotes() {
        return notes.size();
    }

    /**
     * Get notes within a specific time range (for practice loops)
     */
    public List<Note> getNotesInRange(long startTime, long endTime) {
        List<Note> rangeNotes = new ArrayList<>();
        for (Note note : notes) {
            if (note.getTimestamp() >= startTime && note.getTimestamp() <= endTime) {
                rangeNotes.add(note);
            }
        }
        return rangeNotes;
    }

    /**
     * Get the expected note at a specific timestamp (within tolerance)
     */
    public Note getNoteAtTime(long timestamp, long tolerance) {
        for (Note note : notes) {
            if (Math.abs(note.getTimestamp() - timestamp) <= tolerance) {
                return note;
            }
        }
        return null;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }

    public List<Note> getNotes() { return notes; }
    public void setNotes(List<Note> notes) { this.notes = notes; }

    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }

    public int getTempo() { return tempo; }
    public void setTempo(int tempo) { this.tempo = tempo; }

    public boolean isDownloaded() { return downloaded; }
    public void setDownloaded(boolean downloaded) { this.downloaded = downloaded; }

    public String getLocalPath() { return localPath; }
    public void setLocalPath(String localPath) { this.localPath = localPath; }

    @Override
    public String toString() {
        return String.format("%s - %s (%s, %d notes)", artist, title, difficulty, notes.size());
    }
}
