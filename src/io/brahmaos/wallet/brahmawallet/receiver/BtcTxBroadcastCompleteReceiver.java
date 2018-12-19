package io.brahmaos.wallet.brahmawallet.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import io.brahmaos.wallet.brahmawallet.event.EventTypeDef;
import io.brahmaos.wallet.util.BLog;
import io.brahmaos.wallet.util.RxEventBus;

public class BtcTxBroadcastCompleteReceiver extends BroadcastReceiver {
    protected String tag() {
        return BtcTxBroadcastCompleteReceiver.class.getName();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        BLog.d(tag(), "the btc transaction broadcast complete receiver");
        RxEventBus.get().post(EventTypeDef.BTC_TRANSACTION_BROADCAST_COMPLETE, true);
    }
}
