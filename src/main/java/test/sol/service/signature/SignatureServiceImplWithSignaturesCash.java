package test.sol.service.signature;

import test.sol.pojo.signature.SignatureResponseResult;
import test.sol.pojo.signature.SignaturesResponse;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SignatureServiceImplWithSignaturesCash implements SignatureService {
    private final Set<String> previousSignatures = new HashSet<>();
    @Override
    public Set<String> validateSignature(SignaturesResponse response) {
        if (response == null || response.result() == null) {
            return Collections.emptySet();
        }

        List<SignatureResponseResult> results = response.result();

        Set<String> newSignatures = results.stream()
                .filter(res -> res.err() == null)
                .map(SignatureResponseResult::signature)
                .filter(signature -> !previousSignatures.contains(signature))
                .collect(Collectors.toSet());

        previousSignatures.clear();
        previousSignatures.addAll(newSignatures);

        return newSignatures;
    }
}
