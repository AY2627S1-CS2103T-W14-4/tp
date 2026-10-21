package gigabyte.model.gig;

import java.util.List;
import java.util.function.Predicate;

import gigabyte.commons.util.StringUtil;

/** Tests whether a gig title contains any supplied keyword as a complete word. */
public class GigTitleContainsKeywordsPredicate implements Predicate<Gig> {
    private final List<String> keywords;

    public GigTitleContainsKeywordsPredicate(List<String> keywords) {
        this.keywords = List.copyOf(keywords);
    }

    @Override
    public boolean test(Gig gig) {
        return keywords.stream().anyMatch(keyword ->
                StringUtil.containsWordIgnoreCase(gig.getTitle().toString(), keyword));
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof GigTitleContainsKeywordsPredicate predicate
                && keywords.equals(predicate.keywords);
    }

    @Override
    public int hashCode() {
        return keywords.hashCode();
    }

    @Override
    public String toString() {
        return getClass().getCanonicalName() + "{keywords=" + keywords + "}";
    }
}
