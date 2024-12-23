package test.sol.wallettracker;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class WalletStorage {
    private static final Set<String> wallets = ConcurrentHashMap.newKeySet();

    public static void addWallet(String walletAddress) {
        wallets.add(walletAddress);
    }

    public static Set<String> getWallets() {
        return wallets;
    }
    public static void removeWallet(String wallet){
        wallets.remove(wallet);
    }
}

