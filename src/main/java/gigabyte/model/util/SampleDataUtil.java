package gigabyte.model.util;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import gigabyte.model.GigabyteData;
import gigabyte.model.ReadOnlyGigabyteData;
import gigabyte.model.client.Address;
import gigabyte.model.client.Client;
import gigabyte.model.client.Email;
import gigabyte.model.client.Name;
import gigabyte.model.client.Phone;
import gigabyte.model.gig.Deadline;
import gigabyte.model.gig.Fee;
import gigabyte.model.gig.Gig;
import gigabyte.model.gig.GigStatus;
import gigabyte.model.gig.GigTitle;
import gigabyte.model.tag.Tag;

/**
 * Contains utility methods for populating {@code GigabyteData} with sample data.
 */
public class SampleDataUtil {
    public static Client[] getSampleClients() {
        return new Client[] {
            new Client(new Name("Alex Yeoh"), new Phone("87438807"), new Email("alexyeoh@example.com"),
                new Address("Blk 30 Geylang Street 29, #06-40"),
                getTagSet("friends")),
            new Client(new Name("Bernice Yu"), new Phone("99272758"), new Email("berniceyu@example.com"),
                new Address("Blk 30 Lorong 3 Serangoon Gardens, #07-18"),
                getTagSet("colleagues", "friends")),
            new Client(new Name("Charlotte Oliveiro"), new Phone("93210283"), new Email("charlotte@example.com"),
                new Address("Blk 11 Ang Mo Kio Street 74, #11-04"),
                getTagSet("neighbours")),
            new Client(new Name("David Li"), new Phone("91031282"), new Email("lidavid@example.com"),
                new Address("Blk 436 Serangoon Gardens Street 26, #16-43"),
                getTagSet("family")),
            new Client(new Name("Irfan Ibrahim"), new Phone("92492021"), new Email("irfan@example.com"),
                new Address("Blk 47 Tampines Street 20, #17-35"),
                getTagSet("classmates")),
            new Client(new Name("Roy Balakrishnan"), new Phone("92624417"), new Email("royb@example.com"),
                new Address("Blk 45 Aljunied Street 85, #11-31"),
                getTagSet("colleagues"))
        };
    }

    public static ReadOnlyGigabyteData getSampleGigabyteData() {
        GigabyteData sampleGigabyteData = new GigabyteData();
        Client[] sampleClients = getSampleClients();
        for (Client sampleClient : sampleClients) {
            sampleGigabyteData.addClient(sampleClient);
        }
        sampleGigabyteData.addGig(new Gig(sampleClients[0], new GigTitle("Portfolio website redesign"),
                GigStatus.IN_PROGRESS, new Deadline("2027-01-31"), new Fee("2500.00")));
        sampleGigabyteData.addGig(new Gig(sampleClients[0], new GigTitle("Product launch photography"),
                GigStatus.NOT_STARTED, new Deadline("2027-03-15"), new Fee("1200.00")));
        sampleGigabyteData.addGig(new Gig(sampleClients[1], new GigTitle("Brand identity refresh"),
                GigStatus.COMPLETED, new Deadline("2026-11-30"), new Fee("1800.00")));
        return sampleGigabyteData;
    }

    /**
     * Returns a tag set containing the list of strings given.
     */
    public static Set<Tag> getTagSet(String... strings) {
        return Arrays.stream(strings)
                .map(Tag::new)
                .collect(Collectors.toSet());
    }

}
