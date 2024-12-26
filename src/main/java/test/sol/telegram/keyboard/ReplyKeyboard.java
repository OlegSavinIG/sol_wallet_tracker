package test.sol.telegram.keyboard;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

public class ReplyKeyboard {

    private void sendReplyKeyboard(String chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("📋 Выберите действие:");

        // Создаем разметку клавиатуры
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true); // Делает кнопки адаптивными
        keyboardMarkup.setOneTimeKeyboard(false); // Клавиатура не исчезает после нажатия

        // Создаем список строк клавиатуры
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        // Первая строка кнопок
        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("➕ Добавить кошелек"));
        row1.add(new KeyboardButton("📋 Список кошельков"));

        // Вторая строка кнопок
        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("❌ Удалить кошелек"));

        // Добавляем строки в клавиатуру
        keyboardRows.add(row1);
        keyboardRows.add(row2);

        // Устанавливаем клавиатуру в разметке
        keyboardMarkup.setKeyboard(keyboardRows);

        // Привязываем разметку к сообщению
        message.setReplyMarkup(keyboardMarkup);

        // Отправляем сообщение с клавиатурой
    }

}
