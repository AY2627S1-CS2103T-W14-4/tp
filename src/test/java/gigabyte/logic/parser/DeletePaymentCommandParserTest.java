package gigabyte.logic.parser;

import static gigabyte.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static gigabyte.logic.parser.CommandParserTestUtil.assertParseFailure;
import static gigabyte.logic.parser.CommandParserTestUtil.assertParseSuccess;
import static gigabyte.logic.parser.ParserUtil.MESSAGE_INVALID_INDEX;

import org.junit.jupiter.api.Test;

import gigabyte.commons.core.index.Index;
import gigabyte.logic.commands.DeletePaymentCommand;

public class DeletePaymentCommandParserTest {
    private static final String INVALID_FORMAT =
            String.format(MESSAGE_INVALID_COMMAND_FORMAT, DeletePaymentCommand.MESSAGE_USAGE);

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
        // EP: fewer than three indexes
        assertParseFailure(parser, "", INVALID_FORMAT);
        assertParseFailure(parser, " ", INVALID_FORMAT);
        assertParseFailure(parser, "1", INVALID_FORMAT);
        assertParseFailure(parser, "1 2", INVALID_FORMAT);

        // EP: extra argument after three indexes
        assertParseFailure(parser, "1 2 3 4", INVALID_FORMAT);
        assertParseFailure(parser, "1 2 3 extra", INVALID_FORMAT);
        assertParseFailure(parser, "1 2 3 a/50", INVALID_FORMAT);
    }

    @Test
    public void parse_zeroOrNegativeIndex_throwsParseException() {
        assertInvalidIndexRejected("0"); // Boundary value
        assertInvalidIndexRejected("-1");
    }

    @Test
    public void parse_nonIntegerIndex_throwsParseException() {
        assertInvalidIndexRejected("abc");
        assertInvalidIndexRejected("1.5");
    }

    @Test
    public void parse_indexAboveIntegerMaxValue_throwsParseException() {
        assertInvalidIndexRejected("2147483648"); // Boundary value: Integer.MAX_VALUE + 1
    }

    /** Asserts that {@code invalidIndex} is rejected as the client, gig, and payment index. */
    private void assertInvalidIndexRejected(String invalidIndex) {
        assertParseFailure(parser, invalidIndex + " 1 1", MESSAGE_INVALID_INDEX);
        assertParseFailure(parser, "1 " + invalidIndex + " 1", MESSAGE_INVALID_INDEX);
        assertParseFailure(parser, "1 1 " + invalidIndex, MESSAGE_INVALID_INDEX);
    }
}
