package gigabyte.logic.parser;

import static gigabyte.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static gigabyte.logic.parser.CliSyntax.PREFIX_STATUS;
import static gigabyte.logic.parser.CommandParserTestUtil.assertParseFailure;
import static gigabyte.logic.parser.CommandParserTestUtil.assertParseSuccess;
import static gigabyte.logic.parser.CommandParserTestUtil.rejectedValueMessage;
import static gigabyte.logic.parser.ParserUtil.MESSAGE_INVALID_INDEX;

import java.util.Locale;

import org.junit.jupiter.api.Test;

import gigabyte.commons.core.index.Index;
import gigabyte.logic.Messages;
import gigabyte.logic.commands.SetGigStatusCommand;
import gigabyte.model.gig.GigStatus;

public class SetGigStatusCommandParserTest {
    private static final String INVALID_FORMAT =
            String.format(MESSAGE_INVALID_COMMAND_FORMAT, SetGigStatusCommand.MESSAGE_USAGE);

    private final SetGigStatusCommandParser parser = new SetGigStatusCommandParser();

    @Test
    public void parse_supportedStatuses_acceptsCaseAndSurroundingWhitespace() {
        for (GigStatus status : GigStatus.values()) {
            SetGigStatusCommand expected = new SetGigStatusCommand(
                    Index.fromOneBased(1), Index.fromOneBased(2), status);
            assertParseSuccess(parser, "1 2 s/" + status, expected);
            assertParseSuccess(parser, "1 2 s/ " + status.name().toLowerCase(Locale.ROOT) + "  ", expected);
        }
    }

    @Test
    public void parse_missingFieldsOrWrongNumberOfIndexes_rejectsInput() {
        // EP: missing status
        assertParseFailure(parser, "", INVALID_FORMAT);
        assertParseFailure(parser, "1", INVALID_FORMAT);
        assertParseFailure(parser, "1 2", INVALID_FORMAT);

        // EP: fewer than two indexes
        assertParseFailure(parser, "s/COMPLETED", INVALID_FORMAT);
        assertParseFailure(parser, "1 s/COMPLETED", INVALID_FORMAT);

        // EP: more than two indexes
        assertParseFailure(parser, "1 2 3 s/COMPLETED", INVALID_FORMAT);

        // EP: extra words before the first prefix
        assertParseFailure(parser, "1 2 extra s/COMPLETED", INVALID_FORMAT);
    }

    @Test
    public void parse_zeroOrNegativeIndex_rejectsInput() {
        assertParseFailure(parser, "0 2 s/COMPLETED", MESSAGE_INVALID_INDEX); // Boundary value
        assertParseFailure(parser, "1 -2 s/COMPLETED", MESSAGE_INVALID_INDEX);
    }

    @Test
    public void parse_nonNumericIndex_rejectsInput() {
        assertParseFailure(parser, "one 2 s/COMPLETED", MESSAGE_INVALID_INDEX);
    }

    @Test
    public void parse_indexAboveIntegerMaxValue_rejectsInput() {
        // Boundary value: Integer.MAX_VALUE + 1
        assertParseFailure(parser, "2147483648 2 s/COMPLETED", MESSAGE_INVALID_INDEX);
    }

    @Test
    public void parse_emptyStatus_rejectsInput() {
        assertParseFailure(parser, "1 2 s/", INVALID_FORMAT);
    }

    @Test
    public void parse_unsupportedStatusOrExtraArguments_rejectsInput() {
        // EP: unknown status
        assertParseFailure(parser, "1 2 s/WAITING", rejectedValueMessage(GigStatus.MESSAGE_CONSTRAINTS, "WAITING"));

        // EP: known status written with a space instead of an underscore
        assertParseFailure(parser, "1 2 s/IN PROGRESS",
                rejectedValueMessage(GigStatus.MESSAGE_CONSTRAINTS, "IN PROGRESS"));

        // EP: valid status followed by extra text or an unsupported prefix
        assertParseFailure(parser, "1 2 s/COMPLETED extra",
                rejectedValueMessage(GigStatus.MESSAGE_CONSTRAINTS, "COMPLETED extra"));
        assertParseFailure(parser, "1 2 s/COMPLETED d/2027-01-31",
                rejectedValueMessage(GigStatus.MESSAGE_CONSTRAINTS, "COMPLETED d/2027-01-31"));
        assertParseFailure(parser, "1 2 s/COMPLETED t/x",
                rejectedValueMessage(GigStatus.MESSAGE_CONSTRAINTS, "COMPLETED t/x"));
    }

    @Test
    public void parse_repeatedStatus_rejectsEvenIdenticalValues() {
        String duplicate = Messages.getErrorMessageForDuplicatePrefixes(PREFIX_STATUS);
        assertParseFailure(parser, "1 2 s/IN_PROGRESS s/COMPLETED", duplicate);
        assertParseFailure(parser, "1 2 s/IN_PROGRESS s/IN_PROGRESS", duplicate);
    }
}
