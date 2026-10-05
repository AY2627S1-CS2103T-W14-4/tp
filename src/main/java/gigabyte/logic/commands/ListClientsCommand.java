package gigabyte.logic.commands;

import static gigabyte.model.Model.PREDICATE_SHOW_ALL_CLIENTS;
import static java.util.Objects.requireNonNull;

import gigabyte.model.Model;

/**
 * Lists all clients in the client list to the user.
 */
public class ListClientsCommand extends Command {

    public static final String COMMAND_WORD = "list";

    public static final String MESSAGE_SUCCESS = "Listed all clients.";


    @Override
    public CommandResult execute(Model model) {
        requireNonNull(model);
        model.updateFilteredClientList(PREDICATE_SHOW_ALL_CLIENTS);
        return new CommandResult(MESSAGE_SUCCESS);
    }
}
