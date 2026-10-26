package gigabyte.logic.parser;

import java.util.List;

import gigabyte.commons.core.index.Index;
import gigabyte.logic.commands.MarkPaymentPaidCommand;
import gigabyte.logic.parser.exceptions.ParseException;

/** Parses the three displayed indexes for {@code markpaid}. */
public class MarkPaymentPaidCommandParser implements Parser<MarkPaymentPaidCommand> {
    @Override
    public MarkPaymentPaidCommand parse(String args) throws ParseException {
        List<Index> indexes = ParserUtil.parseIndexes(args, 3, MarkPaymentPaidCommand.MESSAGE_USAGE);
        return new MarkPaymentPaidCommand(indexes.get(0), indexes.get(1), indexes.get(2));
    }
}
