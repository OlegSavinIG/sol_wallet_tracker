package test.sol.pojo.transaction;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import test.sol.utils.TransactionParsedDeserializer;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TransactionInstruction(
        @JsonDeserialize(using = TransactionParsedDeserializer.class)
        TransactionParsed parsed
) {}

