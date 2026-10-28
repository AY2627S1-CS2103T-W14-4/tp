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
    private static final String INVALID_FORMAT =
            String.format(MESSAGE_INVALID_COMMAND_FORMAT, AddPaymentCommand.MESSAGE_USAGE);

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
    public void parse_missingField_throwsParseException() {
        // EP: no fields at all
        assertParseFailure(parser, "", INVALID_FORMAT);
        assertParseFailure(parser, "1 2", INVALID_FORMAT);

        // EP: exactly one field missing
        assertParseFailure(parser, "1 2 a/100", INVALID_FORMAT); // due date
        assertParseFailure(parser, "1 2 d/2027-01-31", INVALID_FORMAT); // amount
    }

    @Test
    public void parse_wrongNumberOfIndexes_throwsParseException() {
        // EP: fewer than two indexes
        assertParseFailure(parser, "a/100 d/2027-01-31", INVALID_FORMAT);
        assertParseFailure(parser, "1 a/100 d/2027-01-31", INVALID_FORMAT);

        // EP: more than two indexes
        assertParseFailure(parser, "1 2 3 a/100 d/2027-01-31", INVALID_FORMAT);
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
        assertParseFailure(parser, "1 1 a/ d/2027-01-31", Fee.MESSAGE_CONSTRAINTS);

        // EP: zero or negative amount
        assertParseFailure(parser, "1 1 a/0 d/2027-01-31", Fee.MESSAGE_CONSTRAINTS); // Boundary value
        assertParseFailure(parser, "1 1 a/-50 d/2027-01-31", Fee.MESSAGE_CONSTRAINTS);

        // EP: more than two decimal places
        assertParseFailure(parser, "1 1 a/1.234 d/2027-01-31", Fee.MESSAGE_CONSTRAINTS);

        // EP: not a plain decimal number
        assertParseFailure(parser, "1 1 a/abc d/2027-01-31", Fee.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, "1 1 a/1e3 d/2027-01-31", Fee.MESSAGE_CONSTRAINTS);

        // EP: unknown prefix becomes part of the amount
        assertParseFailure(parser, "1 1 a/100 x/unknown d/2027-01-31", Fee.MESSAGE_CONSTRAINTS);
    }

    @Test
    public void parse_invalidDueDate_throwsParseException() {
        // EP: empty due date
        assertParseFailure(parser, "1 1 a/100 d/", Deadline.MESSAGE_CONSTRAINTS);

        // EP: correct format but the date does not exist
        assertParseFailure(parser, "1 1 a/100 d/2027-02-29", Deadline.MESSAGE_CONSTRAINTS); // Not a leap year
        assertParseFailure(parser, "1 1 a/100 d/2027-13-01", Deadline.MESSAGE_CONSTRAINTS);

        // EP: wrong date format
        assertParseFailure(parser, "1 1 a/100 d/31-01-2027", Deadline.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, "1 1 a/100 d/2027-1-1", Deadline.MESSAGE_CONSTRAINTS);
    }

    @Test
    public void parse_duplicatePrefixes_throwsParseException() {
        assertParseFailure(parser, "1 1 a/100 a/200 d/2027-01-31",
                Messages.getErrorMessageForDuplicatePrefixes(PREFIX_AMOUNT));
        assertParseFailure(parser, "1 1 a/100 d/2027-01-31 d/2027-02-28",
                Messages.getErrorMessageForDuplicatePrefixes(PREFIX_DEADLINE));
    }

    /** Asserts that {@code invalidIndex} is rejected as both the client index and the gig index. */
    private void assertInvalidIndexRejected(String invalidIndex) {
        assertParseFailure(parser, invalidIndex + " 1 a/100 d/2027-01-31", INVALID_FORMAT);
        assertParseFailure(parser, "1 " + invalidIndex + " a/100 d/2027-01-31", INVALID_FORMAT);
    }
}
