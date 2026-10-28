package gigabyte.storage;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;

/**
 * Prevents application instances from loading and overwriting the same data file concurrently.
 */
public final class DataFileLock implements AutoCloseable {
    private static final Set<Path> lockedPaths = new HashSet<>();

    private final Path lockPath;
    private final FileChannel channel;

    private DataFileLock(Path lockPath, FileChannel channel) {
        this.lockPath = lockPath;
        this.channel = channel;
    }

    /**
     * Acquires an exclusive lock until closed, resolving symbolic links to the same data file.
     *
     * @throws IOException if another instance owns the file or the lock cannot be acquired.
     */
    public static synchronized DataFileLock acquire(Path dataFilePath) throws IOException {
        requireNonNull(dataFilePath);
        Path absolutePath = dataFilePath.toAbsolutePath().normalize();
        Files.createDirectories(absolutePath.getParent());
        Path realPath = Files.exists(absolutePath) ? absolutePath.toRealPath()
                : absolutePath.getParent().toRealPath().resolve(absolutePath.getFileName());
        Path lockPath = realPath.resolveSibling(realPath.getFileName() + ".lock");
        String message = "Gigabyte is already running for " + realPath
                + ". Close that instance before starting another.";
        // Opening and closing another channel can release existing locks on some operating systems.
        if (lockedPaths.contains(lockPath)) {
            throw new IOException(message);
        }

        FileChannel channel = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        try {
            try {
                if (channel.tryLock() == null) {
                    throw new IOException(message);
                }
            } catch (OverlappingFileLockException e) {
                throw new IOException(message, e);
            }
            lockedPaths.add(lockPath);
            return new DataFileLock(lockPath, channel);
        } catch (IOException | RuntimeException e) {
            try {
                channel.close();
            } catch (IOException closeException) {
                e.addSuppressed(closeException);
            }
            throw e;
        }
    }

    @Override
    public void close() throws IOException {
        synchronized (DataFileLock.class) {
            if (channel.isOpen()) {
                channel.close();
                lockedPaths.remove(lockPath);
            }
        }
        // Keep the sidecar: deleting it lets a later instance lock a different file at the same path.
    }
}
