package gigabyte.logic.parser;

import static gigabyte.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;

import gigabyte.commons.core.index.Index;
import gigabyte.logic.commands.MarkPaymentUnpaidCommand;
import gigabyte.logic.parser.exceptions.ParseException;

/** Parses the three displayed indexes for {@code markunpaid}. */
public class MarkPaymentUnpaidCommandParser implements Parser<MarkPaymentUnpaidCommand> {
    private static final int INDEX_COUNT = 3;

    @Override
    public MarkPaymentUnpaidCommand parse(String args) throws ParseException {
        String trimmedArgs = args.trim();
        String[] indexes = trimmedArgs.isEmpty() ? new String[0] : trimmedArgs.split("\\s+");
        if (indexes.length != INDEX_COUNT) {
            throw new ParseException(getInvalidFormatMessage());
        }

        try {
            Index clientIndex = ParserUtil.parseIndex(indexes[0]);
            Index gigIndex = ParserUtil.parseIndex(indexes[1]);
            Index paymentIndex = ParserUtil.parseIndex(indexes[2]);
            return new MarkPaymentUnpaidCommand(clientIndex, gigIndex, paymentIndex);
        } catch (ParseException exception) {
            throw new ParseException(getInvalidFormatMessage(), exception);
        }
    }

    private static String getInvalidFormatMessage() {
        return String.format(MESSAGE_INVALID_COMMAND_FORMAT, MarkPaymentUnpaidCommand.MESSAGE_USAGE);
    }
}
