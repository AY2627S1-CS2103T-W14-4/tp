package gigabyte.logic.parser;

import static gigabyte.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static gigabyte.logic.parser.CliSyntax.PREFIX_AMOUNT;
import static gigabyte.logic.parser.CliSyntax.PREFIX_DEADLINE;

import java.util.Optional;

import gigabyte.commons.core.index.Index;
import gigabyte.logic.commands.EditPaymentCommand;
import gigabyte.logic.parser.exceptions.ParseException;
import gigabyte.model.gig.Deadline;
import gigabyte.model.gig.Fee;

/** Parses displayed indexes and optional replacement details for {@code editpayment}. */
public class EditPaymentCommandParser implements Parser<EditPaymentCommand> {
    private static final int INDEX_COUNT = 3;

    @Override
    public EditPaymentCommand parse(String args) throws ParseException {
        ArgumentMultimap arguments = ArgumentTokenizer.tokenize(args, PREFIX_AMOUNT, PREFIX_DEADLINE);
        String[] indexes = arguments.getPreamble().split("\\s+");
        if (indexes.length != INDEX_COUNT) {
            throw new ParseException(getInvalidFormatMessage());
        }
        arguments.verifyNoDuplicatePrefixesFor(PREFIX_AMOUNT, PREFIX_DEADLINE);

        Index clientIndex;
        Index gigIndex;
        Index paymentIndex;
        try {
            clientIndex = ParserUtil.parseIndex(indexes[0]);
            gigIndex = ParserUtil.parseIndex(indexes[1]);
            paymentIndex = ParserUtil.parseIndex(indexes[2]);
        } catch (ParseException exception) {
            throw new ParseException(getInvalidFormatMessage(), exception);
        }

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

        return new EditPaymentCommand(clientIndex, gigIndex, paymentIndex, amount, dueDate);
    }

    private static String getInvalidFormatMessage() {
        return String.format(MESSAGE_INVALID_COMMAND_FORMAT, EditPaymentCommand.MESSAGE_USAGE);
    }
}
