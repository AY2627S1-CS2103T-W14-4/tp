package gigabyte.logic.parser;

import gigabyte.commons.core.index.Index;
import gigabyte.logic.commands.DeleteClientCommand;
import gigabyte.logic.parser.exceptions.ParseException;

/**
 * Parses input arguments and creates a new DeleteClientCommand object
 */
public class DeleteClientCommandParser implements Parser<DeleteClientCommand> {

    /**
     * Parses the given {@code String} of arguments in the context of the DeleteClientCommand
     * and returns a DeleteClientCommand object for execution.
     * @throws ParseException if the user input does not conform to the expected format
     */
    public DeleteClientCommand parse(String args) throws ParseException {
        Index index = ParserUtil.parseIndexes(args, 1, DeleteClientCommand.MESSAGE_USAGE).get(0);
        return new DeleteClientCommand(index);
    }

}
