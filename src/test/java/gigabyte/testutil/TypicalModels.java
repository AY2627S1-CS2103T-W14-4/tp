package gigabyte.testutil;

import static gigabyte.testutil.TypicalClients.ALICE;
import static gigabyte.testutil.TypicalClients.BENSON;

import gigabyte.model.ModelManager;
import gigabyte.model.client.Client;
import gigabyte.model.gig.Deadline;
import gigabyte.model.gig.Fee;
import gigabyte.model.gig.Gig;
import gigabyte.model.gig.GigStatus;
import gigabyte.model.gig.GigTitle;

/**
 * Models shared by the gig and payment command tests.
 */
public class TypicalModels {
    /**
     * Returns a model that holds only {@code ALICE} and {@code BENSON}, with no gigs or payments.
     */
    public static ModelManager createModel() {
        ModelManager model = new ModelManager();
        model.addClient(ALICE);
        model.addClient(BENSON);
        return model;
    }

    /**
     * Adds a not-started gig for {@code client} to {@code model} and returns the stored copy.
     */
    public static Gig addGig(ModelManager model, Client client) {
        model.addGig(new Gig(client, new GigTitle("Website redesign"), GigStatus.NOT_STARTED,
                new Deadline("2027-12-31"), new Fee("1250")));
        return model.getGigList().get(model.getGigList().size() - 1);
    }
}
