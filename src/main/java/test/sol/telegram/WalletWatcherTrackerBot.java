package test.sol.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import test.sol.redis.TrackWalletsRedis;
import test.sol.telegram.keyboard.InlineKeyboard;
import test.sol.telegram.service.UserStateHandler;
import test.sol.telegram.service.WalletHandlerService;
import test.sol.utils.ConfigLoader;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class WalletWatcherTrackerBot extends TelegramLongPollingBot {
    private static final String BOT_USERNAME = ConfigLoader.getString("WALLETWATCHER_BOT_USERNAME");
    private static final String BOT_TOKEN = ConfigLoader.getString("WALLETWATCHER_BOT_TOKEN");
    private static final Set<String> walletAddresses = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Map<String, String> userWalletMapping = new ConcurrentHashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(WalletWatcherTrackerBot.class);
    private final InlineKeyboard inlineKeyboard;
    private final WalletHandlerService walletHandlerService;
    private final UserStateHandler userStateHandler = new UserStateHandler();


    public WalletWatcherTrackerBot() {
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
                    userStateHandler.clearUserState(chatId);
                    inlineKeyboard.sendInlineKeyboard(chatId);// Удаляем состояние
                    break;
                case "awaiting_wallet_remove":
                    walletHandlerService.handleRemoveWallet("/remove_wallet " + messageText, chatId);
                    userStateHandler.clearUserState(chatId);
                    inlineKeyboard.sendInlineKeyboard(chatId);// Удаляем состояние
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
            inlineKeyboard.sendInlineKeyboard(chatId);
        } else if (messageText.startsWith("/list_wallets")) {
            walletHandlerService.handleListWallets(chatId);
            inlineKeyboard.sendInlineKeyboard(chatId);
        } else if (messageText.startsWith("/remove_wallet")) {
            walletHandlerService.handleRemoveWallet(messageText, chatId);
            inlineKeyboard.sendInlineKeyboard(chatId);
        } else {
            sendMessage(
                    chatId,
                    "ℹ️ Available commands:\n/start" +
                            "\n/add_wallet <wallet_address>" +
                            " - Add a new wallet\n/list_wallets" +
                            " - List all wallets\n/remove_wallet <wallet_address>" +
                            " - Remove a wallet");
            inlineKeyboard.sendInlineKeyboard(chatId);
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
        Set<String> walletChatIDs = TrackWalletsRedis.getWalletChatIDs(walletAddress);
        logger.info("Sending response for chatIds {}", walletChatIDs);
        if (walletChatIDs != null) {
            walletChatIDs.forEach(chatId -> sendMessage(
                    chatId, "🔔 Event detected for wallet https://gmgn.ai/sol/address/"
                    + walletAddress + ":\n" + eventDetails));
        }
    }
}


