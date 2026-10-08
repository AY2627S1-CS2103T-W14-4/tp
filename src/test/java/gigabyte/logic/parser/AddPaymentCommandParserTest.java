package gigabyte.logic.parser;

import static gigabyte.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static gigabyte.logic.parser.CliSyntax.PREFIX_AMOUNT;
import static gigabyte.logic.parser.CliSyntax.PREFIX_DEADLINE;
import static gigabyte.logic.parser.CommandParserTestUtil.assertParseFailure;
import static gigabyte.logic.parser.CommandParserTestUtil.assertParseSuccess;
import static gigabyte.testutil.TypicalIndexes.INDEX_FIRST_CLIENT;
import static gigabyte.testutil.TypicalIndexes.INDEX_SECOND_CLIENT;

import org.junit.jupiter.api.Test;

import gigabyte.logic.Messages;
import gigabyte.logic.commands.AddPaymentCommand;
import gigabyte.model.gig.Deadline;
import gigabyte.model.gig.Fee;

public class AddPaymentCommandParserTest {
    private final AddPaymentCommandParser parser = new AddPaymentCommandParser();

    @Test
    public void parse_validArgumentsAnyOrder_returnsCommand() {
        assertParseSuccess(parser, " 1   2 d/2028-02-29 a/50.5 ",
                new AddPaymentCommand(INDEX_FIRST_CLIENT, INDEX_SECOND_CLIENT, new Fee("50.50"),
                        new Deadline("2028-02-29")));
        assertParseSuccess(parser, "2 1 a/100 d/2027-01-31",
                new AddPaymentCommand(INDEX_SECOND_CLIENT, INDEX_FIRST_CLIENT, new Fee("100"),
                        new Deadline("2027-01-31")));
    }

    @Test
    public void parse_missingFieldsOrWrongNumberOfIndexes_throwsParseException() {
        String expected = String.format(MESSAGE_INVALID_COMMAND_FORMAT, AddPaymentCommand.MESSAGE_USAGE);
        for (String args : new String[] {"", "1 2", "1 2 a/100", "1 2 d/2027-01-31",
            "a/100 d/2027-01-31", "1 a/100 d/2027-01-31", "1 2 3 a/100 d/2027-01-31"}) {
            assertParseFailure(parser, args, expected);
        }
    }

    @Test
    public void parse_invalidIndexes_throwsParseException() {
        String expected = String.format(MESSAGE_INVALID_COMMAND_FORMAT, AddPaymentCommand.MESSAGE_USAGE);
        for (String invalidIndex : new String[] {"0", "-1", "abc", "1.5", "2147483648"}) {
            assertParseFailure(parser, invalidIndex + " 1 a/100 d/2027-01-31", expected);
            assertParseFailure(parser, "1 " + invalidIndex + " a/100 d/2027-01-31", expected);
        }
    }

    @Test
    public void parse_invalidAmount_throwsParseException() {
        for (String amount : new String[] {"", "0", "-50", "1.234", "abc", "1e3", "100 x/unknown"}) {
            assertParseFailure(parser, "1 1 a/" + amount + " d/2027-01-31", Fee.MESSAGE_CONSTRAINTS);
        }
    }

    @Test
    public void parse_invalidDueDate_throwsParseException() {
        for (String date : new String[] {"", "2027-02-29", "2027-13-01", "31-01-2027", "2027-1-1"}) {
            assertParseFailure(parser, "1 1 a/100 d/" + date, Deadline.MESSAGE_CONSTRAINTS);
        }
    }

    @Test
    public void parse_duplicatePrefixes_throwsParseException() {
        assertParseFailure(parser, "1 1 a/100 a/200 d/2027-01-31",
                Messages.getErrorMessageForDuplicatePrefixes(PREFIX_AMOUNT));
        assertParseFailure(parser, "1 1 a/100 d/2027-01-31 d/2027-02-28",
                Messages.getErrorMessageForDuplicatePrefixes(PREFIX_DEADLINE));
    }
}
