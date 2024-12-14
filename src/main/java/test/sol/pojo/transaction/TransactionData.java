package test.sol.pojo.transaction;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionData {
    private TransactionMessage message;

    public TransactionMessage getMessage() {
        return message;
    }

    public void setMessage(TransactionMessage message) {
        this.message = message;
    }
}

