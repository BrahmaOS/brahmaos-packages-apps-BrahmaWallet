package io.brahmaos.wallet.brahmawallet.service;

import android.content.Context;
import android.os.UserManager;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDUtils;
import org.bitcoinj.wallet.DeterministicKeyChain;
import org.bitcoinj.wallet.DeterministicSeed;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Wallet;
import org.web3j.crypto.WalletFile;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.ObjectMapperFactory;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import brahmaos.app.WalletManager;
import brahmaos.content.BrahmaConstants;
import brahmaos.content.BrahmaContext;
import brahmaos.content.WalletData;
import brahmaos.util.DataCryptoUtils;
import io.brahmaos.wallet.brahmawallet.R;
import io.brahmaos.wallet.brahmawallet.WalletApp;
import io.brahmaos.wallet.brahmawallet.api.ApiConst;
import io.brahmaos.wallet.brahmawallet.api.ApiRespResult;
import io.brahmaos.wallet.brahmawallet.api.Networks;
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
import io.brahmaos.wallet.brahmawallet.repository.DataRepository;
import io.brahmaos.wallet.util.BLog;
import io.brahmaos.wallet.util.RxEventBus;
import rx.Completable;
import rx.Observable;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import io.rayup.sdk.RayUpApp;
import io.rayup.sdk.model.Coin;
import io.rayup.sdk.model.CoinQuote;
import io.rayup.sdk.model.EthToken;

public class TokenService extends BaseService{
    @Override
    protected String tag() {
        return TokenService.class.getName();
    }

    // singleton
    private static TokenService instance = new TokenService();
    public static TokenService getInstance() {
        return instance;
    }

    @Override
    public boolean init(Context context) {
        super.init(context);
        return true;
    }

    private List<KyberToken> kyberTokenList = new ArrayList<>();

    public List<KyberToken> getKyberTokenList() {
        return kyberTokenList;
    }

    public void setKyberTokenList(List<KyberToken> kyberTokenList) {
        this.kyberTokenList = kyberTokenList;
    }

    public Completable loadAllTokens(List<AllTokenEntity> tokenEntities) {
        return Completable.fromAction(() -> {
            TokenDao.getInstance().deleteAllTokens();
            TokenDao.getInstance().insertAllTokens(tokenEntities);
        });
    }

    public List<AllTokenEntity> getAllTokensFromDB() {
        return TokenDao.getInstance().getAllTokens();
    }

    public List<AllTokenEntity> getAllShowTokensFromDB() {
        return TokenDao.getInstance().getAllTokens(BrahmaConst.DEFAULT_TOKEN_SHOW_FLAG);
    }

    public List<AllTokenEntity> queryToken(String param) {
        return TokenDao.getInstance().queryAllTokens(param);
    }

    public AllTokenEntity queryTokenByCode(int code) {
        return TokenDao.getInstance().queryAllTokenEntityByCode(code);
    }

    public TokenEntity queryChosenTokenByCode(int code) {
        return TokenDao.getInstance().queryChosenTokenByCode(code);
    }

    /*
     * Get latest erc20 tokens
     */
    public Observable<List<EthToken>> getLatestTokenList() {
        return Observable.create(e -> {
            RayUpApp app = ((WalletApp) context.getApplicationContext()).getRayUpApp();
            List<EthToken> coins = app.loadEthErc20Tokens(0, BrahmaConst.COIN_COUNT);
            BLog.d(tag(), "the size is:" + coins.size());

            List<AllTokenEntity> allTokenEntities = new ArrayList<>();
            // add BRM and ETH
            AllTokenEntity ethToken = new AllTokenEntity(0, "Ethereum", "ETH",
                    "", "", BrahmaConst.DEFAULT_TOKEN_SHOW_FLAG, BrahmaConst.COIN_CODE_ETH);
            AllTokenEntity brmToken = new AllTokenEntity(0, "BrahmaOS", "BRM",
                    "0xd7732e3783b0047aa251928960063f863ad022d8", "", BrahmaConst.DEFAULT_TOKEN_SHOW_FLAG, BrahmaConst.COIN_CODE_BRM);
            allTokenEntities.add(brmToken);
            allTokenEntities.add(ethToken);
            for (EthToken coin : coins) {
                AllTokenEntity tokenEntity = new AllTokenEntity();
                tokenEntity.setAddress(coin.getAddress());
                tokenEntity.setShortName(coin.getSymbol());
                tokenEntity.setName(coin.getName());
                tokenEntity.setCode(coin.getCoinCode());
                tokenEntity.setAvatar(coin.getLogo());
                if (coins.indexOf(coin) < BrahmaConst.DEFAULT_TOKEN_COUNT) {
                    tokenEntity.setShowFlag(BrahmaConst.DEFAULT_TOKEN_SHOW_FLAG);
                }
                if (coin.getCoinCode() != BrahmaConst.COIN_CODE_ETH &&
                        coin.getCoinCode() != BrahmaConst.COIN_CODE_BRM) {
                    allTokenEntities.add(tokenEntity);
                }

            }
            BLog.i(tag(), "the result:" + allTokenEntities.size());

            TokenService.getInstance()
                    .loadAllTokens(allTokenEntities)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(() -> {
                                e.onNext(coins);
                                e.onCompleted();
                            },
                            throwable -> {
                                BLog.e(tag(), "Unable to check token", throwable);
                                e.onError(throwable);
                                e.onCompleted();
                            });
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

    public void checkTokenEntity(TokenEntity token) {
        TokenDao.getInstance().insertChosenToken(token);
        MainService.getInstance().loadChosenTokens();
    }

    public void unCheckTokenEntity(TokenEntity token) {
        TokenDao.getInstance().deleteChosenToken(token.getAddress());
        MainService.getInstance().loadChosenTokens();
    }
}
