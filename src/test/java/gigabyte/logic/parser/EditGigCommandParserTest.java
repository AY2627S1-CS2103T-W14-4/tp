package gigabyte.logic.parser;

import static gigabyte.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static gigabyte.logic.parser.CliSyntax.PREFIX_DEADLINE;
import static gigabyte.logic.parser.CliSyntax.PREFIX_FEE;
import static gigabyte.logic.parser.CliSyntax.PREFIX_TITLE;
import static gigabyte.logic.parser.CommandParserTestUtil.assertParseFailure;
import static gigabyte.logic.parser.CommandParserTestUtil.assertParseSuccess;
import static gigabyte.logic.parser.ParserUtil.MESSAGE_INVALID_INDEX;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import gigabyte.commons.core.index.Index;
import gigabyte.logic.Messages;
import gigabyte.logic.commands.EditGigCommand;
import gigabyte.model.gig.Deadline;
import gigabyte.model.gig.Fee;
import gigabyte.model.gig.GigTitle;

public class EditGigCommandParserTest {
    private static final Index FIRST = Index.fromOneBased(1);
    private static final Index SECOND = Index.fromOneBased(2);

    private final EditGigCommandParser parser = new EditGigCommandParser();
    private final String usage = String.format(MESSAGE_INVALID_COMMAND_FORMAT, EditGigCommand.MESSAGE_USAGE);

    @Test
    public void parse_optionalFieldsAnyOrder_trimsTitle() {
        // EP: all fields edited, in a different order, with extra whitespace
        assertParseSuccess(parser, " 1  2 f/1800 t/  Updated landing page  d/2027-04-30 ",
                new EditGigCommand(FIRST, SECOND, Optional.of(new GigTitle("Updated landing page")),
                        Optional.of(new Deadline("2027-04-30")), Optional.of(new Fee("1800"))));

        // EP: only one field edited
        assertParseSuccess(parser, "1 2 d/2027-04-30", new EditGigCommand(FIRST, SECOND,
                Optional.empty(), Optional.of(new Deadline("2027-04-30")), Optional.empty()));
        assertParseSuccess(parser, "1 2 t/Website", new EditGigCommand(FIRST, SECOND,
                Optional.of(new GigTitle("Website")), Optional.empty(), Optional.empty()));
        assertParseSuccess(parser, "1 2 f/1800", new EditGigCommand(FIRST, SECOND,
                Optional.empty(), Optional.empty(), Optional.of(new Fee("1800"))));

        // Boundary value: longest allowed title (100 characters)
        assertParseSuccess(parser, "1 2 t/" + "x".repeat(100), new EditGigCommand(FIRST, SECOND,
                Optional.of(new GigTitle("x".repeat(100))), Optional.empty(), Optional.empty()));
    }

    @Test
    public void parse_missingFieldsOrWrongNumberOfIndexes_rejectsInput() {
        // EP: no field to edit
        assertParseFailure(parser, "", usage);
        assertParseFailure(parser, "1 2", usage);

        // EP: fewer or more than two indexes
        assertParseFailure(parser, "1 t/Website", usage);
        assertParseFailure(parser, "1 2 3 t/Website", usage);

        // EP: extra words before the first prefix
        assertParseFailure(parser, "1 2 extra t/Website", usage);
    }

    @Test
    public void parse_statusField_rejectsInput() {
        // EP: status on its own
        assertParseFailure(parser, "1 2 s/COMPLETED", usage);

        // EP: status alongside editable fields
        assertParseFailure(parser, "1 2 t/Website s/COMPLETED", usage);
        assertParseFailure(parser, "1 2 s/COMPLETED f/100", usage);
    }

    @Test
    public void parse_zeroOrNegativeIndex_rejectsInput() {
        assertInvalidIndexRejected("0"); // Boundary value
        assertInvalidIndexRejected("-1");
    }

    @Test
    public void parse_nonIntegerIndex_rejectsInput() {
        assertInvalidIndexRejected("abc");
        assertInvalidIndexRejected("1.5");
    }

    @Test
    public void parse_indexAboveIntegerMaxValue_rejectsInput() {
        assertInvalidIndexRejected("2147483648"); // Boundary value: Integer.MAX_VALUE + 1
    }

    @Test
    public void parse_invalidTitle_rejectsEntireEdit() {
        // EP: blank title
        assertParseFailure(parser, "1 1 f/100 t/", GigTitle.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, "1 1 f/100 t/  ", GigTitle.MESSAGE_CONSTRAINTS);

        // EP: title longer than 100 characters
        assertParseFailure(parser, "1 1 f/100 t/" + "x".repeat(101), GigTitle.MESSAGE_CONSTRAINTS); // Boundary value
    }

    @Test
    public void parse_invalidDeadline_rejectsEntireEdit() {
        // EP: empty deadline
        assertParseFailure(parser, "1 1 t/Website d/", Deadline.MESSAGE_CONSTRAINTS);

        // EP: correct format but the date does not exist
        assertParseFailure(parser, "1 1 t/Website d/2027-02-29", Deadline.MESSAGE_CONSTRAINTS); // Not a leap year
        assertParseFailure(parser, "1 1 t/Website d/2027-13-01", Deadline.MESSAGE_CONSTRAINTS);

        // EP: wrong date format
        assertParseFailure(parser, "1 1 t/Website d/2027-1-1", Deadline.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, "1 1 t/Website d/31-01-2027", Deadline.MESSAGE_CONSTRAINTS);
    }

    @Test
    public void parse_invalidFee_rejectsEntireEdit() {
        // EP: empty fee
        assertParseFailure(parser, "1 1 t/Website f/", Fee.MESSAGE_CONSTRAINTS);

        // EP: zero or negative fee
        assertParseFailure(parser, "1 1 t/Website f/0", Fee.MESSAGE_CONSTRAINTS); // Boundary value
        assertParseFailure(parser, "1 1 t/Website f/-1", Fee.MESSAGE_CONSTRAINTS);

        // EP: not a plain decimal number
        assertParseFailure(parser, "1 1 t/Website f/abc", Fee.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, "1 1 t/Website f/1e3", Fee.MESSAGE_CONSTRAINTS);

        // EP: more than two decimal places
        assertParseFailure(parser, "1 1 t/Website f/1.234", Fee.MESSAGE_CONSTRAINTS);
    }

    @Test
    public void parse_repeatedPrefixes_rejectsEvenIdenticalValues() {
        assertParseFailure(parser, "1 1 t/Website t/Website",
                Messages.getErrorMessageForDuplicatePrefixes(PREFIX_TITLE));
        assertParseFailure(parser, "1 1 d/2027-01-31 d/2027-02-28",
                Messages.getErrorMessageForDuplicatePrefixes(PREFIX_DEADLINE));
        assertParseFailure(parser, "1 1 f/100 f/200",
                Messages.getErrorMessageForDuplicatePrefixes(PREFIX_FEE));
    }

    /** Asserts that {@code invalidIndex} is rejected as both the client index and the gig index. */
    private void assertInvalidIndexRejected(String invalidIndex) {
        assertParseFailure(parser, invalidIndex + " 1 t/Website", MESSAGE_INVALID_INDEX);
        assertParseFailure(parser, "1 " + invalidIndex + " t/Website", MESSAGE_INVALID_INDEX);
    }
}
