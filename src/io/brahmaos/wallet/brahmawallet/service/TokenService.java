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
import io.brahmaos.wallet.util.BLog;
import io.brahmaos.wallet.util.RxEventBus;
import rx.Completable;
import rx.Observable;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

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

                                                        TokenService.getInstance()
                                                                .loadAllTokens(allTokenEntities)
                                                                .subscribeOn(Schedulers.io())
                                                                .observeOn(AndroidSchedulers.mainThread())
                                                                .subscribe(() -> {

                                                                        },
                                                                        throwable -> {
                                                                            BLog.e(tag(), "Unable to check token", throwable);
                                                                        });
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

    public void checkTokenEntity(TokenEntity token) {
        TokenDao.getInstance().insertChosenToken(token);
        MainService.getInstance().loadChosenTokens();
    }

    public void unCheckTokenEntity(TokenEntity token) {
        TokenDao.getInstance().deleteChosenToken(token.getAddress());
        MainService.getInstance().loadChosenTokens();
    }
}
