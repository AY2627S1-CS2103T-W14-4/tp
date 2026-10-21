package gigabyte.logic.parser;

import static gigabyte.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static gigabyte.logic.parser.CliSyntax.PREFIX_AMOUNT;
import static gigabyte.logic.parser.CliSyntax.PREFIX_DEADLINE;
import static gigabyte.logic.parser.CommandParserTestUtil.assertParseFailure;
import static gigabyte.logic.parser.CommandParserTestUtil.assertParseSuccess;
import static gigabyte.testutil.TypicalIndexes.INDEX_FIRST_CLIENT;
import static gigabyte.testutil.TypicalIndexes.INDEX_SECOND_CLIENT;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import gigabyte.commons.core.index.Index;
import gigabyte.logic.Messages;
import gigabyte.logic.commands.EditPaymentCommand;
import gigabyte.model.gig.Deadline;
import gigabyte.model.gig.Fee;

public class EditPaymentCommandParserTest {
    private static final Index THIRD = Index.fromOneBased(3);

    private final EditPaymentCommandParser parser = new EditPaymentCommandParser();

    @Test
    public void parse_validArgumentsAnyOrder_returnsCommand() {
        assertParseSuccess(parser, " 1  2 3 a/650.00 ",
                new EditPaymentCommand(INDEX_FIRST_CLIENT, INDEX_SECOND_CLIENT, THIRD,
                        Optional.of(new Fee("650")), Optional.empty()));
        assertParseSuccess(parser, "2 1 3 d/2027-05-15",
                new EditPaymentCommand(INDEX_SECOND_CLIENT, INDEX_FIRST_CLIENT, THIRD,
                        Optional.empty(), Optional.of(new Deadline("2027-05-15"))));
        assertParseSuccess(parser, "1 2 3 d/2027-05-15 a/650",
                new EditPaymentCommand(INDEX_FIRST_CLIENT, INDEX_SECOND_CLIENT, THIRD,
                        Optional.of(new Fee("650")), Optional.of(new Deadline("2027-05-15"))));
    }

    @Test
    public void parse_missingFieldsOrWrongNumberOfIndexes_throwsParseException() {
        String expectedFormat = String.format(MESSAGE_INVALID_COMMAND_FORMAT, EditPaymentCommand.MESSAGE_USAGE);
        assertParseFailure(parser, "", expectedFormat);
        assertParseFailure(parser, "1 2 a/650", expectedFormat);
        assertParseFailure(parser, "1 2 3 4 a/650", expectedFormat);
        assertParseFailure(parser, "1 2 3", EditPaymentCommand.MESSAGE_NOT_EDITED);
    }

    @Test
    public void parse_invalidIndexes_throwsParseException() {
        String expected = String.format(MESSAGE_INVALID_COMMAND_FORMAT, EditPaymentCommand.MESSAGE_USAGE);
        for (String invalidIndex : new String[] {"0", "-1", "abc", "1.5", "2147483648"}) {
            assertParseFailure(parser, invalidIndex + " 1 1 a/650", expected);
            assertParseFailure(parser, "1 " + invalidIndex + " 1 a/650", expected);
            assertParseFailure(parser, "1 1 " + invalidIndex + " a/650", expected);
        }
    }

    @Test
    public void parse_invalidAmountOrDueDate_throwsParseException() {
        for (String amount : new String[] {"", "0", "-50", "1.234", "abc", "1e3"}) {
            assertParseFailure(parser, "1 1 1 a/" + amount, Fee.MESSAGE_CONSTRAINTS);
        }
        for (String date : new String[] {"", "2027-02-29", "2027-13-01", "31-01-2027", "2027-1-1"}) {
            assertParseFailure(parser, "1 1 1 d/" + date, Deadline.MESSAGE_CONSTRAINTS);
        }
    }

    @Test
    public void parse_duplicatePrefixes_throwsParseException() {
        assertParseFailure(parser, "1 1 1 a/100 a/200",
                Messages.getErrorMessageForDuplicatePrefixes(PREFIX_AMOUNT));
        assertParseFailure(parser, "1 1 1 d/2027-01-31 d/2027-02-28",
                Messages.getErrorMessageForDuplicatePrefixes(PREFIX_DEADLINE));
    }
}
