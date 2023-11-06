package no.shhsoft.k3aembedded;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

final class FileUtils {

    private FileUtils() {
    }

    public static Path createTempDirectory(final String prefix) {
        try {
            return Files.createTempDirectory(prefix);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void deleteRecursively(final File directory) {
        final File[] filesAndDirectories = directory.listFiles();
        if (filesAndDirectories != null) {
            for (final File containedFileOrDirectory : filesAndDirectories) {
                if (containedFileOrDirectory.isDirectory()) {
                    deleteRecursively(containedFileOrDirectory);
                }
                delete(containedFileOrDirectory);
            }
        }
        delete(directory);
    }

    private static void delete(final File fileOrDirectory) {
        if (!fileOrDirectory.delete()) {
            System.err.println("Unable to delete \"" + fileOrDirectory + "\". Ignoring.");
        }
    }

}
