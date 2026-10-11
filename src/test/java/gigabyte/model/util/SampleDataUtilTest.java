package gigabyte.model.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import gigabyte.model.ReadOnlyGigabyteData;

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
}
