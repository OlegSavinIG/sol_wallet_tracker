package test.sol;

import test.sol.service.SolanaAccountCreationScanner;
import test.sol.service.SolanaDefiScanner;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SolanaScannerScheduler {

    public static void main(String[] args) {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

        Runnable accountCreationTask = () -> {
            try {
                SolanaAccountCreationScanner.main(null);
            } catch (Exception e) {
                System.err.println("Ошибка при выполнении SolanaAccountCreationScanner: " + e.getMessage());
                e.printStackTrace();
            }
        };
        Runnable defiScannerTask = () -> {
            try {
                SolanaDefiScanner.main(null);
            } catch (Exception e) {
                System.err.println("Ошибка при выполнении SolanaDefiScanner: " + e.getMessage());
                e.printStackTrace();
            }
        };

        scheduler.scheduleAtFixedRate(accountCreationTask, 0, 4, TimeUnit.MINUTES);
        scheduler.scheduleAtFixedRate(defiScannerTask, 0, 10, TimeUnit.MINUTES);

        System.out.println("Планировщик запущен. Задачи выполняются по расписанию.");
    }
}
