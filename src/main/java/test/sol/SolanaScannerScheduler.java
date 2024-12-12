package test.sol;

import test.sol.service.SolanaAccountCreationScanner;
import test.sol.service.SolanaDefiScanner;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SolanaScannerScheduler {

    public static void main(String[] args) throws InterruptedException {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

        Runnable accountCreationTask = () -> {
            try {
                SolanaAccountCreationScanner.main(null); // Запускаем ваш основной класс
            } catch (Exception e) {
                System.err.println("Ошибка при выполнении задачи: " + e.getMessage());
                e.printStackTrace();
            }
        };
        Thread.sleep(3000);
        Runnable defiScannerTask = () -> {
            try {
                SolanaDefiScanner.main(null);
            } catch (Exception e) {
                System.err.println("Ошибка при выполнении defiScannerTask: " + e.getMessage());
                e.printStackTrace();
            }
        };

        scheduler.scheduleAtFixedRate(accountCreationTask, 0, 7, TimeUnit.MINUTES);
        scheduler.scheduleAtFixedRate(defiScannerTask, 0, 10, TimeUnit.MINUTES);

        System.out.println("Планировщик запущен. Задачи выполняются по расписанию.");
    }
}
