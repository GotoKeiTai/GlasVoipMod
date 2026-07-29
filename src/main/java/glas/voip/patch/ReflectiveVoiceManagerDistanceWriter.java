package glas.voip.patch;

import java.lang.reflect.Field;

public class ReflectiveVoiceManagerDistanceWriter implements DistanceWriter {

    private static final String VOICE_MANAGER_CLASS = "zombie.core.raknet.VoiceManager";

    @Override
    public void write(TierDistanceCalculator.Distances distances) {
        try {
            Class<?> voiceManagerClass = Class.forName(VOICE_MANAGER_CLASS);
            setStaticFloatField(voiceManagerClass, "minDistance", distances.minDistance());
            setStaticFloatField(voiceManagerClass, "maxDistance", distances.maxDistance());
        } catch (ReflectiveOperationException e) {
            System.err.println("[GlasVoipMod] failed to write " + VOICE_MANAGER_CLASS + " distance fields: " + e);
        }
    }

    private static void setStaticFloatField(Class<?> owner, String fieldName, float value) throws ReflectiveOperationException {
        Field field = owner.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.setFloat(null, value);
    }
}
