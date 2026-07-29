package glas.voip.patch;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TierDistanceCalculatorTest {

    private final TierDistanceCalculator calculator = new TierDistanceCalculator(new TierDistanceCalculator.Distances[]{
            new TierDistanceCalculator.Distances(1.0f, 3.0f),
            new TierDistanceCalculator.Distances(2.0f, 10.0f),
            new TierDistanceCalculator.Distances(4.0f, 30.0f)
    });

    @ParameterizedTest
    @CsvSource({
            "0, 1.0, 3.0",
            "1, 2.0, 10.0",
            "2, 4.0, 30.0"
    })
    void distancesForTier_validTier_returnsConfiguredDistances(int tier, float expectedMin, float expectedMax) {
        TierDistanceCalculator.Distances result = calculator.distancesForTier(tier);

        assertEquals(expectedMin, result.minDistance());
        assertEquals(expectedMax, result.maxDistance());
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
