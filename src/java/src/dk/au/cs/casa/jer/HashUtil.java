package dk.au.cs.casa.jer;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

public class HashUtil {

    public static String shaDirOrFile(Path file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA");
            Collection<Path> files;
            if (Files.isDirectory(file)) {
                files = FileUtils.listFiles(file.toFile(), TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE).stream().map(File::toPath).collect(Collectors.toList());
            } else {
                files = Collections.singletonList(file);
            }
            files.stream()
                    .filter(f -> {
                        try {
                            return !Files.isDirectory(f) && !Files.isHidden(f);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .sorted()
                    .forEach(f -> {
                        try (FileInputStream fis = new FileInputStream(f.toFile())) {
                            byte[] byteArray = new byte[1024];
                            int bytesCount = 0;
                            while ((bytesCount = fis.read(byteArray)) != -1) {
                                digest.update(byteArray, 0, bytesCount);
                            }
                            fis.close();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
            final byte[] hash = digest.digest();
            String hexString = "";
            for (int i = 0; i < hash.length; i++) {
                if ((0xff & hash[i]) < 0x10) {
                    hexString += "0" + Integer.toHexString((0xFF & hash[i]));
                } else {
                    hexString += Integer.toHexString(0xFF & hash[i]);
                }
            }
            return hexString;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
