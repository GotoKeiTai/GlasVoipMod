package glas.voip.spike;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.instrument.ClassFileTransformer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.ProtectionDomain;

public class VoiceManagerTransformer implements ClassFileTransformer {

    public static final String TARGET_CLASS = "zombie/core/raknet/VoiceManager";

    private final Path dumpOutputPath;

    public VoiceManagerTransformer(Path dumpOutputPath) {
        this.dumpOutputPath = dumpOutputPath;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                             ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if (!TARGET_CLASS.equals(className)) {
            return null;
        }

        // The JVM swallows exceptions thrown from transform() -- it neither propagates them
        // nor lets class loading fail, so without an explicit log line here a dump failure is
        // silently indistinguishable from "the transformer never matched" during manual
        // in-game verification (the whole point of this spike).
        try {
            dumpBytecode(classfileBuffer);
            System.out.println("[GlasVoipMod spike] wrote bytecode dump to " + dumpOutputPath);
        } catch (Exception e) {
            System.err.println("[GlasVoipMod spike] failed to dump " + TARGET_CLASS + " bytecode: " + e);
            e.printStackTrace();
        }

        return null;
    }

    private void dumpBytecode(byte[] classBytes) throws IOException {
        ClassReader reader = new ClassReader(classBytes);
        StringWriter output = new StringWriter();
        reader.accept(new TraceClassVisitor(new PrintWriter(output)), 0);
        Files.writeString(dumpOutputPath, output.toString());
    }
}
