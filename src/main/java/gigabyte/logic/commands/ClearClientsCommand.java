package gigabyte.logic.commands;

import static java.util.Objects.requireNonNull;

import gigabyte.model.GigabyteData;
import gigabyte.model.Model;

/**
 * Clears the client list.
 */
public class ClearClientsCommand extends Command {

    public static final String COMMAND_WORD = "clear";
    public static final String MESSAGE_SUCCESS = "Address book has been cleared!";


    @Override
    public CommandResult execute(Model model) {
        requireNonNull(model);
        model.setGigabyteData(new GigabyteData());
        return new CommandResult(MESSAGE_SUCCESS);
    }
}
