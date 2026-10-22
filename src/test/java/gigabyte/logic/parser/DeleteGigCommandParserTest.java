package gigabyte.logic.parser;

import static gigabyte.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static gigabyte.logic.parser.CommandParserTestUtil.assertParseFailure;
import static gigabyte.logic.parser.CommandParserTestUtil.assertParseSuccess;

import org.junit.jupiter.api.Test;

import gigabyte.commons.core.index.Index;
import gigabyte.logic.commands.DeleteGigCommand;

public class DeleteGigCommandParserTest {
    private final DeleteGigCommandParser parser = new DeleteGigCommandParser();

    @Test
    public void parse_twoPositiveIndexes_returnsCommand() {
        DeleteGigCommand command = new DeleteGigCommand(Index.fromOneBased(1), Index.fromOneBased(2));
        assertParseSuccess(parser, "1 2", command);
        assertParseSuccess(parser, " \t1  \t2  ", command);
    }

    @Test
    public void parse_invalidInput_rejectsMissingExtraAndInvalidIndexes() {
        String error = String.format(MESSAGE_INVALID_COMMAND_FORMAT, DeleteGigCommand.MESSAGE_USAGE);
        for (String args : new String[] {"", " ", "1", "1 2 3", "1 2 s/COMPLETED"}) {
            assertParseFailure(parser, args, error);
        }
        for (String index : new String[] {"0", "-1", "abc", "1.5", "2147483648"}) {
            assertParseFailure(parser, index + " 1", error);
            assertParseFailure(parser, "1 " + index, error);
        }
    }
}
