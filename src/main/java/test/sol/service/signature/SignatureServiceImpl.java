package test.sol.service.signature;

import test.sol.pojo.signature.SignatureResponseResult;
import test.sol.pojo.signature.SignaturesResponse;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SignatureServiceImpl implements SignatureService {
    @Override
    public Set<String> validateSignature(SignaturesResponse response) {
        List<SignatureResponseResult> result = response.result();
        if (result != null) {
            return result.stream()
                    .filter(res -> res.err() == null)
                    .map(SignatureResponseResult::signature)
                    .collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }
}
