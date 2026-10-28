package gigabyte.logic.parser;

import static gigabyte.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static gigabyte.logic.Messages.MESSAGE_UNKNOWN_COMMAND;
import static gigabyte.testutil.Assert.assertThrows;
import static gigabyte.testutil.TypicalIndexes.INDEX_FIRST_CLIENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import gigabyte.commons.core.index.Index;
import gigabyte.logic.commands.AddClientCommand;
import gigabyte.logic.commands.AddGigCommand;
import gigabyte.logic.commands.AddPaymentCommand;
import gigabyte.logic.commands.ClearClientsCommand;
import gigabyte.logic.commands.DeadlinesCommand;
import gigabyte.logic.commands.DeleteClientCommand;
import gigabyte.logic.commands.DeleteGigCommand;
import gigabyte.logic.commands.DeletePaymentCommand;
import gigabyte.logic.commands.EditClientCommand;
import gigabyte.logic.commands.EditGigCommand;
import gigabyte.logic.commands.EditPaymentCommand;
import gigabyte.logic.commands.ExitCommand;
import gigabyte.logic.commands.FilterGigsCommand;
import gigabyte.logic.commands.FindClientCommand;
import gigabyte.logic.commands.FindGigCommand;
import gigabyte.logic.commands.HelpCommand;
import gigabyte.logic.commands.ListClientsCommand;
import gigabyte.logic.commands.MarkPaymentPaidCommand;
import gigabyte.logic.commands.MarkPaymentUnpaidCommand;
import gigabyte.logic.commands.OutstandingCommand;
import gigabyte.logic.commands.SetGigStatusCommand;
import gigabyte.logic.commands.SummaryCommand;
import gigabyte.logic.parser.exceptions.ParseException;
import gigabyte.model.client.Client;
import gigabyte.model.client.ClientNameContainsKeywordsPredicate;
import gigabyte.model.gig.Deadline;
import gigabyte.model.gig.Fee;
import gigabyte.model.gig.GigStatus;
import gigabyte.model.gig.GigTitle;
import gigabyte.model.gig.GigTitleContainsKeywordsPredicate;
import gigabyte.testutil.ClientBuilder;
import gigabyte.testutil.ClientUtil;
import gigabyte.testutil.EditClientDescriptorBuilder;

public class GigabyteParserTest {

    private final GigabyteParser parser = new GigabyteParser();

    @Test
    public void parseCommand_deleteGig() throws Exception {
        assertEquals(new DeleteGigCommand(INDEX_FIRST_CLIENT, Index.fromOneBased(2)),
                parser.parseCommand("deletegig 1 2"));
        assertThrows(ParseException.class,
                String.format(MESSAGE_INVALID_COMMAND_FORMAT, DeleteGigCommand.MESSAGE_USAGE), () ->
                        parser.parseCommand("deletegig 1 2 3"));
    }

    @Test
    public void parseCommand_editGig() throws Exception {
        assertEquals(new EditGigCommand(INDEX_FIRST_CLIENT, INDEX_FIRST_CLIENT,
                Optional.of(new GigTitle("Updated website")), Optional.empty(),
                Optional.empty()), parser.parseCommand("editgig 1 1 t/Updated website"));
    }

    @Test
    public void parseCommand_outstanding_acceptsNoArgumentsAndRejectsExtraInput() throws Exception {
        // EP: command word only, with or without surrounding spaces
        assertTrue(parser.parseCommand("outstanding") instanceof OutstandingCommand);
        assertTrue(parser.parseCommand("  outstanding  ") instanceof OutstandingCommand);

        // EP: command word followed by any argument
        String error = String.format(MESSAGE_INVALID_COMMAND_FORMAT, OutstandingCommand.MESSAGE_USAGE);
        assertThrows(ParseException.class, error, () -> parser.parseCommand("outstanding 1"));
        assertThrows(ParseException.class, error, () -> parser.parseCommand("outstanding s/PAID"));
    }

    @Test
    public void parseCommand_deadlines() throws Exception {
        assertTrue(parser.parseCommand(DeadlinesCommand.COMMAND_WORD) instanceof DeadlinesCommand);
    }

    @Test
    public void parseCommand_filterGigs() throws Exception {
        assertEquals(new FilterGigsCommand(GigStatus.IN_PROGRESS), parser.parseCommand("gigs s/in_progress"));
    }

    @Test
    public void parseCommand_add() throws Exception {
        Client client = new ClientBuilder().build();
        AddClientCommand command = (AddClientCommand) parser.parseCommand(ClientUtil.getAddClientCommand(client));
        assertEquals(new AddClientCommand(client), command);
    }

    @Test
    public void parseCommand_addGig() throws Exception {
        assertEquals(new AddGigCommand(INDEX_FIRST_CLIENT, new GigTitle("Website redesign"),
                GigStatus.NOT_STARTED, new Deadline("2027-01-31"), new Fee("1250.00")),
                parser.parseCommand("addgig 1 t/Website redesign s/NOT_STARTED d/2027-01-31 f/1250.00"));
    }

    @Test
    public void parseCommand_addPayment() throws Exception {
        assertEquals(new AddPaymentCommand(INDEX_FIRST_CLIENT, INDEX_FIRST_CLIENT, new Fee("50.00"),
                new Deadline("2027-01-31")), parser.parseCommand("addpayment 1 1 a/50.00 d/2027-01-31"));
    }

    @Test
    public void parseCommand_markPaymentPaid() throws Exception {
        assertEquals(new MarkPaymentPaidCommand(INDEX_FIRST_CLIENT, INDEX_FIRST_CLIENT,
                Index.fromOneBased(3)), parser.parseCommand("markpaid 1 1 3"));
    }

    @Test
    public void parseCommand_markPaymentUnpaid() throws Exception {
        assertEquals(new MarkPaymentUnpaidCommand(INDEX_FIRST_CLIENT, INDEX_FIRST_CLIENT,
                Index.fromOneBased(3)), parser.parseCommand("markunpaid 1 1 3"));
    }

    @Test
    public void parseCommand_deletePayment() throws Exception {
        assertEquals(new DeletePaymentCommand(INDEX_FIRST_CLIENT, Index.fromOneBased(2),
                Index.fromOneBased(3)), parser.parseCommand("deletepayment 1 2 3"));
        assertThrows(ParseException.class,
                String.format(MESSAGE_INVALID_COMMAND_FORMAT, DeletePaymentCommand.MESSAGE_USAGE), () ->
                        parser.parseCommand("deletepayment 1 2"));
    }

    @Test
    public void parseCommand_editPayment() throws Exception {
        assertEquals(new EditPaymentCommand(INDEX_FIRST_CLIENT, INDEX_FIRST_CLIENT, Index.fromOneBased(3),
                Optional.of(new Fee("650")), Optional.of(new Deadline("2027-05-15"))),
                parser.parseCommand("editpayment 1 1 3 a/650 d/2027-05-15"));
    }

    @Test
    public void parseCommand_setGigStatus() throws Exception {
        assertEquals(new SetGigStatusCommand(INDEX_FIRST_CLIENT, INDEX_FIRST_CLIENT, GigStatus.COMPLETED),
                parser.parseCommand("setstatus 1 1 s/completed"));
    }

    @Test
    public void parseCommand_clear() throws Exception {
        assertTrue(parser.parseCommand(ClearClientsCommand.COMMAND_WORD) instanceof ClearClientsCommand);
        assertTrue(parser.parseCommand(ClearClientsCommand.COMMAND_WORD + " 3") instanceof ClearClientsCommand);
    }

    @Test
    public void parseCommand_delete() throws Exception {
        DeleteClientCommand command = (DeleteClientCommand) parser.parseCommand(
                DeleteClientCommand.COMMAND_WORD + " " + INDEX_FIRST_CLIENT.getOneBased());
        assertEquals(new DeleteClientCommand(INDEX_FIRST_CLIENT), command);
    }

    @Test
    public void parseCommand_edit() throws Exception {
        Client client = new ClientBuilder().build();
        EditClientCommand.EditClientDescriptor descriptor = new EditClientDescriptorBuilder(client).build();
        EditClientCommand command = (EditClientCommand) parser.parseCommand(EditClientCommand.COMMAND_WORD + " "
                + INDEX_FIRST_CLIENT.getOneBased() + " " + ClientUtil.getEditClientDescriptorDetails(descriptor));
        assertEquals(new EditClientCommand(INDEX_FIRST_CLIENT, descriptor), command);
    }

    @Test
    public void parseCommand_exit() throws Exception {
        assertTrue(parser.parseCommand(ExitCommand.COMMAND_WORD) instanceof ExitCommand);
        assertTrue(parser.parseCommand(ExitCommand.COMMAND_WORD + " 3") instanceof ExitCommand);
    }

    @Test
    public void parseCommand_find() throws Exception {
        List<String> keywords = List.of("foo", "bar", "baz");
        FindClientCommand command = (FindClientCommand) parser.parseCommand(
                FindClientCommand.COMMAND_WORD + " " + keywords.stream().collect(Collectors.joining(" ")));
        assertEquals(new FindClientCommand(new ClientNameContainsKeywordsPredicate(keywords)), command);
    }

    @Test
    public void parseCommand_findgig() throws Exception {
        List<String> keywords = List.of("website", "redesign");
        assertEquals(new FindGigCommand(new GigTitleContainsKeywordsPredicate(keywords)),
                parser.parseCommand("findgig website redesign"));
    }

    @Test
    public void parseCommand_findgigWithoutKeywords_throwsParseException() {
        assertThrows(ParseException.class, () -> parser.parseCommand("findgig   "));
    }

    @Test
    public void parseCommand_summary() throws Exception {
        assertTrue(parser.parseCommand(SummaryCommand.COMMAND_WORD) instanceof SummaryCommand);
    }

    @Test
    public void parseCommand_help() throws Exception {
        assertTrue(parser.parseCommand(HelpCommand.COMMAND_WORD) instanceof HelpCommand);
        assertTrue(parser.parseCommand(HelpCommand.COMMAND_WORD + " 3") instanceof HelpCommand);
    }

    @Test
    public void parseCommand_list() throws Exception {
        assertTrue(parser.parseCommand(ListClientsCommand.COMMAND_WORD) instanceof ListClientsCommand);
        assertTrue(parser.parseCommand(ListClientsCommand.COMMAND_WORD + " 3") instanceof ListClientsCommand);
    }

    @Test
    public void parseCommand_upperAndMixedCaseCommandWords_parsedAsLowerCase() throws Exception {
        // EP: upper-case command word
        assertTrue(parser.parseCommand("LIST") instanceof ListClientsCommand);
        assertTrue(parser.parseCommand("OUTSTANDING") instanceof OutstandingCommand);

        // EP: mixed-case command word
        assertTrue(parser.parseCommand("List") instanceof ListClientsCommand);
        assertTrue(parser.parseCommand("hElP") instanceof HelpCommand);
        assertEquals(new AddGigCommand(INDEX_FIRST_CLIENT, new GigTitle("Website redesign"),
                GigStatus.NOT_STARTED, new Deadline("2027-01-31"), new Fee("1250.00")),
                parser.parseCommand("AddGig 1 t/Website redesign s/NOT_STARTED d/2027-01-31 f/1250.00"));

        // Argument validation still applies after the command word is lower-cased
        assertThrows(ParseException.class,
                String.format(MESSAGE_INVALID_COMMAND_FORMAT, OutstandingCommand.MESSAGE_USAGE), () ->
                        parser.parseCommand("OUTSTANDING 1"));
    }

    @Test
    public void parseCommand_upperCaseCommandWord_preservesArgumentCase() throws Exception {
        assertEquals(new AddGigCommand(INDEX_FIRST_CLIENT, new GigTitle("Website Redesign"),
                GigStatus.NOT_STARTED, new Deadline("2027-01-31"), new Fee("1250.00")),
                parser.parseCommand("ADDGIG 1 t/Website Redesign s/NOT_STARTED d/2027-01-31 f/1250.00"));
        assertEquals(new FindGigCommand(new GigTitleContainsKeywordsPredicate(List.of("Website", "REDESIGN"))),
                parser.parseCommand("FINDGIG Website REDESIGN"));
    }

    @Test
    public void parseCommand_unrecognisedInput_throwsParseException() {
        assertThrows(ParseException.class, String.format(MESSAGE_INVALID_COMMAND_FORMAT, HelpCommand.MESSAGE_USAGE), ()
            -> parser.parseCommand(""));
    }

    @Test
    public void parseCommand_unknownCommand_throwsParseException() {
        assertThrows(ParseException.class, MESSAGE_UNKNOWN_COMMAND, () -> parser.parseCommand("unknownCommand"));
        assertThrows(ParseException.class, MESSAGE_UNKNOWN_COMMAND, () -> parser.parseCommand("LISTS"));
    }
}
