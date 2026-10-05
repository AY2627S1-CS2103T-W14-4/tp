package gigabyte.logic.parser;

import static gigabyte.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static gigabyte.logic.Messages.MESSAGE_UNKNOWN_COMMAND;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import gigabyte.commons.core.LogsCenter;
import gigabyte.logic.commands.AddClientCommand;
import gigabyte.logic.commands.ClearClientsCommand;
import gigabyte.logic.commands.Command;
import gigabyte.logic.commands.DeleteClientCommand;
import gigabyte.logic.commands.EditClientCommand;
import gigabyte.logic.commands.ExitCommand;
import gigabyte.logic.commands.FindClientCommand;
import gigabyte.logic.commands.HelpCommand;
import gigabyte.logic.commands.ListClientsCommand;
import gigabyte.logic.parser.exceptions.ParseException;

/**
 * Parses user input.
 */
public class GigabyteParser {

    /**
     * Used for initial separation of command word and args.
     */
    private static final Pattern BASIC_COMMAND_FORMAT = Pattern.compile("(?<commandWord>\\S+)(?<arguments>.*)");
    private static final Logger logger = LogsCenter.getLogger(GigabyteParser.class);

    /**
     * Parses user input into command for execution.
     *
     * @param userInput full user input string
     * @return the command based on the user input
     * @throws ParseException if the user input does not conform to the expected format
     */
    public Command parseCommand(String userInput) throws ParseException {
        final Matcher matcher = BASIC_COMMAND_FORMAT.matcher(userInput.trim());
        if (!matcher.matches()) {
            throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, HelpCommand.MESSAGE_USAGE));
        }

        final String commandWord = matcher.group("commandWord");
        final String arguments = matcher.group("arguments");

        // Note to developers: Change LOG_LEVEL in LogsCenter to enable lower level (i.e., FINE, FINER and lower)
        // log messages such as the one below.
        // Lower level log messages are used sparingly to minimize noise in the code.
        logger.fine("Command word: " + commandWord + "; Arguments: " + arguments);

        return switch (commandWord) {
            case AddClientCommand.COMMAND_WORD -> new AddClientCommandParser().parse(arguments);
            case EditClientCommand.COMMAND_WORD -> new EditClientCommandParser().parse(arguments);
            case DeleteClientCommand.COMMAND_WORD -> new DeleteClientCommandParser().parse(arguments);
            case ClearClientsCommand.COMMAND_WORD -> new ClearClientsCommand();
            case FindClientCommand.COMMAND_WORD -> new FindClientCommandParser().parse(arguments);
            case ListClientsCommand.COMMAND_WORD -> new ListClientsCommand();
            case ExitCommand.COMMAND_WORD -> new ExitCommand();
            case HelpCommand.COMMAND_WORD -> new HelpCommand();
            default -> {
                logger.finer("This user input caused a ParseException: " + userInput);
                throw new ParseException(MESSAGE_UNKNOWN_COMMAND);
            }
        };
    }

}
