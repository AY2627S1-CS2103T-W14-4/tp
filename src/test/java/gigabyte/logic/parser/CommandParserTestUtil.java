package gigabyte.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gigabyte.logic.commands.Command;
import gigabyte.logic.parser.exceptions.ParseException;
import gigabyte.model.gig.Deadline;
import gigabyte.model.gig.GigStatus;

/**
 * Contains helper methods for testing command parsers.
 */
public class CommandParserTestUtil {

    public static String rejectedValueMessage(String constraint, String value) {
        return String.format("%s (rejected value: '%s')", constraint, value);
    }

    /**
     * Asserts that the parsing of {@code userInput} by {@code parser} is successful and the command created
     * is equal to {@code expectedCommand}.
     */
    public static void assertParseSuccess(Parser<? extends Command> parser, String userInput,
            Command expectedCommand) {
        try {
            Command command = parser.parse(userInput);
            assertEquals(expectedCommand, command);
        } catch (ParseException pe) {
            throw new IllegalArgumentException("Invalid userInput.", pe);
        }
    }

    /**
     * Asserts that the parsing of {@code userInput} by {@code parser} is unsuccessful and the error message
     * is equal to {@code expectedMessage}.
     */
    public static void assertParseFailure(Parser<? extends Command> parser, String userInput, String expectedMessage) {
        try {
            parser.parse(userInput);
            throw new AssertionError("The expected ParseException was not thrown.");
        } catch (ParseException pe) {
            if (expectedMessage.equals(Deadline.MESSAGE_CONSTRAINTS)
                    || expectedMessage.equals(GigStatus.MESSAGE_CONSTRAINTS)) {
                // Most parser cases assert the validation category; dedicated regression cases assert the exact value.
                assertTrue(pe.getMessage().startsWith(expectedMessage + " (rejected value: '"));
            } else {
                assertEquals(expectedMessage, pe.getMessage());
            }
        }
    }
}
