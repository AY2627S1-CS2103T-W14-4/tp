package gigabyte.logic.parser;

import java.util.List;

import gigabyte.commons.core.index.Index;
import gigabyte.logic.commands.MarkPaymentUnpaidCommand;
import gigabyte.logic.parser.exceptions.ParseException;

/** Parses the three displayed indexes for {@code markunpaid}. */
public class MarkPaymentUnpaidCommandParser implements Parser<MarkPaymentUnpaidCommand> {
    @Override
    public MarkPaymentUnpaidCommand parse(String args) throws ParseException {
        List<Index> indexes = ParserUtil.parseIndexes(args, 3, MarkPaymentUnpaidCommand.MESSAGE_USAGE);
        return new MarkPaymentUnpaidCommand(indexes.get(0), indexes.get(1), indexes.get(2));
    }
}
