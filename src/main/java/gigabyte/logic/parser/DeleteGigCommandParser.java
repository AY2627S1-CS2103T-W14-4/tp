package gigabyte.logic.parser;

import java.util.List;

import gigabyte.commons.core.index.Index;
import gigabyte.logic.commands.DeleteGigCommand;
import gigabyte.logic.parser.exceptions.ParseException;

/**
 * Parses the displayed client index and per-client gig index for {@code deletegig}.
 */
public class DeleteGigCommandParser implements Parser<DeleteGigCommand> {
    @Override
    public DeleteGigCommand parse(String args) throws ParseException {
        List<Index> indexes = ParserUtil.parseIndexes(args, 2, DeleteGigCommand.MESSAGE_USAGE);
        return new DeleteGigCommand(indexes.get(0), indexes.get(1));
    }
}
