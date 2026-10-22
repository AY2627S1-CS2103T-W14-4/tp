package gigabyte.logic.parser;

import static gigabyte.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static gigabyte.logic.parser.CommandParserTestUtil.assertParseFailure;
import static gigabyte.logic.parser.CommandParserTestUtil.assertParseSuccess;
import static gigabyte.testutil.TypicalIndexes.INDEX_FIRST_CLIENT;
import static gigabyte.testutil.TypicalIndexes.INDEX_SECOND_CLIENT;

import org.junit.jupiter.api.Test;

import gigabyte.commons.core.index.Index;
import gigabyte.logic.commands.MarkPaymentUnpaidCommand;

public class MarkPaymentUnpaidCommandParserTest {
    private final MarkPaymentUnpaidCommandParser parser = new MarkPaymentUnpaidCommandParser();

    @Test
    public void parse_validArguments_returnsCommand() {
        assertParseSuccess(parser, " 1   2  3 ", new MarkPaymentUnpaidCommand(INDEX_FIRST_CLIENT,
                INDEX_SECOND_CLIENT, Index.fromOneBased(3)));
    }

    @Test
    public void parse_missingOrExtraIndexes_throwsParseException() {
        String expected = String.format(MESSAGE_INVALID_COMMAND_FORMAT, MarkPaymentUnpaidCommand.MESSAGE_USAGE);
        for (String args : new String[] {"", "1", "1 2", "1 2 3 4", "1 2 3 extra"}) {
            assertParseFailure(parser, args, expected);
        }
    }

    @Test
    public void parse_invalidIndexes_throwsParseException() {
        String expected = String.format(MESSAGE_INVALID_COMMAND_FORMAT, MarkPaymentUnpaidCommand.MESSAGE_USAGE);
        for (String invalidIndex : new String[] {"abc", "0", "-1", "1.5", "2147483648"}) {
            assertParseFailure(parser, invalidIndex + " 1 1", expected);
            assertParseFailure(parser, "1 " + invalidIndex + " 1", expected);
            assertParseFailure(parser, "1 1 " + invalidIndex, expected);
        }
    }
}
