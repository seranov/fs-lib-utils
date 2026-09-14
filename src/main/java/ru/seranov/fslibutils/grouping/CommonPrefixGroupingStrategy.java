package ru.seranov.fslibutils.grouping;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Corpus-aware grouping strategy that clusters file names purely by their common prefixes.
 *
 * <p>Nothing is assumed about separators or about the format of the shared (title) part.
 * The algorithm is recursive:
 * <ol>
 *   <li>For a cluster of names the longest common prefix is computed.</li>
 *   <li>If every name in the cluster is that prefix followed by a "sequence suffix"
 *       (a page / part designator such as {@code p01.jpg}, {@code - 1.jpg} or {@code a1.jpg}),
 *       the cluster is a group. Its common prefix — with the leaked sequence marker
 *       ({@code " p"}, {@code " - "}, …) trimmed off — becomes the sub-folder name.</li>
 *   <li>Otherwise the cluster is split at the first differing character and every part is
 *       processed recursively, so several works sharing a long prefix are separated.</li>
 * </ol>
 *
 * <p>Finally the prefix shared by all groups (the author, e.g. {@code SpicyXGames - }) is
 * removed from every sub-folder name. Groups consisting of a single file are not returned —
 * such files stay in place.
 */
@Slf4j
@Component
public class CommonPrefixGroupingStrategy implements BatchGroupingStrategy {

    /**
     * What is left of a file name inside a group: an optional sequence marker (letters)
     * followed by an index and an optional extension, e.g. {@code 01.jpg}, {@code p1.jpg},
     * {@code a1.jpg}.
     */
    private static final Pattern SEQUENCE_SUFFIX =
            Pattern.compile("[\\s._,\\-]*[A-Za-z]{0,6}\\d+(?:\\.[A-Za-z0-9]+)?");

    /** Shared index digits such as the {@code 0} in {@code p01}/{@code p02}. */
    private static final Pattern TRAILING_DIGITS = Pattern.compile("\\d+$");

    /** Sequence marker such as {@code " p"} that leaked into the common prefix. */
    private static final Pattern TRAILING_ALPHA_MARKER = Pattern.compile("[\\s._,\\-]+[A-Za-z]{1,4}$");

    /** Trailing separators such as {@code " - "} that leaked into the common prefix. */
    private static final Pattern TRAILING_SEPARATORS = Pattern.compile("[\\s._,\\-]+$");

    /** Leading separators left over once a shared prefix has been stripped. */
    private static final Pattern LEADING_SEPARATORS = Pattern.compile("^[\\s._,\\-]+");

    // ---- BatchGroupingStrategy ------------------------------------------

    @Override
    public Map<String, List<Path>> group(List<Path> files) {
        Map<String, List<Path>> result = new LinkedHashMap<>();
        if (files.size() < 2) {
            return result;
        }

        List<Candidate> candidates = files.stream()
                .map(path -> new Candidate(path, path.getFileName().toString()))
                .toList();

        List<CandidateGroup> groups = new ArrayList<>();
        cluster(candidates, groups);
        groups.removeIf(group -> group.members().size() < 2);

        String sharedPrefix = sharedPrefixOf(groups);
        for (CandidateGroup group : groups) {
            String name = stripPrefix(group.name(), sharedPrefix);
            result.computeIfAbsent(name, _ -> new ArrayList<>())
                    .addAll(group.members().stream().map(Candidate::path).toList());
        }

        log.debug("'{}' file(s) -> {} group(s), shared prefix '{}'", files.size(), result.size(), sharedPrefix);
        return result;
    }

    // ---- clustering -----------------------------------------------------

    private void cluster(List<Candidate> candidates, List<CandidateGroup> out) {
        if (candidates.size() == 1) {
            out.add(new CandidateGroup(candidates.getFirst().name(), candidates));
            return;
        }

        String common = longestCommonPrefix(candidates);
        List<String> remainders = candidates.stream()
                .map(candidate -> candidate.name().substring(common.length()))
                .toList();

        if (!remainders.isEmpty() && remainders.stream().allMatch(this::isSequenceSuffix)) {
            out.add(new CandidateGroup(trimSequenceMarker(common), candidates));
            return;
        }

        Map<String, List<Candidate>> parts = new LinkedHashMap<>();
        for (int i = 0; i < candidates.size(); i++) {
            String remainder = remainders.get(i);
            Candidate candidate = candidates.get(i);
            if (remainder.isEmpty()) {
                out.add(new CandidateGroup(common, List.of(candidate)));
            } else {
                parts.computeIfAbsent(remainder.substring(0, 1), _ -> new ArrayList<>()).add(candidate);
            }
        }
        for (List<Candidate> part : parts.values()) {
            cluster(part, out);
        }
    }

    private boolean isSequenceSuffix(String remainder) {
        return SEQUENCE_SUFFIX.matcher(remainder).matches();
    }

    // ---- helpers --------------------------------------------------------

    private String sharedPrefixOf(List<CandidateGroup> groups) {
        List<Candidate> members = groups.stream()
                .flatMap(group -> group.members().stream())
                .toList();
        if (members.size() < 2) {
            return "";
        }
        String prefix = longestCommonPrefix(members);
        boolean shorterThanAll = groups.stream().allMatch(group -> group.name().length() > prefix.length());
        return shorterThanAll ? prefix : "";
    }

    private String stripPrefix(String name, String prefix) {
        String stripped = prefix.isEmpty() ? name : name.substring(prefix.length());
        stripped = LEADING_SEPARATORS.matcher(stripped).replaceFirst("").strip();
        return stripped.isEmpty() ? name : stripped;
    }

    private String trimSequenceMarker(String name) {
        String result = name;
        String withoutDigits = TRAILING_DIGITS.matcher(result).replaceFirst("");
        if (withoutDigits.length() < result.length()) {
            result = withoutDigits;
        }
        result = TRAILING_ALPHA_MARKER.matcher(result).replaceFirst("");
        result = TRAILING_SEPARATORS.matcher(result).replaceFirst("").stripTrailing();
        return result.isBlank() ? name : result;
    }

    private static String longestCommonPrefix(List<Candidate> candidates) {
        String prefix = candidates.getFirst().name();
        for (int i = 1; i < candidates.size() && !prefix.isEmpty(); i++) {
            String value = candidates.get(i).name();
            int length = Math.min(prefix.length(), value.length());
            int index = 0;
            while (index < length && prefix.charAt(index) == value.charAt(index)) {
                index++;
            }
            prefix = prefix.substring(0, index);
        }
        return prefix;
    }

    private record Candidate(Path path, String name) {
    }

    private record CandidateGroup(String name, List<Candidate> members) {
    }
}
