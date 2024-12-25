package test.sol.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import test.sol.telegram.keyboard.InlineKeyboard;
import test.sol.telegram.service.UserStateHandler;
import test.sol.telegram.service.WalletHandlerService;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class WalletTrackerBot extends TelegramLongPollingBot {

    private static final String BOT_USERNAME = "Sol_Wallet_WatcherBot";
    private static final String BOT_TOKEN = "8144297666:AAHHYXwjQJ2Cu65Nnyb25OTFfIWly10F6gU";
    private static final Set<String> walletAddresses = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Map<String, String> userWalletMapping = new ConcurrentHashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(WalletTrackerBot.class);
    private final InlineKeyboard inlineKeyboard;
    private final WalletHandlerService walletHandlerService;
    private final UserStateHandler userStateHandler = new UserStateHandler();


    public WalletTrackerBot() {
        inlineKeyboard = new InlineKeyboard(this);
        walletHandlerService = new WalletHandlerService(walletAddresses, userWalletMapping, this);
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
            messageProcessor(update);
        }
        if (update.hasCallbackQuery()) {
            callbackQueryProcessor(update);
        }
    }

    private void callbackQueryProcessor(Update update) {
        String callbackData = update.getCallbackQuery().getData();
        String chatId = update.getCallbackQuery().getMessage().getChatId().toString();

        logger.info("CallbackQuery received: {}", callbackData);

        switch (callbackData) {
            case "add_wallet":
                userStateHandler.setUserState(chatId, "awaiting_wallet_add"); // Устанавливаем состояние
                sendMessage(chatId, "Введите номер кошелька для добавления:");
                break;
            case "list_wallets":
                walletHandlerService.handleListWallets(chatId);
                break;
            case "remove_wallet":
                userStateHandler.setUserState(chatId, "awaiting_wallet_remove"); // Устанавливаем состояние
                sendMessage(chatId, "Введите номер кошелька для удаления:");
                break;
            default:
                sendMessage(chatId, "❓ Неизвестное действие.");
        }
    }


    private void messageProcessor(Update update) {
        String messageText = update.getMessage().getText();
        String chatId = update.getMessage().getChatId().toString();

        logger.info("Received message: {}", messageText);

        // Проверяем, находится ли пользователь в каком-либо состоянии
        if (userStateHandler.hasState(chatId)) {
            String currentState = userStateHandler.getUserState(chatId);

            switch (currentState) {
                case "awaiting_wallet_add":
                    walletHandlerService.handleAddWallet("/add_wallet " + messageText, chatId);
                    userStateHandler.clearUserState(chatId); // Удаляем состояние
                    break;
                case "awaiting_wallet_remove":
                    walletHandlerService.handleRemoveWallet("/remove_wallet " + messageText, chatId);
                    userStateHandler.clearUserState(chatId); // Удаляем состояние
                    break;
                default:
                    sendMessage(chatId, "⚠️ Неизвестное состояние. Используйте /start, чтобы начать.");
                    userStateHandler.clearUserState(chatId); // Очищаем некорректное состояние
            }
            return; // Прерываем обработку, если состояние было обработано
        }

        // Обработка обычных команд
        if (messageText.equals("/start")) {
            inlineKeyboard.sendInlineKeyboard(chatId);
        } else if (messageText.startsWith("/add_wallet")) {
            walletHandlerService.handleAddWallet(messageText, chatId);
        } else if (messageText.startsWith("/list_wallets")) {
            walletHandlerService.handleListWallets(chatId);
        } else if (messageText.startsWith("/remove_wallet")) {
            walletHandlerService.handleRemoveWallet(messageText, chatId);
        } else {
            sendMessage(
                    chatId,
                    "ℹ️ Available commands:\n/add_wallet <wallet_address>" +
                            " - Add a new wallet\n/list_wallets" +
                            " - List all wallets\n/remove_wallet <wallet_address>" +
                            " - Remove a wallet");
        }
    }


    public void sendMessage(String chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void notifyUserAboutEvent(String walletAddress, String eventDetails) {
        String chatId = userWalletMapping.get(walletAddress);
        logger.info("Sending response for chatId {}", chatId);
        if (chatId != null) {
            sendMessage(chatId, "🔔 Event detected for wallet " + walletAddress + ":\n" + eventDetails);
        }
    }
}


