package glas.voip.patch;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class TierPatchTest {

    @AfterEach
    void resetStaticInstance() {
        TierPatch.instance = null;
    }

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

    @Test
    void apply_negativeTierFromProvider_doesNotWrite() {
        RecordingDistanceWriter writer = new RecordingDistanceWriter();
        PlayerTierProvider provider = onlineId -> -1;
        TierPatch patch = new TierPatch(provider, calculator, writer);

        patch.apply((short) 7);

        assertNull(writer.written);
    }

    @Test
    void apply_providerThrows_doesNotPropagate() {
        RecordingDistanceWriter writer = new RecordingDistanceWriter();
        PlayerTierProvider provider = onlineId -> {
            throw new RuntimeException("boom");
        };
        TierPatch patch = new TierPatch(provider, calculator, writer);

        assertDoesNotThrow(() -> patch.apply((short) 7));
        assertNull(writer.written);
    }

    @Test
    void apply_writerThrows_doesNotPropagate() {
        DistanceWriter writer = distances -> {
            throw new RuntimeException("boom");
        };
        PlayerTierProvider provider = onlineId -> 0;
        TierPatch patch = new TierPatch(provider, calculator, writer);

        assertDoesNotThrow(() -> patch.apply((short) 7));
    }

    @Test
    void applyTierDistances_noInstanceSet_doesNotThrow() {
        TierPatch.instance = null;

        assertDoesNotThrow(() -> TierPatch.applyTierDistances((short) 7));
    }

    @Test
    void applyTierDistances_instanceSet_delegatesToInstance() {
        RecordingDistanceWriter writer = new RecordingDistanceWriter();
        PlayerTierProvider provider = onlineId -> 1;
        TierPatch.instance = new TierPatch(provider, calculator, writer);

        TierPatch.applyTierDistances((short) 7);

        assertNotNull(writer.written);
        assertEquals(2.0f, writer.written.minDistance());
        assertEquals(10.0f, writer.written.maxDistance());
    }
}
