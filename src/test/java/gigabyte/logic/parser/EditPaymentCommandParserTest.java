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
    private static final String INVALID_FORMAT =
            String.format(MESSAGE_INVALID_COMMAND_FORMAT, EditPaymentCommand.MESSAGE_USAGE);

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
    public void parse_wrongNumberOfIndexes_throwsParseException() {
        // EP: fewer than three indexes
        assertParseFailure(parser, "", INVALID_FORMAT);
        assertParseFailure(parser, "1 2 a/650", INVALID_FORMAT);

        // EP: more than three indexes
        assertParseFailure(parser, "1 2 3 4 a/650", INVALID_FORMAT);
    }

    @Test
    public void parse_noFieldToEdit_throwsParseException() {
        assertParseFailure(parser, "1 2 3", EditPaymentCommand.MESSAGE_NOT_EDITED);
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

    @Test
    public void parse_invalidAmount_throwsParseException() {
        // EP: empty amount
        assertParseFailure(parser, "1 1 1 a/", Fee.MESSAGE_CONSTRAINTS);

        // EP: zero or negative amount
        assertParseFailure(parser, "1 1 1 a/0", Fee.MESSAGE_CONSTRAINTS); // Boundary value
        assertParseFailure(parser, "1 1 1 a/-50", Fee.MESSAGE_CONSTRAINTS);

        // EP: more than two decimal places
        assertParseFailure(parser, "1 1 1 a/1.234", Fee.MESSAGE_CONSTRAINTS);

        // EP: not a plain decimal number
        assertParseFailure(parser, "1 1 1 a/abc", Fee.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, "1 1 1 a/1e3", Fee.MESSAGE_CONSTRAINTS);
    }

    @Test
    public void parse_invalidDueDate_throwsParseException() {
        // EP: empty due date
        assertParseFailure(parser, "1 1 1 d/", Deadline.MESSAGE_CONSTRAINTS);

        // EP: correct format but the date does not exist
        assertParseFailure(parser, "1 1 1 d/2027-02-29", Deadline.MESSAGE_CONSTRAINTS); // Not a leap year
        assertParseFailure(parser, "1 1 1 d/2027-13-01", Deadline.MESSAGE_CONSTRAINTS);

        // EP: wrong date format
        assertParseFailure(parser, "1 1 1 d/31-01-2027", Deadline.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, "1 1 1 d/2027-1-1", Deadline.MESSAGE_CONSTRAINTS);
    }

    @Test
    public void parse_duplicatePrefixes_throwsParseException() {
        assertParseFailure(parser, "1 1 1 a/100 a/200",
                Messages.getErrorMessageForDuplicatePrefixes(PREFIX_AMOUNT));
        assertParseFailure(parser, "1 1 1 d/2027-01-31 d/2027-02-28",
                Messages.getErrorMessageForDuplicatePrefixes(PREFIX_DEADLINE));
    }

    /** Asserts that {@code invalidIndex} is rejected as the client, gig, and payment index. */
    private void assertInvalidIndexRejected(String invalidIndex) {
        assertParseFailure(parser, invalidIndex + " 1 1 a/650", INVALID_FORMAT);
        assertParseFailure(parser, "1 " + invalidIndex + " 1 a/650", INVALID_FORMAT);
        assertParseFailure(parser, "1 1 " + invalidIndex + " a/650", INVALID_FORMAT);
    }
}
