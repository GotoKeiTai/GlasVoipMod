package glas.voip.spike;

import java.lang.instrument.Instrumentation;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Agent {

    public static void premain(String agentArgs, Instrumentation inst) {
        Path dumpPath = Paths.get(System.getProperty("user.home"), "glasvoipmod-voicemanager-dump.txt");
        inst.addTransformer(new VoiceManagerTransformer(dumpPath));
        System.out.println("[GlasVoipMod spike] agent attached, watching for "
                + VoiceManagerTransformer.TARGET_CLASS
                + " -- dump will be written to " + dumpPath);
    }
}
