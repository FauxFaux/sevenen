package com.goeswhere.sevenen;


import com.google.common.io.ByteStreams;
import net.sourceforge.blowfishj.streams.BlowfishInputStream;
import org.tukaani.xz.LZMAInputStream;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Unpack {
    public static void main(String[] args) throws Exception {
        if (1 != args.length) {
            System.err.println("usage: path/to/resource/root");
            System.exit(2);
        }

        final Path root = Paths.get(args[0]);

        final byte[] globalKey = ByteStreams.toByteArray(Unpack.class.getResourceAsStream("/global.key"));

        final byte[] localKey;
        final Path infoPath = root.resolve("Resources").resolve("infos");
        try (final ObjectInputStream ois = new ObjectInputStream(decrypterFor(globalKey,
                new FileInputStream(infoPath.toFile())))) {

            assertEquals("OK", ois.readUTF());
            System.out.println("Doc root?: " + ois.readUTF());
            System.out.println("    Time?: " + ois.readLong());
            localKey = new byte[128];
            assertEquals(localKey.length, ois.read(localKey), "short read");
        }

        Files.walk(root.resolve("Static"))
                .filter(p -> p.toFile().getAbsolutePath().endsWith(".7en"))
                .parallel()
                .forEach(p -> {
                    try {
                        final Path dest = removeExtension(p);
                        if (Files.exists(dest)) {
                            return;
                        }

                        final Path tmp = Files.createTempFile(p.getParent(), ".wip", ".tmp");

                        try (final LZMAInputStream in = new LZMAInputStream(
                                decrypterFor(
                                        localKey,
                                        new FileInputStream(p.toFile())));
                             final OutputStream out = new FileOutputStream(tmp.toFile())) {
                            ByteStreams.copy(in, out);
                        }

                        Files.move(tmp, dest);
                    } catch (Exception e) {
                        throw new IllegalStateException("processing " + p.toAbsolutePath(), e);
                    }
                });
    }

    private static BlowfishInputStream decrypterFor(byte[] key, InputStream inner) throws IOException {
        return new BlowfishInputStream(key, 0, key.length, inner);
    }

    private static Path removeExtension(Path p) {
        final String str = p.toFile().getAbsolutePath();
        final int dot = str.lastIndexOf('.');
        return Paths.get(str.substring(0, dot));
    }
}
