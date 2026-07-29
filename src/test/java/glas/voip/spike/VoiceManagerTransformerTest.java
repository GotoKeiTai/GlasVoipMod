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

    @Test
    void transform_matchingClassName_writesDumpWithBytecodeText(@TempDir Path tempDir) throws IOException {
        Path dumpPath = tempDir.resolve("dump.txt");
        VoiceManagerTransformer transformer = new VoiceManagerTransformer(dumpPath);
        byte[] realClassBytes = readOwnClassBytes();

        byte[] result = transformer.transform(
                null, VoiceManagerTransformer.TARGET_CLASS, null, null, realClassBytes);

        assertNull(result, "spike only observes, must not alter the returned bytecode");
        assertTrue(Files.exists(dumpPath));
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
