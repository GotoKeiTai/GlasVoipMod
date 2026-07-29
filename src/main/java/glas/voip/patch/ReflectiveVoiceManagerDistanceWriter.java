package glas.voip.patch;

import java.lang.reflect.Field;

public class ReflectiveVoiceManagerDistanceWriter implements DistanceWriter {

    private static final String VOICE_MANAGER_CLASS = "zombie.core.raknet.VoiceManager";

    // Resolved once and cached rather than looked up on every call: this write() runs from
    // bytecode injected into the game's per-frame audio loop, and Class.forName +
    // getDeclaredField + setAccessible on every frame for every speaking player is real,
    // avoidable overhead for something that can never change at runtime.
    private Field minDistanceField;
    private Field maxDistanceField;

    // If resolution ever fails (e.g. a game update renames these fields), stop retrying and
    // stop logging -- otherwise a single persistent failure would re-pay the full
    // reflection+exception cost and spam stderr every single frame, forever.
    private boolean resolutionFailed;

    @Override
    public void write(TierDistanceCalculator.Distances distances) {
        if (resolutionFailed) {
            return;
        }

        try {
            resolveFieldsIfNeeded();
            minDistanceField.setFloat(null, distances.minDistance());
            maxDistanceField.setFloat(null, distances.maxDistance());
        } catch (ReflectiveOperationException e) {
            resolutionFailed = true;
            System.err.println("[GlasVoipMod] failed to resolve " + VOICE_MANAGER_CLASS
                    + " distance fields, disabling further writes: " + e);
        }
    }

    private void resolveFieldsIfNeeded() throws ReflectiveOperationException {
        if (minDistanceField != null) {
            return;
        }

        Class<?> voiceManagerClass = Class.forName(VOICE_MANAGER_CLASS);
        minDistanceField = voiceManagerClass.getDeclaredField("minDistance");
        minDistanceField.setAccessible(true);
        maxDistanceField = voiceManagerClass.getDeclaredField("maxDistance");
        maxDistanceField.setAccessible(true);
    }
}
