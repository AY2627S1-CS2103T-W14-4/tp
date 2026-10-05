package gigabyte.logic.parser;

import static gigabyte.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static gigabyte.logic.parser.CommandParserTestUtil.assertParseFailure;
import static gigabyte.logic.parser.CommandParserTestUtil.assertParseSuccess;

import java.util.List;

import org.junit.jupiter.api.Test;

import gigabyte.logic.commands.FindClientCommand;
import gigabyte.model.client.ClientNameContainsKeywordsPredicate;

public class FindClientCommandParserTest {

    private FindClientCommandParser parser = new FindClientCommandParser();

    @Test
    public void parse_emptyArg_throwsParseException() {
        assertParseFailure(parser, "     ",
                String.format(MESSAGE_INVALID_COMMAND_FORMAT, FindClientCommand.MESSAGE_USAGE));
    }

    @Test
    public void parse_validArgs_returnsFindClientCommand() {
        // no leading and trailing whitespaces
        FindClientCommand expectedFindClientCommand =
                new FindClientCommand(new ClientNameContainsKeywordsPredicate(List.of("Alice", "Bob")));
        assertParseSuccess(parser, "Alice Bob", expectedFindClientCommand);

        // multiple whitespaces between keywords
        assertParseSuccess(parser, " \n Alice \n \t Bob  \t", expectedFindClientCommand);
    }

}
