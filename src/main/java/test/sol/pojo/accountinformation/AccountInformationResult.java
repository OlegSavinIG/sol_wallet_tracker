package test.sol.pojo.accountinformation;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AccountInformationResult(
        List<AccountInformationValue> value
) {
}
