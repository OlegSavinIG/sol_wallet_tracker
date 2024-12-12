package test.sol.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Instruction {
    private ParsedInstruction parsed;

    public ParsedInstruction getParsed() {
        return parsed;
    }

    public void setParsed(ParsedInstruction parsed) {
        this.parsed = parsed;
    }
}

