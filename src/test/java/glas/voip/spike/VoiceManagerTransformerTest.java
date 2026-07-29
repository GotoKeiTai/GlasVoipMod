package glas.voip.spike;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class VoiceManagerTransformerTest {

    @Test
    void transform_nonMatchingClassName_returnsNullAndWritesNoDump(@TempDir Path tempDir) throws IOException {
        Path dumpPath = tempDir.resolve("dump.txt");
        VoiceManagerTransformer transformer = new VoiceManagerTransformer(dumpPath);

        byte[] result = transformer.transform(
                null, "some/other/ClassName", null, null, new byte[]{1, 2, 3});

        assertNull(result);
        assertFalse(Files.exists(dumpPath));
    }

    // Since VoiceManagerTransformer was wired to actually patch (not just dump), this class's
    // own bytecode -- fed in as a stand-in "matching class name" -- has no UpdateVMClient
    // method, so BytecodeInjector.inject() now throws IllegalStateException, which transform()
    // catches and turns into a null return. That's a real, separately meaningful behavior
    // (dump-then-injection-failure both handled gracefully), not the same thing this test used
    // to check ("spike only observes") -- renamed and reworded to describe what actually
    // happens now, rather than leave a stale assertion that passes for the wrong reason.
    //
    // Deliberately NOT tested here: the successful-injection path through transform() end to
    // end. Constructing a synthetic fixture whose class-file internal name is literally
    // "zombie/core/raknet/VoiceManager" (required both for the transform() class-name check
    // and for BytecodeInjector's field-owner match) adds real complexity for something already
    // covered by BytecodeInjectorTest (the injector's core matching/insertion logic, unit
    // tested directly) and, more importantly, by Task 9's manual verification against the real
    // game -- the only place "does the fully-wired transformer actually work" can be proven
    // for real.
    @Test
    void transform_matchingClassNameButNoInjectionTarget_stillWritesDumpAndReturnsNull(@TempDir Path tempDir) throws IOException {
        Path dumpPath = tempDir.resolve("dump.txt");
        VoiceManagerTransformer transformer = new VoiceManagerTransformer(dumpPath);
        byte[] realClassBytes = readOwnClassBytes();

        byte[] result = transformer.transform(
                null, VoiceManagerTransformer.TARGET_CLASS, null, null, realClassBytes);

        assertNull(result, "injection has no matching target in this class, so transform() must fail gracefully to null rather than crash class loading");
        assertTrue(Files.exists(dumpPath), "the dump attempt is independent of the injection attempt and should still succeed");
        String dumpContent = Files.readString(dumpPath);
        assertTrue(dumpContent.contains("VoiceManagerTransformerTest"),
                "dump should contain a disassembly of the class we fed it");
    }

    private byte[] readOwnClassBytes() throws IOException {
        String resourceName = VoiceManagerTransformerTest.class.getSimpleName() + ".class";
        try (InputStream in = VoiceManagerTransformerTest.class.getResourceAsStream(resourceName)) {
            assertNotNull(in, "test class bytes must be loadable from the classpath");
            return in.readAllBytes();
        }
    }
}
