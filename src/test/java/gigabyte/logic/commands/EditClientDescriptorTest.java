package gigabyte.logic.commands;

import static gigabyte.logic.commands.CommandTestUtil.DESC_AMY;
import static gigabyte.logic.commands.CommandTestUtil.DESC_BOB;
import static gigabyte.logic.commands.CommandTestUtil.VALID_ADDRESS_BOB;
import static gigabyte.logic.commands.CommandTestUtil.VALID_EMAIL_BOB;
import static gigabyte.logic.commands.CommandTestUtil.VALID_NAME_BOB;
import static gigabyte.logic.commands.CommandTestUtil.VALID_PHONE_BOB;
import static gigabyte.logic.commands.CommandTestUtil.VALID_TAG_HUSBAND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import gigabyte.logic.commands.EditClientCommand.EditClientDescriptor;
import gigabyte.testutil.EditClientDescriptorBuilder;

public class EditClientDescriptorTest {

    @Test
    public void equals() {
        // same values -> returns true
        EditClientDescriptor descriptorWithSameValues = new EditClientCommand.EditClientDescriptor(DESC_AMY);
        assertTrue(DESC_AMY.equals(descriptorWithSameValues));

        // same object -> returns true
        assertTrue(DESC_AMY.equals(DESC_AMY));

        // null -> returns false
        assertFalse(DESC_AMY.equals(null));

        // different types -> returns false
        assertFalse(DESC_AMY.equals(5));

        // different values -> returns false
        assertFalse(DESC_AMY.equals(DESC_BOB));

        // different name -> returns false
        EditClientCommand.EditClientDescriptor editedAmy = new EditClientDescriptorBuilder(DESC_AMY)
                .withName(VALID_NAME_BOB).build();
        assertFalse(DESC_AMY.equals(editedAmy));

        // different phone -> returns false
        editedAmy = new EditClientDescriptorBuilder(DESC_AMY).withPhone(VALID_PHONE_BOB).build();
        assertFalse(DESC_AMY.equals(editedAmy));

        // different email -> returns false
        editedAmy = new EditClientDescriptorBuilder(DESC_AMY).withEmail(VALID_EMAIL_BOB).build();
        assertFalse(DESC_AMY.equals(editedAmy));

        // different address -> returns false
        editedAmy = new EditClientDescriptorBuilder(DESC_AMY).withAddress(VALID_ADDRESS_BOB).build();
        assertFalse(DESC_AMY.equals(editedAmy));

        // different tags -> returns false
        editedAmy = new EditClientDescriptorBuilder(DESC_AMY).withTags(VALID_TAG_HUSBAND).build();
        assertFalse(DESC_AMY.equals(editedAmy));
    }

    @Test
    public void toStringMethod() {
        EditClientCommand.EditClientDescriptor editClientDescriptor = new EditClientCommand.EditClientDescriptor();
        String expected = EditClientCommand.EditClientDescriptor.class.getCanonicalName() + "{name="
                + editClientDescriptor.getName().orElse(null) + ", phone="
                + editClientDescriptor.getPhone().orElse(null) + ", email="
                + editClientDescriptor.getEmail().orElse(null) + ", address="
                + editClientDescriptor.getAddress().orElse(null) + ", tags="
                + editClientDescriptor.getTags().orElse(null) + "}";
        assertEquals(expected, editClientDescriptor.toString());
    }
}
