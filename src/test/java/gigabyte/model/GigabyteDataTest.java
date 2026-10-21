package gigabyte.model;

import static gigabyte.logic.commands.CommandTestUtil.VALID_ADDRESS_BOB;
import static gigabyte.logic.commands.CommandTestUtil.VALID_TAG_HUSBAND;
import static gigabyte.testutil.Assert.assertThrows;
import static gigabyte.testutil.TypicalClients.ALICE;
import static gigabyte.testutil.TypicalClients.BENSON;
import static gigabyte.testutil.TypicalClients.getTypicalGigabyteData;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

import gigabyte.model.client.Client;
import gigabyte.model.client.exceptions.DuplicateClientException;
import gigabyte.model.gig.Deadline;
import gigabyte.model.gig.Fee;
import gigabyte.model.gig.Gig;
import gigabyte.model.gig.GigStatus;
import gigabyte.model.gig.GigTitle;
import gigabyte.model.gig.PaymentObligation;
import gigabyte.model.gig.exceptions.GigNotFoundException;
import gigabyte.testutil.ClientBuilder;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class GigabyteDataTest {
    private static final GigTitle TITLE = new GigTitle("Website redesign");
    private static final GigTitle OTHER_TITLE = new GigTitle("Logo design");

    private final GigabyteData gigabyteData = new GigabyteData();

    @Test
    public void constructor() {
        assertEquals(List.of(), gigabyteData.getClientList());
    }

    @Test
    public void readOnlyDataWithoutPaymentObligations_returnsEmptyList() {
        ReadOnlyGigabyteData data = new GigabyteDataStub(List.of());
        assertEquals(List.of(), data.getPaymentObligationList());
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
        Gig gig = new Gig(ALICE, TITLE, GigStatus.NOT_STARTED, new Deadline("2026-12-31"), new Fee("100"));
        gigabyteData.addGig(gig);
        PaymentObligation obligation = new PaymentObligation(gig, new Fee("50"),
                new Deadline("2026-11-30"), false);

        gigabyteData.addPaymentObligation(obligation);

        assertEquals(List.of(obligation), gigabyteData.getPaymentObligationList());
    }

    @Test
    public void addPaymentObligation_missingGig_throwsGigNotFoundException() {
        PaymentObligation obligation = new PaymentObligation(
                new Gig(ALICE, TITLE, GigStatus.NOT_STARTED, new Deadline("2026-12-31"), new Fee("100")),
                new Fee("50"), new Deadline("2026-11-30"), false);

        assertThrows(GigNotFoundException.class, () -> gigabyteData.addPaymentObligation(obligation));
    }

    @Test
    public void deletePaymentObligation_identicalPayments_removesExactInstanceAndPreservesOrder() {
        gigabyteData.addClient(ALICE);
        gigabyteData.addGig(new Gig(ALICE, TITLE, GigStatus.NOT_STARTED,
                new Deadline("2099-12-31"), new Fee("100")));
        Gig gig = gigabyteData.getGigList().get(0);
        PaymentObligation payment = new PaymentObligation(gig, new Fee("50"), new Deadline("2099-01-31"), false);
        gigabyteData.addPaymentObligation(payment);
        gigabyteData.addPaymentObligation(payment);
        gigabyteData.addPaymentObligation(payment.withPaid(true));
        List<PaymentObligation> before = List.copyOf(gigabyteData.getPaymentObligationList());

        gigabyteData.deletePaymentObligation(before.get(1));

        assertEquals(2, gigabyteData.getPaymentObligationList().size());
        assertSame(before.get(0), gigabyteData.getPaymentObligationList().get(0));
        assertSame(before.get(2), gigabyteData.getPaymentObligationList().get(1));
        assertSame(gig, gigabyteData.getGigList().get(0));
        assertSame(ALICE, gigabyteData.getClientList().get(0));
        assertThrows(IllegalArgumentException.class, () -> gigabyteData.deletePaymentObligation(before.get(1)));
        assertSame(before.get(0), gigabyteData.getPaymentObligationList().get(0));
        assertEquals(2, gigabyteData.getPaymentObligationList().size());
    }

    @Test
    public void deletePaymentObligation_equalButUnstoredInstance_throwsAndLeavesDataUnchanged() {
        gigabyteData.addClient(ALICE);
        gigabyteData.addGig(new Gig(ALICE, TITLE, GigStatus.NOT_STARTED,
                new Deadline("2099-12-31"), new Fee("100")));
        PaymentObligation payment = new PaymentObligation(gigabyteData.getGigList().get(0), new Fee("50"),
                new Deadline("2099-01-31"), false);
        gigabyteData.addPaymentObligation(payment);
        GigabyteData before = new GigabyteData(gigabyteData);

        assertThrows(IllegalArgumentException.class, () -> gigabyteData.deletePaymentObligation(payment));

        assertEquals(before, gigabyteData);
    }

    @Test
    public void deletePaymentObligation_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> gigabyteData.deletePaymentObligation(null));
    }
  
    @Test
    public void setPaymentObligation_exactTarget_replacesOnlySelectedIdenticalObligation() {
        gigabyteData.addClient(ALICE);
        Gig gig = new Gig(ALICE, TITLE, GigStatus.NOT_STARTED, new Deadline("2026-12-31"), new Fee("100"));
        gigabyteData.addGig(gig);
        PaymentObligation first = new PaymentObligation(gig, new Fee("50"),
                new Deadline("2026-11-30"), false);
        PaymentObligation second = new PaymentObligation(gig, new Fee("50"),
                new Deadline("2026-11-30"), false);
        gigabyteData.addPaymentObligation(first);
        gigabyteData.addPaymentObligation(second);
        PaymentObligation storedFirst = gigabyteData.getPaymentObligationList().get(0);
        PaymentObligation storedSecond = gigabyteData.getPaymentObligationList().get(1);
        PaymentObligation replacement = storedSecond.withDetails(new Fee("75"), storedSecond.getDueDate());

        gigabyteData.setPaymentObligation(storedSecond, replacement);

        assertSame(storedFirst, gigabyteData.getPaymentObligationList().get(0));
        assertNotSame(storedSecond, gigabyteData.getPaymentObligationList().get(1));
        assertEquals(new Fee("75"), gigabyteData.getPaymentObligationList().get(1).getAmount());
    }

    @Test
    public void setPaymentObligation_equalCopy_replacesStoredObligation() {
        gigabyteData.addClient(ALICE);
        Gig gig = new Gig(ALICE, TITLE, GigStatus.NOT_STARTED, new Deadline("2026-12-31"), new Fee("100"));
        gigabyteData.addGig(gig);
        PaymentObligation original = new PaymentObligation(gig, new Fee("50"),
                new Deadline("2026-11-30"), false);
        gigabyteData.addPaymentObligation(original);
        PaymentObligation stored = gigabyteData.getPaymentObligationList().get(0);
        PaymentObligation equalCopy = new PaymentObligation(stored.getGig(), stored.getAmount(),
                stored.getDueDate(), stored.isPaid());
        PaymentObligation replacement = equalCopy.withDetails(new Fee("75"), equalCopy.getDueDate());

        gigabyteData.setPaymentObligation(equalCopy, replacement);

        assertEquals(new Fee("75"), gigabyteData.getPaymentObligationList().get(0).getAmount());
        assertSame(stored.getGig(), gigabyteData.getPaymentObligationList().get(0).getGig());
    }

    @Test
    public void setPaymentObligation_equalCopyForFieldIdenticalGig_replacesMatchingGigObligation() {
        gigabyteData.addClient(ALICE);
        Gig firstGig = new Gig(ALICE, TITLE, GigStatus.NOT_STARTED,
                new Deadline("2026-12-31"), new Fee("100"));
        Gig secondGig = new Gig(ALICE, TITLE, GigStatus.NOT_STARTED,
                new Deadline("2026-12-31"), new Fee("100"));
        gigabyteData.addGig(firstGig);
        gigabyteData.addGig(secondGig);
        PaymentObligation firstPayment = new PaymentObligation(firstGig, new Fee("50"),
                new Deadline("2026-11-30"), false);
        PaymentObligation secondPayment = new PaymentObligation(secondGig, new Fee("50"),
                new Deadline("2026-11-30"), false);
        gigabyteData.addPaymentObligation(firstPayment);
        gigabyteData.addPaymentObligation(secondPayment);
        PaymentObligation storedFirst = gigabyteData.getPaymentObligationList().get(0);
        PaymentObligation storedSecond = gigabyteData.getPaymentObligationList().get(1);
        PaymentObligation secondCopy = new PaymentObligation(storedSecond.getGig(), storedSecond.getAmount(),
                storedSecond.getDueDate(), storedSecond.isPaid());
        PaymentObligation replacement = secondCopy.withDetails(new Fee("75"), secondCopy.getDueDate());

        gigabyteData.setPaymentObligation(secondCopy, replacement);

        assertSame(storedFirst, gigabyteData.getPaymentObligationList().get(0));
        assertEquals(new Fee("75"), gigabyteData.getPaymentObligationList().get(1).getAmount());
        assertTrue(gigabyteData.getPaymentObligationList().get(1).getGig().hasSameUid(secondGig));
    }

    @Test
    public void setPaymentObligation_changedAssociationOrState_throwsAndLeavesDataUnchanged() {
        gigabyteData.addClient(ALICE);
        gigabyteData.addClient(BENSON);
        Gig gig = new Gig(ALICE, TITLE, GigStatus.NOT_STARTED, new Deadline("2026-12-31"), new Fee("100"));
        Gig otherGig = new Gig(BENSON, OTHER_TITLE, GigStatus.NOT_STARTED,
                new Deadline("2026-12-31"), new Fee("100"));
        gigabyteData.addGig(gig);
        gigabyteData.addGig(otherGig);
        gigabyteData.addPaymentObligation(new PaymentObligation(gig, new Fee("50"),
                new Deadline("2026-11-30"), false));
        PaymentObligation stored = gigabyteData.getPaymentObligationList().get(0);
        PaymentObligation changedAssociation = new PaymentObligation(gigabyteData.getGigList().get(1),
                new Fee("75"), stored.getDueDate(), false);
        PaymentObligation changedState = new PaymentObligation(stored.getGig(), new Fee("75"),
                stored.getDueDate(), true);

        assertThrows(IllegalArgumentException.class, () ->
                gigabyteData.setPaymentObligation(stored, changedAssociation));
        assertThrows(IllegalArgumentException.class, () ->
                gigabyteData.setPaymentObligation(stored, changedState));
        assertSame(stored, gigabyteData.getPaymentObligationList().get(0));
    }

    @Test
    public void setPaymentObligation_missingTarget_throwsAndLeavesDataUnchanged() {
        gigabyteData.addClient(ALICE);
        Gig gig = new Gig(ALICE, TITLE, GigStatus.NOT_STARTED, new Deadline("2026-12-31"), new Fee("100"));
        gigabyteData.addGig(gig);
        gigabyteData.addPaymentObligation(new PaymentObligation(gig, new Fee("50"),
                new Deadline("2026-11-30"), false));
        PaymentObligation stored = gigabyteData.getPaymentObligationList().get(0);
        PaymentObligation missing = new PaymentObligation(stored.getGig(), new Fee("25"),
                new Deadline("2026-10-31"), false);
        PaymentObligation replacement = missing.withDetails(new Fee("75"), missing.getDueDate());

        assertThrows(IllegalArgumentException.class, () ->
                gigabyteData.setPaymentObligation(missing, replacement));
        assertSame(stored, gigabyteData.getPaymentObligationList().get(0));
    }

    @Test
    public void setClient_withPaymentObligation_relinksObligationAtomically() {
        gigabyteData.addClient(ALICE);
        Gig gig = new Gig(ALICE, TITLE, GigStatus.NOT_STARTED, new Deadline("2026-12-31"), new Fee("100"));
        gigabyteData.addGig(gig);
        gigabyteData.addPaymentObligation(new PaymentObligation(gig, new Fee("50"),
                new Deadline("2026-11-30"), false));
        Client editedAlice = new ClientBuilder(ALICE).withAddress("Updated address").build();

        gigabyteData.setClient(ALICE, editedAlice);

        assertEquals(editedAlice, gigabyteData.getGigList().get(0).getClient());
        assertEquals(TITLE, gigabyteData.getGigList().get(0).getTitle());
        assertEquals(editedAlice, gigabyteData.getPaymentObligationList().get(0).getGig().getClient());
        assertEquals(TITLE, gigabyteData.getPaymentObligationList().get(0).getGig().getTitle());
    }

    @Test
    public void setClients_withPaymentObligation_relinksObligationAtomically() {
        gigabyteData.addClient(ALICE);
        Gig gig = new Gig(ALICE, TITLE, GigStatus.NOT_STARTED, new Deadline("2026-12-31"), new Fee("100"));
        gigabyteData.addGig(gig);
        gigabyteData.addPaymentObligation(new PaymentObligation(gig, new Fee("50"),
                new Deadline("2026-11-30"), false));
        Client editedAlice = new ClientBuilder(ALICE).withAddress("Updated address").build();

        gigabyteData.setClients(List.of(editedAlice));

        assertEquals(editedAlice, gigabyteData.getGigList().get(0).getClient());
        assertSame(gigabyteData.getGigList().get(0),
                gigabyteData.getPaymentObligationList().get(0).getGig());
    }

    @Test
    public void setGigs_withPaymentObligation_relinksObligationAtomically() {
        gigabyteData.addClient(ALICE);
        Gig gig = new Gig(ALICE, TITLE, GigStatus.NOT_STARTED, new Deadline("2026-12-31"), new Fee("100"));
        gigabyteData.addGig(gig);
        gigabyteData.addPaymentObligation(new PaymentObligation(gig, new Fee("50"),
                new Deadline("2026-11-30"), false));
        Gig replacementGig = new Gig(ALICE, TITLE, GigStatus.COMPLETED,
                new Deadline("2026-12-31"), new Fee("100"));

        gigabyteData.setGigs(List.of(replacementGig));

        assertEquals(GigStatus.COMPLETED, gigabyteData.getGigList().get(0).getStatus());
        assertSame(gigabyteData.getGigList().get(0),
                gigabyteData.getPaymentObligationList().get(0).getGig());
    }

    @Test
    public void setGig_withPaymentObligation_preservesUidAssociationAndRelinksObligation() {
        gigabyteData.addClient(ALICE);
        Gig originalGig = new Gig(ALICE, TITLE, GigStatus.NOT_STARTED,
                new Deadline("2026-12-31"), new Fee("100"));
        gigabyteData.addGig(originalGig);
        Gig storedGig = gigabyteData.getGigList().get(0);
        PaymentObligation obligation = new PaymentObligation(storedGig, new Fee("50"),
                new Deadline("2026-11-30"), false);
        gigabyteData.addPaymentObligation(obligation);
        Gig replacementGig = storedGig.withStatus(GigStatus.COMPLETED);

        gigabyteData.setGig(storedGig, replacementGig);

        Gig updatedGig = gigabyteData.getGigList().get(0);
        PaymentObligation updatedObligation = gigabyteData.getPaymentObligationList().get(0);
        assertEquals(storedGig.getUid(), updatedGig.getUid());
        assertSame(storedGig.getClient(), updatedGig.getClient());
        assertEquals(GigStatus.COMPLETED, updatedGig.getStatus());
        assertEquals(TITLE, updatedGig.getTitle());
        assertEquals(new Deadline("2026-12-31"), updatedGig.getDeadline());
        assertEquals(new Fee("100"), updatedGig.getAgreedFee());
        assertSame(updatedGig, updatedObligation.getGig());
        assertEquals(obligation.getAmount(), updatedObligation.getAmount());
        assertEquals(obligation.getDueDate(), updatedObligation.getDueDate());
        assertEquals(obligation.isPaid(), updatedObligation.isPaid());
    }

    @Test
    public void setGig_invalidReplacement_throwsAndLeavesDataUnchanged() {
        gigabyteData.addClient(ALICE);
        gigabyteData.addGig(new Gig(ALICE, TITLE, GigStatus.NOT_STARTED,
                new Deadline("2026-12-31"), new Fee("100")));
        Gig storedGig = gigabyteData.getGigList().get(0);
        Gig replacementWithNewUid = storedGig.withStatus(GigStatus.COMPLETED).withNewUid();

        assertThrows(IllegalArgumentException.class, () -> gigabyteData.setGig(storedGig, replacementWithNewUid));

        assertEquals(List.of(storedGig), gigabyteData.getGigList());
    }

    @Test
    public void setGigs_withoutLinkedGig_throwsAndLeavesDataUnchanged() {
        gigabyteData.addClient(ALICE);
        Gig gig = new Gig(ALICE, TITLE, GigStatus.NOT_STARTED, new Deadline("2026-12-31"), new Fee("100"));
        gigabyteData.addGig(gig);
        gigabyteData.addPaymentObligation(new PaymentObligation(gig, new Fee("50"),
                new Deadline("2026-11-30"), false));

        assertThrows(GigNotFoundException.class, () -> gigabyteData.setGigs(List.of()));

        assertEquals(List.of(gig), gigabyteData.getGigList());
        assertSame(gigabyteData.getGigList().get(0),
                gigabyteData.getPaymentObligationList().get(0).getGig());
    }

    @Test
    public void setGigs_reorderedGigs_preservesObligationLink() {
        gigabyteData.addClient(ALICE);
        gigabyteData.addClient(BENSON);
        Gig aliceGig = new Gig(ALICE, TITLE, GigStatus.NOT_STARTED,
                new Deadline("2026-12-31"), new Fee("100"));
        Gig bensonGig = new Gig(BENSON, OTHER_TITLE, GigStatus.IN_PROGRESS,
                new Deadline("2027-01-31"), new Fee("200"));
        gigabyteData.addGig(aliceGig);
        gigabyteData.addGig(bensonGig);
        gigabyteData.addPaymentObligation(new PaymentObligation(aliceGig, new Fee("50"),
                new Deadline("2026-11-30"), false));

        gigabyteData.setGigs(List.of(bensonGig, aliceGig));

        assertSame(gigabyteData.getGigList().get(1),
                gigabyteData.getPaymentObligationList().get(0).getGig());
    }

    @Test
    public void setGigs_unlinkedGigRemoved_preservesObligationLink() {
        gigabyteData.addClient(ALICE);
        gigabyteData.addClient(BENSON);
        Gig aliceGig = new Gig(ALICE, TITLE, GigStatus.NOT_STARTED,
                new Deadline("2026-12-31"), new Fee("100"));
        Gig bensonGig = new Gig(BENSON, OTHER_TITLE, GigStatus.IN_PROGRESS,
                new Deadline("2027-01-31"), new Fee("200"));
        gigabyteData.addGig(aliceGig);
        gigabyteData.addGig(bensonGig);
        gigabyteData.addPaymentObligation(new PaymentObligation(bensonGig, new Fee("50"),
                new Deadline("2026-11-30"), false));

        gigabyteData.setGigs(List.of(bensonGig));

        assertSame(gigabyteData.getGigList().get(0),
                gigabyteData.getPaymentObligationList().get(0).getGig());
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
        assertTrue(gigabyteData.equals(gigabyteData));
        assertFalse(gigabyteData.equals("not gigabyte data"));
        assertEquals(gigabyteData.hashCode(), gigabyteData.hashCode());
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
