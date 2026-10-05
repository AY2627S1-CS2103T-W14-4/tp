package gigabyte.testutil;

import gigabyte.model.GigabyteData;
import gigabyte.model.client.Client;

/**
 * A utility class to help with building GigabyteData objects.
 * Example usage: <br>
 *     {@code GigabyteData ab = new GigabyteBuilder().withClient("John", "Doe").build();}
 */
public class GigabyteBuilder {

    private GigabyteData gigabyteData;

    public GigabyteBuilder() {
        gigabyteData = new GigabyteData();
    }

    public GigabyteBuilder(GigabyteData gigabyteData) {
        this.gigabyteData = gigabyteData;
    }

    /**
     * Adds a new {@code Client} to the {@code GigabyteData} that we are building.
     */
    public GigabyteBuilder withClient(Client client) {
        gigabyteData.addClient(client);
        return this;
    }

    public GigabyteData build() {
        return gigabyteData;
    }
}
