package com.shredforge.core.model;

import java.util.Objects;

/**
 * Represents an available audio input device.
 */
public record AudioDeviceInfo(
        String id,
        String name,
        String description,
        boolean isDefault) {

    public AudioDeviceInfo {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        description = description == null ? "" : description;
    }

    /**
     * Creates an AudioDeviceInfo for the system default device.
     */
    public static AudioDeviceInfo systemDefault() {
        return new AudioDeviceInfo("default", "System Default", "Uses the system's default audio input", true);
    }

    @Override
    public String toString() {
        return name + (isDefault ? " (Default)" : "");
    }
}
