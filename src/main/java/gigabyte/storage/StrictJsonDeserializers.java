package gigabyte.storage;

import java.io.IOException;
import java.math.BigInteger;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;

/**
 * Deserializers that accept only one JSON scalar type, so that values such as {@code "5025"} or {@code "true"}
 * are rejected rather than converted. A JSON {@code null} is not passed to these deserializers and is left for
 * the adapter's missing-field check.
 */
final class StrictJsonDeserializers {

    private StrictJsonDeserializers() {}

    /** Accepts a JSON integer only. Strings, floating-point numbers and booleans are rejected. */
    static class IntegerDeserializer extends StdScalarDeserializer<BigInteger> {
        IntegerDeserializer() {
            super(BigInteger.class);
        }

        @Override
        public BigInteger deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            if (parser.getCurrentToken() != JsonToken.VALUE_NUMBER_INT) {
                throw JsonMappingException.from(parser, "Expected a JSON integer.");
            }
            return parser.getBigIntegerValue();
        }
    }

    /** Accepts a JSON {@code true} or {@code false} only. Strings and numbers are rejected. */
    static class BooleanDeserializer extends StdScalarDeserializer<Boolean> {
        BooleanDeserializer() {
            super(Boolean.class);
        }

        @Override
        public Boolean deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            JsonToken token = parser.getCurrentToken();
            if (token != JsonToken.VALUE_TRUE && token != JsonToken.VALUE_FALSE) {
                throw JsonMappingException.from(parser, "Expected a JSON boolean.");
            }
            return token == JsonToken.VALUE_TRUE;
        }
    }
}
