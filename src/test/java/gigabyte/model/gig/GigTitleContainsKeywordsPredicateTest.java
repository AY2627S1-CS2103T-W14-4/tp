package gigabyte.model.gig;

import static gigabyte.testutil.TypicalClients.ALICE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

public class GigTitleContainsKeywordsPredicateTest {
    private static Gig gig(String title) {
        return new Gig(ALICE, new GigTitle(title), GigStatus.IN_PROGRESS,
                new Deadline("2027-01-01"), new Fee("100"));
    }

    @Test
    public void test_matchesAnyKeywordCaseInsensitively() {
        GigTitleContainsKeywordsPredicate predicate =
                new GigTitleContainsKeywordsPredicate(List.of("WEBSITE", "logo"));

        assertTrue(predicate.test(gig("Website redesign")));
        assertTrue(predicate.test(gig("Brand logo")));
        assertFalse(predicate.test(gig("Mobile application")));
    }

    @Test
    public void test_requiresCompleteWords() {
        GigTitleContainsKeywordsPredicate predicate =
                new GigTitleContainsKeywordsPredicate(List.of("site"));

        assertFalse(predicate.test(gig("Website redesign")));
        assertTrue(new GigTitleContainsKeywordsPredicate(List.of("website")).test(gig("Website redesign")));
    }
}
