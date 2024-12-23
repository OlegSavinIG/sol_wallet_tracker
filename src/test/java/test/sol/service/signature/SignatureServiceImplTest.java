package test.sol.service.signature;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import test.sol.pojo.signature.SignatureResponseResult;
import test.sol.pojo.signature.SignaturesResponse;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignatureServiceImplTest {

    private SignatureServiceImpl signatureService;

    @BeforeEach
    void setUp() {
        signatureService = new SignatureServiceImpl();
    }

    @Test
    void validateSignature_withValidSignatures_returnsValidSignatures() {
        // Arrange
        SignatureResponseResult validResult1 = new SignatureResponseResult("signature1", null);
        SignatureResponseResult validResult2 = new SignatureResponseResult("signature2", null);
        SignaturesResponse response = new SignaturesResponse(1, List.of(validResult1, validResult2));

        // Act
        Set<String> validSignatures = signatureService.validateSignature(response);

        // Assert
        assertNotNull(validSignatures);
        assertEquals(2, validSignatures.size());
        assertTrue(validSignatures.contains("signature1"));
        assertTrue(validSignatures.contains("signature2"));
    }

    @Test
    void validateSignature_withErrors_ignoresInvalidSignatures() {
        // Arrange
        SignatureResponseResult validResult = new SignatureResponseResult("validSignature", null);
        SignatureResponseResult invalidResult = new SignatureResponseResult("invalidSignature", new Object());
        SignaturesResponse response = new SignaturesResponse(1, List.of(validResult, invalidResult));

        // Act
        Set<String> validSignatures = signatureService.validateSignature(response);

        // Assert
        assertNotNull(validSignatures);
        assertEquals(1, validSignatures.size());
        assertTrue(validSignatures.contains("validSignature"));
        assertFalse(validSignatures.contains("invalidSignature"));
    }

    @Test
    void validateSignature_withNullResult_returnsEmptySet() {
        // Arrange
        SignaturesResponse response = new SignaturesResponse(1, null);

        // Act
        Set<String> validSignatures = signatureService.validateSignature(response);

        // Assert
        assertNotNull(validSignatures);
        assertTrue(validSignatures.isEmpty());
    }

    @Test
    void validateSignature_withEmptyResult_returnsEmptySet() {
        // Arrange
        SignaturesResponse response = new SignaturesResponse(1, List.of());

        // Act
        Set<String> validSignatures = signatureService.validateSignature(response);

        // Assert
        assertNotNull(validSignatures);
        assertTrue(validSignatures.isEmpty());
    }
}
