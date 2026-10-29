package gigabyte.logic;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import gigabyte.logic.parser.Prefix;
import gigabyte.model.client.Client;

/**
 * Container for user visible messages.
 */
public class Messages {

    public static final String MESSAGE_UNKNOWN_COMMAND = "Unknown command.";
    public static final String MESSAGE_INVALID_COMMAND_FORMAT = "Invalid command format!\n%1$s";
    public static final String MESSAGE_INVALID_CLIENT_DISPLAYED_INDEX = "The client index provided is invalid.";
    public static final String MESSAGE_INVALID_GIG_INDEX =
            "The gig index provided is invalid for this client.";
    public static final String MESSAGE_INVALID_PAYMENT_INDEX =
            "The payment index provided is invalid for this gig.";
    public static final String MESSAGE_CLIENTS_LISTED_OVERVIEW = "%1$d client(s) listed!";
    public static final String MESSAGE_DUPLICATE_CLIENT_NAME = "Another client already has this name. "
            + "Names are compared ignoring case, leading and trailing spaces, and repeated spaces.";
    public static final String MESSAGE_SHARED_CONTACT_NOTE = "Note: %1$s has the same %2$s.";
    public static final String MESSAGE_DUPLICATE_FIELDS =
                "Multiple values specified for the following single-valued field(s): ";

    /**
     * Returns an error message indicating the duplicate prefixes.
     */
    public static String getErrorMessageForDuplicatePrefixes(Prefix... duplicatePrefixes) {
        assert duplicatePrefixes.length > 0;

        Set<String> duplicateFields =
                Stream.of(duplicatePrefixes).map(Prefix::toString).collect(Collectors.toSet());

        return MESSAGE_DUPLICATE_FIELDS + String.join(" ", duplicateFields);
    }

    /**
     * Formats the {@code client} for display to the user.
     */
    public static String format(Client client) {
        final StringBuilder builder = new StringBuilder();
        builder.append(client.getName())
                .append("; Phone: ")
                .append(client.getPhone())
                .append("; Email: ")
                .append(client.getEmail())
                .append("; Address: ")
                .append(client.getAddress())
                .append("; Tags: ");
        client.getTags().forEach(builder::append);
        return builder.toString();
    }

    /**
     * Returns one note for each client in {@code clients}, other than {@code client} (compared by UID),
     * that has the same phone number or email as {@code client}. Each note starts on a new line.
     * Returns an empty string if there is no such client.
     */
    public static String formatSharedContactNotes(Client client, List<Client> clients) {
        final StringBuilder builder = new StringBuilder();
        for (Client other : clients) {
            String sharedFields = other.hasSameUid(client) ? "" : describeSharedContactFields(client, other);
            if (!sharedFields.isEmpty()) {
                builder.append("\n")
                        .append(String.format(MESSAGE_SHARED_CONTACT_NOTE, other.getName(), sharedFields));
            }
        }
        return builder.toString();
    }

    /**
     * Returns which of phone number and email {@code client} shares with {@code other},
     * or an empty string if it shares neither.
     */
    private static String describeSharedContactFields(Client client, Client other) {
        boolean hasSamePhone = other.hasSamePhone(client);
        boolean hasSameEmail = other.hasSameEmail(client);
        if (hasSamePhone && hasSameEmail) {
            return "phone number and email";
        } else if (hasSamePhone) {
            return "phone number";
        } else if (hasSameEmail) {
            return "email";
        }
        return "";
    }

}
