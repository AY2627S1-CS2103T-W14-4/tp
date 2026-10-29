package gigabyte.logic;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.logging.Logger;

import gigabyte.commons.core.GuiSettings;
import gigabyte.commons.core.LogsCenter;
import gigabyte.logic.commands.Command;
import gigabyte.logic.commands.CommandResult;
import gigabyte.logic.commands.ExitCommand;
import gigabyte.logic.commands.HelpCommand;
import gigabyte.logic.commands.exceptions.CommandException;
import gigabyte.logic.parser.GigabyteParser;
import gigabyte.logic.parser.exceptions.ParseException;
import gigabyte.model.Model;
import gigabyte.model.client.Client;
import gigabyte.model.gig.Gig;
import gigabyte.model.gig.PaymentObligation;
import gigabyte.storage.Storage;
import javafx.collections.ObservableList;

/**
 * The main LogicManager of the app.
 */
public class LogicManager implements Logic {
    public static final String FILE_OPS_ERROR_FORMAT = "Could not save data due to the following error: %s";

    public static final String FILE_OPS_PERMISSION_ERROR_FORMAT =
            "Could not save data to file %s due to insufficient permissions to write to the file or the folder.";

    public static final String DATA_RECOVERY_ERROR_FORMAT =
            "Commands are disabled because the unreadable data file could not be backed up. "
            + "Only help and exit are available. Close Gigabyte, back up or repair %s, and restart.";

    private final Logger logger = LogsCenter.getLogger(LogicManager.class);

    private final Model model;
    private final Storage storage;
    private final GigabyteParser gigabyteParser;

    /**
     * Constructs a {@code LogicManager} with the given {@code Model} and {@code Storage}.
     */
    public LogicManager(Model model, Storage storage) {
        this.model = model;
        this.storage = storage;
        gigabyteParser = new GigabyteParser();
    }

    @Override
    public CommandResult execute(String commandText) throws CommandException, ParseException {
        logger.info("----------------[USER COMMAND][" + commandText + "]");

        CommandResult commandResult;
        Command command = gigabyteParser.parseCommand(commandText);
        if (storage.isDataSavingBlocked()) {
            if (command instanceof HelpCommand || command instanceof ExitCommand) {
                return command.execute(model);
            }
            throw new CommandException(String.format(DATA_RECOVERY_ERROR_FORMAT, storage.getGigabyteDataFilePath()));
        }
        commandResult = command.execute(model);

        try {
            storage.saveGigabyteData(model.getGigabyteData());
        } catch (AccessDeniedException e) {
            throw new CommandException(String.format(FILE_OPS_PERMISSION_ERROR_FORMAT, e.getMessage()), e);
        } catch (IOException ioe) {
            throw new CommandException(String.format(FILE_OPS_ERROR_FORMAT, ioe.getMessage()), ioe);
        }

        return commandResult;
    }

    @Override
    public ObservableList<Client> getFilteredClientList() {
        return model.getFilteredClientList();
    }

    @Override
    public ObservableList<Gig> getGigList() {
        return model.getGigList();
    }

    @Override
    public ObservableList<PaymentObligation> getPaymentObligationList() {
        return model.getPaymentObligationList();
    }

    @Override
    public GuiSettings getGuiSettings() {
        return model.getGuiSettings();
    }

    @Override
    public void setGuiSettings(GuiSettings guiSettings) {
        model.setGuiSettings(guiSettings);
    }
}
