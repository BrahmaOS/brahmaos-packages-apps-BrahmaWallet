package io.brahmaos.wallet.brahmawallet.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;
import android.view.WindowManager;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.web3j.protocol.ObjectMapperFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.brahmaos.wallet.brahmawallet.R;
import io.brahmaos.wallet.brahmawallet.api.ApiConst;
import io.brahmaos.wallet.brahmawallet.api.ApiRespResult;
import io.brahmaos.wallet.brahmawallet.api.Networks;
import io.brahmaos.wallet.brahmawallet.common.BrahmaConfig;
import io.brahmaos.wallet.brahmawallet.common.BrahmaConst;
import io.brahmaos.wallet.brahmawallet.db.entity.AllTokenEntity;
import io.brahmaos.wallet.brahmawallet.model.TokensVersionInfo;
import io.brahmaos.wallet.brahmawallet.service.MainService;
import io.brahmaos.wallet.brahmawallet.service.TokenService;
import io.brahmaos.wallet.brahmawallet.ui.base.BaseActivity;
import io.brahmaos.wallet.util.BLog;
import io.brahmaos.wallet.util.CommonUtil;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import io.rayup.sdk.RayUpApp;
import io.rayup.sdk.model.Coin;
import io.rayup.sdk.model.CoinQuote;
import io.rayup.sdk.model.EthToken;

/**
 *  Splash Page.
 */
public class SplashActivity extends BaseActivity {

    @Override
    protected String tag() {
        return SplashActivity.class.getName();
    }

    // display time length
    private static final int DISPLAY_LEN = 2000;
    // the flag get all tokens
    private boolean flagAllTokens = false;
    // End of countdown sign
    private boolean flagCountdown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_splash);
    }

    @Override
    protected void onStart() {
        super.onStart();
        List<AllTokenEntity> allTokens = TokenService.getInstance().getAllShowTokensFromDB();
        splashCountdown();
        if (allTokens != null && allTokens.size() > 0) {
            AllTokenEntity allTokenEntity = allTokens.get(0);
            if (allTokenEntity.getCode() > 0) {
                flagAllTokens = true;
                jumpToMain();
                return;
            }
        }

        TokenService.getInstance().getLatestTokenList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<List<EthToken>>() {
                    @Override
                    public void onNext(List<EthToken> coins) {
                        flagAllTokens = true;
                        jumpToMain();
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        BLog.e(tag(), "Unable to load token", e);
                        flagAllTokens = true;
                        jumpToMain();
                    }

                    @Override
                    public void onCompleted() {

                    }
                });
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    // main page
    private void splashCountdown() {
        new Handler().postDelayed(() -> {
            flagCountdown = true;
            jumpToMain();
        }, DISPLAY_LEN);
    }

    // main page
    private void jumpToMain() {
        if (flagCountdown && flagAllTokens) {
            if (BrahmaConfig.getInstance().isTouchId() && CommonUtil.isFinger(this)) {
                Intent intent = new Intent();
                intent.setClass(SplashActivity.this, FingerActivity.class);
                startActivity(intent);
                finish();
            } else {
                Intent intent = new Intent();
                intent.setClass(SplashActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        }
    }
}
