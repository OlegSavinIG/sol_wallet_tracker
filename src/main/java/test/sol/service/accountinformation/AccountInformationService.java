package test.sol.service.accountinformation;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public interface AccountInformationService {
    List<String> isPositiveBalance(Set<String> wallets) throws IOException;
}
