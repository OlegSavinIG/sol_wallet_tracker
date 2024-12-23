package test.sol.pojo.notification;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RpcResponse {
    private Params params;
    @JsonProperty("apiVersion")
    private String apiVersion;
    String method;
    private long slot;
   private int subscription;

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public long getSlot() {
        return slot;
    }

    public void setSlot(long slot) {
        this.slot = slot;
    }

    public Params getParams() {
        return params;
    }

    public void setParams(Params params) {
        this.params = params;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public int getSubscription() {
        return subscription;
    }

    public void setSubscription(int subscription) {
        this.subscription = subscription;
    }
}
