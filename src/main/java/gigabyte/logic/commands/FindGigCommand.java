package gigabyte.logic.commands;

import static java.util.Objects.requireNonNull;

import gigabyte.commons.util.ToStringBuilder;
import gigabyte.model.Model;
import gigabyte.model.gig.GigTitleContainsKeywordsPredicate;

/** Finds gigs whose titles contain any supplied keyword. */
public class FindGigCommand extends Command {
    public static final String COMMAND_WORD = "findgig";
    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Finds gigs whose titles contain any of the specified "
            + "keywords (case-insensitive, complete words).\n"
            + "Parameters: KEYWORD [MORE_KEYWORDS]...";
    public static final String MESSAGE_NO_MATCHES = "No gigs match the supplied title keywords.";

    private final GigTitleContainsKeywordsPredicate predicate;

    public FindGigCommand(GigTitleContainsKeywordsPredicate predicate) {
        this.predicate = requireNonNull(predicate);
    }

    @Override
    public CommandResult execute(Model model) {
        requireNonNull(model);
        long count = model.getGigList().stream().filter(predicate).count();
        String feedback = count == 0 ? MESSAGE_NO_MATCHES : count + " gig(s) listed!";
        return CommandResult.createGigTitleSearchResult(feedback, predicate);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof FindGigCommand command && predicate.equals(command.predicate);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).add("predicate", predicate).toString();
    }
}
