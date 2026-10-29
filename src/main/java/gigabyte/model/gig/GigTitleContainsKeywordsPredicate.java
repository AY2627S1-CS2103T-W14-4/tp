package gigabyte.model.gig;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Tests whether a gig title contains any supplied keyword as a complete word.
 * Matching ignores case and punctuation at the start or end of a word, so {@code invoice} matches
 * {@code Invoice, overdue}. Punctuation inside a word is kept, so {@code commerce} does not match {@code e-commerce}.
 * Symbols such as {@code +} and {@code $} are not punctuation, so {@code c} does not match {@code C++}.
 */
public class GigTitleContainsKeywordsPredicate implements Predicate<Gig> {
    private static final Pattern EDGE_PUNCTUATION = Pattern.compile("^\\p{P}+|\\p{P}+$");

    private final List<String> keywords;

    /** Creates a predicate that matches gig titles against any supplied keyword. */
    public GigTitleContainsKeywordsPredicate(List<String> keywords) {
        this.keywords = List.copyOf(keywords);
    }

    @Override
    public boolean test(Gig gig) {
        String[] titleWords = gig.getTitle().toString().split("\\s+");
        return keywords.stream().anyMatch(keyword ->
                Arrays.stream(titleWords).anyMatch(titleWord -> isWordMatch(titleWord, keyword)));
    }

    /**
     * Returns true if {@code titleWord} equals {@code keyword}, ignoring case and edge punctuation.
     * A keyword made only of punctuation must match the title word exactly.
     */
    private static boolean isWordMatch(String titleWord, String keyword) {
        if (titleWord.equalsIgnoreCase(keyword)) {
            return true;
        }
        String strippedKeyword = stripEdgePunctuation(keyword);
        return !strippedKeyword.isEmpty() && stripEdgePunctuation(titleWord).equalsIgnoreCase(strippedKeyword);
    }

    private static String stripEdgePunctuation(String word) {
        return EDGE_PUNCTUATION.matcher(word).replaceAll("");
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
