package test.sol.telegram.keyboard;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import test.sol.telegram.WalletTrackerBot;

import java.util.ArrayList;
import java.util.List;

public class InlineKeyboard {
    private final WalletTrackerBot bot;

    public InlineKeyboard(WalletTrackerBot bot) {
        this.bot = bot;
    }

    public void sendInlineKeyboard(String chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("📋 Выберите действие:");

        // Создаем разметку для встроенных кнопок
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();

        // Создаем список строк кнопок
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        // Первая строка кнопок
        InlineKeyboardButton buttonAddWallet = new InlineKeyboardButton();
        buttonAddWallet.setText("➕ Add Wallet");
        buttonAddWallet.setCallbackData("add_wallet");
        InlineKeyboardButton buttonListWallets = new InlineKeyboardButton();
        buttonListWallets.setText("📋 List Wallets");
        buttonListWallets.setCallbackData("list_wallets");
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(buttonAddWallet);
        row1.add(buttonListWallets);

        // Вторая строка кнопок
        InlineKeyboardButton buttonRemoveWallet = new InlineKeyboardButton();
        buttonRemoveWallet.setText("❌ Remove Wallet");
        buttonRemoveWallet.setCallbackData("remove_wallet");
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(buttonRemoveWallet);

        // Добавляем строки в клавиатуру
        keyboard.add(row1);
        keyboard.add(row2);

        // Устанавливаем клавиатуру в разметке
        inlineKeyboardMarkup.setKeyboard(keyboard);

        // Привязываем разметку к сообщению
        message.setReplyMarkup(inlineKeyboardMarkup);

        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}

