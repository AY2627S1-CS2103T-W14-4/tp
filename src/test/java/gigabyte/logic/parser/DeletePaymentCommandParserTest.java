package gigabyte.logic.parser;

import static gigabyte.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static gigabyte.logic.parser.CommandParserTestUtil.assertParseFailure;
import static gigabyte.logic.parser.CommandParserTestUtil.assertParseSuccess;

import org.junit.jupiter.api.Test;

import gigabyte.commons.core.index.Index;
import gigabyte.logic.commands.DeletePaymentCommand;

public class DeletePaymentCommandParserTest {
    private final DeletePaymentCommandParser parser = new DeletePaymentCommandParser();

    @Test
    public void parse_validArguments_returnsCommand() {
        DeletePaymentCommand expected = new DeletePaymentCommand(Index.fromOneBased(1),
                Index.fromOneBased(2), Index.fromOneBased(3));
        assertParseSuccess(parser, "1 2 3", expected);
        assertParseSuccess(parser, " 1\t  2  3 ", expected);
    }

    @Test
    public void parse_missingOrExtraIndexes_throwsParseException() {
        String expected = String.format(MESSAGE_INVALID_COMMAND_FORMAT, DeletePaymentCommand.MESSAGE_USAGE);
        for (String args : new String[] {"", " ", "1", "1 2", "1 2 3 4", "1 2 3 extra", "1 2 3 a/50"}) {
            assertParseFailure(parser, args, expected);
        }
    }

    @Test
    public void parse_invalidIndexes_throwsParseException() {
        String expected = String.format(MESSAGE_INVALID_COMMAND_FORMAT, DeletePaymentCommand.MESSAGE_USAGE);
        for (String invalidIndex : new String[] {"abc", "0", "-1", "1.5", "2147483648"}) {
            assertParseFailure(parser, invalidIndex + " 1 1", expected);
            assertParseFailure(parser, "1 " + invalidIndex + " 1", expected);
            assertParseFailure(parser, "1 1 " + invalidIndex, expected);
        }
    }
}
