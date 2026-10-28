package gigabyte.logic.parser;

import static gigabyte.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static gigabyte.logic.parser.CliSyntax.PREFIX_DEADLINE;
import static gigabyte.logic.parser.CliSyntax.PREFIX_FEE;
import static gigabyte.logic.parser.CliSyntax.PREFIX_STATUS;
import static gigabyte.logic.parser.CliSyntax.PREFIX_TITLE;
import static gigabyte.logic.parser.CommandParserTestUtil.assertParseFailure;
import static gigabyte.logic.parser.CommandParserTestUtil.assertParseSuccess;
import static gigabyte.testutil.TypicalIndexes.INDEX_FIRST_CLIENT;

import org.junit.jupiter.api.Test;

import gigabyte.logic.Messages;
import gigabyte.logic.commands.AddGigCommand;
import gigabyte.model.gig.Deadline;
import gigabyte.model.gig.Fee;
import gigabyte.model.gig.GigStatus;
import gigabyte.model.gig.GigTitle;

public class AddGigCommandParserTest {
    private static final String INVALID_FORMAT =
            String.format(MESSAGE_INVALID_COMMAND_FORMAT, AddGigCommand.MESSAGE_USAGE);
    private static final String VALID_FIELDS = " t/Title s/NOT_STARTED d/2027-01-31 f/100";

    private final AddGigCommandParser parser = new AddGigCommandParser();

    @Test
    public void parse_validFieldsAnyOrder_returnsCommand() {
        assertParseSuccess(parser, " 1 f/1250.5 t/ Website redesign — phase 2! d/2027-01-31 s/in_progress ",
                new AddGigCommand(INDEX_FIRST_CLIENT, new GigTitle("Website redesign — phase 2!"),
                        GigStatus.IN_PROGRESS, new Deadline("2027-01-31"),
                        new Fee("1250.50")));
        assertParseSuccess(parser, "1 s/COMPLETED d/2028-02-29 f/1 t/Logo & brand refresh (Q2)",
                new AddGigCommand(INDEX_FIRST_CLIENT, new GigTitle("Logo & brand refresh (Q2)"),
                        GigStatus.COMPLETED, new Deadline("2028-02-29"), new Fee("1")));
    }

    @Test
    public void parse_missingCompulsoryField_throwsParseException() {
        // EP: no fields at all
        assertParseFailure(parser, "", INVALID_FORMAT);
        assertParseFailure(parser, "1", INVALID_FORMAT);

        // EP: exactly one compulsory field missing
        assertParseFailure(parser, "t/Title s/NOT_STARTED d/2027-01-31 f/100", INVALID_FORMAT); // client index
        assertParseFailure(parser, "1 s/NOT_STARTED d/2027-01-31 f/100", INVALID_FORMAT); // title
        assertParseFailure(parser, "1 t/Title d/2027-01-31 f/100", INVALID_FORMAT); // status
        assertParseFailure(parser, "1 t/Title s/NOT_STARTED f/100", INVALID_FORMAT); // deadline
        assertParseFailure(parser, "1 t/Title s/NOT_STARTED d/2027-01-31", INVALID_FORMAT); // fee
    }

    @Test
    public void parse_zeroOrNegativeIndex_throwsParseException() {
        assertParseFailure(parser, "0" + VALID_FIELDS, INVALID_FORMAT); // Boundary value
        assertParseFailure(parser, "-1" + VALID_FIELDS, INVALID_FORMAT);
    }

    @Test
    public void parse_nonNumericIndex_throwsParseException() {
        assertParseFailure(parser, "abc" + VALID_FIELDS, INVALID_FORMAT);
    }

    @Test
    public void parse_indexAboveIntegerMaxValue_throwsParseException() {
        // Boundary value: Integer.MAX_VALUE + 1
        assertParseFailure(parser, "2147483648" + VALID_FIELDS, INVALID_FORMAT);
    }

    @Test
    public void parse_invalidTitle_throwsParseException() {
        // EP: empty title
        assertParseFailure(parser, "1 t/ s/NOT_STARTED d/2027-01-31 f/100", GigTitle.MESSAGE_CONSTRAINTS);

        // EP: title of only non-breaking spaces
        assertParseFailure(parser, "1 t/\u00A0 s/NOT_STARTED d/2027-01-31 f/100", GigTitle.MESSAGE_CONSTRAINTS);

        // EP: title longer than 100 characters
        assertParseFailure(parser, "1 t/" + "a".repeat(101) + " s/NOT_STARTED d/2027-01-31 f/100",
                GigTitle.MESSAGE_CONSTRAINTS); // Boundary value
    }

    @Test
    public void parse_invalidStatus_throwsParseException() {
        // EP: empty status
        assertParseFailure(parser, "1 t/Title s/ d/2027-01-31 f/100", GigStatus.MESSAGE_CONSTRAINTS);

        // EP: unknown status
        assertParseFailure(parser, "1 t/Title s/unknown d/2027-01-31 f/100", GigStatus.MESSAGE_CONSTRAINTS);

        // EP: known status written with a space instead of an underscore
        assertParseFailure(parser, "1 t/Title s/in progress d/2027-01-31 f/100", GigStatus.MESSAGE_CONSTRAINTS);
    }

    @Test
    public void parse_invalidDeadline_throwsParseException() {
        // EP: empty deadline
        assertParseFailure(parser, "1 t/Title s/NOT_STARTED d/ f/100", Deadline.MESSAGE_CONSTRAINTS);

        // EP: correct format but the date does not exist
        assertParseFailure(parser, "1 t/Title s/NOT_STARTED d/2027-02-29 f/100",
                Deadline.MESSAGE_CONSTRAINTS); // Not a leap year
        assertParseFailure(parser, "1 t/Title s/NOT_STARTED d/2027-13-01 f/100", Deadline.MESSAGE_CONSTRAINTS);

        // EP: wrong date format
        assertParseFailure(parser, "1 t/Title s/NOT_STARTED d/31-01-2027 f/100", Deadline.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, "1 t/Title s/NOT_STARTED d/2027-1-1 f/100", Deadline.MESSAGE_CONSTRAINTS);
    }

    @Test
    public void parse_invalidFee_throwsParseException() {
        // EP: empty fee
        assertParseFailure(parser, "1 t/Title s/NOT_STARTED d/2027-01-31 f/", Fee.MESSAGE_CONSTRAINTS);

        // EP: zero or negative fee
        assertParseFailure(parser, "1 t/Title s/NOT_STARTED d/2027-01-31 f/0",
                Fee.MESSAGE_CONSTRAINTS); // Boundary value
        assertParseFailure(parser, "1 t/Title s/NOT_STARTED d/2027-01-31 f/-1", Fee.MESSAGE_CONSTRAINTS);

        // EP: more than two decimal places
        assertParseFailure(parser, "1 t/Title s/NOT_STARTED d/2027-01-31 f/1.234", Fee.MESSAGE_CONSTRAINTS);

        // EP: not a plain decimal number
        assertParseFailure(parser, "1 t/Title s/NOT_STARTED d/2027-01-31 f/abc", Fee.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, "1 t/Title s/NOT_STARTED d/2027-01-31 f/1e3", Fee.MESSAGE_CONSTRAINTS);
    }

    @Test
    public void parse_duplicatePrefixes_throwsParseException() {
        String valid = "1 t/Title s/NOT_STARTED d/2027-01-31 f/100";
        assertParseFailure(parser, valid + " t/Another title",
                Messages.getErrorMessageForDuplicatePrefixes(PREFIX_TITLE));
        assertParseFailure(parser, valid + " s/COMPLETED",
                Messages.getErrorMessageForDuplicatePrefixes(PREFIX_STATUS));
        assertParseFailure(parser, valid + " d/2027-02-28",
                Messages.getErrorMessageForDuplicatePrefixes(PREFIX_DEADLINE));
        assertParseFailure(parser, valid + " f/200", Messages.getErrorMessageForDuplicatePrefixes(PREFIX_FEE));
    }
}
