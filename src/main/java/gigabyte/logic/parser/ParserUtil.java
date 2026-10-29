package gigabyte.logic.parser;

import static gigabyte.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import gigabyte.commons.core.index.Index;
import gigabyte.commons.util.StringUtil;
import gigabyte.logic.parser.exceptions.ParseException;
import gigabyte.model.client.Address;
import gigabyte.model.client.Email;
import gigabyte.model.client.Name;
import gigabyte.model.client.Phone;
import gigabyte.model.gig.Deadline;
import gigabyte.model.gig.Fee;
import gigabyte.model.gig.GigStatus;
import gigabyte.model.gig.GigTitle;
import gigabyte.model.tag.Tag;

/**
 * Contains utility methods used for parsing strings in the various *Parser classes.
 */
public class ParserUtil {

    public static final String MESSAGE_INVALID_INDEX = "Index must be a positive integer.";

    /**
     * Parses {@code oneBasedIndex} into an {@code Index} and returns it. Leading and trailing whitespaces will be
     * trimmed.
     * @throws ParseException if the specified index is invalid (not a non-zero unsigned integer).
     */
    public static Index parseIndex(String oneBasedIndex) throws ParseException {
        String trimmedIndex = oneBasedIndex.trim();
        if (!StringUtil.isNonZeroUnsignedInteger(trimmedIndex)) {
            throw new ParseException(MESSAGE_INVALID_INDEX);
        }
        return Index.fromOneBased(Integer.parseInt(trimmedIndex));
    }

    /**
     * Parses the whitespace-separated indexes in {@code indexes} and returns them in order.
     * The number of indexes is checked before any index is parsed, so a wrong number of indexes is a format error,
     * while the right number of indexes with an invalid value is an index error.
     *
     * @param indexes The part of the user input that holds only the indexes, such as a command's preamble.
     * @param expectedCount The number of indexes the command takes.
     * @param commandUsage The usage message of the command, shown if the number of indexes is wrong.
     * @throws ParseException with the invalid command format message if there are not exactly
     *     {@code expectedCount} indexes, or with {@link #MESSAGE_INVALID_INDEX} if any index is invalid.
     */
    public static List<Index> parseIndexes(String indexes, int expectedCount, String commandUsage)
            throws ParseException {
        requireNonNull(indexes);
        requireNonNull(commandUsage);
        assert expectedCount > 0;

        String trimmedIndexes = indexes.trim();
        String[] tokens = trimmedIndexes.isEmpty() ? new String[0] : trimmedIndexes.split("\\s+");
        if (tokens.length != expectedCount) {
            throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, commandUsage));
        }

        List<Index> parsedIndexes = new ArrayList<>();
        for (String token : tokens) {
            parsedIndexes.add(parseIndex(token));
        }
        return parsedIndexes;
    }

    /**
     * Parses a {@code String name} into a {@code Name}.
     * Leading and trailing whitespaces will be trimmed.
     *
     * @throws ParseException if the given {@code name} is invalid.
     */
    public static Name parseName(String name) throws ParseException {
        requireNonNull(name);
        String trimmedName = name.trim();
        if (!Name.isValidName(trimmedName)) {
            throw new ParseException(Name.MESSAGE_CONSTRAINTS);
        }
        return new Name(trimmedName);
    }

    /**
     * Parses a {@code String phone} into a {@code Phone}.
     * Leading and trailing whitespaces will be trimmed.
     *
     * @throws ParseException if the given {@code phone} is invalid.
     */
    public static Phone parsePhone(String phone) throws ParseException {
        requireNonNull(phone);
        String trimmedPhone = phone.trim();
        if (!Phone.isValidPhone(trimmedPhone)) {
            throw new ParseException(Phone.MESSAGE_CONSTRAINTS);
        }
        return new Phone(trimmedPhone);
    }

    /**
     * Parses a {@code String address} into an {@code Address}.
     * Leading and trailing whitespaces will be trimmed.
     *
     * @throws ParseException if the given {@code address} is invalid.
     */
    public static Address parseAddress(String address) throws ParseException {
        requireNonNull(address);
        String trimmedAddress = address.trim();
        if (!Address.isValidAddress(trimmedAddress)) {
            throw new ParseException(Address.MESSAGE_CONSTRAINTS);
        }
        return new Address(trimmedAddress);
    }

    /**
     * Parses a {@code String email} into an {@code Email}.
     * Leading and trailing whitespaces will be trimmed.
     *
     * @throws ParseException if the given {@code email} is invalid.
     */
    public static Email parseEmail(String email) throws ParseException {
        requireNonNull(email);
        String trimmedEmail = email.trim();
        if (!Email.isValidEmail(trimmedEmail)) {
            throw new ParseException(Email.MESSAGE_CONSTRAINTS);
        }
        return new Email(trimmedEmail);
    }

    /**
     * Parses a {@code String tag} into a {@code Tag}.
     * Leading and trailing whitespaces will be trimmed.
     *
     * @throws ParseException if the given {@code tag} is invalid or longer than {@link Tag#MAX_LENGTH}.
     */
    public static Tag parseTag(String tag) throws ParseException {
        requireNonNull(tag);
        String trimmedTag = tag.trim();
        if (!Tag.isValidTagName(trimmedTag) || !Tag.isWithinLengthLimit(trimmedTag)) {
            throw new ParseException(Tag.MESSAGE_CONSTRAINTS);
        }
        return new Tag(trimmedTag);
    }

    /**
     * Parses {@code Collection<String> tags} into a {@code Set<Tag>}.
     */
    public static Set<Tag> parseTags(Collection<String> tags) throws ParseException {
        requireNonNull(tags);
        final Set<Tag> tagSet = new HashSet<>();
        for (String tagName : tags) {
            tagSet.add(parseTag(tagName));
        }
        return tagSet;
    }

    /**
     * Parses a gig status, ignoring case and surrounding whitespace.
     */
    public static GigStatus parseGigStatus(String status) throws ParseException {
        requireNonNull(status);
        try {
            return GigStatus.parse(status);
        } catch (IllegalArgumentException exception) {
            throw new ParseException(String.format("%s (rejected value: '%s')",
                    GigStatus.MESSAGE_CONSTRAINTS, status.trim()), exception);
        }
    }

    /**
     * Parses a gig title, removing surrounding whitespace.
     */
    public static GigTitle parseGigTitle(String title) throws ParseException {
        requireNonNull(title);
        if (!GigTitle.isValidTitle(title)) {
            throw new ParseException(GigTitle.MESSAGE_CONSTRAINTS);
        }
        return new GigTitle(title);
    }

    /**
     * Parses a valid gig deadline, trimming surrounding whitespace.
     */
    public static Deadline parseDeadline(String deadline) throws ParseException {
        requireNonNull(deadline);
        String trimmedDeadline = deadline.trim();
        if (!Deadline.isValidDeadline(trimmedDeadline)) {
            throw new ParseException(String.format("%s (rejected value: '%s')",
                    Deadline.MESSAGE_CONSTRAINTS, trimmedDeadline));
        }
        return new Deadline(trimmedDeadline);
    }

    /**
     * Parses a positive gig fee, trimming surrounding whitespace.
     */
    public static Fee parseFee(String fee) throws ParseException {
        requireNonNull(fee);
        String trimmedFee = fee.trim();
        if (!Fee.isValidFee(trimmedFee)) {
            throw new ParseException(Fee.MESSAGE_CONSTRAINTS);
        }
        return new Fee(trimmedFee);
    }
}
