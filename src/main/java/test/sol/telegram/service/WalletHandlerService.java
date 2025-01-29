package test.sol.telegram.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import test.sol.redis.TrackWalletsRedis;
import test.sol.telegram.WalletWatcherTrackerBot;
import test.sol.wallettracker.queuelistener.RemoveWalletQueue;
import test.sol.wallettracker.queuelistener.AddWalletQueue;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class WalletHandlerService {
    private final Set<String> walletAddresses;
    private final Map<String, String> userWalletMapping;
    private final WalletWatcherTrackerBot bot;
    private static final Logger logger = LoggerFactory.getLogger(WalletHandlerService.class);

    public WalletHandlerService(Set<String> walletAddresses, Map<String, String> userWalletMapping, WalletWatcherTrackerBot bot) {
        this.walletAddresses = walletAddresses;
        this.userWalletMapping = userWalletMapping;
        this.bot = bot;
    }

    public void handleAddWallet(String messageText, String chatId) {
        String[] parts = messageText.split(" ", 2);
        if (parts.length < 2) {
            logger.warn("Invalid /add_wallet command received from chat: {}", chatId);
            bot.sendMessage(chatId, "❌ Invalid command. Use /add_wallet <wallet_address>");
            return;
        }
        String walletAddress = parts[1];
        if (isValidWalletAddress(walletAddress)) {
            if (walletAddresses.add(walletAddress)) {
                userWalletMapping.put(walletAddress, chatId);
                notifyTrackingService(walletAddress);
                TrackWalletsRedis.saveWalletChatID(walletAddress, chatId);
                logger.info("New wallet added: {}", walletAddress);
                bot.sendMessage(chatId, "✅ Wallet added successfully: " + walletAddress);
            } else {
                logger.warn("Wallet already exists: {}", walletAddress);
                bot.sendMessage(chatId, "⚠️ Wallet already exists: " + walletAddress);
            }
        } else {
            logger.error("Invalid wallet address: {}", walletAddress);
            bot.sendMessage(chatId, "❌ Invalid wallet address.");
        }
    }

    public void handleListWallets(String chatId) {
        if (walletAddresses.isEmpty()) {
            bot.sendMessage(chatId, "ℹ️ No wallets are being tracked.");
        } else {
            String wallets = String.join("\n", walletAddresses);
            bot.sendMessage(chatId, "ℹ️ Tracked wallets:\n" + wallets);
        }
    }

    public void handleRemoveWallet(String messageText, String chatId) {
        String[] parts = messageText.split(" ", 2);
        if (parts.length < 2) {
            bot.sendMessage(chatId, "❌ Invalid command. Use /remove_wallet <wallet_address>");
            return;
        }
        String walletAddress = parts[1];
        if (isValidWalletAddress(walletAddress)) {
//            if (walletAddresses.remove(walletAddress)) {
                userWalletMapping.remove(walletAddress);
                TrackWalletsRedis.deleteWalletChatIDs(walletAddress);
                RemoveWalletQueue.addWallet(walletAddress);
                bot.sendMessage(chatId, "✅ Wallet removed successfully: " + walletAddress);
//            } else {
//                bot.sendMessage(chatId, "⚠️ Wallet not found: " + walletAddress);
//            }
        } else {
            logger.error("Invalid wallet address: {}", walletAddress);
            bot.sendMessage(chatId, "❌ Invalid wallet address.");
        }
    }

    private boolean isValidWalletAddress(String address) {
        return Pattern.matches("^[A-Za-z0-9]{43,48}$", address);
    }

    private void notifyTrackingService(String walletAddress) {
        AddWalletQueue.addWallet(walletAddress);
        logger.info("✅ Wallet added to tracking service: {}", walletAddress);
    }
}

