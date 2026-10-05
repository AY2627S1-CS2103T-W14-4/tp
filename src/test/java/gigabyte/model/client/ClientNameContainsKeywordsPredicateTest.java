package gigabyte.model.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import gigabyte.testutil.ClientBuilder;

public class ClientNameContainsKeywordsPredicateTest {

    @Test
    public void equals() {
        List<String> firstPredicateKeywordList = List.of("first");
        List<String> secondPredicateKeywordList = List.of("first", "second");

        ClientNameContainsKeywordsPredicate firstPredicate =
                new ClientNameContainsKeywordsPredicate(firstPredicateKeywordList);
        ClientNameContainsKeywordsPredicate secondPredicate =
                new ClientNameContainsKeywordsPredicate(secondPredicateKeywordList);

        // same object -> returns true
        assertTrue(firstPredicate.equals(firstPredicate));

        // same values -> returns true
        ClientNameContainsKeywordsPredicate firstPredicateCopy =
                new ClientNameContainsKeywordsPredicate(firstPredicateKeywordList);
        assertTrue(firstPredicate.equals(firstPredicateCopy));

        // different types -> returns false
        assertFalse(firstPredicate.equals(1));

        // null -> returns false
        assertFalse(firstPredicate.equals(null));

        // different client -> returns false
        assertFalse(firstPredicate.equals(secondPredicate));
    }

    @Test
    public void test_nameContainsKeywords_returnsTrue() {
        // One keyword
        ClientNameContainsKeywordsPredicate predicate = new ClientNameContainsKeywordsPredicate(List.of("Alice"));
        assertTrue(predicate.test(new ClientBuilder().withName("Alice Bob").build()));

        // Multiple keywords
        predicate = new ClientNameContainsKeywordsPredicate(List.of("Alice", "Bob"));
        assertTrue(predicate.test(new ClientBuilder().withName("Alice Bob").build()));

        // Only one matching keyword
        predicate = new ClientNameContainsKeywordsPredicate(List.of("Bob", "Carol"));
        assertTrue(predicate.test(new ClientBuilder().withName("Alice Carol").build()));

        // Mixed-case keywords
        predicate = new ClientNameContainsKeywordsPredicate(List.of("aLIce", "bOB"));
        assertTrue(predicate.test(new ClientBuilder().withName("Alice Bob").build()));
    }

    @Test
    public void test_nameDoesNotContainKeywords_returnsFalse() {
        // Zero keywords
        ClientNameContainsKeywordsPredicate predicate = new ClientNameContainsKeywordsPredicate(List.of());
        assertFalse(predicate.test(new ClientBuilder().withName("Alice").build()));

        // Non-matching keyword
        predicate = new ClientNameContainsKeywordsPredicate(List.of("Carol"));
        assertFalse(predicate.test(new ClientBuilder().withName("Alice Bob").build()));

        // Keywords match phone, email and address, but do not match name
        predicate = new ClientNameContainsKeywordsPredicate(List.of("12345", "alice@email.com", "Main", "Street"));
        assertFalse(predicate.test(new ClientBuilder().withName("Alice").withPhone("12345")
                .withEmail("alice@email.com").withAddress("Main Street").build()));
    }

    @Test
    public void toStringMethod() {
        List<String> keywords = List.of("keyword1", "keyword2");
        ClientNameContainsKeywordsPredicate predicate = new ClientNameContainsKeywordsPredicate(keywords);

        String expected = ClientNameContainsKeywordsPredicate.class.getCanonicalName() + "{keywords=" + keywords + "}";
        assertEquals(expected, predicate.toString());
    }
}
