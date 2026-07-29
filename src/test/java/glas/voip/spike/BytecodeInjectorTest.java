package glas.voip.spike;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BytecodeInjectorTest {

    @Test
    void inject_insertsPatchCallBeforeTriggerField() throws IOException {
        byte[] fixtureBytes = readFixtureBytes();
        BytecodeInjector injector = new BytecodeInjector(
                "glas/voip/spike/InjectionFixture", "computeSomething",
                "glas/voip/spike/InjectionFixture", "maxDistance",
                "glas/voip/patch/TierPatch", "applyTierDistances", "(S)V");

        byte[] transformed = injector.inject(fixtureBytes);

        String disassembly = disassemble(transformed);
        int patchCallIndex = disassembly.indexOf("INVOKESTATIC glas/voip/patch/TierPatch.applyTierDistances (S)V");
        int triggerFieldIndex = disassembly.indexOf("GETSTATIC glas/voip/spike/InjectionFixture.maxDistance : F");

        assertTrue(patchCallIndex >= 0, "patch call should be present in transformed bytecode");
        assertTrue(triggerFieldIndex >= 0, "trigger field read should still be present");
        assertTrue(patchCallIndex < triggerFieldIndex, "patch call must come before the trigger field read");
    }

    @Test
    void inject_nonMatchingMethod_leavesBytecodeUnchanged() throws IOException {
        byte[] fixtureBytes = readFixtureBytes();
        BytecodeInjector injector = new BytecodeInjector(
                "glas/voip/spike/InjectionFixture", "someOtherMethodName",
                "glas/voip/spike/InjectionFixture", "maxDistance",
                "glas/voip/patch/TierPatch", "applyTierDistances", "(S)V");

        byte[] transformed = injector.inject(fixtureBytes);

        String disassembly = disassemble(transformed);
        assertFalse(disassembly.contains("TierPatch.applyTierDistances"),
                "no injection should happen when the target method name doesn't match");
    }

    private byte[] readFixtureBytes() throws IOException {
        String resourceName = InjectionFixture.class.getSimpleName() + ".class";
        try (InputStream in = InjectionFixture.class.getResourceAsStream(resourceName)) {
            assertNotNull(in, "fixture class bytes must be loadable from the classpath");
            return in.readAllBytes();
        }
    }

    private String disassemble(byte[] classBytes) {
        ClassReader reader = new ClassReader(classBytes);
        StringWriter output = new StringWriter();
        reader.accept(new TraceClassVisitor(new PrintWriter(output)), 0);
        return output.toString();
    }
}
