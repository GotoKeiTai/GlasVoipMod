package glas.voip.spike;

import glas.voip.patch.LuaGlobalTierProvider;
import glas.voip.patch.ReflectiveVoiceManagerDistanceWriter;
import glas.voip.patch.TierDistanceCalculator;
import glas.voip.patch.TierPatch;

import java.lang.instrument.Instrumentation;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Agent {

    public static void premain(String agentArgs, Instrumentation inst) {
        Path dumpPath = Paths.get(System.getProperty("user.home"), "glasvoipmod-voicemanager-dump.txt");
        inst.addTransformer(new VoiceManagerTransformer(dumpPath));

        // Placeholder distances until the Lua mod (next sub-project) reads real values from
        // server.json. Tier 1 (talk) matches ServerOptions.VoiceMinDistance/VoiceMaxDistance's
        // own vanilla defaults (10.0/100.0, confirmed via decompilation) so a player who never
        // changes tier keeps today's exact behavior.
        TierDistanceCalculator calculator = new TierDistanceCalculator(new TierDistanceCalculator.Distances[]{
                new TierDistanceCalculator.Distances(1.0f, 3.0f),
                new TierDistanceCalculator.Distances(10.0f, 100.0f),
                new TierDistanceCalculator.Distances(15.0f, 200.0f)
        });
        TierPatch.instance = new TierPatch(
                new LuaGlobalTierProvider(),
                calculator,
                new ReflectiveVoiceManagerDistanceWriter());

        System.out.println("[GlasVoipMod spike] agent attached, watching for "
                + VoiceManagerTransformer.TARGET_CLASS
                + " -- dump will be written to " + dumpPath);
    }
}
