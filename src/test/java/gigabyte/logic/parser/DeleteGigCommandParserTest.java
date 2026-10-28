package gigabyte.logic.parser;

import static gigabyte.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static gigabyte.logic.parser.CommandParserTestUtil.assertParseFailure;
import static gigabyte.logic.parser.CommandParserTestUtil.assertParseSuccess;
import static gigabyte.logic.parser.ParserUtil.MESSAGE_INVALID_INDEX;

import org.junit.jupiter.api.Test;

import gigabyte.commons.core.index.Index;
import gigabyte.logic.commands.DeleteGigCommand;

public class DeleteGigCommandParserTest {
    private static final String INVALID_FORMAT =
            String.format(MESSAGE_INVALID_COMMAND_FORMAT, DeleteGigCommand.MESSAGE_USAGE);

    private final DeleteGigCommandParser parser = new DeleteGigCommandParser();

    @Test
    public void parse_twoPositiveIndexes_returnsCommand() {
        DeleteGigCommand command = new DeleteGigCommand(Index.fromOneBased(1), Index.fromOneBased(2));
        assertParseSuccess(parser, "1 2", command);
        assertParseSuccess(parser, " \t1  \t2  ", command);
    }

    @Test
    public void parse_wrongNumberOfArguments_throwsParseException() {
        // EP: fewer than two indexes
        assertParseFailure(parser, "", INVALID_FORMAT);
        assertParseFailure(parser, " ", INVALID_FORMAT);
        assertParseFailure(parser, "1", INVALID_FORMAT);

        // EP: extra argument after two indexes
        assertParseFailure(parser, "1 2 3", INVALID_FORMAT);
        assertParseFailure(parser, "1 2 s/COMPLETED", INVALID_FORMAT);
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

    /** Asserts that {@code invalidIndex} is rejected as both the client index and the gig index. */
    private void assertInvalidIndexRejected(String invalidIndex) {
        assertParseFailure(parser, invalidIndex + " 1", MESSAGE_INVALID_INDEX);
        assertParseFailure(parser, "1 " + invalidIndex, MESSAGE_INVALID_INDEX);
    }
}
