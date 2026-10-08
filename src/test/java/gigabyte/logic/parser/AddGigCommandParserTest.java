package gigabyte.logic.parser;

import static gigabyte.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static gigabyte.logic.parser.CliSyntax.PREFIX_DEADLINE;
import static gigabyte.logic.parser.CliSyntax.PREFIX_FEE;
import static gigabyte.logic.parser.CliSyntax.PREFIX_STATUS;
import static gigabyte.logic.parser.CommandParserTestUtil.assertParseFailure;
import static gigabyte.logic.parser.CommandParserTestUtil.assertParseSuccess;
import static gigabyte.testutil.TypicalIndexes.INDEX_FIRST_CLIENT;

import org.junit.jupiter.api.Test;

import gigabyte.logic.Messages;
import gigabyte.logic.commands.AddGigCommand;
import gigabyte.model.gig.Deadline;
import gigabyte.model.gig.Fee;
import gigabyte.model.gig.GigStatus;

public class AddGigCommandParserTest {
    private final AddGigCommandParser parser = new AddGigCommandParser();

    @Test
    public void parse_validFieldsAnyOrder_returnsCommand() {
        assertParseSuccess(parser, " 1 f/1250.5 d/2027-01-31 s/in_progress ",
                new AddGigCommand(INDEX_FIRST_CLIENT, GigStatus.IN_PROGRESS, new Deadline("2027-01-31"),
                        new Fee("1250.50")));
        assertParseSuccess(parser, "1 s/COMPLETED d/2028-02-29 f/1",
                new AddGigCommand(INDEX_FIRST_CLIENT, GigStatus.COMPLETED, new Deadline("2028-02-29"), new Fee("1")));
    }

    @Test
    public void parse_missingFieldsOrInvalidIndex_throwsParseException() {
        String expected = String.format(MESSAGE_INVALID_COMMAND_FORMAT, AddGigCommand.MESSAGE_USAGE);
        for (String args : new String[] {"", "1", "1 d/2027-01-31 f/100", "1 s/NOT_STARTED f/100",
            "1 s/NOT_STARTED d/2027-01-31", "s/NOT_STARTED d/2027-01-31 f/100",
            "0 s/NOT_STARTED d/2027-01-31 f/100", "-1 s/NOT_STARTED d/2027-01-31 f/100",
            "abc s/NOT_STARTED d/2027-01-31 f/100", "2147483648 s/NOT_STARTED d/2027-01-31 f/100"}) {
            assertParseFailure(parser, args, expected);
        }
    }

    @Test
    public void parse_invalidStatus_throwsParseException() {
        for (String status : new String[] {"", "unknown", "in progress"}) {
            assertParseFailure(parser, "1 s/" + status + " d/2027-01-31 f/100", GigStatus.MESSAGE_CONSTRAINTS);
        }
    }

    @Test
    public void parse_invalidDeadline_throwsParseException() {
        for (String deadline : new String[] {"", "2027-02-29", "2027-13-01", "31-01-2027", "2027-1-1"}) {
            assertParseFailure(parser, "1 s/NOT_STARTED d/" + deadline + " f/100", Deadline.MESSAGE_CONSTRAINTS);
        }
    }

    @Test
    public void parse_invalidFee_throwsParseException() {
        for (String fee : new String[] {"", "0", "-1", "1.234", "abc", "1e3"}) {
            assertParseFailure(parser, "1 s/NOT_STARTED d/2027-01-31 f/" + fee, Fee.MESSAGE_CONSTRAINTS);
        }
    }

    @Test
    public void parse_duplicatePrefixes_throwsParseException() {
        String valid = "1 s/NOT_STARTED d/2027-01-31 f/100";
        assertParseFailure(parser, valid + " s/COMPLETED",
                Messages.getErrorMessageForDuplicatePrefixes(PREFIX_STATUS));
        assertParseFailure(parser, valid + " d/2027-02-28",
                Messages.getErrorMessageForDuplicatePrefixes(PREFIX_DEADLINE));
        assertParseFailure(parser, valid + " f/200", Messages.getErrorMessageForDuplicatePrefixes(PREFIX_FEE));
    }
}
