package ru.seranov.fslibutils.grouping;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SeriesTitleGroupingStrategyTest {

    private final SeriesTitleGroupingStrategy strategy = new SeriesTitleGroupingStrategy();

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
        "SpicyXGames - 5000 Follower Picnic (135432291) p01.jpg,       5000 Follower Picnic (135432291)",
        "SpicyXGames - 5000 Follower Picnic (135432291) p12.jpg,       5000 Follower Picnic (135432291)",
        "SpicyXGames - Uncle sent Emily something.. (132481276) p1.jpg,  Uncle sent Emily something.. (132481276)",
        "SpicyXGames - Uncle sent Emily something.. (132481276) p3.jpg,  Uncle sent Emily something.. (132481276)",
    })
    void shouldExtractGroupFromFilename(String filename, String expectedGroup) {
        Path file = Path.of(filename.trim());
        Optional<String> result = strategy.resolveGroup(file);
        assertTrue(result.isPresent(), "Expected a group for: " + filename);
        assertEquals(expectedGroup.trim(), result.get());
    }

    @Test
    void shouldReturnEmptyForFilenameWithoutIdPattern() {
        Path file = Path.of("plain-file-no-id.jpg");
        Optional<String> result = strategy.resolveGroup(file);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEmptyForFilenameWithoutSeparator() {
        Path file = Path.of("NoSeparatorHere (12345) p01.jpg");
        Optional<String> result = strategy.resolveGroup(file);
        assertTrue(result.isEmpty(), "File without author separator should not match");
    }

    @Test
    void shouldGroupAllFilesInSameSeriesTogether() {
        SeriesTitleGroupingStrategy s = new SeriesTitleGroupingStrategy();
        String base = "Author - Series Title (99999) p";
        String group1 = s.resolveGroup(Path.of(base + "01.jpg")).orElseThrow();
        String group2 = s.resolveGroup(Path.of(base + "02.jpg")).orElseThrow();
        assertEquals(group1, group2);
    }
}
