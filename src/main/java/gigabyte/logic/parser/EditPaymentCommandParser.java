package gigabyte.logic.parser;

import static gigabyte.logic.parser.CliSyntax.PREFIX_AMOUNT;
import static gigabyte.logic.parser.CliSyntax.PREFIX_DEADLINE;

import java.util.List;
import java.util.Optional;

import gigabyte.commons.core.index.Index;
import gigabyte.logic.commands.EditPaymentCommand;
import gigabyte.logic.parser.exceptions.ParseException;
import gigabyte.model.gig.Deadline;
import gigabyte.model.gig.Fee;

/** Parses displayed indexes and optional replacement details for {@code editpayment}. */
public class EditPaymentCommandParser implements Parser<EditPaymentCommand> {
    @Override
    public EditPaymentCommand parse(String args) throws ParseException {
        ArgumentMultimap arguments = ArgumentTokenizer.tokenize(args, PREFIX_AMOUNT, PREFIX_DEADLINE);
        List<Index> indexes = ParserUtil.parseIndexes(arguments.getPreamble(), 3, EditPaymentCommand.MESSAGE_USAGE);
        arguments.verifyNoDuplicatePrefixesFor(PREFIX_AMOUNT, PREFIX_DEADLINE);

        Optional<Fee> amount = Optional.empty();
        if (arguments.getValue(PREFIX_AMOUNT).isPresent()) {
            amount = Optional.of(ParserUtil.parseFee(arguments.getValue(PREFIX_AMOUNT).get()));
        }
        Optional<Deadline> dueDate = Optional.empty();
        if (arguments.getValue(PREFIX_DEADLINE).isPresent()) {
            dueDate = Optional.of(ParserUtil.parseDeadline(arguments.getValue(PREFIX_DEADLINE).get()));
        }
        if (amount.isEmpty() && dueDate.isEmpty()) {
            throw new ParseException(EditPaymentCommand.MESSAGE_NOT_EDITED);
        }

        return new EditPaymentCommand(indexes.get(0), indexes.get(1), indexes.get(2), amount, dueDate);
    }
}
