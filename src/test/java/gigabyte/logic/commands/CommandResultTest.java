package gigabyte.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import gigabyte.model.gig.GigStatus;
import gigabyte.model.gig.GigTitleContainsKeywordsPredicate;

public class CommandResultTest {
    @Test
    public void deadlineAndOutstandingViews_useIndependentFlags() {
        CommandResult deadlines = CommandResult.deadlineOverview("feedback");
        assertTrue(deadlines.shouldShowDeadlineOverview());
        assertFalse(deadlines.shouldShowOutstandingPayments());
        assertFalse(CommandResult.forOutstandingPayments("feedback").shouldShowDeadlineOverview());
        assertFalse(new CommandResult("feedback").shouldShowDeadlineOverview());
        assertEquals(deadlines, CommandResult.deadlineOverview("feedback"));
        assertEquals(deadlines.hashCode(), CommandResult.deadlineOverview("feedback").hashCode());
        assertNotEquals(new CommandResult("feedback"), deadlines);
        assertNotEquals(deadlines, CommandResult.forOutstandingPayments("feedback"));
    }

    @Test
    public void outstandingPayments_includedInResultIdentity() {
        CommandResult result = CommandResult.forOutstandingPayments("feedback");
        assertTrue(result.shouldShowOutstandingPayments());
        assertFalse(new CommandResult("feedback").shouldShowOutstandingPayments());
        assertEquals(result, CommandResult.forOutstandingPayments("feedback"));
        assertEquals(result.hashCode(), CommandResult.forOutstandingPayments("feedback").hashCode());
        assertNotEquals(result, new CommandResult("feedback"));
        assertNotEquals(new CommandResult("feedback"), result);
        assertThrows(NullPointerException.class, () -> CommandResult.forOutstandingPayments(null));
    }

    @Test
    public void gigStatusFilter_includedInResultIdentity() {
        CommandResult result = new CommandResult("feedback", GigStatus.IN_PROGRESS);
        CommandResult same = new CommandResult("feedback", GigStatus.IN_PROGRESS);
        assertEquals(GigStatus.IN_PROGRESS, result.getGigStatusFilter());
        assertNull(new CommandResult("feedback").getGigStatusFilter());
        assertEquals(result, same);
        assertEquals(result.hashCode(), same.hashCode());
        assertNotEquals(result, new CommandResult("feedback", GigStatus.COMPLETED));
        assertNotEquals(result, new CommandResult("feedback"));
        assertThrows(NullPointerException.class, () -> new CommandResult("feedback", (GigStatus) null));
    }

    @Test
    public void equals() {
        CommandResult commandResult = new CommandResult("feedback");

        // same values -> returns true
        assertTrue(commandResult.equals(new CommandResult("feedback")));
        assertTrue(commandResult.equals(new CommandResult("feedback", false, false)));

        // same object -> returns true
        assertTrue(commandResult.equals(commandResult));

        // null -> returns false
        assertFalse(commandResult.equals(null));

        // different types -> returns false
        assertFalse(commandResult.equals(0.5f));

        // different feedbackToUser value -> returns false
        assertFalse(commandResult.equals(new CommandResult("different")));

        // different showHelp value -> returns false
        assertFalse(commandResult.equals(new CommandResult("feedback", true, false)));

        // different exit value -> returns false
        assertFalse(commandResult.equals(new CommandResult("feedback", false, true)));
    }

    @Test
    public void hashcode() {
        CommandResult commandResult = new CommandResult("feedback");

        // same values -> returns same hashcode
        assertEquals(commandResult.hashCode(), new CommandResult("feedback").hashCode());

        // different feedbackToUser value -> returns different hashcode
        assertNotEquals(commandResult.hashCode(), new CommandResult("different").hashCode());

        // different showHelp value -> returns different hashcode
        assertNotEquals(commandResult.hashCode(), new CommandResult("feedback", true, false).hashCode());

        // different exit value -> returns different hashcode
        assertNotEquals(commandResult.hashCode(), new CommandResult("feedback", false, true).hashCode());
    }

    @Test
    public void toStringMethod() {
        CommandResult commandResult = new CommandResult("feedback");
        String expected = CommandResult.class.getCanonicalName() + "{feedbackToUser="
                + commandResult.getFeedbackToUser() + ", showHelp=" + commandResult.shouldShowHelp()
                + ", exit=" + commandResult.shouldExit()
                + ", gigStatusFilter=null, showOutstandingPayments=false, showDeadlineOverview=false, "
                + "gigTitleSearch=null}";
        assertEquals(expected, commandResult.toString());
    }

    @Test
    public void toString_gigTitleSearch_includesSearchState() {
        CommandResult commandResult = CommandResult.createGigTitleSearchResult("feedback",
                new GigTitleContainsKeywordsPredicate(List.of("website")));
        String expected = CommandResult.class.getCanonicalName() + "{feedbackToUser=feedback, showHelp=false, "
                + "exit=false, gigStatusFilter=null, showOutstandingPayments=false, showDeadlineOverview=false, "
                + "gigTitleSearch=gigabyte.model.gig.GigTitleContainsKeywordsPredicate{keywords=[website]}}";

        assertEquals(expected, commandResult.toString());
    }
}
