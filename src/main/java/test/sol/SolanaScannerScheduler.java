package test.sol;

import test.sol.defiwebsocket.SolanaDefiWebSocketValidator;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SolanaScannerScheduler {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    public static void main(String[] args) {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        Runnable initializeWebSocketValidator = () -> {
            try {
                SolanaDefiWebSocketValidator.main(args);
            } catch (Exception e) {
                System.err.println("Error while initializing SolanaDefiWebSocketValidator: " + e.getMessage());
                e.printStackTrace();
            }
        };

        initializeWebSocketValidator.run();

        Runnable accountCreationTask = () -> {
            try {
                SolanaNewWalletScanner.main(null);
            } catch (Exception e) {
                System.err.println("Error while executing SolanaNewWalletScanner: " + e.getMessage());
                e.printStackTrace();
            }
        };

        Runnable defiScannerTask = () -> {
            try {
                SolanaDefiScanner.main(null);
            } catch (Exception e) {
                System.err.println("Error while executing SolanaDefiScanner: " + e.getMessage());
                e.printStackTrace();
            }
        };

        Runnable combinedTask = () -> {
            try {
                accountCreationTask.run();
                TimeUnit.SECONDS.sleep(25);
                defiScannerTask.run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Task interrupted: " + e.getMessage());
            } catch (Exception e) {
                System.err.println("Error in combinedTask: " + e.getMessage());
                e.printStackTrace();
            }
        };

        scheduler.scheduleWithFixedDelay(combinedTask, 0, 4, TimeUnit.MINUTES);

        System.out.println("Scheduler started. Tasks are running on schedule at: " + LocalDateTime.now().format(TIME_FORMATTER));
    }
}
