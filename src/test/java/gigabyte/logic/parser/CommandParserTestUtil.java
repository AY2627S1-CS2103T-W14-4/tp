package gigabyte.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gigabyte.logic.commands.Command;
import gigabyte.logic.parser.exceptions.ParseException;

/**
 * Contains helper methods for testing command parsers.
 */
public class CommandParserTestUtil {

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
            assertValidationMessage(expectedMessage, pe.getMessage());
        }
    }

    private static void assertValidationMessage(String expectedMessage, String actualMessage) {
        if (actualMessage.startsWith(expectedMessage + " (rejected value: '") && actualMessage.endsWith("')")) {
            assertTrue(actualMessage.length() <= expectedMessage.length() + 110);
            return;
        }
        assertEquals(expectedMessage, actualMessage);
    }
}
