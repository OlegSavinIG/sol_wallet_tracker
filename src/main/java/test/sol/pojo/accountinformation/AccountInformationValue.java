package test.sol.pojo.accountinformation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AccountInformationValue(
        long lamports
) {
}
