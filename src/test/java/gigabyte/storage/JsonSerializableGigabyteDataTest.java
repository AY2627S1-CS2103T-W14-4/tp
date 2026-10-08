package gigabyte.storage;

import static gigabyte.testutil.Assert.assertThrows;
import static gigabyte.testutil.TypicalClients.ALICE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import gigabyte.commons.exceptions.IllegalValueException;
import gigabyte.commons.util.JsonUtil;
import gigabyte.model.GigabyteData;
import gigabyte.testutil.TypicalClients;

public class JsonSerializableGigabyteDataTest {

    private static final Path TEST_DATA_FOLDER = Paths.get("src", "test", "data", "JsonSerializableGigabyteDataTest");
    private static final Path TYPICAL_CLIENTS_FILE = TEST_DATA_FOLDER.resolve("typicalClientsGigabyteData.json");
    private static final Path INVALID_CLIENT_FILE = TEST_DATA_FOLDER.resolve("invalidClientGigabyteData.json");
    private static final Path DUPLICATE_CLIENT_FILE = TEST_DATA_FOLDER.resolve("duplicateClientGigabyteData.json");

    @Test
    public void toModelType_typicalClientsFile_success() throws Exception {
        JsonSerializableGigabyteData dataFromFile = JsonUtil.readJsonFile(TYPICAL_CLIENTS_FILE,
                JsonSerializableGigabyteData.class).get();
        GigabyteData gigabyteDataFromFile = dataFromFile.toModelType();
        GigabyteData typicalClientsGigabyteData = TypicalClients.getTypicalGigabyteData();
        assertEquals(gigabyteDataFromFile, typicalClientsGigabyteData);
    }

    @Test
    public void toModelType_invalidClientFile_throwsIllegalValueException() throws Exception {
        JsonSerializableGigabyteData dataFromFile = JsonUtil.readJsonFile(INVALID_CLIENT_FILE,
                JsonSerializableGigabyteData.class).get();
        assertThrows(IllegalValueException.class, dataFromFile::toModelType);
    }

    @Test
    public void toModelType_duplicateClients_throwsIllegalValueException() throws Exception {
        JsonSerializableGigabyteData dataFromFile = JsonUtil.readJsonFile(DUPLICATE_CLIENT_FILE,
                JsonSerializableGigabyteData.class).get();
        assertThrows(IllegalValueException.class, JsonSerializableGigabyteData.MESSAGE_DUPLICATE_CLIENT,
                dataFromFile::toModelType);
    }

    @Test
    public void toModelType_nullGig_throwsIllegalValueException() {
        JsonSerializableGigabyteData data = new JsonSerializableGigabyteData(List.of(new JsonAdaptedClient(ALICE)),
                Arrays.asList((JsonAdaptedGig) null));
        assertThrows(IllegalValueException.class, JsonAdaptedGig.MISSING_FIELD_MESSAGE, data::toModelType);
    }

}
