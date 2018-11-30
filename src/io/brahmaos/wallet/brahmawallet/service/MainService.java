package io.brahmaos.wallet.brahmawallet.service;

import android.arch.lifecycle.LiveData;
import android.content.Context;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDUtils;
import org.bitcoinj.wallet.DeterministicKeyChain;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.UnreadableWalletException;
import org.spongycastle.util.encoders.Hex;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Wallet;
import org.web3j.crypto.WalletFile;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.ObjectMapperFactory;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.utils.Numeric;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Array;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Bytes;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;

import brahmaos.app.WalletManager;
import brahmaos.content.BrahmaContext;
import brahmaos.content.WalletData;
import brahmaos.util.DataCryptoUtils;
import brahmaos.content.BrahmaConstants;
import android.util.Log;

import javax.crypto.BadPaddingException;

import io.brahmaos.wallet.brahmawallet.BuildConfig;
import io.brahmaos.wallet.brahmawallet.R;
import io.brahmaos.wallet.brahmawallet.WalletApp;
import io.brahmaos.wallet.brahmawallet.api.ApiConst;
import io.brahmaos.wallet.brahmawallet.api.ApiRespResult;
import io.brahmaos.wallet.brahmawallet.api.Networks;
import io.brahmaos.wallet.brahmawallet.api.Web3jNetworks;
import io.brahmaos.wallet.brahmawallet.api.Web3jRespResult;
import io.brahmaos.wallet.brahmawallet.common.BrahmaConfig;
import io.brahmaos.wallet.brahmawallet.common.BrahmaConst;
import io.brahmaos.wallet.brahmawallet.db.TokenDao;
import io.brahmaos.wallet.brahmawallet.db.entity.AccountEntity;
import io.brahmaos.wallet.brahmawallet.db.entity.AllTokenEntity;
import io.brahmaos.wallet.brahmawallet.db.entity.TokenEntity;
import io.brahmaos.wallet.brahmawallet.event.EventTypeDef;
import io.brahmaos.wallet.brahmawallet.model.AccountAssets;
import io.brahmaos.wallet.brahmawallet.model.CryptoCurrency;
import io.brahmaos.wallet.brahmawallet.model.KyberToken;
import io.brahmaos.wallet.brahmawallet.model.TokensVersionInfo;
import io.brahmaos.wallet.util.BLog;
import io.brahmaos.wallet.util.RxEventBus;
import rx.Completable;
import rx.CompletableSubscriber;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class MainService extends BaseService{
    @Override
    protected String tag() {
        return MainService.class.getName();
    }

    // singleton
    private static MainService instance = new MainService();
    public static MainService getInstance() {
        return instance;
    }

    @Override
    public boolean init(Context context) {
        super.init(context);
        BrahmaWeb3jService.getInstance().init(context);
        TransactionService.getInstance().init(context);
        EthAccountManager.getInstance().init(context);
        loadAllAccounts();
        loadChosenTokens();
        return true;
    }

    private List<CryptoCurrency> cryptoCurrencies = new ArrayList<>();
    private List<AccountAssets> accountAssetsList = new ArrayList<>();
    private List<KyberToken> kyberTokenList = new ArrayList<>();
    private AccountEntity newMnemonicAccount = new AccountEntity();
    private List<AllTokenEntity> allTokenEntityList = new ArrayList<>();
    private List<AccountEntity> allAccounts = new ArrayList<>();
    private List<TokenEntity> mChosenTokens = new ArrayList<>();

    public List<CryptoCurrency> getCryptoCurrencies() {
        return cryptoCurrencies;
    }

    public void setCryptoCurrencies(List<CryptoCurrency> cryptoCurrencies) {
        this.cryptoCurrencies = cryptoCurrencies;
    }

    public List<AccountAssets> getAccountAssetsList() {
        return accountAssetsList;
    }

    public void setAccountAssetsList(List<AccountAssets> accountAssetsList) {
        this.accountAssetsList = accountAssetsList;
    }

    public List<KyberToken> getKyberTokenList() {
        return kyberTokenList;
    }

    public void setKyberTokenList(List<KyberToken> kyberTokenList) {
        this.kyberTokenList = kyberTokenList;
    }

    public List<AllTokenEntity> getAllTokenEntityList() {
        return allTokenEntityList;
    }

    public AccountEntity getNewMnemonicAccount() {
        return newMnemonicAccount;
    }

    public void setNewMnemonicAccount(AccountEntity newMnemonicAccount) {
        this.newMnemonicAccount = newMnemonicAccount;
    }

    public void loadCryptoCurrencies(List<CryptoCurrency> currencies) {
        if (currencies != null) {
            for (CryptoCurrency currency : currencies) {
                for (CryptoCurrency localCurrency : cryptoCurrencies) {
                    if (localCurrency.getTokenAddress().toLowerCase().equals(currency.getTokenAddress().toLowerCase())) {
                        cryptoCurrencies.remove(localCurrency);
                        break;
                    }
                }
                cryptoCurrencies.add(currency);
            }
        }
    }

    public void loadAllTokens(List<AllTokenEntity> tokenEntities) {
        allTokenEntityList = tokenEntities;
    }

    /*
     * Fetch token price
     */
    public Observable<List<CryptoCurrency>> fetchCurrenciesFromNet(String symbols) {
        return Observable.create(e -> {
            Networks.getInstance().getWalletApi()
                    .getCryptoCurrencies(symbols)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<ApiRespResult>() {

                        @Override
                        public void onCompleted() {

                        }

                        @Override
                        public void onError(Throwable throwable) {
                            throwable.printStackTrace();
                            BLog.d("MainService", "fetch currency on error");
                            e.onError(throwable);
                        }

                        @Override
                        public void onNext(ApiRespResult apr) {
                            if (apr.getResult() == 0 && apr.getData().containsKey(ApiConst.PARAM_QUOTES)) {
                                ObjectMapper objectMapper = ObjectMapperFactory.getObjectMapper();
                                try {
                                    List<CryptoCurrency> currencies = objectMapper.readValue(objectMapper.writeValueAsString(apr.getData().get(ApiConst.PARAM_QUOTES)), new TypeReference<List<CryptoCurrency>>() {});
                                    loadCryptoCurrencies(currencies);
                                    e.onNext(cryptoCurrencies);
                                } catch (IOException e1) {
                                    e1.printStackTrace();
                                    e.onError(e1);
                                }
                            } else {
                                BLog.e(tag(), "onError - " + apr.getResult());
                                e.onNext(null);
                            }
                            e.onCompleted();
                        }
                    });
        });
    }

    /*
     * Get tokens version
     */
    public void getTokensLatestVersion() {
        Networks.getInstance().getWalletApi()
                .getLatestTokensVersion(ApiConst.TOKEN_TYPE_ERC20)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<ApiRespResult>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(ApiRespResult apr) {
                        if (apr != null && apr.getResult() == 0) {
                            if (apr.getData() != null
                                    && apr.getData().get(ApiConst.PARAM_VER_INFO) != null) {
                                ObjectMapper objectMapper = ObjectMapperFactory.getObjectMapper();
                                try {
                                    TokensVersionInfo newTokenVersion = objectMapper.readValue(objectMapper.writeValueAsString(apr.getData().get(ApiConst.PARAM_VER_INFO)), new TypeReference<TokensVersionInfo>() {});
                                    if (newTokenVersion.getVer()  > BrahmaConfig.getInstance().getTokenListVersion()) {
                                        Networks.getInstance().getWalletApi()
                                                .getAllTokens()
                                                .subscribeOn(Schedulers.io())
                                                .observeOn(AndroidSchedulers.mainThread())
                                                .subscribe(new Observer<List<List<Object>>>() {
                                                    @Override
                                                    public void onCompleted() {
                                                    }

                                                    @Override
                                                    public void onError(Throwable e) {
                                                        e.printStackTrace();
                                                    }

                                                    @Override
                                                    public void onNext(List<List<Object>> apiRespResult) {
                                                        BrahmaConfig.getInstance().setTokenListVersion(newTokenVersion.getVer());
                                                        BrahmaConfig.getInstance().setTokenListHash(newTokenVersion.getIpfsHash());

                                                        List<AllTokenEntity> allTokenEntities = new ArrayList<>();
                                                        // add BRM and ETH
                                                        AllTokenEntity ethToken = new AllTokenEntity(0, "Ethereum", "ETH",
                                                                "", "", BrahmaConst.DEFAULT_TOKEN_SHOW_FLAG);
                                                        AllTokenEntity brmToken = new AllTokenEntity(0, "BrahmaOS", "BRM",
                                                                "0xd7732e3783b0047aa251928960063f863ad022d8", "", BrahmaConst.DEFAULT_TOKEN_SHOW_FLAG);
                                                        allTokenEntities.add(brmToken);
                                                        allTokenEntities.add(ethToken);
                                                        for (List<Object> token : apiRespResult) {
                                                            if (!token.get(2).toString().toLowerCase().equals(BrahmaConst.BRAHMAOS_TOKEN)) {
                                                                AllTokenEntity tokenEntity = new AllTokenEntity();
                                                                tokenEntity.setAddress(token.get(0).toString());
                                                                tokenEntity.setShortName(token.get(1).toString());
                                                                tokenEntity.setName(token.get(2).toString());
                                                                HashMap avatarObj = (HashMap) token.get(3);
                                                                tokenEntity.setAvatar(avatarObj.get("128x128").toString());
                                                                if (apiRespResult.indexOf(token) < BrahmaConst.DEFAULT_TOKEN_COUNT) {
                                                                    tokenEntity.setShowFlag(BrahmaConst.DEFAULT_TOKEN_SHOW_FLAG);
                                                                } else {
                                                                    tokenEntity.setShowFlag(BrahmaConst.DEFAULT_TOKEN_HIDE_FLAG);
                                                                }
                                                                allTokenEntities.add(tokenEntity);
                                                            }

                                                        }
                                                        BLog.i(tag(), "the result:" + allTokenEntities.size());

                                                        MainService.getInstance().loadAllTokens(allTokenEntities);


                                                    }
                                                });
                                    } else {
                                        BLog.d(tag(), "this is the latest version");
                                    }
                                } catch (IOException e1) {
                                    e1.printStackTrace();
                                }
                            }
                        }
                    }
                });
    }

    /*
     * Get kyber tokens
     */
    public Observable<List<KyberToken>> getKyberTokens() {
        return Observable.create(e -> {
            Networks.getInstance().getKyperApi()
                    .getKyberPairsTokens()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<LinkedHashMap<String, Object>>() {
                        @Override
                        public void onCompleted() {
                            e.onCompleted();
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            throwable.printStackTrace();
                            e.onError(throwable);
                        }

                        @Override
                        public void onNext(LinkedHashMap<String, Object> apr) {
                            if (apr != null) {
                                BLog.i(tag(), apr.toString());
                                kyberTokenList.clear();
                                ObjectMapper objectMapper = ObjectMapperFactory.getObjectMapper();
                                for(Map.Entry<String, Object> entry: apr.entrySet()){
                                    try {
                                        KyberToken kyberToken = objectMapper.readValue(objectMapper.writeValueAsString(entry.getValue()), new TypeReference<KyberToken>() {});
                                        kyberTokenList.add(kyberToken);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                                Collections.sort(kyberTokenList);
                                e.onNext(kyberTokenList);
                            }
                        }
                    });
        });
    }

    public List<AccountEntity> getAllAccounts() {
        return allAccounts;
    }

    public AccountEntity getEthereumAccountByAddress(String address) {
        for (AccountEntity accountEntity : allAccounts) {
            if (accountEntity.getType() == BrahmaConst.ETH_ACCOUNT_TYPE &&
                    accountEntity.getAddress().toLowerCase().equals(address.toLowerCase())) {
                return accountEntity;
            }
        }
        return null;
    }

    public void loadAllAccounts() {
        allAccounts.clear();
        WalletManager mWalletManager = (WalletManager) context.getSystemService(BrahmaContext.WALLET_SERVICE);
        List<WalletData> mWalletDatas = mWalletManager.getAllWallets();
        BLog.d(tag(), mWalletDatas.toString());
        for (WalletData walletData : mWalletDatas) {
            AccountEntity accountEntity = new AccountEntity();
            accountEntity.setId(mWalletDatas.indexOf(walletData) + 1);
            accountEntity.setName(walletData.name);
            accountEntity.setAddress(walletData.address);
            accountEntity.setDefault(walletData.isDefault);
            if (walletData.keyPath.equals(BrahmaConstants.BIP_ETH_PATH)) {
                accountEntity.setType(BrahmaConst.ETH_ACCOUNT_TYPE);
            } else {
                accountEntity.setType(BrahmaConst.BTC_ACCOUNT_TYPE);
            }
            allAccounts.add(accountEntity);
        }
    }

    public List<AccountEntity> getEthereumAccounts() {
        List<AccountEntity> accountEntities = new ArrayList<>();
        if (allAccounts != null && allAccounts.size() > 0) {
            for (AccountEntity accountEntity : allAccounts) {
                if (accountEntity.getType() == BrahmaConst.ETH_ACCOUNT_TYPE) {
                    if (accountEntity.isDefault()) {
                        accountEntities.add(0, accountEntity);
                    } else {
                        accountEntities.add(accountEntity);
                    }
                }
            }
            return accountEntities;
        } else {
            return accountEntities;
        }
    }

    public List<AccountEntity> getBitcoinAccounts() {
        List<AccountEntity> accountEntities = new ArrayList<>();
        if (allAccounts != null && allAccounts.size() > 0) {
            for (AccountEntity accountEntity : allAccounts) {
                if (accountEntity.getType() == BrahmaConst.BTC_ACCOUNT_TYPE) {
                    if (accountEntity.isDefault()) {
                        accountEntities.add(0, accountEntity);
                    } else {
                        accountEntities.add(accountEntity);
                    }
                }
            }
            return accountEntities;
        } else {
            return accountEntities;
        }
    }

    public List<TokenEntity> getAllChosenTokens() {
        return mChosenTokens;
    }

    public void loadChosenTokens() {
        TokenEntity eth = new TokenEntity();
        eth.setName("Ethereum");
        eth.setAddress("");
        eth.setShortName("ETH");
        eth.setAvatar(String.valueOf(R.drawable.icon_eth));

        TokenEntity brm = new TokenEntity();
        brm.setName("BrahmaOS");
        brm.setAddress("0xd7732e3783b0047aa251928960063f863ad022d8");
        brm.setShortName("BRM");
        brm.setAvatar(String.valueOf(R.drawable.icon_brm));

        mChosenTokens.add(eth);
        mChosenTokens.add(brm);
        mChosenTokens.addAll(TokenDao.getInstance().loadChosenTokens());
    }

    /*
     * Get all the token's assets for all accounts
     */
    public void loadTotalAccountAssets() {
        if (allAccounts != null && allAccounts.size() > 0 && mChosenTokens != null && mChosenTokens.size() > 0) {
            BLog.i("view model", "start get account assets");
            // init the assets
            accountAssetsList = new ArrayList<>();
            for (AccountEntity accountEntity : allAccounts) {
                if (accountEntity.getType() == BrahmaConst.BTC_ACCOUNT_TYPE) {
                    // TODO fetch btc balance
                } else {
                    for (TokenEntity tokenEntity : mChosenTokens) {
                        if (!tokenEntity.getName().toLowerCase().equals(BrahmaConst.BITCOIN)) {
                            EthAccountManager.getInstance().getEthereumBalanceByAddress(accountEntity, tokenEntity);
                        }
                    }
                }
            }
        }
    }

    /*
     * If the token asset of the account has already exists, then replace it with the new assets.
     * When all assets has exists, post value to main page
     */
    public synchronized void checkTokenAsset(AccountAssets assets) {
        for (AccountAssets localAssets : accountAssetsList) {
            if (localAssets.getAccountEntity().getAddress().equals(assets.getAccountEntity().getAddress()) &&
                    localAssets.getTokenEntity().getAddress().equals(assets.getTokenEntity().getAddress())) {
                accountAssetsList.remove(localAssets);
                break;
            }
        }
        accountAssetsList.add(assets);
        RxEventBus.get().post(EventTypeDef.LOAD_ACCOUNT_ASSETS, true);
    }

    public Observable<String> getPrivateKeyByPassword(int userId, String password) {
        return Observable.create(e -> {
            try {
                UserManager um = (UserManager) context.getSystemService(Context.USER_SERVICE);
                String cryptoedMne = um.getUserDefaultMnemonicHex(userId);
                String mnemonics = DataCryptoUtils.aes128Decrypt(cryptoedMne, password);
                long timeSeconds = System.currentTimeMillis() / 1000;
                DeterministicSeed seed = new DeterministicSeed(mnemonics, null, "", timeSeconds);
                DeterministicKeyChain chain = DeterministicKeyChain.builder().seed(seed).build();
                List<ChildNumber> keyPath = HDUtils.parsePath("M/44H/60H/0H/0/0");
                DeterministicKey key = chain.getKeyByPath(keyPath, true);
                BigInteger privateKey = key.getPrivKey();
                if (WalletUtils.isValidPrivateKey(privateKey.toString(16))) {
                    e.onNext(privateKey.toString(16));
                } else {
                    e.onNext("");
                }
            } catch (Exception e1) {
                e1.printStackTrace();
                e.onError(e1);
            }
            e.onCompleted();
        });
    }

    public Observable<String> getKeystoreByPassword(int userId, String password) {
        return Observable.create(e -> {
            try {
                UserManager um = (UserManager) context.getSystemService(Context.USER_SERVICE);
                String cryptoedMne = um.getUserDefaultMnemonicHex(userId);
                long timeSeconds = System.currentTimeMillis() / 1000;
                String mnemonics = DataCryptoUtils.aes128Decrypt(cryptoedMne, password);
                DeterministicSeed seed = new DeterministicSeed(mnemonics, null, "", timeSeconds);
                DeterministicKeyChain chain = DeterministicKeyChain.builder().seed(seed).build();
                List<ChildNumber> keyPath = HDUtils.parsePath("M/44H/60H/0H/0/0");
                DeterministicKey key = chain.getKeyByPath(keyPath, true);
                BigInteger privateKey = key.getPrivKey();
                ECKeyPair ecKeyPair = ECKeyPair.create(privateKey);
                WalletFile walletFile = Wallet.createLight(password, ecKeyPair);
                ObjectMapper objectMapper = ObjectMapperFactory.getObjectMapper();
                String keystore = objectMapper.writeValueAsString(walletFile);
                e.onNext(keystore);
            } catch (Exception e1) {
                e1.printStackTrace();
                e.onError(e1);
            }
            e.onCompleted();
        });
    }

    public Completable deleteAccountByPassword(int userId) {
        return Completable.fromAction(() -> {

        });
    }
}
