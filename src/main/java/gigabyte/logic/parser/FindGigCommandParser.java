package gigabyte.logic.parser;

import static gigabyte.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;

import java.util.Arrays;

import gigabyte.logic.commands.FindGigCommand;
import gigabyte.logic.parser.exceptions.ParseException;
import gigabyte.model.gig.GigTitleContainsKeywordsPredicate;

/** Parses arguments for {@code findgig}. */
public class FindGigCommandParser implements Parser<FindGigCommand> {
    @Override
    public FindGigCommand parse(String args) throws ParseException {
        String trimmed = args.trim();
        if (trimmed.isEmpty()) {
            throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, FindGigCommand.MESSAGE_USAGE));
        }
        return new FindGigCommand(new GigTitleContainsKeywordsPredicate(Arrays.asList(trimmed.split("\\s+"))));
    }
}
