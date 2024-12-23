package test.sol.service.signature;

import test.sol.pojo.signature.SignaturesResponse;

import java.util.Set;

public interface SignatureService {
    Set<String> validateSignature(SignaturesResponse response);
}
