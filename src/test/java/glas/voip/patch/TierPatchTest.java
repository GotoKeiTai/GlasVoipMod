package glas.voip.patch;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class TierPatchTest {

    private static class RecordingDistanceWriter implements DistanceWriter {
        TierDistanceCalculator.Distances written;

        @Override
        public void write(TierDistanceCalculator.Distances distances) {
            this.written = distances;
        }
    }

    private final TierDistanceCalculator calculator = new TierDistanceCalculator(new TierDistanceCalculator.Distances[]{
            new TierDistanceCalculator.Distances(1.0f, 3.0f),
            new TierDistanceCalculator.Distances(2.0f, 10.0f),
            new TierDistanceCalculator.Distances(4.0f, 30.0f)
    });

    @Test
    void apply_knownTier_writesCalculatedDistances() {
        RecordingDistanceWriter writer = new RecordingDistanceWriter();
        PlayerTierProvider provider = onlineId -> 2;
        TierPatch patch = new TierPatch(provider, calculator, writer);

        patch.apply((short) 7);

        assertNotNull(writer.written);
        assertEquals(4.0f, writer.written.minDistance());
        assertEquals(30.0f, writer.written.maxDistance());
    }

    @Test
    void apply_unknownTier_doesNotWrite() {
        RecordingDistanceWriter writer = new RecordingDistanceWriter();
        PlayerTierProvider provider = onlineId -> null;
        TierPatch patch = new TierPatch(provider, calculator, writer);

        patch.apply((short) 7);

        assertNull(writer.written);
    }

    @Test
    void apply_invalidTierFromProvider_doesNotWrite() {
        RecordingDistanceWriter writer = new RecordingDistanceWriter();
        PlayerTierProvider provider = onlineId -> 99;
        TierPatch patch = new TierPatch(provider, calculator, writer);

        patch.apply((short) 7);

        assertNull(writer.written);
    }
}
