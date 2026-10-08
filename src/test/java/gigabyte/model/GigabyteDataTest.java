package gigabyte.model;

import static gigabyte.logic.commands.CommandTestUtil.VALID_ADDRESS_BOB;
import static gigabyte.logic.commands.CommandTestUtil.VALID_TAG_HUSBAND;
import static gigabyte.testutil.Assert.assertThrows;
import static gigabyte.testutil.TypicalClients.ALICE;
import static gigabyte.testutil.TypicalClients.getTypicalGigabyteData;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

import gigabyte.model.client.Client;
import gigabyte.model.client.exceptions.DuplicateClientException;
import gigabyte.model.gig.Gig;
import gigabyte.model.gig.Deadline;
import gigabyte.model.gig.Fee;
import gigabyte.model.gig.GigStatus;
import gigabyte.model.gig.PaymentObligation;
import gigabyte.model.gig.exceptions.GigNotFoundException;
import gigabyte.testutil.ClientBuilder;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class GigabyteDataTest {

    private final GigabyteData gigabyteData = new GigabyteData();

    @Test
    public void constructor() {
        assertEquals(List.of(), gigabyteData.getClientList());
    }

    @Test
    public void resetData_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> gigabyteData.resetData(null));
    }

    @Test
    public void resetData_withValidReadOnlyGigabyteData_replacesData() {
        GigabyteData newData = getTypicalGigabyteData();
        gigabyteData.resetData(newData);
        assertEquals(newData, gigabyteData);
    }

    @Test
    public void resetData_withDuplicateClients_throwsDuplicateClientException() {
        // Two clients with the same identity fields
        Client editedAlice = new ClientBuilder(ALICE).withAddress(VALID_ADDRESS_BOB).withTags(VALID_TAG_HUSBAND)
                .build();
        List<Client> newClients = List.of(ALICE, editedAlice);
        GigabyteDataStub newData = new GigabyteDataStub(newClients);

        assertThrows(DuplicateClientException.class, () -> gigabyteData.resetData(newData));
    }

    @Test
    public void hasClient_nullClient_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> gigabyteData.hasClient(null));
    }

    @Test
    public void hasClient_clientNotInGigabyteData_returnsFalse() {
        assertFalse(gigabyteData.hasClient(ALICE));
    }

    @Test
    public void hasClient_clientInGigabyteData_returnsTrue() {
        gigabyteData.addClient(ALICE);
        assertTrue(gigabyteData.hasClient(ALICE));
    }

    @Test
    public void hasClient_clientWithSameIdentityFieldsInGigabyteData_returnsTrue() {
        gigabyteData.addClient(ALICE);
        Client editedAlice = new ClientBuilder(ALICE).withAddress(VALID_ADDRESS_BOB).withTags(VALID_TAG_HUSBAND)
                .build();
        assertTrue(gigabyteData.hasClient(editedAlice));
    }

    @Test
    public void addPaymentObligation_existingGig_addsObligation() {
        gigabyteData.addClient(ALICE);
        Gig gig = new Gig(ALICE, GigStatus.NOT_STARTED, new Deadline("2026-12-31"), new Fee("100"));
        gigabyteData.addGig(gig);
        PaymentObligation obligation = new PaymentObligation(gig, new Fee("50"),
                new Deadline("2026-11-30"), false);

        gigabyteData.addPaymentObligation(obligation);

        assertEquals(List.of(obligation), gigabyteData.getPaymentObligationList());
    }

    @Test
    public void addPaymentObligation_missingGig_throwsGigNotFoundException() {
        PaymentObligation obligation = new PaymentObligation(
                new Gig(ALICE, GigStatus.NOT_STARTED, new Deadline("2026-12-31"), new Fee("100")),
                new Fee("50"), new Deadline("2026-11-30"), false);

        assertThrows(GigNotFoundException.class, () -> gigabyteData.addPaymentObligation(obligation));
    }

    @Test
    public void getClientList_modifyList_throwsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () -> gigabyteData.getClientList().remove(0));
    }

    @Test
    public void toStringMethod() {
        String expected = GigabyteData.class.getCanonicalName()
                + "{clients=" + gigabyteData.getClientList()
                + ", gigs=" + gigabyteData.getGigList()
                + ", paymentObligations=" + gigabyteData.getPaymentObligationList() + "}";
        assertEquals(expected, gigabyteData.toString());
    }

    /**
     * A stub ReadOnlyGigabyteData whose clients list can violate interface constraints.
     */
    private static class GigabyteDataStub implements ReadOnlyGigabyteData {
        private final ObservableList<Client> clients = FXCollections.observableArrayList();
        private final ObservableList<Gig> gigs = FXCollections.observableArrayList();

        GigabyteDataStub(Collection<Client> clients) {
            this.clients.setAll(clients);
        }

        @Override
        public ObservableList<Client> getClientList() {
            return clients;
        }

        @Override
        public ObservableList<Gig> getGigList() {
            return gigs;
        }
    }

}
