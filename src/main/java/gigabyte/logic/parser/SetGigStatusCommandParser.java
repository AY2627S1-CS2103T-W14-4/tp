package gigabyte.logic.parser;

import static gigabyte.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static gigabyte.logic.parser.CliSyntax.PREFIX_STATUS;

import java.util.List;

import gigabyte.commons.core.index.Index;
import gigabyte.logic.commands.SetGigStatusCommand;
import gigabyte.logic.parser.exceptions.ParseException;
import gigabyte.model.gig.GigStatus;

/** Parses client and gig indexes and the requested status. */
public class SetGigStatusCommandParser implements Parser<SetGigStatusCommand> {
    @Override
    public SetGigStatusCommand parse(String args) throws ParseException {
        ArgumentMultimap arguments = ArgumentTokenizer.tokenize(args, PREFIX_STATUS);
        if (arguments.getValue(PREFIX_STATUS).isEmpty() || arguments.getValue(PREFIX_STATUS).get().isBlank()) {
            throw new ParseException(
                    String.format(MESSAGE_INVALID_COMMAND_FORMAT, SetGigStatusCommand.MESSAGE_USAGE));
        }
        List<Index> indexes = ParserUtil.parseIndexes(arguments.getPreamble(), 2, SetGigStatusCommand.MESSAGE_USAGE);
        arguments.verifyNoDuplicatePrefixesFor(PREFIX_STATUS);

        GigStatus status = ParserUtil.parseGigStatus(arguments.getValue(PREFIX_STATUS).get());
        return new SetGigStatusCommand(indexes.get(0), indexes.get(1), status);
    }
}
