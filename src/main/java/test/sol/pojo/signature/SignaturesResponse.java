package test.sol.pojo.signature;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
@JsonIgnoreProperties(ignoreUnknown = true)
public record SignaturesResponse(
        Integer id,
        @JsonProperty("result")
        List<SignatureResponseResult> result
) {}
