/*
 * Copyright 2017, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.brahmaos.wallet.brahmawallet;

import android.app.Application;
import android.content.Intent;
import android.content.IntentFilter;

import java.util.Timer;
import java.util.TimerTask;

import brahmaos.content.BrahmaIntent;
import io.brahmaos.wallet.brahmawallet.common.BrahmaConfig;
import io.brahmaos.wallet.brahmawallet.common.BrahmaConst;
import io.brahmaos.wallet.brahmawallet.db.WalletDBMgr;
import io.brahmaos.wallet.brahmawallet.receiver.BtcDownloadProgressReceiver;
import io.brahmaos.wallet.brahmawallet.receiver.BtcTransactionReceiver;
import io.brahmaos.wallet.brahmawallet.receiver.BtcTxBroadcastCompleteReceiver;
import io.brahmaos.wallet.brahmawallet.repository.DataRepository;
import io.brahmaos.wallet.brahmawallet.service.MainService;
import io.brahmaos.wallet.brahmawallet.ui.FingerActivity;
import io.brahmaos.wallet.util.CommonUtil;
import io.rayup.sdk.RayUpApp;

/**
 * Android Application class. Used for accessing singletons.
 */
public class WalletApp extends Application {
    private boolean firstOpenApp = true;
    private static Boolean isTimeOut = false;
    private RayUpApp rayUpApp;
    private Timer timerTimeOut = new Timer();
    private TimerTask timerTask = new TimerTask() {
        @Override
        public void run() {
            isTimeOut = true;
        }
    };


    @Override
    public void onCreate() {
        super.onCreate();
        // init sqlite
        WalletDBMgr.getInstance().init(getApplicationContext());
        MainService.getInstance().init(getApplicationContext());
        // init the config
        BrahmaConfig.getInstance().init(getApplicationContext());
        rayUpApp = RayUpApp.initialize(BrahmaConst.rayupAccessKeyId, BrahmaConst.rayupAccessKeySecret);
        AppFrontBackHelper helper = new AppFrontBackHelper();
        helper.register(WalletApp.this, new AppFrontBackHelper.OnAppStatusListener() {
            @Override
            public void onFront() {
                if (!firstOpenApp && isTimeOut && BrahmaConfig.getInstance().isTouchId()
                        && CommonUtil.isFinger(getApplicationContext())) {
                    Intent intent = new Intent(WalletApp.this, FingerActivity.class);
                    startActivity(intent);
                }
            }

            @Override
            public void onBack() {
                firstOpenApp = false;
                isTimeOut = false;
                if (timerTimeOut != null) {
                    timerTimeOut.cancel();
                }
                timerTimeOut = new Timer();
                timerTask = new TimerTask() {
                    @Override
                    public void run() {
                        isTimeOut = true;
                    }
                };
                timerTimeOut.schedule(timerTask, 5000);
            }
        });

        BtcDownloadProgressReceiver btcDownloadProgressReceiver = new BtcDownloadProgressReceiver();
        IntentFilter intentFilter = new IntentFilter(BrahmaIntent.ACTION_CHAIN_DOWNLOAD_PROGRESS);
        getApplicationContext().registerReceiver(btcDownloadProgressReceiver, intentFilter);

        BtcTransactionReceiver btcTransactionReceiver = new BtcTransactionReceiver();
        IntentFilter txIntentFilter = new IntentFilter(BrahmaIntent.ACTION_TRANSACTION_CONFIDENCE_CHANGED);
        getApplicationContext().registerReceiver(btcTransactionReceiver, txIntentFilter);

        BtcTxBroadcastCompleteReceiver btcTxBroadcastCompleteReceiver = new BtcTxBroadcastCompleteReceiver();
        IntentFilter txBroadcastCompleteIntentFilter = new IntentFilter(BrahmaIntent.ACTION_TRANSACTION_BROADCAST_COMPLETE);
        getApplicationContext().registerReceiver(btcTxBroadcastCompleteReceiver, txBroadcastCompleteIntentFilter);
    }

    public RayUpApp getRayUpApp() {
        return rayUpApp;
    }

    public boolean isFirstOpenApp() {
        return firstOpenApp;
    }

    public DataRepository getRepository() {
        return DataRepository.getInstance();
    }
}
