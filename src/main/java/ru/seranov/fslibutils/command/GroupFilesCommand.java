package ru.seranov.fslibutils.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.seranov.fslibutils.grouping.FileGroupingStrategy;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

/**
 * CLI command: {@code group [--dir <path>] [--dry-run]}
 *
 * <p>Scans the target directory for regular files (non-recursive), resolves
 * a sub-folder name for each file using the configured
 * {@link FileGroupingStrategy} list, and moves the files into those
 * sub-folders.
 *
 * <p>Options:
 * <ul>
 *   <li>{@code --dir <path>} – directory to process (default: current working directory)</li>
 *   <li>{@code --dry-run}    – print planned moves without executing them</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GroupFilesCommand implements CliCommand {

    private final List<FileGroupingStrategy> strategies;

    @Override
    public String name() {
        return "group";
    }

    @Override
    public int execute(String[] args) {
        ParsedArgs parsed = parseArgs(args);
        Path dir = parsed.dir();
        boolean dryRun = parsed.dryRun();

        if (!Files.isDirectory(dir)) {
            log.error("Not a directory: {}", dir);
            return 1;
        }

        log.info("Grouping files in: {} {}", dir.toAbsolutePath(), dryRun ? "[DRY RUN]" : "");

        Map<String, List<Path>> groups = collectGroups(dir);

        if (groups.isEmpty()) {
            log.info("No files matched any grouping strategy.");
            return 0;
        }

        int moved = 0;
        int errors = 0;
        for (Map.Entry<String, List<Path>> entry : groups.entrySet()) {
            String groupName = entry.getKey();
            Path targetDir = dir.resolve(groupName);
            log.info("Group '{}' ({} file(s)):", groupName, entry.getValue().size());

            if (!dryRun) {
                try {
                    Files.createDirectories(targetDir);
                } catch (IOException e) {
                    log.error("Failed to create directory {}: {}", targetDir, e.getMessage());
                    errors++;
                    continue;
                }
            }

            for (Path src : entry.getValue()) {
                Path dest = targetDir.resolve(src.getFileName());
                log.info("  {} -> {}", src.getFileName(), groupName + "/" + src.getFileName());
                if (!dryRun) {
                    try {
                        Files.move(src, dest, StandardCopyOption.ATOMIC_MOVE);
                        moved++;
                    } catch (AtomicMoveNotSupportedException e) {
                        try {
                            Files.move(src, dest, StandardCopyOption.REPLACE_EXISTING);
                            moved++;
                        } catch (IOException ex) {
                            log.error("Failed to move {}: {}", src.getFileName(), ex.getMessage());
                            errors++;
                        }
                    } catch (IOException e) {
                        log.error("Failed to move {}: {}", src.getFileName(), e.getMessage());
                        errors++;
                    }
                }
            }
        }

        if (dryRun) {
            log.info("Dry run complete. {} file(s) would be moved into {} group(s).",
                    groups.values().stream().mapToInt(List::size).sum(), groups.size());
        } else {
            log.info("Done. Moved: {}, Errors: {}", moved, errors);
        }

        return errors > 0 ? 2 : 0;
    }

    // ---- helpers --------------------------------------------------------

    private Map<String, List<Path>> collectGroups(Path dir) {
        Map<String, List<Path>> groups = new LinkedHashMap<>();
        try (Stream<Path> stream = Files.list(dir)) {
            stream.filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .forEach(file -> resolveGroup(file).ifPresent(
                            group -> groups.computeIfAbsent(group, k -> new ArrayList<>()).add(file)));
        } catch (IOException e) {
            log.error("Failed to list directory {}: {}", dir, e.getMessage());
        }
        return groups;
    }

    private Optional<String> resolveGroup(Path file) {
        for (FileGroupingStrategy strategy : strategies) {
            Optional<String> result = strategy.resolveGroup(file);
            if (result.isPresent()) {
                return result;
            }
        }
        return Optional.empty();
    }

    private ParsedArgs parseArgs(String[] args) {
        Path dir = Paths.get(System.getProperty("user.dir"));
        boolean dryRun = false;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--dir" -> {
                    if (i + 1 < args.length) {
                        dir = Paths.get(args[++i]);
                    }
                }
                case "--dry-run" -> dryRun = true;
                default -> log.warn("Unknown option: {}", args[i]);
            }
        }
        return new ParsedArgs(dir, dryRun);
    }

    private record ParsedArgs(Path dir, boolean dryRun) {}
}
