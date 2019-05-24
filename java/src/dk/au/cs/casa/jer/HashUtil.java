package dk.au.cs.casa.jer;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HashUtil {

    private static Collection<Path> getFilesFromDirOrFile(Path file) {
        Collection<Path> files = new ArrayList<>();
        if (Files.isDirectory(file)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(file)) {
                stream.forEach(f -> {
                    files.addAll(getFilesFromDirOrFile(f));
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            files.add(file);
        }
        return files;
    }

    public static String shaDirOrFile(Path fileOrDirectory) {
        Collection<Path> files = getFilesFromDirOrFile(fileOrDirectory);
        return shaFiles(files);
    }

    public static String shaDirOrFile(Collection<Path> filesOrDirectories) {
        Collection<Path> files = filesOrDirectories.stream().flatMap(p -> getFilesFromDirOrFile(p).stream()).collect(Collectors.toSet());
        return shaFiles(files);
    }

    /**
     * Computes the SHA-1 sum of the given files.
     *
     * NB: the file is read as UTF-8 and linebreaks are normalized to \n for the sake of the computation.
     */
    public static String shaFiles(Collection<Path> files) {
        Charset charset = Charset.forName("UTF-8");
        byte[] NEWLINE = "\n".getBytes(charset);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = new byte[digest.getDigestLength()];
            files.stream()
                    .filter(f -> {
                        try {
                            return !Files.isDirectory(f) && !Files.isHidden(f);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .filter(f -> f.toString().endsWith(".js") || f. toString().endsWith(".html") || f.toString().endsWith(".htm"))
                    .forEach(f -> {
                        try (Stream<String> lines = Files.lines(f, charset)) {
                            lines.forEach(line -> {
                                digest.update(line.getBytes(charset));
                                digest.update(NEWLINE);
                            });
                        } catch (Exception e) {
                            try {
                                digest.update(Files.readAllBytes(f)); // could not be read with the chosen charset: just read the raw bytes
                            } catch (IOException e1) {
                                throw new RuntimeException(e1);
                            }
                        }
                        byte[] filehash = digest.digest();
                        for (int i = 0; i < hash.length; i++)
                            hash[i] ^= filehash[i];
                    });
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                if ((0xff & b) < 0x10) {
                    hexString.append("0");
                }
                hexString.append(Integer.toHexString(0xFF & b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
