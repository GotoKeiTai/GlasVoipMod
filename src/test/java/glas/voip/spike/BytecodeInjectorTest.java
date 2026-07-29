package glas.voip.spike;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BytecodeInjectorTest {

    private static final String PATCH_CALL = "INVOKESTATIC glas/voip/patch/TierPatch.applyTierDistances (S)V";

    @Test
    void inject_insertsPatchCallBeforeAdjacentFieldPair_onlyOnce() throws IOException {
        byte[] fixtureBytes = readFixtureBytes();
        BytecodeInjector injector = new BytecodeInjector(
                "computeSomething",
                "glas/voip/spike/InjectionFixture", "maxDistance",
                "glas/voip/spike/InjectionFixture", "minDistance",
                "glas/voip/patch/TierPatch", "applyTierDistances", "(S)V");

        byte[] transformed = injector.inject(fixtureBytes);

        String disassembly = disassemble(transformed);
        assertEquals(1, countOccurrences(disassembly, PATCH_CALL),
                "the fixture has a second, non-adjacent read of maxDistance later in the method -- "
                        + "the injector must match only the adjacent maxDistance/minDistance pair, not that second site");

        int aloadIndex = disassembly.indexOf("ALOAD 8");
        int invokeGetOnlineIdIndex = disassembly.indexOf("INVOKEVIRTUAL zombie/characters/IsoPlayer.getOnlineID ()S");
        int patchCallIndex = disassembly.indexOf(PATCH_CALL);
        int firstFieldIndex = disassembly.indexOf("GETSTATIC glas/voip/spike/InjectionFixture.maxDistance : F");
        int secondFieldIndex = disassembly.indexOf("GETSTATIC glas/voip/spike/InjectionFixture.minDistance : F");

        assertTrue(aloadIndex >= 0, "injected ALOAD 8 should be present");
        assertTrue(aloadIndex < invokeGetOnlineIdIndex, "ALOAD 8 must come before the getOnlineID call");
        assertTrue(invokeGetOnlineIdIndex < patchCallIndex, "getOnlineID call must come before the patch call");
        assertTrue(patchCallIndex < firstFieldIndex, "patch call must come before the first trigger field read");
        assertTrue(firstFieldIndex < secondFieldIndex, "the two trigger field reads must remain adjacent and in order");
    }

    @Test
    void inject_nonMatchingMethod_throwsRatherThanSilentlyDoingNothing() throws IOException {
        byte[] fixtureBytes = readFixtureBytes();
        BytecodeInjector injector = new BytecodeInjector(
                "someOtherMethodName",
                "glas/voip/spike/InjectionFixture", "maxDistance",
                "glas/voip/spike/InjectionFixture", "minDistance",
                "glas/voip/patch/TierPatch", "applyTierDistances", "(S)V");

        assertThrows(IllegalStateException.class, () -> injector.inject(fixtureBytes),
                "zero matches must fail loudly, not silently return the class unchanged");
    }

    @Test
    void inject_matchingMethodButNoAdjacentPair_throwsRatherThanSilentlyDoingNothing() throws IOException {
        byte[] fixtureBytes = readFixtureBytes();
        BytecodeInjector injector = new BytecodeInjector(
                "computeSomething",
                "glas/voip/spike/InjectionFixture", "minDistance",
                "glas/voip/spike/InjectionFixture", "maxDistance",
                "glas/voip/patch/TierPatch", "applyTierDistances", "(S)V");

        assertThrows(IllegalStateException.class, () -> injector.inject(fixtureBytes),
                "the fixture never reads minDistance immediately followed by maxDistance (only the reverse order), so this must fail loudly too");
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

    private int countOccurrences(String haystack, String needle) {
        int count = 0;
        int index = 0;
        while ((index = haystack.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }
}
