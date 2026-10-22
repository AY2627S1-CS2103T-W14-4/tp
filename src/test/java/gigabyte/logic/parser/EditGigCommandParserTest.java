package gigabyte.logic.parser;

import static gigabyte.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static gigabyte.logic.parser.CliSyntax.PREFIX_DEADLINE;
import static gigabyte.logic.parser.CliSyntax.PREFIX_FEE;
import static gigabyte.logic.parser.CliSyntax.PREFIX_TITLE;
import static gigabyte.logic.parser.CommandParserTestUtil.assertParseFailure;
import static gigabyte.logic.parser.CommandParserTestUtil.assertParseSuccess;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import gigabyte.commons.core.index.Index;
import gigabyte.logic.Messages;
import gigabyte.logic.commands.EditGigCommand;
import gigabyte.model.gig.Deadline;
import gigabyte.model.gig.Fee;
import gigabyte.model.gig.GigTitle;

public class EditGigCommandParserTest {
    private final EditGigCommandParser parser = new EditGigCommandParser();
    private final String usage = String.format(MESSAGE_INVALID_COMMAND_FORMAT, EditGigCommand.MESSAGE_USAGE);

    @Test
    public void parse_optionalFieldsAnyOrder_trimsTitle() {
        assertParseSuccess(parser, " 1  2 f/1800 t/  Updated landing page  d/2027-04-30 ",
                new EditGigCommand(Index.fromOneBased(1), Index.fromOneBased(2),
                        Optional.of(new GigTitle("Updated landing page")),
                        Optional.of(new Deadline("2027-04-30")), Optional.of(new Fee("1800"))));
        assertParseSuccess(parser, "1 2 d/2027-04-30", new EditGigCommand(Index.fromOneBased(1),
                Index.fromOneBased(2), Optional.empty(), Optional.of(new Deadline("2027-04-30")), Optional.empty()));
        assertParseSuccess(parser, "1 2 t/Website", new EditGigCommand(Index.fromOneBased(1),
                Index.fromOneBased(2), Optional.of(new GigTitle("Website")), Optional.empty(), Optional.empty()));
        assertParseSuccess(parser, "1 2 t/" + "x".repeat(100), new EditGigCommand(Index.fromOneBased(1),
                Index.fromOneBased(2), Optional.of(new GigTitle("x".repeat(100))), Optional.empty(), Optional.empty()));
        assertParseSuccess(parser, "1 2 f/1800", new EditGigCommand(Index.fromOneBased(1),
                Index.fromOneBased(2), Optional.empty(), Optional.empty(), Optional.of(new Fee("1800"))));
    }

    @Test
    public void parse_missingFieldsExtraIndexesOrStatus_rejectsInput() {
        for (String input : new String[] {"", "1 2", "1 t/Website", "1 2 3 t/Website", "1 2 s/COMPLETED",
            "1 2 t/Website s/COMPLETED", "1 2 s/COMPLETED f/100"}) {
            assertParseFailure(parser, input, usage);
        }
        for (String invalid : new String[] {"0", "-1", "abc", "1.5", "2147483648"}) {
            assertParseFailure(parser, invalid + " 1 t/Website", usage);
            assertParseFailure(parser, "1 " + invalid + " t/Website", usage);
        }
    }

    @Test
    public void parse_invalidField_rejectsEntireEdit() {
        for (String title : new String[] {"", "  ", "x".repeat(101)}) {
            assertParseFailure(parser, "1 1 f/100 t/" + title, GigTitle.MESSAGE_CONSTRAINTS);
        }
        for (String date : new String[] {"", "2027-02-29", "2027-13-01", "2027-1-1", "31-01-2027"}) {
            assertParseFailure(parser, "1 1 t/Website d/" + date, Deadline.MESSAGE_CONSTRAINTS);
        }
        for (String fee : new String[] {"", "0", "-1", "abc", "1.234", "1e3"}) {
            assertParseFailure(parser, "1 1 t/Website f/" + fee, Fee.MESSAGE_CONSTRAINTS);
        }
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
}
