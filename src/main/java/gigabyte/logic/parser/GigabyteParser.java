package gigabyte.logic.parser;

import static gigabyte.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static gigabyte.logic.Messages.MESSAGE_UNKNOWN_COMMAND;

import java.util.Locale;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import gigabyte.commons.core.LogsCenter;
import gigabyte.logic.commands.AddClientCommand;
import gigabyte.logic.commands.AddGigCommand;
import gigabyte.logic.commands.AddPaymentCommand;
import gigabyte.logic.commands.ClearClientsCommand;
import gigabyte.logic.commands.Command;
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

        // Command words are not case-sensitive; arguments keep their case.
        final String commandWord = matcher.group("commandWord").toLowerCase(Locale.ROOT);
        final String arguments = matcher.group("arguments");

        // Note to developers: Change LOG_LEVEL in LogsCenter to enable lower level (i.e., FINE, FINER and lower)
        // log messages such as the one below.
        // Lower level log messages are used sparingly to minimize noise in the code.
        logger.fine("Command word: " + commandWord + "; Arguments: " + arguments);

        return switch (commandWord) {
            case AddClientCommand.COMMAND_WORD -> new AddClientCommandParser().parse(arguments);
            case AddGigCommand.COMMAND_WORD -> new AddGigCommandParser().parse(arguments);
            case AddPaymentCommand.COMMAND_WORD -> new AddPaymentCommandParser().parse(arguments);
            case MarkPaymentPaidCommand.COMMAND_WORD -> new MarkPaymentPaidCommandParser().parse(arguments);
            case MarkPaymentUnpaidCommand.COMMAND_WORD -> new MarkPaymentUnpaidCommandParser().parse(arguments);
            case DeletePaymentCommand.COMMAND_WORD -> new DeletePaymentCommandParser().parse(arguments);
            case EditPaymentCommand.COMMAND_WORD -> new EditPaymentCommandParser().parse(arguments);
            case SetGigStatusCommand.COMMAND_WORD -> new SetGigStatusCommandParser().parse(arguments);
            case EditClientCommand.COMMAND_WORD -> new EditClientCommandParser().parse(arguments);
            case EditGigCommand.COMMAND_WORD -> new EditGigCommandParser().parse(arguments);
            case DeleteClientCommand.COMMAND_WORD -> new DeleteClientCommandParser().parse(arguments);
            case DeleteGigCommand.COMMAND_WORD -> new DeleteGigCommandParser().parse(arguments);
            case DeadlinesCommand.COMMAND_WORD -> {
                requireNoArguments(arguments, DeadlinesCommand.MESSAGE_USAGE);
                yield new DeadlinesCommand();
            }
            case ClearClientsCommand.COMMAND_WORD -> new ClearClientsCommand();
            case FindClientCommand.COMMAND_WORD -> new FindClientCommandParser().parse(arguments);
            case FindGigCommand.COMMAND_WORD -> new FindGigCommandParser().parse(arguments);
            case FilterGigsCommand.COMMAND_WORD -> new FilterGigsCommandParser().parse(arguments);
            case ListClientsCommand.COMMAND_WORD -> new ListClientsCommand();
            case OutstandingCommand.COMMAND_WORD -> {
                requireNoArguments(arguments, OutstandingCommand.MESSAGE_USAGE);
                yield new OutstandingCommand();
            }
            case SummaryCommand.COMMAND_WORD -> new SummaryCommand();
            case ExitCommand.COMMAND_WORD -> new ExitCommand();
            case HelpCommand.COMMAND_WORD -> new HelpCommand();
            default -> {
                logger.finer("This user input caused a ParseException: " + userInput);
                throw new ParseException(MESSAGE_UNKNOWN_COMMAND);
            }
        };
    }

    /**
     * Rejects any input after the command word of a command that takes no parameters.
     *
     * @param arguments the input after the command word
     * @param messageUsage the usage message of the command
     * @throws ParseException if {@code arguments} is not blank
     */
    private static void requireNoArguments(String arguments, String messageUsage) throws ParseException {
        if (!arguments.isBlank()) {
            throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, messageUsage));
        }
    }

}
