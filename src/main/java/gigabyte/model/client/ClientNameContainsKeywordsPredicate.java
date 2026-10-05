package gigabyte.model.client;

import java.util.List;
import java.util.function.Predicate;

import gigabyte.commons.util.StringUtil;
import gigabyte.commons.util.ToStringBuilder;

/**
 * Tests that a {@code Client}'s {@code Name} matches any of the keywords given.
 */
public class ClientNameContainsKeywordsPredicate implements Predicate<Client> {
    private final List<String> keywords;

    public ClientNameContainsKeywordsPredicate(List<String> keywords) {
        this.keywords = keywords;
    }

    @Override
    public boolean test(Client client) {
        return keywords.stream()
                .anyMatch(keyword -> StringUtil.containsWordIgnoreCase(client.getName().fullName, keyword));
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof ClientNameContainsKeywordsPredicate otherClientNameContainsKeywordsPredicate)) {
            return false;
        }

        return keywords.equals(otherClientNameContainsKeywordsPredicate.keywords);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).add("keywords", keywords).toString();
    }
}
