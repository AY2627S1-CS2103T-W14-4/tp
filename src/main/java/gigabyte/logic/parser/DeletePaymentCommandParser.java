package gigabyte.logic.parser;

import java.util.List;

import gigabyte.commons.core.index.Index;
import gigabyte.logic.commands.DeletePaymentCommand;
import gigabyte.logic.parser.exceptions.ParseException;

/** Parses the three displayed indexes for {@code deletepayment}. */
public class DeletePaymentCommandParser implements Parser<DeletePaymentCommand> {
    @Override
    public DeletePaymentCommand parse(String args) throws ParseException {
        List<Index> indexes = ParserUtil.parseIndexes(args, 3, DeletePaymentCommand.MESSAGE_USAGE);
        return new DeletePaymentCommand(indexes.get(0), indexes.get(1), indexes.get(2));
    }
}
