package test.sol;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SolanaScannerScheduler {

    public static void main(String[] args) {
        // Создаем планировщик с одним потоком
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        // Задача, которую нужно выполнять каждые 10 минут
        Runnable task = () -> {
            try {
                SolanaAccountCreationScanner.main(null); // Запускаем ваш основной класс
            } catch (Exception e) {
                System.err.println("Ошибка при выполнении задачи: " + e.getMessage());
                e.printStackTrace();
            }
        };

        // Запуск задачи с начальной задержкой 0 секунд и интервалом 10 минут
        scheduler.scheduleAtFixedRate(task, 0, 9, TimeUnit.MINUTES);

        // Примечание: приложение будет работать в бесконечном цикле, пока вы его не завершите.
        System.out.println("Планировщик запущен. Задача будет выполняться каждые 9 минут.");
    }
}
