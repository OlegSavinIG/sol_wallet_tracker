package test.sol;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SolanaScannerScheduler {

    public static void main(String[] args) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        Runnable accountCreationTask = () -> {
            try {
                System.out.println("Starting accountCreationTask at: " + LocalTime.now());
                Thread.sleep(60 * 1000);
                SolanaAccountCreationScanner.main(null);
                System.out.println("Finished accountCreationTask at: " + LocalTime.now());
            } catch (Exception e) {
                System.err.println("Error while executing SolanaAccountCreationScanner: " + e.getMessage());
                e.printStackTrace();
            }
        };

        Runnable defiScannerTask = new Runnable() {
            private LocalDateTime lastExecutionTime = null;

            @Override
            public void run() {
                try {
                    System.out.println("Starting defiScannerTask at: " + LocalTime.now());
                    if (isNightPeriod()) {
                        if (lastExecutionTime == null || lastExecutionTime.isBefore(LocalDateTime.now().minusHours(1))) {
                            System.out.println("Running defiScannerTask in night mode");
                            SolanaDefiScanner.main(null);
                            lastExecutionTime = LocalDateTime.now();
                        } else {
                            System.out.println("defiScannerTask skipped: executed less than an hour ago (night mode).");
                        }
                    } else {
                        try {
                            System.out.println("Delaying defiScannerTask for 4 minutes in day mode");
                            Thread.sleep(3 * 60 * 1000);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        System.out.println("Running defiScannerTask in day mode");
                        SolanaDefiScanner.main(null);
                    }
                    System.out.println("Finished defiScannerTask at: " + LocalTime.now());
                } catch (Exception e) {
                    System.err.println("Error while executing SolanaDefiScanner: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        };

        scheduler.scheduleWithFixedDelay(accountCreationTask, 0, 4, TimeUnit.MINUTES);

        scheduler.scheduleWithFixedDelay(() -> {
            try {
                defiScannerTask.run();
            } catch (Exception e) {
                System.err.println("Error in scheduler: " + e.getMessage());
                e.printStackTrace();
            }
        }, 0, 1, TimeUnit.MINUTES);

        System.out.println("Scheduler started. Tasks are running on schedule at: " + LocalDateTime.now());
    }

    private static boolean isNightPeriod() {
        LocalTime now = LocalTime.now();
        LocalTime startNight = LocalTime.of(3, 0);
        LocalTime endNight = LocalTime.of(9, 0);
        boolean isNight = !now.isBefore(startNight) && !now.isAfter(endNight);
        System.out.println("isNightPeriod: " + isNight + " at: " + now);
        return isNight;
    }
}
