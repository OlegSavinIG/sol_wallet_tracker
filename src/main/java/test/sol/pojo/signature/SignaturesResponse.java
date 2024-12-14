package test.sol.pojo.signature;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SignaturesResponse {
    private List<SignatureResult> result;

    public List<SignatureResult> getResult() {
        return result;
    }

    public void setResult(List<SignatureResult> result) {
        this.result = result;
    }
}
