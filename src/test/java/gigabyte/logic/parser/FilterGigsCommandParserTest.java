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

        // EP: no status prefix
        assertParseFailure(parser, "", usage);
        assertParseFailure(parser, " ", usage);
        assertParseFailure(parser, "IN_PROGRESS", usage);

        // EP: unexpected text before the status prefix
        assertParseFailure(parser, "1 s/IN_PROGRESS", usage);
        assertParseFailure(parser, "x/foo s/IN_PROGRESS", usage);
    }

    @Test
    public void parse_invalidStatusOrExtraArguments_rejectsInput() {
        // EP: empty status
        assertParseFailure(parser, " s/", GigStatus.MESSAGE_CONSTRAINTS);

        // EP: unknown status
        assertParseFailure(parser, " s/WAITING", GigStatus.MESSAGE_CONSTRAINTS);

        // EP: known status written with a space instead of an underscore
        assertParseFailure(parser, " s/IN PROGRESS", GigStatus.MESSAGE_CONSTRAINTS);

        // EP: valid status followed by extra text, an unsupported prefix, or a second status
        assertParseFailure(parser, " s/IN_PROGRESS extra", GigStatus.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, " s/IN_PROGRESS d/2027-01-31", GigStatus.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, " s/IN_PROGRESS,COMPLETED", GigStatus.MESSAGE_CONSTRAINTS);
    }

    @Test
    public void parse_repeatedStatus_rejectsEvenIdenticalValues() {
        String duplicate = Messages.getErrorMessageForDuplicatePrefixes(PREFIX_STATUS);
        assertParseFailure(parser, " s/IN_PROGRESS s/COMPLETED", duplicate);
        assertParseFailure(parser, " s/IN_PROGRESS s/IN_PROGRESS", duplicate);
    }
}
