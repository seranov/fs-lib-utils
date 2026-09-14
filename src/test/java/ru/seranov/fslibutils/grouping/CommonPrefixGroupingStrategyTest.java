package ru.seranov.fslibutils.grouping;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommonPrefixGroupingStrategyTest {

    private final CommonPrefixGroupingStrategy strategy = new CommonPrefixGroupingStrategy();

    @Test
    void shouldGroupCorpusWithMixedSequenceFormats() {
        List<Path> files = List.of(
                Path.of("SpicyXGames - 5,000 Follower Picnic (135432291) p01.jpg"),
                Path.of("SpicyXGames - 5,000 Follower Picnic (135432291) p02.jpg"),
                Path.of("SpicyXGames - 5,000 Follower Picnic (135432291) p03.jpg"),
                Path.of("SpicyXGames - 5,000 Follower Picnic (135432291) p04.jpg"),
                Path.of("SpicyXGames - 5,000 Follower Picnic (135432291) p05.jpg"),
                Path.of("SpicyXGames - 5,000 Follower Picnic (135432291) p06.jpg"),
                Path.of("SpicyXGames - 5,000 Follower Picnic (135432291) p07.jpg"),
                Path.of("SpicyXGames - 5,000 Follower Picnic (135432291) p08.jpg"),
                Path.of("SpicyXGames - 5,000 Follower Picnic (135432291) p09.jpg"),
                Path.of("SpicyXGames - 5,000 Follower Picnic (135432291) p10.jpg"),
                Path.of("SpicyXGames - 5,000 Follower Picnic (135432291) p11.jpg"),
                Path.of("SpicyXGames - 5,000 Follower Picnic (135432291) p12.jpg"),
                Path.of("SpicyXGames - Preview - Becky Blackbell (2021) - 1.jpg"),
                Path.of("SpicyXGames - Preview - Becky Blackbell (2021) - 2.jpg"),
                Path.of("SpicyXGames - Preview - Becky Blackbell (2021) - 3.jpg"),
                Path.of("SpicyXGames - Preview - Becky Blackbell (2021) - 4.jpg"),
                Path.of("SpicyXGames - Preview - Becky Blackbell (2021) - 5.jpg"),
                Path.of("SpicyXGames - Preview - Becky Blackbell (2021) - 6.jpg"),
                Path.of("SpicyXGames - Preview - Becky Blackbell (2021) - a1.jpg"),
                Path.of("SpicyXGames - Preview - Becky with Anya's Dad p1.jpg"),
                Path.of("SpicyXGames - Preview - Becky with Anya's Dad p2.jpg"),
                Path.of("SpicyXGames - Preview - Becky with Anya's Dad p3.jpg"),
                Path.of("SpicyXGames - Preview - Becky with Anya's Dad p4.jpg"),
                Path.of("SpicyXGames - Uncle sent Emily something.. (132481276) p1.jpg"),
                Path.of("SpicyXGames - Uncle sent Emily something.. (132481276) p2.jpg"),
                Path.of("SpicyXGames - Uncle sent Emily something.. (132481276) p3.jpg"));

        Map<String, List<Path>> groups = strategy.group(files);

        assertEquals(4, groups.size(), () -> "Unexpected groups: " + groups.keySet());
        assertEquals(12, groups.get("5,000 Follower Picnic (135432291)").size());
        assertEquals(7, groups.get("Preview - Becky Blackbell (2021)").size());
        assertEquals(4, groups.get("Preview - Becky with Anya's Dad").size());
        assertEquals(3, groups.get("Uncle sent Emily something.. (132481276)").size());
    }

    @Test
    void shouldSeparateWorksSharingTheSameAuthor() {
        List<Path> files = List.of(
                Path.of("Author - Alpha (1) p1.jpg"),
                Path.of("Author - Alpha (1) p2.jpg"),
                Path.of("Author - Beta (2) p1.jpg"),
                Path.of("Author - Beta (2) p2.jpg"));

        Map<String, List<Path>> groups = strategy.group(files);

        assertEquals(2, groups.size(), () -> "Unexpected groups: " + groups.keySet());
        assertEquals(2, groups.get("Alpha (1)").size());
        assertEquals(2, groups.get("Beta (2)").size());
    }

    @Test
    void shouldKeepAuthorPrefixWhenOnlyOneWorkPresent() {
        List<Path> files = List.of(
                Path.of("SpicyXGames - 5,000 Follower Picnic (135432291) p01.jpg"),
                Path.of("SpicyXGames - 5,000 Follower Picnic (135432291) p02.jpg"));

        Map<String, List<Path>> groups = strategy.group(files);

        assertEquals(1, groups.size());
        assertEquals(2, groups.get("SpicyXGames - 5,000 Follower Picnic (135432291)").size());
    }

    @Test
    void shouldGroupWithoutAnyIdentifierOrSeparatorConvention() {
        List<Path> files = List.of(
                Path.of("Author - Title p01.jpg"),
                Path.of("Author - Title p02.jpg"));

        Map<String, List<Path>> groups = strategy.group(files);

        assertEquals(1, groups.size());
        assertEquals(2, groups.get("Author - Title").size());
    }

    @Test
    void shouldIgnoreUnrelatedSingleFileWorks() {
        List<Path> files = List.of(
                Path.of("Alpha (1) p1.jpg"),
                Path.of("Beta (2) p2.jpg"));

        Map<String, List<Path>> groups = strategy.group(files);

        assertTrue(groups.isEmpty(), () -> "Unrelated single-file works must not form groups: " + groups.keySet());
    }

    @Test
    void shouldReturnEmptyForLessThanTwoFiles() {
        assertTrue(strategy.group(List.of(Path.of("Author - Solo (1) p1.jpg"))).isEmpty());
    }
}
