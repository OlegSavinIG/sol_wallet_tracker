package test.sol.telegram.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserStateHandler {
    private final Map<String, String> userStates = new ConcurrentHashMap<>();

    // Устанавливаем состояние для пользователя
    public void setUserState(String chatId, String state) {
        userStates.put(chatId, state);
    }

    // Получаем текущее состояние пользователя
    public String getUserState(String chatId) {
        return userStates.get(chatId);
    }

    // Удаляем состояние пользователя
    public void clearUserState(String chatId) {
        userStates.remove(chatId);
    }

    // Проверяем, есть ли состояние для пользователя
    public boolean hasState(String chatId) {
        return userStates.containsKey(chatId);
    }
}

