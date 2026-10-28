package gigabyte.logic.parser;

import static gigabyte.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static gigabyte.logic.parser.ParserUtil.MESSAGE_INVALID_INDEX;
import static gigabyte.testutil.Assert.assertThrows;
import static gigabyte.testutil.TypicalIndexes.INDEX_FIRST_CLIENT;
import static gigabyte.testutil.TypicalIndexes.INDEX_SECOND_CLIENT;
import static gigabyte.testutil.TypicalIndexes.INDEX_THIRD_CLIENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import gigabyte.logic.parser.exceptions.ParseException;
import gigabyte.model.client.Address;
import gigabyte.model.client.Email;
import gigabyte.model.client.Name;
import gigabyte.model.client.Phone;
import gigabyte.model.gig.GigTitle;
import gigabyte.model.tag.Tag;

public class ParserUtilTest {
    private static final String INVALID_NAME = "R@chel";
    private static final String INVALID_PHONE = "+65a1234";
    private static final String INVALID_ADDRESS = " ";
    private static final String INVALID_EMAIL = "example.com";
    private static final String INVALID_TAG = "#friend";
    private static final String TAG_OVER_LENGTH_LIMIT = "a".repeat(Tag.MAX_LENGTH + 1);

    private static final String VALID_NAME = "Rachel Walker";
    private static final String VALID_PHONE = "123456";
    private static final String VALID_ADDRESS = "123 Main Street #0505";
    private static final String VALID_EMAIL = "rachel@example.com";
    private static final String VALID_TAG_1 = "friend";
    private static final String VALID_TAG_2 = "neighbour";
    private static final String VALID_GIG_TITLE = "Website redesign — phase 2!";

    private static final String WHITESPACE = " \t\r\n";

    private static final String USAGE = "command INDEX...";
    private static final String INVALID_FORMAT = String.format(MESSAGE_INVALID_COMMAND_FORMAT, USAGE);

    @Test
    public void parseIndex_invalidInput_throwsParseException() {
        assertThrows(ParseException.class, () -> ParserUtil.parseIndex("10 a"));
    }

    @Test
    public void parseIndex_outOfRangeInput_throwsParseException() {
        assertThrows(ParseException.class, MESSAGE_INVALID_INDEX, ()
            -> ParserUtil.parseIndex(Long.toString(Integer.MAX_VALUE + 1)));
    }

    @Test
    public void parseIndex_validInput_success() throws Exception {
        // No whitespaces
        assertEquals(INDEX_FIRST_CLIENT, ParserUtil.parseIndex("1"));

        // Leading and trailing whitespaces
        assertEquals(INDEX_FIRST_CLIENT, ParserUtil.parseIndex("  1  "));
    }

    @Test
    public void parseIndexes_validInput_returnsIndexesInOrder() throws Exception {
        assertEquals(List.of(INDEX_SECOND_CLIENT, INDEX_FIRST_CLIENT, INDEX_THIRD_CLIENT),
                ParserUtil.parseIndexes(" 2 \t1  3 ", 3, USAGE));
    }

    @Test
    public void parseIndexes_wrongNumberOfIndexes_throwsInvalidFormat() {
        // EP: no index
        assertThrows(ParseException.class, INVALID_FORMAT, () -> ParserUtil.parseIndexes(WHITESPACE, 1, USAGE));

        // EP: fewer indexes than expected
        assertThrows(ParseException.class, INVALID_FORMAT, () -> ParserUtil.parseIndexes("1 2", 3, USAGE));

        // EP: more indexes than expected, even if the extra one is invalid
        assertThrows(ParseException.class, INVALID_FORMAT, () -> ParserUtil.parseIndexes("1 2 0", 2, USAGE));
    }

    @Test
    public void parseIndexes_invalidIndexValue_throwsInvalidIndex() {
        // EP: invalid value in the first and the last position
        assertThrows(ParseException.class, MESSAGE_INVALID_INDEX, () -> ParserUtil.parseIndexes("0 1", 2, USAGE));
        assertThrows(ParseException.class, MESSAGE_INVALID_INDEX, () -> ParserUtil.parseIndexes("1 abc", 2, USAGE));
    }

    @Test
    public void parseName_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ParserUtil.parseName((String) null));
    }

    @Test
    public void parseName_invalidValue_throwsParseException() {
        assertThrows(ParseException.class, () -> ParserUtil.parseName(INVALID_NAME));
    }

    @Test
    public void parseName_validValueWithoutWhitespace_returnsName() throws Exception {
        Name expectedName = new Name(VALID_NAME);
        assertEquals(expectedName, ParserUtil.parseName(VALID_NAME));
    }

    @Test
    public void parseName_validValueWithWhitespace_returnsTrimmedName() throws Exception {
        String nameWithWhitespace = WHITESPACE + VALID_NAME + WHITESPACE;
        Name expectedName = new Name(VALID_NAME);
        assertEquals(expectedName, ParserUtil.parseName(nameWithWhitespace));
    }

    @Test
    public void parsePhone_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ParserUtil.parsePhone((String) null));
    }

    @Test
    public void parsePhone_invalidValue_throwsParseException() {
        assertThrows(ParseException.class, () -> ParserUtil.parsePhone(INVALID_PHONE));
    }

    @Test
    public void parsePhone_validValueWithoutWhitespace_returnsPhone() throws Exception {
        Phone expectedPhone = new Phone(VALID_PHONE);
        assertEquals(expectedPhone, ParserUtil.parsePhone(VALID_PHONE));
    }

    @Test
    public void parsePhone_validValueWithWhitespace_returnsTrimmedPhone() throws Exception {
        String phoneWithWhitespace = WHITESPACE + VALID_PHONE + WHITESPACE;
        Phone expectedPhone = new Phone(VALID_PHONE);
        assertEquals(expectedPhone, ParserUtil.parsePhone(phoneWithWhitespace));
    }

    @Test
    public void parsePhone_formattedValueWithWhitespace_keepsInnerSpaces() throws Exception {
        String formattedPhone = "+65 9123 4567";
        Phone expectedPhone = new Phone(formattedPhone);
        assertEquals(expectedPhone, ParserUtil.parsePhone(WHITESPACE + formattedPhone + WHITESPACE));
    }

    @Test
    public void parseAddress_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ParserUtil.parseAddress((String) null));
    }

    @Test
    public void parseAddress_invalidValue_throwsParseException() {
        assertThrows(ParseException.class, () -> ParserUtil.parseAddress(INVALID_ADDRESS));
    }

    @Test
    public void parseAddress_validValueWithoutWhitespace_returnsAddress() throws Exception {
        Address expectedAddress = new Address(VALID_ADDRESS);
        assertEquals(expectedAddress, ParserUtil.parseAddress(VALID_ADDRESS));
    }

    @Test
    public void parseAddress_validValueWithWhitespace_returnsTrimmedAddress() throws Exception {
        String addressWithWhitespace = WHITESPACE + VALID_ADDRESS + WHITESPACE;
        Address expectedAddress = new Address(VALID_ADDRESS);
        assertEquals(expectedAddress, ParserUtil.parseAddress(addressWithWhitespace));
    }

    @Test
    public void parseEmail_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ParserUtil.parseEmail((String) null));
    }

    @Test
    public void parseEmail_invalidValue_throwsParseException() {
        assertThrows(ParseException.class, () -> ParserUtil.parseEmail(INVALID_EMAIL));
    }

    @Test
    public void parseEmail_validValueWithoutWhitespace_returnsEmail() throws Exception {
        Email expectedEmail = new Email(VALID_EMAIL);
        assertEquals(expectedEmail, ParserUtil.parseEmail(VALID_EMAIL));
    }

    @Test
    public void parseEmail_validValueWithWhitespace_returnsTrimmedEmail() throws Exception {
        String emailWithWhitespace = WHITESPACE + VALID_EMAIL + WHITESPACE;
        Email expectedEmail = new Email(VALID_EMAIL);
        assertEquals(expectedEmail, ParserUtil.parseEmail(emailWithWhitespace));
    }

    @Test
    public void parseTag_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ParserUtil.parseTag(null));
    }

    @Test
    public void parseTag_invalidValue_throwsParseException() {
        assertThrows(ParseException.class, () -> ParserUtil.parseTag(INVALID_TAG));
    }

    @Test
    public void parseTag_overLengthLimit_throwsParseException() {
        assertThrows(ParseException.class, Tag.MESSAGE_CONSTRAINTS, () -> ParserUtil.parseTag(TAG_OVER_LENGTH_LIMIT));
    }

    @Test
    public void parseTag_atLengthLimit_returnsTag() throws Exception {
        String tagAtLimit = "a".repeat(Tag.MAX_LENGTH);
        assertEquals(new Tag(tagAtLimit), ParserUtil.parseTag(WHITESPACE + tagAtLimit + WHITESPACE));
    }

    @Test
    public void parseTag_validValueWithoutWhitespace_returnsTag() throws Exception {
        Tag expectedTag = new Tag(VALID_TAG_1);
        assertEquals(expectedTag, ParserUtil.parseTag(VALID_TAG_1));
    }

    @Test
    public void parseTag_validValueWithWhitespace_returnsTrimmedTag() throws Exception {
        String tagWithWhitespace = WHITESPACE + VALID_TAG_1 + WHITESPACE;
        Tag expectedTag = new Tag(VALID_TAG_1);
        assertEquals(expectedTag, ParserUtil.parseTag(tagWithWhitespace));
    }

    @Test
    public void parseTags_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ParserUtil.parseTags(null));
    }

    @Test
    public void parseTags_collectionWithInvalidTags_throwsParseException() {
        assertThrows(ParseException.class, () -> ParserUtil.parseTags(List.of(VALID_TAG_1, INVALID_TAG)));
    }

    @Test
    public void parseTags_emptyCollection_returnsEmptySet() throws Exception {
        assertTrue(ParserUtil.parseTags(List.of()).isEmpty());
    }

    @Test
    public void parseTags_collectionWithValidTags_returnsTagSet() throws Exception {
        Set<Tag> actualTagSet = ParserUtil.parseTags(List.of(VALID_TAG_1, VALID_TAG_2));
        Set<Tag> expectedTagSet = Set.of(new Tag(VALID_TAG_1), new Tag(VALID_TAG_2));

        assertEquals(expectedTagSet, actualTagSet);
    }

    @Test
    public void parseGigTitle_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ParserUtil.parseGigTitle(null));
    }

    @Test
    public void parseGigTitle_invalidValue_throwsParseException() {
        assertThrows(ParseException.class, GigTitle.MESSAGE_CONSTRAINTS, () ->
                ParserUtil.parseGigTitle("   "));
        assertThrows(ParseException.class, GigTitle.MESSAGE_CONSTRAINTS, () ->
                ParserUtil.parseGigTitle("a".repeat(101)));
    }

    @Test
    public void parseGigTitle_validValue_returnsNormalizedTitle() throws Exception {
        assertEquals(new GigTitle(VALID_GIG_TITLE),
                ParserUtil.parseGigTitle(WHITESPACE + VALID_GIG_TITLE + WHITESPACE));
    }
}
