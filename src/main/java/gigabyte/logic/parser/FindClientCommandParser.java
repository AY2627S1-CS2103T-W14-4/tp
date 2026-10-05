package gigabyte.logic.parser;

import static gigabyte.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;

import java.util.List;

import gigabyte.logic.commands.FindClientCommand;
import gigabyte.logic.parser.exceptions.ParseException;
import gigabyte.model.client.ClientNameContainsKeywordsPredicate;

/**
 * Parses input arguments and creates a new FindClientCommand object
 */
public class FindClientCommandParser implements Parser<FindClientCommand> {

    /**
     * Parses the given {@code String} of arguments in the context of the FindClientCommand
     * and returns a FindClientCommand object for execution.
     * @throws ParseException if the user input does not conform to the expected format
     */
    public FindClientCommand parse(String args) throws ParseException {
        String trimmedArgs = args.trim();
        if (trimmedArgs.isEmpty()) {
            throw new ParseException(
                    String.format(MESSAGE_INVALID_COMMAND_FORMAT, FindClientCommand.MESSAGE_USAGE));
        }

        String[] nameKeywords = trimmedArgs.split("\\s+");

        return new FindClientCommand(new ClientNameContainsKeywordsPredicate(List.of(nameKeywords)));
    }

}
