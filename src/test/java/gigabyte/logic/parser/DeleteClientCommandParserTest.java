package gigabyte.logic.parser;

import static gigabyte.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static gigabyte.logic.parser.CommandParserTestUtil.assertParseFailure;
import static gigabyte.logic.parser.CommandParserTestUtil.assertParseSuccess;
import static gigabyte.logic.parser.ParserUtil.MESSAGE_INVALID_INDEX;
import static gigabyte.testutil.TypicalIndexes.INDEX_FIRST_CLIENT;

import org.junit.jupiter.api.Test;

import gigabyte.logic.commands.DeleteClientCommand;

public class DeleteClientCommandParserTest {
    private static final String INVALID_FORMAT =
            String.format(MESSAGE_INVALID_COMMAND_FORMAT, DeleteClientCommand.MESSAGE_USAGE);

    private DeleteClientCommandParser parser = new DeleteClientCommandParser();

    @Test
    public void parse_validArgs_returnsDeleteClientCommand() {
        assertParseSuccess(parser, "1", new DeleteClientCommand(INDEX_FIRST_CLIENT));
        assertParseSuccess(parser, " \t1  ", new DeleteClientCommand(INDEX_FIRST_CLIENT));
    }

    @Test
    public void parse_missingOrExtraIndex_throwsParseException() {
        // EP: no index
        assertParseFailure(parser, "", INVALID_FORMAT);
        assertParseFailure(parser, " ", INVALID_FORMAT);

        // EP: extra argument after the index
        assertParseFailure(parser, "1 2", INVALID_FORMAT);
        assertParseFailure(parser, "1 abc", INVALID_FORMAT);
    }

    @Test
    public void parse_zeroOrNegativeIndex_throwsParseException() {
        assertParseFailure(parser, "0", MESSAGE_INVALID_INDEX); // Boundary value
        assertParseFailure(parser, "-1", MESSAGE_INVALID_INDEX);
    }

    @Test
    public void parse_nonIntegerIndex_throwsParseException() {
        assertParseFailure(parser, "a", MESSAGE_INVALID_INDEX);
        assertParseFailure(parser, "1.5", MESSAGE_INVALID_INDEX);
    }

    @Test
    public void parse_indexAboveIntegerMaxValue_throwsParseException() {
        assertParseFailure(parser, "2147483648", MESSAGE_INVALID_INDEX); // Boundary value: Integer.MAX_VALUE + 1
    }
}
