package io.brahmaos.wallet.brahmawallet.service;

import android.content.Context;

import java.io.IOException;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Wallet;
import org.web3j.crypto.WalletFile;
import org.web3j.crypto.WalletUtils;
import org.web3j.crypto.ECKeyPair;
import org.web3j.protocol.ObjectMapperFactory;
import org.web3j.crypto.CipherException;

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

    public void getEthereumBalanceByAddress(AccountEntity account, TokenEntity token) {
        WalletManager walletManager = (WalletManager) context.getSystemService(BrahmaContext.WALLET_SERVICE);
        walletManager.getEthereumBalanceByAddress(getEthereumUrl(), account.getAddress(),
                token.getAddress(), new GetEthereumBalanceListener(account, token));
    }

    private class GetEthereumBalanceListener implements WalletManager.OnETHBlanceGetListener {
        private AccountEntity account;
        private TokenEntity token;

        public GetEthereumBalanceListener(AccountEntity account, TokenEntity token) {
            this.account = account;
            this.token = token;
        }

        @Override
        public void onETHBlanceGetError() {
            BLog.d(tag(), "get " + account.getName() + "'s " + token.getName() + " balances error");
            AccountAssets assets = new AccountAssets(account, token, BigInteger.ZERO);
            MainService.getInstance().checkTokenAsset(assets);
        }

        @Override
        public void onETHBlanceGetSuccess(String balance) {
            BLog.d(tag(), "get " + account.getName() + "'s " + token.getName() + " balances is: " + balance);
            AccountAssets assets = new AccountAssets(account, token, new BigInteger(balance));
            MainService.getInstance().checkTokenAsset(assets);
        }
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

    public Observable<String> getKeystoreByPassword(String address, String password) {
        return Observable.create(e -> {
            try {
                WalletManager walletManager = (WalletManager) context.getSystemService(BrahmaContext.WALLET_SERVICE);
                WalletData walletData = walletManager.getWalletDataByAddress(address);
                BLog.d(tag(), "the keystore is: " + walletData.keyStore);
                ObjectMapper objectMapper = ObjectMapperFactory.getObjectMapper();
                WalletFile walletFile = objectMapper.readValue(walletData.keyStore, WalletFile.class);
                if (BrahmaWeb3jService.getInstance().isValidKeystore(walletFile, password)) {
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
                WalletData walletData = walletManager.getWalletDataByAddress(address);
                ObjectMapper objectMapper = ObjectMapperFactory.getObjectMapper();
                WalletFile walletFile = objectMapper.readValue(walletData.keyStore, WalletFile.class);
                Credentials credentials = Credentials.create(Wallet.decrypt(password, walletFile));
                BigInteger privateKey = credentials.getEcKeyPair().getPrivateKey();
                BLog.e(tag(), "the private key is:" + privateKey.toString(16));
                e.onNext(privateKey.toString(16));
            } catch (Exception e1) {
                e1.printStackTrace();
                e.onError(e1);
            }
            e.onCompleted();
        });
    }

    public Observable<Integer> deleteEthereumAccount(String address) {
        return Observable.create(e -> {
            WalletManager walletManager = (WalletManager) context.getSystemService(BrahmaContext.WALLET_SERVICE);
            e.onNext(walletManager.deleteWalletByAddress(address));
            e.onCompleted();
        });
    }
}
