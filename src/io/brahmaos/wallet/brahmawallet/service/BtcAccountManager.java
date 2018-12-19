package io.brahmaos.wallet.brahmawallet.service;

import android.content.Context;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Peer;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.Utils;
import org.bitcoinj.core.listeners.DownloadProgressTracker;
import org.bitcoinj.core.listeners.TransactionConfidenceEventListener;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.SendRequest;
import org.bitcoinj.wallet.Wallet;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

import brahmaos.app.WalletManager;
import brahmaos.content.BrahmaContext;
import brahmaos.content.TransactionDetails;
import brahmaos.content.WalletData;
import io.brahmaos.wallet.brahmawallet.BuildConfig;
import io.brahmaos.wallet.brahmawallet.db.entity.AccountEntity;
import io.brahmaos.wallet.brahmawallet.event.EventTypeDef;
import io.brahmaos.wallet.brahmawallet.model.BitcoinDownloadProgress;
import io.brahmaos.wallet.util.BLog;
import io.brahmaos.wallet.util.CommonUtil;
import io.brahmaos.wallet.util.RxEventBus;
import rx.Observable;

public class BtcAccountManager extends BaseService {
    @Override
    protected String tag() {
        return BtcAccountManager.class.getName();
    }

    // singleton
    private static BtcAccountManager instance = new BtcAccountManager();
    public static BtcAccountManager getInstance() {
        return instance;
    }

    public static int BYTES_PER_BTC_KB = 1000;
    public static int MIN_CONFIRM_BLOCK_HEIGHT = 6;

    @Override
    public boolean init(Context context) {
        super.init(context);
        return true;
    }

    public NetworkParameters getNetworkParams() {
        if (BuildConfig.TEST_FLAG) {
            return TestNet3Params.get();
        } else {
            return MainNetParams.get();
        }
    }

    public String transfer(String accountAddress, String receiveAddress, String password,
                            BigDecimal amount, long fee, String remark) {
        WalletManager walletManager = (WalletManager) context.getSystemService(BrahmaContext.WALLET_SERVICE);
        return walletManager.transferBitcoin(accountAddress, receiveAddress, password, amount.doubleValue(),
                fee, remark);
    }

    public Observable<WalletData> importBtcAccountWithMnemonics(String name, String password, String data) {
        return Observable.create(e -> {
            WalletManager mWalletManager = (WalletManager) context.getSystemService(BrahmaContext.WALLET_SERVICE);
            WalletData ethAccountData = mWalletManager.importBitcoinWallet(name, password, data, WalletManager.IMPORT_BY_MNEMONICS);
            MainService.getInstance().loadAllAccounts();
            e.onNext(ethAccountData);
            e.onCompleted();
        });
    }

    public Observable<WalletData> createBtcAccount(String name, String password) {
        return Observable.create(e -> {
            WalletManager mWalletManager = (WalletManager) context.getSystemService(BrahmaContext.WALLET_SERVICE);
            WalletData ethAccountData = mWalletManager.createBitcoinWallet(name, password);
            MainService.getInstance().loadAllAccounts();
            e.onNext(ethAccountData);
            e.onCompleted();
        });
    }

    public long getBtcAccountBalance(String address) {
        WalletManager walletManager = (WalletManager) context.getSystemService(BrahmaContext.WALLET_SERVICE);
        return walletManager.getBitcoinBalance(address);
    }

    public String getBtcLastBlockSeenTime(String address) {
        WalletManager walletManager = (WalletManager) context.getSystemService(BrahmaContext.WALLET_SERVICE);
        return walletManager.getBitcoinLastBlockSeenTime(address);
    }

    public String getBtcCurrentReceiveAddress(String address) {
        WalletManager walletManager = (WalletManager) context.getSystemService(BrahmaContext.WALLET_SERVICE);
        return walletManager.getBitcoinCurrentReceiveAddress(address);
    }

    public long getBtcLastBlockSeenHeight(String address) {
        WalletManager walletManager = (WalletManager) context.getSystemService(BrahmaContext.WALLET_SERVICE);
        return walletManager.getBitcoinLastBlockSeenHeight(address);
    }

    public int getBtcPrivateKeyCount(String address) {
        WalletManager walletManager = (WalletManager) context.getSystemService(BrahmaContext.WALLET_SERVICE);
        return walletManager.getBitcoinPrivateKeysCount(address);
    }

    public long getBitcoinPendingTxAmount(String address) {
        WalletManager walletManager = (WalletManager) context.getSystemService(BrahmaContext.WALLET_SERVICE);
        return walletManager.getBitcoinPendingTxAmount(address);
    }

    public Observable<Integer> updateAccountPassword(String accountAddress, String oldPassword, String newPassword) {
        return Observable.create(e -> {
            WalletManager mWalletManager = (WalletManager) context.getSystemService(BrahmaContext.WALLET_SERVICE);
            int ret = mWalletManager.updateBitcoinWalletPassword(accountAddress, oldPassword, newPassword);
            e.onNext(ret);
            e.onCompleted();
        });
    }

    public boolean isValidBtcAddress(String address) {
        try {
            Address.fromBase58(getNetworkParams(), address);
            return true;
        } catch (Exception e) {
            e.fillInStackTrace();
            return false;
        }
    }

    public int getTransactionAccount(String address) {
        List<TransactionDetails> transactions = getTransactionsByTime(address);
        if (transactions == null) {
            return 0;
        } else {
            return transactions.size();
        }
    }

    public List<TransactionDetails> getTransactionsByTime(String address) {
        WalletManager walletManager = (WalletManager) context.getSystemService(BrahmaContext.WALLET_SERVICE);
        return walletManager.getBitcoinTransactionsByTime(address);
    }
}
