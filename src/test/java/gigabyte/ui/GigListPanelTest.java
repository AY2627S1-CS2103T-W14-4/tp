package gigabyte.ui;

import static gigabyte.testutil.TypicalClients.ALICE;
import static gigabyte.testutil.TypicalClients.BENSON;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import gigabyte.model.gig.Deadline;
import gigabyte.model.gig.Fee;
import gigabyte.model.gig.Gig;
import gigabyte.model.gig.GigStatus;

public class GigListPanelTest {
    private static final Gig ALICE_GIG = new Gig(ALICE, GigStatus.IN_PROGRESS,
            new Deadline("2026-12-31"), new Fee("100"));

    @Test
    public void isGigForClient_matchingClient_returnsTrue() {
        assertTrue(GigListPanel.isGigForClient(ALICE_GIG, ALICE));
    }

    @Test
    public void isGigForClient_differentClient_returnsFalse() {
        assertFalse(GigListPanel.isGigForClient(ALICE_GIG, BENSON));
    }

    @Test
    public void isGigForClient_nullClient_returnsFalse() {
        assertFalse(GigListPanel.isGigForClient(ALICE_GIG, null));
    }
}
