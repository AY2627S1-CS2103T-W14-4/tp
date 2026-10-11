package gigabyte.logic.parser;

import static gigabyte.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static gigabyte.logic.parser.CliSyntax.PREFIX_DEADLINE;
import static gigabyte.logic.parser.CliSyntax.PREFIX_FEE;
import static gigabyte.logic.parser.CliSyntax.PREFIX_STATUS;
import static gigabyte.logic.parser.CliSyntax.PREFIX_TITLE;

import gigabyte.commons.core.index.Index;
import gigabyte.logic.commands.AddGigCommand;
import gigabyte.logic.parser.exceptions.ParseException;
import gigabyte.model.gig.Deadline;
import gigabyte.model.gig.Fee;
import gigabyte.model.gig.GigStatus;
import gigabyte.model.gig.GigTitle;

/**
 * Parses arguments for creating a gig linked to an existing client.
 */
public class AddGigCommandParser implements Parser<AddGigCommand> {
    @Override
    public AddGigCommand parse(String args) throws ParseException {
        ArgumentMultimap arguments = ArgumentTokenizer.tokenize(args, PREFIX_TITLE, PREFIX_STATUS,
                PREFIX_DEADLINE, PREFIX_FEE);
        if (arguments.getValue(PREFIX_TITLE).isEmpty() || arguments.getValue(PREFIX_STATUS).isEmpty()
                || arguments.getValue(PREFIX_DEADLINE).isEmpty()
                || arguments.getValue(PREFIX_FEE).isEmpty()) {
            throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, AddGigCommand.MESSAGE_USAGE));
        }
        arguments.verifyNoDuplicatePrefixesFor(PREFIX_TITLE, PREFIX_STATUS, PREFIX_DEADLINE, PREFIX_FEE);

        Index clientIndex;
        try {
            clientIndex = ParserUtil.parseIndex(arguments.getPreamble());
        } catch (ParseException exception) {
            throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, AddGigCommand.MESSAGE_USAGE),
                    exception);
        }
        GigTitle title = ParserUtil.parseGigTitle(arguments.getValue(PREFIX_TITLE).get());
        GigStatus status = ParserUtil.parseGigStatus(arguments.getValue(PREFIX_STATUS).get());
        Deadline deadline = ParserUtil.parseDeadline(arguments.getValue(PREFIX_DEADLINE).get());
        Fee fee = ParserUtil.parseFee(arguments.getValue(PREFIX_FEE).get());
        return new AddGigCommand(clientIndex, title, status, deadline, fee);
    }
}
