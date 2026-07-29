package glas.voip.patch;

public class TierDistanceCalculator {

    public record Distances(float minDistance, float maxDistance) {}

    private final Distances[] tierDistances;

    public TierDistanceCalculator(Distances[] tierDistances) {
        this.tierDistances = tierDistances;
    }

    public Distances distancesForTier(int tier) {
        if (tier < 0 || tier >= tierDistances.length) {
            throw new IllegalArgumentException("Unknown tier: " + tier);
        }
        return tierDistances[tier];
    }
}
