package de.jotibi.onewayelytra.service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FlightTracker {

    private final Map<UUID, Boolean> wasGliding = new HashMap<>();

    public void setGliding(UUID uuid, boolean gliding) {
        wasGliding.put(uuid, gliding);
    }

    public boolean wasGliding(UUID uuid) {
        return wasGliding.getOrDefault(uuid, false);
    }

    public void clearState(UUID uuid) {
        wasGliding.remove(uuid);
    }
}
