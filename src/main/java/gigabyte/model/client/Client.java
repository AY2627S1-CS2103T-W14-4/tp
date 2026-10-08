package gigabyte.model.client;

import static gigabyte.commons.util.CollectionUtil.requireAllNonNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import gigabyte.commons.util.ToStringBuilder;
import gigabyte.model.tag.Tag;

/**
 * Represents a Client in the client list.
 * Guarantees: details are present and not null, field values are validated, immutable.
 */
public class Client {

    private final UUID uid;

    // Identity fields
    private final Name name;
    private final Phone phone;
    private final Email email;

    // Data fields
    private final Address address;
    private final Set<Tag> tags = new HashSet<>();

    /**
     * Every field must be present and not null.
     */
    public Client(Name name, Phone phone, Email email, Address address, Set<Tag> tags) {
        this(UUID.randomUUID(), name, phone, email, address, tags);
    }

    /**
     * Constructs a {@code Client} with a stable identifier.
     */
    public Client(UUID uid, Name name, Phone phone, Email email, Address address, Set<Tag> tags) {
        requireAllNonNull(uid, name, phone, email, address, tags);
        this.uid = uid;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.tags.addAll(tags);
    }

    /** Returns this client's stable identifier. */
    public UUID getUid() {
        return uid;
    }

    /** Returns whether both clients have the same stable identifier. */
    public boolean hasSameUid(Client otherClient) {
        return otherClient != null && uid.equals(otherClient.uid);
    }

    /** Returns whether both clients have the same display name. */
    public boolean hasSameName(Client otherClient) {
        return otherClient != null && name.equals(otherClient.name);
    }

    public Name getName() {
        return name;
    }

    public Phone getPhone() {
        return phone;
    }

    public Email getEmail() {
        return email;
    }

    public Address getAddress() {
        return address;
    }

    /**
     * Returns an immutable tag set, which throws {@code UnsupportedOperationException}
     * if modification is attempted.
     */
    public Set<Tag> getTags() {
        return Collections.unmodifiableSet(tags);
    }

    /**
     * Returns true if both clients have the same name. Use {@link #hasSameUid(Client)} for relationships.
     */
    public boolean isSameClient(Client otherClient) {
        return hasSameName(otherClient);
    }

    /** Returns true if all client details match. Stable identity is compared with {@link #hasSameUid(Client)}. */
    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof Client otherClient)) {
            return false;
        }

        return name.equals(otherClient.name)
                && phone.equals(otherClient.phone)
                && email.equals(otherClient.email)
                && address.equals(otherClient.address)
                && tags.equals(otherClient.tags);
    }

    @Override
    public int hashCode() {
        // use this method for custom fields hashing instead of implementing your own
        return Objects.hash(name, phone, email, address, tags);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("name", name)
                .add("phone", phone)
                .add("email", email)
                .add("address", address)
                .add("tags", tags)
                .toString();
    }

}
