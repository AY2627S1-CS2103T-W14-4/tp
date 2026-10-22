package gigabyte.logic.parser;

import static gigabyte.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;

import gigabyte.logic.commands.DeleteGigCommand;
import gigabyte.logic.parser.exceptions.ParseException;

/**
 * Parses the displayed client index and per-client gig index for {@code deletegig}.
 */
public class DeleteGigCommandParser implements Parser<DeleteGigCommand> {
    @Override
    public DeleteGigCommand parse(String args) throws ParseException {
        String[] indexes = args.trim().split("\\s+");
        String error = String.format(MESSAGE_INVALID_COMMAND_FORMAT, DeleteGigCommand.MESSAGE_USAGE);
        if (indexes.length != 2) {
            throw new ParseException(error);
        }
        try {
            return new DeleteGigCommand(ParserUtil.parseIndex(indexes[0]), ParserUtil.parseIndex(indexes[1]));
        } catch (ParseException exception) {
            throw new ParseException(error, exception);
        }
    }
}
