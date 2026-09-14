package ru.seranov.fslibutils.grouping;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Strategy that derives a target sub-folder name for a given file.
 * Implementations must be stateless and thread-safe.
 */
public interface FileGroupingStrategy {

    /**
     * Returns the sub-folder name that the given file should be moved into,
     * or {@link Optional#empty()} if this strategy does not apply to the file.
     */
    Optional<String> resolveGroup(Path file);
}
