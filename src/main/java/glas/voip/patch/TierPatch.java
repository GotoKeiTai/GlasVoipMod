package glas.voip.patch;

public class TierPatch {

    /**
     * Set by Agent.premain at startup; called from the bytecode injected into
     * VoiceManager.UpdateVMClient(). Static because the injected INVOKESTATIC
     * has no way to obtain an instance reference.
     */
    public static TierPatch instance;

    private final PlayerTierProvider tierProvider;
    private final TierDistanceCalculator distanceCalculator;
    private final DistanceWriter distanceWriter;

    public TierPatch(PlayerTierProvider tierProvider, TierDistanceCalculator distanceCalculator, DistanceWriter distanceWriter) {
        this.tierProvider = tierProvider;
        this.distanceCalculator = distanceCalculator;
        this.distanceWriter = distanceWriter;
    }

    public static void applyTierDistances(short onlineId) {
        if (instance != null) {
            instance.apply(onlineId);
        }
    }

    void apply(short onlineId) {
        Integer tier = tierProvider.getTierFor(onlineId);
        if (tier == null) {
            return;
        }

        TierDistanceCalculator.Distances distances;
        try {
            distances = distanceCalculator.distancesForTier(tier);
        } catch (IllegalArgumentException e) {
            System.err.println("[GlasVoipMod] invalid tier " + tier + " for player " + onlineId + ": " + e);
            return;
        }

        distanceWriter.write(distances);
    }
}
