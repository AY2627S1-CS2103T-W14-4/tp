package gigabyte.model;

import gigabyte.model.client.Client;
import gigabyte.model.gig.Gig;
import gigabyte.model.gig.PaymentObligation;
import javafx.collections.ObservableList;

/**
 * Unmodifiable view of Gigabyte data.
 */
public interface ReadOnlyGigabyteData {

    /**
     * Returns an unmodifiable view of the clients.
     */
    ObservableList<Client> getClientList();

    /**
     * Returns an unmodifiable view of the gigs.
     */
    ObservableList<Gig> getGigList();

    default ObservableList<PaymentObligation> getPaymentObligationList() {
        return javafx.collections.FXCollections.emptyObservableList();
    }
}
