package gigabyte.model.gig;

import static gigabyte.testutil.TypicalClients.ALICE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

public class GigTitleContainsKeywordsPredicateTest {
    private static Gig createGig(String title) {
        return new Gig(ALICE, new GigTitle(title), GigStatus.IN_PROGRESS,
                new Deadline("2027-01-01"), new Fee("100"));
    }

    @Test
    public void test_matchesAnyKeywordCaseInsensitively() {
        GigTitleContainsKeywordsPredicate predicate =
                new GigTitleContainsKeywordsPredicate(List.of("WEBSITE", "logo"));

        // EP: title contains one keyword in a different case
        assertTrue(predicate.test(createGig("Website redesign")));
        assertTrue(predicate.test(createGig("Brand logo")));

        // EP: title contains no keywords
        assertFalse(predicate.test(createGig("Mobile application")));
    }

    @Test
    public void test_requiresCompleteWords() {
        GigTitleContainsKeywordsPredicate predicate =
                new GigTitleContainsKeywordsPredicate(List.of("site"));

        // EP: keyword is only part of a word in the title
        assertFalse(predicate.test(createGig("Website redesign")));

        // EP: keyword matches a whole word in the title
        assertTrue(new GigTitleContainsKeywordsPredicate(List.of("website")).test(createGig("Website redesign")));
    }

    @Test
    public void test_ignoresPunctuationAtWordEdges() {
        GigTitleContainsKeywordsPredicate predicate =
                new GigTitleContainsKeywordsPredicate(List.of("invoice"));

        // EP: title word ends with punctuation
        assertTrue(predicate.test(createGig("Invoice, overdue")));

        // EP: title word is wrapped in punctuation
        assertTrue(predicate.test(createGig("(Invoice) follow-up")));
        assertTrue(predicate.test(createGig("\"invoice!\"")));

        // EP: keyword has the same punctuation as the title word
        assertTrue(new GigTitleContainsKeywordsPredicate(List.of("invoice,")).test(createGig("Invoice, overdue")));

        // EP: keyword has punctuation that the title word does not
        assertTrue(new GigTitleContainsKeywordsPredicate(List.of("invoice?")).test(createGig("Invoice overdue")));
    }

    @Test
    public void test_keepsPunctuationInsideWords() {
        // EP: keyword is only part of a hyphenated title word
        assertFalse(new GigTitleContainsKeywordsPredicate(List.of("commerce")).test(createGig("E-commerce site")));
        assertFalse(new GigTitleContainsKeywordsPredicate(List.of("up")).test(createGig("Invoice follow-up.")));

        // EP: keyword is the whole hyphenated title word
        assertTrue(new GigTitleContainsKeywordsPredicate(List.of("e-commerce")).test(createGig("E-commerce site")));
        assertTrue(new GigTitleContainsKeywordsPredicate(List.of("follow-up")).test(createGig("Invoice follow-up.")));
    }

    @Test
    public void test_keepsSymbolsAndMarksAtWordEdges() {
        // EP: title word ends with symbols that are not punctuation
        assertFalse(new GigTitleContainsKeywordsPredicate(List.of("c")).test(createGig("C++ migration")));
        assertTrue(new GigTitleContainsKeywordsPredicate(List.of("c++")).test(createGig("C++ migration")));

        // EP: title word starts with a currency symbol
        assertFalse(new GigTitleContainsKeywordsPredicate(List.of("100")).test(createGig("$100 logo")));
        assertTrue(new GigTitleContainsKeywordsPredicate(List.of("$100")).test(createGig("$100, logo")));

        // EP: title word ends with a combining accent mark
        assertFalse(new GigTitleContainsKeywordsPredicate(List.of("cafe")).test(createGig("Café menu")));
    }

    @Test
    public void test_punctuationOnlyKeyword_requiresExactWord() {
        GigTitleContainsKeywordsPredicate predicate =
                new GigTitleContainsKeywordsPredicate(List.of("-"));

        // EP: title contains the same punctuation-only word
        assertTrue(predicate.test(createGig("Design - phase 2")));

        // EP: punctuation-only keyword does not match every word
        assertFalse(predicate.test(createGig("Logo, draft")));
    }
}
