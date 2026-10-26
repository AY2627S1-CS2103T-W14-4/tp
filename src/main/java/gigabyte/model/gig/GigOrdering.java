package gigabyte.model.gig;

import java.util.List;

import gigabyte.model.client.Client;

/** Provides the source-list order used when displaying a client's gigs. */
public final class GigOrdering {
    private GigOrdering() {}

    /** Returns the gigs for {@code client} in the order they appear in {@code source}. */
    public static List<Gig> forClient(List<Gig> source, Client client) {
        return source.stream().filter(gig -> isForClient(gig, client)).toList();
    }

    /** Returns whether {@code gig} belongs to {@code client}. */
    public static boolean isForClient(Gig gig, Client client) {
        return client != null && gig.getClient().hasSameUid(client);
    }
}
