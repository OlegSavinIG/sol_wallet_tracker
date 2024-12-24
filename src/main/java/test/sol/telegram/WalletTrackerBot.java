package test.sol.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import test.sol.wallettracker.queuelistener.RemoveWalletQueue;
import test.sol.wallettracker.queuelistener.WalletQueue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class WalletTrackerBot extends TelegramLongPollingBot {

    private static final String BOT_USERNAME = "Sol_Wallet_WatcherBot";
    private static final String BOT_TOKEN = "8144297666:AAHHYXwjQJ2Cu65Nnyb25OTFfIWly10F6gU";
    private static final Set<String> walletAddresses = new HashSet<>();
    private static final Map<String, String> userWalletMapping = new HashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(WalletTrackerBot.class);

    public static void main(String[] args) {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new WalletTrackerBot());
            System.out.println("✅ Bot started successfully!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getBotUsername() {
        return BOT_USERNAME;
    }

    @Override
    public String getBotToken() {
        return BOT_TOKEN;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            String chatId = update.getMessage().getChatId().toString();

            logger.info("Received message: {}", messageText);

            if (messageText.startsWith("/add_wallet")) {
                handleAddWallet(messageText, chatId);
            } else if (messageText.startsWith("/list_wallets")) {
                handleListWallets(chatId);
            } else if (messageText.startsWith("/remove_wallet")) {
                handleRemoveWallet(messageText, chatId);
            } else {
                sendMessage(
                        chatId,
                        "ℹ️ Available commands:\n/add_wallet <wallet_address>" +
                                " - Add a new wallet\n/list_wallets" +
                                " - List all wallets\n/remove_wallet <wallet_address>" +
                                " - Remove a wallet");
            }
        }
    }

    private void handleAddWallet(String messageText, String chatId) {
        String[] parts = messageText.split(" ", 2);
        if (parts.length < 2) {
            logger.warn("Invalid /add_wallet command received from chat: {}", chatId);
            sendMessage(chatId, "❌ Invalid command. Use /add_wallet <wallet_address>");
            return;
        }
        String walletAddress = parts[1];
        if (isValidWalletAddress(walletAddress)) {
            if (walletAddresses.add(walletAddress)) {
                userWalletMapping.put(walletAddress, chatId);
                notifyTrackingService(walletAddress);
                logger.info("New wallet added: {}", walletAddress);
                sendMessage(chatId, "✅ Wallet added successfully: " + walletAddress);
            } else {
                logger.warn("Wallet already exists: {}", walletAddress);
                sendMessage(chatId, "⚠️ Wallet already exists: " + walletAddress);
            }
        } else {
            logger.error("Invalid wallet address: {}", walletAddress);
            sendMessage(chatId, "❌ Invalid wallet address.");
        }
    }

    private void handleListWallets(String chatId) {
        if (walletAddresses.isEmpty()) {
            sendMessage(chatId, "ℹ️ No wallets are being tracked.");
        } else {
            String wallets = String.join("\n", walletAddresses);
            sendMessage(chatId, "ℹ️ Tracked wallets:\n" + wallets);
        }
    }

    private void handleRemoveWallet(String messageText, String chatId) {
        String[] parts = messageText.split(" ", 2);
        if (parts.length < 2) {
            sendMessage(chatId, "❌ Invalid command. Use /remove_wallet <wallet_address>");
            return;
        }
        String walletAddress = parts[1];
        if (isValidWalletAddress(walletAddress)) {
            if (walletAddresses.remove(walletAddress)) {
                userWalletMapping.remove(walletAddress);
                RemoveWalletQueue.addWallet(walletAddress); // Добавляем в очередь на удаление
                sendMessage(chatId, "✅ Wallet removal queued: " + walletAddress);
                sendMessage(chatId, "✅ Wallet removed successfully: " + walletAddress);
            } else {
                sendMessage(chatId, "⚠️ Wallet not found: " + walletAddress);
            }
        } else {
            logger.error("Invalid wallet address: {}", walletAddress);
            sendMessage(chatId, "❌ Invalid wallet address.");
        }
    }

    private boolean isValidWalletAddress(String address) {
        return Pattern.matches("^[A-Za-z0-9]{43,48}$", address);
    }

    private void sendMessage(String chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void notifyTrackingService(String walletAddress) {
        WalletQueue.addWallet(walletAddress);
        logger.info("✅ Wallet added to tracking service: {}", walletAddress);
    }


    public void notifyUserAboutEvent(String walletAddress, String eventDetails) {
        String chatId = userWalletMapping.get(walletAddress);
        if (chatId != null) {
            sendMessage(chatId, "🔔 Event detected for wallet " + walletAddress + ":\n" + eventDetails);
        }
    }
}


