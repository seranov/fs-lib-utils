package ru.seranov.fslibutils.grouping;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Strategy that derives target sub-folder names for a whole set of files at once.
 *
 * <p>A batch strategy is corpus-aware: it may inspect all files together (for example
 * to detect a prefix shared by every file). Implementations must be stateless and
 * thread-safe.
 */
public interface BatchGroupingStrategy {

    /**
     * Groups the given files into sub-folders.
     *
     * @param files regular files of a single directory, sorted by file name
     * @return ordered map of sub-folder name to the files assigned to it;
     *         empty if this strategy does not apply to the given files
     */
    Map<String, List<Path>> group(List<Path> files);
}
