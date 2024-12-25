package test.sol.utils;

import java.util.concurrent.atomic.AtomicInteger;

public class WalletIdGenerator {
    private static final AtomicInteger globalId = new AtomicInteger(1);
    public static int getNextId() {
        return globalId.getAndIncrement();
    }
}
