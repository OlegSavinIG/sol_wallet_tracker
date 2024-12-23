package test.sol.pojo.notification;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigInteger;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Value {
    private long lamports;
    private String owner;
    private Object data;
    private boolean executable;
    private long space;

    // Getters and setters
    public long getLamports() {
        return lamports;
    }

    public void setLamports(long lamports) {
        this.lamports = lamports;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public Object getData() {
        return data;
    }

    public void setData(AccountData data) {
        this.data = data;
    }

    public boolean isExecutable() {
        return executable;
    }

    public void setExecutable(boolean executable) {
        this.executable = executable;
    }

    public long getSpace() {
        return space;
    }

    public void setSpace(long space) {
        this.space = space;
    }

}
