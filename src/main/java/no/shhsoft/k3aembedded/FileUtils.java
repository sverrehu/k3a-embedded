package no.shhsoft.k3aembedded;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    public static int execClass(final Class<?> target, final String... args) {
        final List<String> command = new ArrayList<>();
        command.add(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add(target.getName());
        if (args != null) {
            command.addAll(Arrays.asList(args));
        }
        try {
            final Process process = new ProcessBuilder(command).start();
            process.waitFor();
            System.out.println(new String(process.getInputStream().readAllBytes()));
            System.err.println(new String(process.getErrorStream().readAllBytes()));
            return process.exitValue();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

}
