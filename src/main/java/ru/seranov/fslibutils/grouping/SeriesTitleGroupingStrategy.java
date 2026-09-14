package ru.seranov.fslibutils.grouping;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Groups files by extracting the "series title (id)" part from filenames.
 *
 * <p>Supported filename pattern:
 * <pre>
 *   &lt;Author&gt; - &lt;Title (ID)&gt; &lt;page-suffix&gt;.&lt;ext&gt;
 * </pre>
 * Example: {@code SpicyXGames - 5,000 Follower Picnic (135432291) p01.jpg}
 *
 * <p>The captured group becomes the target sub-folder name:
 * {@code 5,000 Follower Picnic (135432291)}
 *
 * <p>The separator pattern and capture group are configurable via
 * {@code application.properties}:
 * <pre>
 *   grouping.series-title.separator-regex = \\s+-\\s+
 *   grouping.series-title.title-pattern    = (.*\\(\\d+\\))
 * </pre>
 */
@Slf4j
@Component
@ConfigurationProperties(prefix = "grouping.series-title")
public class SeriesTitleGroupingStrategy implements FileGroupingStrategy {

    /**
     * Regex that separates the author prefix from the title+id part.
     * Default: one or more spaces, a dash, one or more spaces.
     */
    private String separatorRegex = "\\s+-\\s+";

    /**
     * Regex for the title+id part (must contain exactly one capturing group
     * which becomes the folder name). Matched after the separator.
     * Default: everything up to and including a trailing {@code (digits)}.
     */
    private String titlePattern = "(.*\\(\\d+\\))";

    /** Compiled pattern, lazily initialised. */
    private volatile Pattern compiled;

    // ---- ConfigurationProperties setters --------------------------------

    public synchronized void setSeparatorRegex(String separatorRegex) {
        this.separatorRegex = separatorRegex;
        this.compiled = null;
    }

    public synchronized void setTitlePattern(String titlePattern) {
        this.titlePattern = titlePattern;
        this.compiled = null;
    }

    // ---- FileGroupingStrategy -------------------------------------------

    @Override
    public Optional<String> resolveGroup(Path file) {
        String filename = file.getFileName().toString();
        Matcher m = getPattern().matcher(filename);
        if (!m.find()) {
            log.debug("No group match for file: {}", filename);
            return Optional.empty();
        }
        String group = m.group(1).trim();
        log.debug("File '{}' -> group '{}'", filename, group);
        return Optional.of(group);
    }

    // ---- helpers --------------------------------------------------------

    private Pattern getPattern() {
        if (compiled == null) {
            synchronized (this) {
                if (compiled == null) {
                    String full = ".+" + separatorRegex + titlePattern;
                    compiled = Pattern.compile(full);
                }
            }
        }
        return compiled;
    }
}
