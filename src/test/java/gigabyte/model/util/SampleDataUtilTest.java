package gigabyte.model.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import gigabyte.model.ReadOnlyGigabyteData;
import gigabyte.model.gig.GigStatus;

public class SampleDataUtilTest {

    @Test
    public void getSampleGigabyteData_containsTitledGigsLinkedToCanonicalClients() {
        ReadOnlyGigabyteData sampleData = SampleDataUtil.getSampleGigabyteData();

        assertFalse(sampleData.getGigList().isEmpty());
        sampleData.getGigList().forEach(gig -> {
            assertTrue(gig.getTitle().toString().length() > 0);
            assertSame(sampleData.getClientList().stream()
                    .filter(client -> client.hasSameUid(gig.getClient()))
                    .findFirst()
                    .orElseThrow(), gig.getClient());
        });
    }

    @Test
    public void getSampleGigabyteData_containsUsableFixtures() {
        ReadOnlyGigabyteData sampleData = SampleDataUtil.getSampleGigabyteData();

        assertEquals(6, sampleData.getClientList().size());
        assertEquals(4, sampleData.getGigList().size());
        assertEquals(3, sampleData.getPaymentObligationList().size());
        assertEquals(1, sampleData.getPaymentObligationList().stream()
                .filter(obligation -> obligation.isPaid()).count());
        assertEquals(2, sampleData.getPaymentObligationList().stream()
                .filter(obligation -> !obligation.isPaid()).count());
        assertEquals(1, sampleData.getGigList().stream()
                .filter(gig -> sampleData.getPaymentObligationList().stream()
                        .noneMatch(obligation -> obligation.getGig().hasSameUid(gig))).count());
        assertEquals(1, sampleData.getGigList().stream()
                .filter(gig -> gig.getStatus() == GigStatus.NOT_STARTED).count());
        assertEquals(2, sampleData.getGigList().stream()
                .filter(gig -> gig.getStatus() == GigStatus.IN_PROGRESS).count());
        assertEquals(1, sampleData.getGigList().stream()
                .filter(gig -> gig.getStatus() == GigStatus.COMPLETED).count());
        sampleData.getPaymentObligationList().forEach(obligation -> assertSame(
                sampleData.getGigList().stream()
                        .filter(gig -> gig.hasSameUid(obligation.getGig())).findFirst().orElseThrow(),
                obligation.getGig()));
    }
}
