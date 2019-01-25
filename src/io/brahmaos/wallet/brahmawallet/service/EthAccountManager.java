package io.brahmaos.wallet.brahmawallet.service;

import android.content.Context;

import org.bitcoinj.wallet.UnreadableWalletException;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jFactory;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.utils.Convert;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import brahmaos.app.WalletManager;
import brahmaos.content.BrahmaContext;
import brahmaos.content.WalletData;
import io.brahmaos.wallet.brahmawallet.BuildConfig;
import io.brahmaos.wallet.brahmawallet.common.BrahmaConfig;
import io.brahmaos.wallet.brahmawallet.common.BrahmaConst;
import io.brahmaos.wallet.brahmawallet.db.entity.AccountEntity;
import io.brahmaos.wallet.brahmawallet.db.entity.TokenEntity;
import io.brahmaos.wallet.brahmawallet.model.AccountAssets;
import io.brahmaos.wallet.brahmawallet.model.KyberToken;
import io.brahmaos.wallet.util.BLog;

import io.brahmaos.wallet.util.CommonUtil;
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

    /**
     *  Get the hash value of the toke list based on the ReliableTokens contract address
     */
    public Observable<List<String>> getExpectedRate(String srcAddress, String destAddress) {
        return Observable.create((Subscriber<? super List<String>> e) -> {
            try {
                BLog.i(tag(), "the srcAddress is: " + srcAddress);
                BLog.i(tag(), "the destAddress is: " + destAddress);

                WalletManager walletManager = (WalletManager) context.getSystemService(BrahmaContext.WALLET_SERVICE);
                List<String> rateResult = walletManager.getExpectedRate(getEthereumUrl(), srcAddress, destAddress);
                BLog.d(tag(), "the list rate result is:" + rateResult.toString());
                e.onNext(rateResult);
            } catch (Exception e1) {
                e1.printStackTrace();
                e.onError(e1);
            }
            e.onCompleted();
        });
    }

    /**
     * Initiate a instant exchange transaction request and
     * issue different events at different stages of the transaction,
     * for example: verifying the account, sending request
     * @return 1: verifying the account 2: sending request 10: transfer success
     */
    public Observable<String> sendInstantExchangeTransfer(AccountEntity account, KyberToken sendToken, KyberToken receiveToken,
                                                           BigDecimal sendAmount, BigDecimal maxReceiveAmount, BigInteger minConversionRate,
                                                           String password, BigDecimal gasPrice, BigInteger gasLimit) {
        return Observable.create(e -> {
            try {
                WalletManager walletManager = (WalletManager) context.getSystemService(BrahmaContext.WALLET_SERVICE);
                String hash = walletManager.exchangeToken(getEthereumUrl(), sendToken.getContractAddress(), receiveToken.getContractAddress(),
                        sendAmount.doubleValue(), account.getAddress(), CommonUtil.convertWeiFromEther(maxReceiveAmount).toString(),
                        minConversionRate.toString(), password, gasPrice.doubleValue(), gasLimit.longValue());
                BLog.i(tag(), "===> transactionHash: " + hash);
                e.onNext(hash);
            } catch (Exception e1) {
                e1.printStackTrace();
                e.onError(e1);
            }
            e.onCompleted();
        });
    }

    /**
     * Initiate contract approve request and
     * issue different events at different stages of the transaction,
     * for example: verifying the account, sending request
     * @return 1: verifying the account 2: sending request 10: transfer success
     */
    public Observable<String> sendContractApproveTransfer(AccountEntity account, KyberToken sendToken, BigDecimal sendAmount,
                                                           String password, BigDecimal gasPrice, BigInteger gasLimit) {
        return Observable.create(e -> {
            try {
                WalletManager walletManager = (WalletManager) context.getSystemService(BrahmaContext.WALLET_SERVICE);
                String hash = walletManager.approveKyberNetwork(getEthereumUrl(), sendToken.getContractAddress(),
                        sendAmount.doubleValue(), account.getAddress(), password, gasPrice.doubleValue(), gasLimit.longValue());
                e.onNext(hash);
            } catch (Exception e1) {
                e1.printStackTrace();
                e.onError(e1);
            }
            e.onCompleted();
        });
    }

    /**
     * Get contract allowance.
     * issue different events at different stages of the transaction,
     * for example: verifying the account, sending request
     * @return Uint: allowance amount
     */
    public Observable<BigInteger> getContractAllowance(AccountEntity account, KyberToken sendToken) {
        return Observable.create(e -> {
            try {
                WalletManager walletManager = (WalletManager) context.getSystemService(BrahmaContext.WALLET_SERVICE);
                String allowance = walletManager.getContractAllowance(getEthereumUrl(), account.getAddress(), sendToken.getContractAddress());
                e.onNext(new BigInteger(allowance));
            } catch (Exception e1) {
                e1.printStackTrace();
                e.onError(e1);
            }
            e.onCompleted();
        });
    }
}
