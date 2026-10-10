package gigabyte.logic.parser;

import static gigabyte.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static gigabyte.logic.Messages.MESSAGE_UNKNOWN_COMMAND;
import static gigabyte.testutil.Assert.assertThrows;
import static gigabyte.testutil.TypicalIndexes.INDEX_FIRST_CLIENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import gigabyte.logic.commands.AddClientCommand;
import gigabyte.logic.commands.AddGigCommand;
import gigabyte.logic.commands.AddPaymentCommand;
import gigabyte.logic.commands.ClearClientsCommand;
import gigabyte.logic.commands.DeleteClientCommand;
import gigabyte.logic.commands.EditClientCommand;
import gigabyte.logic.commands.ExitCommand;
import gigabyte.logic.commands.FindClientCommand;
import gigabyte.logic.commands.HelpCommand;
import gigabyte.logic.commands.ListClientsCommand;
import gigabyte.logic.parser.exceptions.ParseException;
import gigabyte.model.client.Client;
import gigabyte.model.client.ClientNameContainsKeywordsPredicate;
import gigabyte.model.gig.Deadline;
import gigabyte.model.gig.Fee;
import gigabyte.model.gig.GigStatus;
import gigabyte.model.gig.GigTitle;
import gigabyte.testutil.ClientBuilder;
import gigabyte.testutil.ClientUtil;
import gigabyte.testutil.EditClientDescriptorBuilder;

public class GigabyteParserTest {

    private final GigabyteParser parser = new GigabyteParser();

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
    public void parseCommand_unrecognisedInput_throwsParseException() {
        assertThrows(ParseException.class, String.format(MESSAGE_INVALID_COMMAND_FORMAT, HelpCommand.MESSAGE_USAGE), ()
            -> parser.parseCommand(""));
    }

    @Test
    public void parseCommand_unknownCommand_throwsParseException() {
        assertThrows(ParseException.class, MESSAGE_UNKNOWN_COMMAND, () -> parser.parseCommand("unknownCommand"));
    }
}
