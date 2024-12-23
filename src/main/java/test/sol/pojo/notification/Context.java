package test.sol.pojo.notification;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Context {
    long slot;

    public long getSlot() {
        return slot;
    }

    public void setSlot(long slot) {
        this.slot = slot;
    }
}
