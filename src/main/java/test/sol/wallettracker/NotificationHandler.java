package test.sol.wallettracker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import test.sol.pojo.notification.Value;
import test.sol.telegram.WalletTrackerBot;
public class NotificationHandler {
    private static final Logger logger = LoggerFactory.getLogger(NotificationHandler.class);
    private static final WalletTrackerBot bot = new WalletTrackerBot();

    public void handleNotification(Integer subscription) {
        String wallet = SubscriptionWalletStorage.getWalletBySubscription(subscription);
        logger.info("\uD83D\uDCB3 Wallet: {}", wallet);
        bot.notifyUserAboutEvent(wallet , "New event");
    }

    private void processValue(String address, Value value) {
        logger.info("Processing value for address: {} with balance: {} lamports", address, value.getLamports());
    }

    public void handleSubscribeNotification(int result, int id) {
        SubscriptionWalletStorage.addSubscriptionWithId(result, id);
    }
}