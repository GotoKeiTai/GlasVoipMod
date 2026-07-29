package glas.voip.patch;

public class TierPatch {

    /**
     * Set by Agent.premain at startup; called from the bytecode injected into
     * VoiceManager.UpdateVMClient(). Static because the injected INVOKESTATIC
     * has no way to obtain an instance reference. Volatile because the write
     * happens on the agent-attach thread and the read happens on whatever
     * thread runs the game's voice-processing loop, with no other
     * synchronization between them.
     */
    public static volatile TierPatch instance;

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
        // This is called from bytecode injected directly into VoiceManager.UpdateVMClient(),
        // the game's own per-frame audio loop -- an uncaught exception here would propagate
        // straight into vanilla game code, so nothing (provider lookup, tier validation, or
        // the actual field write) is allowed to escape this method.
        try {
            Integer tier = tierProvider.getTierFor(onlineId);
            if (tier == null) {
                return;
            }

            TierDistanceCalculator.Distances distances = distanceCalculator.distancesForTier(tier);
            distanceWriter.write(distances);
        } catch (Exception e) {
            System.err.println("[GlasVoipMod] failed to apply tier distances for player " + onlineId + ": " + e);
        }
    }
}
