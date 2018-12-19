package io.brahmaos.wallet.brahmawallet.service;

import android.content.Context;

import java.math.BigDecimal;
import java.math.BigInteger;

import brahmaos.app.WalletManager;
import brahmaos.content.BrahmaContext;
import brahmaos.content.WalletData;
import io.brahmaos.wallet.brahmawallet.BuildConfig;
import io.brahmaos.wallet.brahmawallet.common.BrahmaConst;
import io.brahmaos.wallet.brahmawallet.db.entity.AccountEntity;
import io.brahmaos.wallet.brahmawallet.db.entity.TokenEntity;
import io.brahmaos.wallet.brahmawallet.model.AccountAssets;
import io.brahmaos.wallet.util.BLog;

import rx.Completable;
import rx.CompletableSubscriber;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class EthAccountManager extends BaseService {
    @Override
    protected String tag() {
        return EthAccountManager.class.getName();
    }

    // singleton
    private static EthAccountManager instance = new EthAccountManager();
    public static EthAccountManager getInstance() {
        return instance;
    }

    @Override
    public boolean init(Context context) {
        super.init(context);
        return true;
    }

    public String getEthereumUrl() {
        if (BuildConfig.TEST_FLAG) {
            return BrahmaConst.ROPSTEN_TEST_URL;
        } else {
            return BrahmaConst.MAINNET_URL;
        }
    }

    public Observable<WalletData> createEthAccount(String name, String password) {
        return Observable.create(e -> {
            WalletManager mWalletManager = (WalletManager) context.getSystemService(BrahmaContext.WALLET_SERVICE);
            WalletData ethAccountData = mWalletManager.createEthereumWallet(name, password);
            MainService.getInstance().loadAllAccounts();
            e.onNext(ethAccountData);
            e.onCompleted();
        });
    }

    public Observable<WalletData> restoreEthAccountWithKeystore(String name, String password, String data) {
        return Observable.create(e -> {
            WalletManager mWalletManager = (WalletManager) context.getSystemService(BrahmaContext.WALLET_SERVICE);
            WalletData ethAccountData = mWalletManager.importEthereumWallet(name, password, data, WalletManager.IMPORT_BY_KEYSTORE);
            MainService.getInstance().loadAllAccounts();
            e.onNext(ethAccountData);
            e.onCompleted();
        });
    }

    public Observable<WalletData> restoreEthAccountWithPrivateKey(String name, String password, String data) {
        return Observable.create(e -> {
            WalletManager mWalletManager = (WalletManager) context.getSystemService(BrahmaContext.WALLET_SERVICE);
            WalletData ethAccountData = mWalletManager.importEthereumWallet(name, password, data, WalletManager.IMPORT_BY_PRIVATE_KEY);
            MainService.getInstance().loadAllAccounts();
            e.onNext(ethAccountData);
            e.onCompleted();
        });
    }

    public Observable<WalletData> restoreEthAccountWithMnemonics(String name, String password, String data) {
        return Observable.create(e -> {
            WalletManager mWalletManager = (WalletManager) context.getSystemService(BrahmaContext.WALLET_SERVICE);
            WalletData ethAccountData = mWalletManager.importEthereumWallet(name, password, data, WalletManager.IMPORT_BY_MNEMONICS);
            MainService.getInstance().loadAllAccounts();
            e.onNext(ethAccountData);
            e.onCompleted();
        });
    }

    public Observable<String> getEthereumGasPrice() {
        return Observable.create(e -> {
            WalletManager mWalletManager = (WalletManager) context.getSystemService(BrahmaContext.WALLET_SERVICE);
            String gasPrice = mWalletManager.getEthereumGasPrice(getEthereumUrl());
            e.onNext(gasPrice);
            e.onCompleted();
        });
    }

    public Observable<AccountAssets> getEthereumBalanceByAddress(AccountEntity account, TokenEntity token) {
        return Observable.create(e -> {
            WalletManager walletManager = (WalletManager) context.getSystemService(BrahmaContext.WALLET_SERVICE);
            String tokenBalanceStr = walletManager.getEthereumBalanceStringByAddress(getEthereumUrl(), account.getAddress(),
                    token.getAddress());
            BigInteger tokenBalance = BigInteger.ZERO;
            if (tokenBalanceStr != null) {
                tokenBalance = new BigInteger(tokenBalanceStr);
            }
            AccountAssets assets = new AccountAssets(account, token, tokenBalance);
            e.onNext(assets);
            e.onCompleted();
        });
    }

    public Observable<String> sendTransfer(AccountEntity account, TokenEntity token, String password,
                                           String destinationAddress, BigDecimal amount,
                                           BigDecimal gasPrice, BigInteger gasLimit, String remark) {
        return Observable.create(e -> {
            WalletManager mWalletManager = (WalletManager) context.getSystemService(BrahmaContext.WALLET_SERVICE);
            String txHash = mWalletManager.transferEthereum(getEthereumUrl(), account.getAddress(),
                    token.getAddress(), password, destinationAddress, amount.doubleValue(),
                    gasPrice.doubleValue(), gasLimit.longValue(), remark);
            e.onNext(txHash);
            e.onCompleted();
        });
    }

    public Observable<Integer> updateAccountPassword(String accountAddress,
                                                     String oldPassword, String newPassword) {
        return Observable.create(e -> {
            WalletManager mWalletManager = (WalletManager) context.getSystemService(BrahmaContext.WALLET_SERVICE);
            int ret = mWalletManager.updateEthereumWalletPassword(accountAddress, oldPassword, newPassword);
            e.onNext(ret);
            e.onCompleted();
        });
    }

    public Observable<Boolean> checkPasswordForWallet(String address, String password) {
        return Observable.create(e -> {
            try {
                WalletManager walletManager = (WalletManager) context.getSystemService(BrahmaContext.WALLET_SERVICE);
                WalletData walletData = walletManager.getWalletDataByAddress(address);
                e.onNext(walletManager.checkPasswordForWallet(walletData, password));
            } catch (Exception e1) {
                e1.printStackTrace();
                e.onError(e1);
            }
            e.onCompleted();
        });
    }

    public Observable<String> getKeystoreByPassword(String address, String password) {
        return Observable.create(e -> {
            try {
                WalletManager walletManager = (WalletManager) context.getSystemService(BrahmaContext.WALLET_SERVICE);
                WalletData walletData = walletManager.getWalletDataByAddress(address);
                if (walletManager.checkPasswordForWallet(walletData, password)) {
                    e.onNext(walletData.keyStore);
                } else {
                    e.onNext(null);
                }
            } catch (Exception e1) {
                e1.printStackTrace();
                e.onError(e1);
            }
            e.onCompleted();
        });
    }

    public Observable<String> getPrivateKeyByPassword(String address, String password) {
        return Observable.create(e -> {
            try {
                WalletManager walletManager = (WalletManager) context.getSystemService(BrahmaContext.WALLET_SERVICE);
                e.onNext(walletManager.exportEthereumWalletPrivateKey(address, password));
            } catch (Exception e1) {
                e1.printStackTrace();
                e.onError(e1);
            }
            e.onCompleted();
        });
    }

    public Observable<Integer> deleteEthereumAccount(String address, String password) {
        return Observable.create(e -> {
            WalletManager walletManager = (WalletManager) context.getSystemService(BrahmaContext.WALLET_SERVICE);
            e.onNext(walletManager.deleteWalletByAddress(address, password));
            e.onCompleted();
        });
    }
}
