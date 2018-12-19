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

public class BtcTransactionReceiver extends BroadcastReceiver {
    protected String tag() {
        return BtcTransactionReceiver.class.getName();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        BLog.d(tag(), "the btc transaction broadcast receiver");
        RxEventBus.get().post(EventTypeDef.BTC_TRANSACTION_CHANGE, true);
    }
}
