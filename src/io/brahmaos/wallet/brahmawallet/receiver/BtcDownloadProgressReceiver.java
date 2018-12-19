package io.brahmaos.wallet.brahmawallet.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.Date;

import brahmaos.content.BrahmaIntent;
import io.brahmaos.wallet.brahmawallet.event.EventTypeDef;
import io.brahmaos.wallet.brahmawallet.model.BitcoinDownloadProgress;
import io.brahmaos.wallet.util.BLog;
import io.brahmaos.wallet.util.RxEventBus;

public class BtcDownloadProgressReceiver extends BroadcastReceiver {
    protected String tag() {
        return BtcDownloadProgressReceiver.class.getName();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        BitcoinDownloadProgress progress = new BitcoinDownloadProgress();
        progress.setProgressPercentage(intent.getDoubleExtra(BrahmaIntent.EXTRA_PCT, 0));
        progress.setBlocksLeft(intent.getIntExtra(BrahmaIntent.EXTRA_BLOCKS_SO_FAR, 0));
        progress.setDownloaded(false);
        progress.setCurrentBlockDateString(intent.getStringExtra(BrahmaIntent.EXTRA_DATE_STRING));
        BLog.d(tag(), progress.toString());
        RxEventBus.get().post(EventTypeDef.BTC_ACCOUNT_SYNC, progress);
    }
}
