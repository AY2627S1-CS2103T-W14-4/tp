package gigabyte.logic.parser;

import static gigabyte.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static gigabyte.logic.parser.CliSyntax.PREFIX_STATUS;
import static gigabyte.logic.parser.CommandParserTestUtil.assertParseFailure;
import static gigabyte.logic.parser.CommandParserTestUtil.assertParseSuccess;

import java.util.Locale;

import org.junit.jupiter.api.Test;

import gigabyte.commons.core.index.Index;
import gigabyte.logic.Messages;
import gigabyte.logic.commands.SetGigStatusCommand;
import gigabyte.model.gig.GigStatus;

public class SetGigStatusCommandParserTest {
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
    public void parse_missingOrUnexpectedIndexes_rejectsInput() {
        String usage = String.format(MESSAGE_INVALID_COMMAND_FORMAT, SetGigStatusCommand.MESSAGE_USAGE);
        for (String input : new String[] {"", "1", "1 2", "1 2 3 s/COMPLETED", "one 2 s/COMPLETED",
            "0 2 s/COMPLETED", "1 -2 s/COMPLETED", "2147483648 2 s/COMPLETED"}) {
            assertParseFailure(parser, input, usage);
        }
    }

    @Test
    public void parse_missingUnsupportedOrExtraArguments_rejectsInput() {
        String usage = String.format(MESSAGE_INVALID_COMMAND_FORMAT, SetGigStatusCommand.MESSAGE_USAGE);
        assertParseFailure(parser, "1 2 s/", usage);
        for (String input : new String[] {"1 2 s/WAITING", "1 2 s/IN PROGRESS", "1 2 s/COMPLETED extra",
            "1 2 s/COMPLETED d/2027-01-31"}) {
            assertParseFailure(parser, input, GigStatus.MESSAGE_CONSTRAINTS);
        }
    }

    @Test
    public void parse_repeatedStatus_rejectsEvenIdenticalValues() {
        String duplicate = Messages.getErrorMessageForDuplicatePrefixes(PREFIX_STATUS);
        assertParseFailure(parser, "1 2 s/IN_PROGRESS s/COMPLETED", duplicate);
        assertParseFailure(parser, "1 2 s/IN_PROGRESS s/IN_PROGRESS", duplicate);
    }
}
