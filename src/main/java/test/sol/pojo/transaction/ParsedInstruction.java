package test.sol.pojo.transaction;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ParsedInstruction {
    private String type;
    private InstructionInfo info;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public InstructionInfo getInfo() {
        return info;
    }

    public void setInfo(InstructionInfo info) {
        this.info = info;
    }
}

