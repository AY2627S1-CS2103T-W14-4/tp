package gigabyte.logic.parser;

import static gigabyte.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static gigabyte.logic.parser.CliSyntax.PREFIX_AMOUNT;
import static gigabyte.logic.parser.CliSyntax.PREFIX_DEADLINE;

import java.util.List;

import gigabyte.commons.core.index.Index;
import gigabyte.logic.commands.AddPaymentCommand;
import gigabyte.logic.parser.exceptions.ParseException;
import gigabyte.model.gig.Deadline;
import gigabyte.model.gig.Fee;

/**
 * Parses a client's gig indexes, amount, and due date for a payment obligation.
 */
public class AddPaymentCommandParser implements Parser<AddPaymentCommand> {
    @Override
    public AddPaymentCommand parse(String args) throws ParseException {
        ArgumentMultimap arguments = ArgumentTokenizer.tokenize(args, PREFIX_AMOUNT, PREFIX_DEADLINE);
        if (arguments.getValue(PREFIX_AMOUNT).isEmpty() || arguments.getValue(PREFIX_DEADLINE).isEmpty()) {
            throw new ParseException(
                    String.format(MESSAGE_INVALID_COMMAND_FORMAT, AddPaymentCommand.MESSAGE_USAGE));
        }
        List<Index> indexes = ParserUtil.parseIndexes(arguments.getPreamble(), 2, AddPaymentCommand.MESSAGE_USAGE);
        arguments.verifyNoDuplicatePrefixesFor(PREFIX_AMOUNT, PREFIX_DEADLINE);

        Fee amount = ParserUtil.parseFee(arguments.getValue(PREFIX_AMOUNT).get());
        Deadline dueDate = ParserUtil.parseDeadline(arguments.getValue(PREFIX_DEADLINE).get());
        return new AddPaymentCommand(indexes.get(0), indexes.get(1), amount, dueDate);
    }
}
