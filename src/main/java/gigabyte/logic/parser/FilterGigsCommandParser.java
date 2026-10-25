package gigabyte.logic.parser;

import static gigabyte.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static gigabyte.logic.parser.CliSyntax.PREFIX_STATUS;

import gigabyte.logic.commands.FilterGigsCommand;
import gigabyte.logic.parser.exceptions.ParseException;

/**
 * Parses a single required status for filtering gigs across all clients.
 */
public class FilterGigsCommandParser implements Parser<FilterGigsCommand> {
    @Override
    public FilterGigsCommand parse(String args) throws ParseException {
        ArgumentMultimap arguments = ArgumentTokenizer.tokenize(args, PREFIX_STATUS);
        if (!arguments.getPreamble().isEmpty() || arguments.getValue(PREFIX_STATUS).isEmpty()) {
            throw new ParseException(
                    String.format(MESSAGE_INVALID_COMMAND_FORMAT, FilterGigsCommand.MESSAGE_USAGE));
        }
        arguments.verifyNoDuplicatePrefixesFor(PREFIX_STATUS);
        return new FilterGigsCommand(ParserUtil.parseGigStatus(arguments.getValue(PREFIX_STATUS).get()));
    }
}
