package glas.voip.patch;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TierDistanceCalculatorTest {

    private final TierDistanceCalculator calculator = new TierDistanceCalculator(new TierDistanceCalculator.Distances[]{
            new TierDistanceCalculator.Distances(1.0f, 3.0f),
            new TierDistanceCalculator.Distances(2.0f, 10.0f),
            new TierDistanceCalculator.Distances(4.0f, 30.0f)
    });

    @Test
    void distancesForTier_validTier_returnsConfiguredDistances() {
        TierDistanceCalculator.Distances result = calculator.distancesForTier(1);

        assertEquals(2.0f, result.minDistance());
        assertEquals(10.0f, result.maxDistance());
    }

    @Test
    void distancesForTier_differentValidTier_returnsDifferentDistances() {
        TierDistanceCalculator.Distances result = calculator.distancesForTier(2);

        assertEquals(4.0f, result.minDistance());
        assertEquals(30.0f, result.maxDistance());
    }

    @Test
    void distancesForTier_negativeTier_throws() {
        assertThrows(IllegalArgumentException.class, () -> calculator.distancesForTier(-1));
    }

    @Test
    void distancesForTier_tierTooLarge_throws() {
        assertThrows(IllegalArgumentException.class, () -> calculator.distancesForTier(3));
    }
}
