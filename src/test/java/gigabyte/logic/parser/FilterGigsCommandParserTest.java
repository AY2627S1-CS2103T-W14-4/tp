package gigabyte.logic.parser;

import static gigabyte.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static gigabyte.logic.parser.CliSyntax.PREFIX_STATUS;
import static gigabyte.logic.parser.CommandParserTestUtil.assertParseFailure;
import static gigabyte.logic.parser.CommandParserTestUtil.assertParseSuccess;

import java.util.Locale;

import org.junit.jupiter.api.Test;

import gigabyte.logic.Messages;
import gigabyte.logic.commands.FilterGigsCommand;
import gigabyte.model.gig.GigStatus;

public class FilterGigsCommandParserTest {
    private final FilterGigsCommandParser parser = new FilterGigsCommandParser();

    @Test
    public void parse_supportedStatuses_acceptsCaseAndSurroundingWhitespace() {
        for (GigStatus status : GigStatus.values()) {
            assertParseSuccess(parser, " s/" + status, new FilterGigsCommand(status));
            assertParseSuccess(parser, "  s/ " + status.name().toLowerCase(Locale.ROOT) + "  ",
                    new FilterGigsCommand(status));
        }
    }

    @Test
    public void parse_missingStatusOrExtraPreamble_rejectsInput() {
        String usage = String.format(MESSAGE_INVALID_COMMAND_FORMAT, FilterGigsCommand.MESSAGE_USAGE);
        for (String input : new String[] {"", " ", "IN_PROGRESS", "1 s/IN_PROGRESS", "x/foo s/IN_PROGRESS"}) {
            assertParseFailure(parser, input, usage);
        }
    }

    @Test
    public void parse_invalidStatusOrExtraArguments_rejectsInput() {
        for (String input : new String[] {" s/", " s/WAITING", " s/IN PROGRESS", " s/IN_PROGRESS extra",
            " s/IN_PROGRESS d/2027-01-31", " s/IN_PROGRESS,COMPLETED"}) {
            assertParseFailure(parser, input, GigStatus.MESSAGE_CONSTRAINTS);
        }
    }

    @Test
    public void parse_repeatedStatus_rejectsEvenIdenticalValues() {
        String duplicate = Messages.getErrorMessageForDuplicatePrefixes(PREFIX_STATUS);
        assertParseFailure(parser, " s/IN_PROGRESS s/COMPLETED", duplicate);
        assertParseFailure(parser, " s/IN_PROGRESS s/IN_PROGRESS", duplicate);
    }
}
