package test.sol.telegram;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import test.sol.utils.ConfigLoader;

import java.io.IOException;

public class TelegramInformationMessageHandler {
    public static void sendToTelegram(String message) {
        String botToken = ConfigLoader.getString("INFORMATION_BOT_TOKEN");
        String chatId = ConfigLoader.getString("INFORMATION_BOT_ID"); // Укажите ID вашего чата (можно получить с помощью метода /getUpdates)

        String url = String.format(
                "https://api.telegram.org/bot%s/sendMessage?chat_id=%s&text=%s",
                botToken, chatId, message
        );

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                System.err.println("Ошибка отправки сообщения в Telegram: " + response.body().string());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
