package test.sol.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import test.sol.pojo.transaction.TransactionParsed;

import java.io.IOException;

public class TransactionParsedDeserializer extends JsonDeserializer<TransactionParsed> {
    @Override
    public TransactionParsed deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        if (parser.isExpectedStartObjectToken()) {
            return parser.readValueAs(TransactionParsed.class);
        } else if (parser.hasTextCharacters()) {
            return null;
        }
        return null;
    }
}
