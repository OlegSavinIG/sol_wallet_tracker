package test.sol.pojo.signature;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SignatureResponseResult(
        @JsonProperty("signature") String signature,
        @JsonProperty("err") Object err
) {}
