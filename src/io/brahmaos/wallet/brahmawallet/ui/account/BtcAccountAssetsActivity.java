package io.brahmaos.wallet.brahmawallet.ui.account;

import android.animation.ObjectAnimator;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.Utils;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.wallet.Wallet;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import io.brahmaos.wallet.brahmawallet.R;
import io.brahmaos.wallet.brahmawallet.common.BrahmaConfig;
import io.brahmaos.wallet.brahmawallet.common.BrahmaConst;
import io.brahmaos.wallet.brahmawallet.common.IntentParam;
import io.brahmaos.wallet.brahmawallet.db.entity.AccountEntity;
import io.brahmaos.wallet.brahmawallet.event.EventTypeDef;
import io.brahmaos.wallet.brahmawallet.model.BitcoinDownloadProgress;
import io.brahmaos.wallet.brahmawallet.model.CryptoCurrency;
import io.brahmaos.wallet.brahmawallet.service.BtcAccountManager;
import io.brahmaos.wallet.brahmawallet.service.ImageManager;
import io.brahmaos.wallet.brahmawallet.service.MainService;
import io.brahmaos.wallet.brahmawallet.ui.base.BaseActivity;
import io.brahmaos.wallet.brahmawallet.viewmodel.AccountViewModel;
import io.brahmaos.wallet.util.CommonUtil;
import io.brahmaos.wallet.util.RxEventBus;
import rx.Observable;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;

public class BtcAccountAssetsActivity extends BaseActivity {

    @Override
    protected String tag() {
        return BtcAccountAssetsActivity.class.getName();
    }

    // UI references.
    private RelativeLayout mLayoutAccountInfo;
    private ImageView ivAccountAvatar;
    private TextView tvAccountName;
    private TextView tvAccountAddress;
    private TextView tvAccountBalance;
    private TextView tvTotalAssets;
    private ImageView ivCurrencyUnit;
    private AppBarLayout appBarLayout;
    private CollapsingToolbarLayout collapsingToolbarLayout;
    private ImageView ivAccountBg;
    private TextView tvBtcPrice;
    private TextView tvBtcPriceUnit;
    private RelativeLayout mLayoutSyncStatus;
    private LinearLayout mLayoutReceiveBtc;
    private LinearLayout mLayoutSendBtc;
    private TextView mTvSyncTime;
    private TextView mTvSyncStatus;
    private ImageView mIvSyncStatus;
    private TextView mTvSyncBlockHeight;
    private TextView mTvSyncBlockDatetime;
    private TextView mTvPrivateKeyNum;
    private RelativeLayout mLayoutTransaction;
    private TextView mTvTransactionNum;
    private RelativeLayout mLayoutPendingTransaction;
    private TextView mTvPendingTransaction;

    private int accountId;
    private AccountEntity account;
    private AccountViewModel mViewModel;
    private WalletAppKit kit;
    private List<CryptoCurrency> cryptoCurrencies = new ArrayList<>();
    private BitcoinDownloadProgress bitcoinDownloadProgress;
    private Observable<BitcoinDownloadProgress> btcSyncStatus;
    private Observable<Boolean> btcAppkitSetup;
    private Observable<Transaction> btcTransactionStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_btc_assets);
        mLayoutAccountInfo = findViewById(R.id.layout_account_info);
        ivAccountAvatar = findViewById(R.id.iv_account_avatar);
        tvAccountName = findViewById(R.id.tv_account_name);
        tvAccountAddress = findViewById(R.id.tv_account_address);
        tvAccountBalance = findViewById(R.id.tv_account_balance);
        tvTotalAssets = findViewById(R.id.tv_total_assets);
        ivCurrencyUnit = findViewById(R.id.iv_currency_unit);
        appBarLayout = findViewById(R.id.layout_app_bar);
        collapsingToolbarLayout = findViewById(R.id.layout_collapsing_toolbar);
        ivAccountBg = findViewById(R.id.iv_account_bg);
        tvBtcPrice = findViewById(R.id.tv_btc_price);
        tvBtcPriceUnit = findViewById(R.id.tv_btc_price_unit);
        mLayoutSyncStatus = findViewById(R.id.layout_sync_status);
        mLayoutReceiveBtc = findViewById(R.id.layout_receive_btc);
        mLayoutSendBtc = findViewById(R.id.layout_send_btc);
        mTvSyncTime = findViewById(R.id.tv_sync_time);
        mTvSyncStatus = findViewById(R.id.tv_sync_status);
        mIvSyncStatus = findViewById(R.id.iv_sync_status);
        mTvSyncBlockHeight = findViewById(R.id.tv_sync_block_height);
        mTvSyncBlockDatetime = findViewById(R.id.tv_sync_block_datetime);
        mTvPrivateKeyNum = findViewById(R.id.tv_private_key_num);
        mLayoutTransaction = findViewById(R.id.layout_transaction);
        mTvTransactionNum = findViewById(R.id.tv_transactions_num);
        mLayoutPendingTransaction = findViewById(R.id.layout_pending_transaction_content);
        mTvPendingTransaction = findViewById(R.id.tv_pending_transaction_content);
        showNavBackBtn();
        accountId = getIntent().getIntExtra(IntentParam.PARAM_ACCOUNT_ID, 0);
        if (accountId <= 0) {
            finish();
        }
        if (BrahmaConfig.getInstance().getCurrencyUnit().equals(BrahmaConst.UNIT_PRICE_CNY)) {
            Glide.with(BtcAccountAssetsActivity.this)
                    .load(R.drawable.currency_cny_white)
                    .into(ivCurrencyUnit);
            tvBtcPriceUnit.setText(BrahmaConst.UNIT_PRICE_CNY);
        } else {
            Glide.with(BtcAccountAssetsActivity.this)
                    .load(R.drawable.currency_usd_white)
                    .into(ivCurrencyUnit);
            tvBtcPriceUnit.setText(BrahmaConst.UNIT_PRICE_USD);
        }
        mViewModel = ViewModelProviders.of(this).get(AccountViewModel.class);
        cryptoCurrencies = MainService.getInstance().getCryptoCurrencies();
        appBarLayout.setExpanded(true);

        // used to receive btc blocks sync progress
        btcSyncStatus = RxEventBus.get().register(EventTypeDef.BTC_ACCOUNT_SYNC, BitcoinDownloadProgress.class);
        btcSyncStatus.onBackpressureBuffer()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<BitcoinDownloadProgress>() {
                    @Override
                    public void onNext(BitcoinDownloadProgress progress) {
                        bitcoinDownloadProgress = progress;
                        if ((int)progress.getProgressPercentage() >= 100 ) {
                            bitcoinDownloadProgress.setDownloaded(true);
                        }
                        if (bitcoinDownloadProgress.isDownloaded()) {
                            ObjectAnimator.ofFloat(mLayoutSyncStatus, "translationY", -mLayoutSyncStatus.getHeight()).start();
                            // show btc account info
                            setBtcTransactionInfo();
                        } else {
                            mLayoutSyncStatus.setVisibility(View.VISIBLE);
                            String syncTime = String.format("%s %s %s", getResources().getString(R.string.from),
                                    CommonUtil.datetimeFormat(progress.getCurrentBlockDate()),
                                    getResources().getString(R.string.start_sync));
                            mTvSyncTime.setText(syncTime);

                            int progressPercent = 1;
                            if ((int) bitcoinDownloadProgress.getProgressPercentage() > progressPercent) {
                                progressPercent = (int) bitcoinDownloadProgress.getProgressPercentage();
                            }
                            mTvSyncStatus.setText(String.format(Locale.US, "%s %d%%",
                                    getResources().getString(R.string.sync), progressPercent));
                            Animation rotate = AnimationUtils.loadAnimation(BtcAccountAssetsActivity.this, R.anim.sync_rotate);
                            if (rotate != null) {
                                mIvSyncStatus.startAnimation(rotate);
                            }
                            ObjectAnimator.ofFloat(mLayoutSyncStatus, "translationY", mLayoutSyncStatus.getHeight()).start();

                            if (kit != null && kit.wallet() != null) {
                                Wallet wallet = kit.wallet();
                                mTvSyncBlockHeight.setText(String.valueOf(wallet.getLastBlockSeenHeight()));
                                if (wallet.getLastBlockSeenTime() != null) {
                                    mTvSyncBlockDatetime.setText(Utils.dateTimeFormat(wallet.getLastBlockSeenTime()));
                                }
                            }
                        }
                    }

                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        Log.i(tag(), e.toString());
                    }
                });

        btcAppkitSetup = RxEventBus.get().register(EventTypeDef.BTC_APP_KIT_INIT_SET_UP, Boolean.class);
        btcAppkitSetup.observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Boolean>() {
                    @Override
                    public void onNext(Boolean flag) {
                        if (flag) {
                            initView();
                            initAssets();
                        }
                    }

                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        Log.i(tag(), e.toString());
                    }
                });

        btcTransactionStatus = RxEventBus.get().register(EventTypeDef.BTC_TRANSACTION_CHANGE, Transaction.class);
        btcTransactionStatus.onBackpressureBuffer()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Transaction>() {
                    @Override
                    public void onNext(Transaction transaction) {
                        setBtcTransactionInfo();
                    }

                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        Log.i(tag(), e.toString());
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mViewModel.getAccountById(accountId)
                .observe(this, (AccountEntity accountEntity) -> {
                    if (accountEntity != null) {
                        account = accountEntity;
                        kit = BtcAccountManager.getInstance().getBtcWalletAppKit(account.getFilename());
                        initView();
                        initAssets();
                    } else {
                        finish();
                    }
                });
    }

    private void initView() {
        ImageManager.showAccountAvatar(BtcAccountAssetsActivity.this, ivAccountAvatar, account);
        ImageManager.showAccountBackground(BtcAccountAssetsActivity.this, ivAccountBg, account);
        tvAccountName.setText(account.getName());

        /*mLayoutAccountInfo.setOnClickListener(v -> {
            Intent intent = new Intent(this, BtcAccountDetailActivity.class);
            intent.putExtra(IntentParam.PARAM_ACCOUNT_ID, account.getId());
            startActivity(intent);
        });*/

        appBarLayout.addOnOffsetChangedListener((appBarLayout, verticalOffset) -> {
            if (Math.abs(verticalOffset) >= appBarLayout.getTotalScrollRange() / 2) {
                collapsingToolbarLayout.setTitle(account.getName());
            } else {
                collapsingToolbarLayout.setTitle("");
            }
        });

        /* mLayoutReceiveBtc.setOnClickListener(v -> {
            Intent intent = new Intent(BtcAccountAssetsActivity.this, BtcAddressQrcodeActivity.class);
            intent.putExtra(IntentParam.PARAM_ACCOUNT_ID, account.getId());
            startActivity(intent);
        });

       mLayoutSendBtc.setOnClickListener(v -> {
            Intent intent = new Intent(BtcAccountAssetsActivity.this, BtcTransferActivity.class);
            intent.putExtra(IntentParam.PARAM_ACCOUNT_INFO, account);
            startActivity(intent);
        });

        mLayoutTransaction.setOnClickListener(v -> {
            Intent intent = new Intent(BtcAccountAssetsActivity.this, BtcTransactionsActivity.class);
            intent.putExtra(IntentParam.PARAM_ACCOUNT_INFO, account);
            startActivity(intent);
        });*/
    }

    private void initAssets() {
        for (CryptoCurrency cryptoCurrency : cryptoCurrencies) {
            if (cryptoCurrency.getName().toLowerCase().equals(BrahmaConst.BITCOIN)) {
                double tokenPrice = cryptoCurrency.getPriceCny();
                if (BrahmaConfig.getInstance().getCurrencyUnit().equals(BrahmaConst.UNIT_PRICE_USD)) {
                    tokenPrice = cryptoCurrency.getPriceUsd();
                }
                tvBtcPrice.setText(String.valueOf(new BigDecimal(tokenPrice).setScale(3, BigDecimal.ROUND_HALF_UP)));
                break;
            }
        }
        BigDecimal totalAssets = BigDecimal.ZERO;
        long balance = 0;
        if ( kit != null && kit.wallet() != null) {
            setBtcTransactionInfo();
        } else {
            BtcAccountManager.getInstance().initExistsWalletAppKit(account);
            tvAccountAddress.setText(CommonUtil.generateSimpleAddress(account.getAddress()));
            tvAccountBalance.setText(String.valueOf(CommonUtil.convertUnit(BrahmaConst.BITCOIN, new BigInteger(String.valueOf(balance)))));
            tvTotalAssets.setText(String.valueOf(totalAssets.setScale(2, BigDecimal.ROUND_HALF_UP)));
        }
    }

    private void setBtcTransactionInfo() {
        if (kit != null && kit.wallet() != null) {
            BigDecimal totalAssets = BigDecimal.ZERO;
            long balance = kit.wallet().getBalance().value;
            tvAccountAddress.setText(CommonUtil.generateSimpleAddress(kit.wallet().currentReceiveAddress().toBase58()));
            if (balance > 0) {
                for (CryptoCurrency currency : cryptoCurrencies) {
                    if (currency.getName().toLowerCase().equals(BrahmaConst.BITCOIN)) {
                        double tokenPrice = currency.getPriceCny();
                        if (BrahmaConfig.getInstance().getCurrencyUnit().equals(BrahmaConst.UNIT_PRICE_USD)) {
                            tokenPrice = currency.getPriceUsd();
                        }
                        BigDecimal tokenValue = new BigDecimal(tokenPrice).multiply(CommonUtil.convertBTCFromSatoshi(new BigInteger(String.valueOf(balance))));
                        totalAssets = totalAssets.add(tokenValue);
                        break;
                    }
                }
            }
            tvAccountBalance.setText(String.valueOf(CommonUtil.convertUnit(BrahmaConst.BITCOIN, new BigInteger(String.valueOf(balance)))));
            tvTotalAssets.setText(String.valueOf(totalAssets.setScale(2, BigDecimal.ROUND_HALF_UP)));
            mTvSyncBlockHeight.setText(String.valueOf(kit.wallet().getLastBlockSeenHeight()));
            if (kit.wallet().getLastBlockSeenTime() != null) {
                mTvSyncBlockDatetime.setText(Utils.dateTimeFormat(kit.wallet().getLastBlockSeenTime()));
            }
            mTvPrivateKeyNum.setText(String.valueOf(kit.wallet().getActiveKeyChain().getIssuedExternalKeys()
                    + kit.wallet().getActiveKeyChain().getIssuedInternalKeys()));
            mTvTransactionNum.setText(String.valueOf(kit.wallet().getTransactionsByTime().size()));

            // exist pending transaction
            if (!kit.wallet().getPendingTransactions().isEmpty()) {
                mLayoutPendingTransaction.setVisibility(View.VISIBLE);
                Iterator iterator = kit.wallet().getPendingTransactions().iterator();
                if (iterator.hasNext()) {
                    Transaction transaction = (Transaction) iterator.next();
                    long txAmount = transaction.getValue(kit.wallet()).value;
                    if (txAmount > 0) {
                        mTvPendingTransaction.setText(String.format("%s %s %s", getString(R.string.prompt_receiving), CommonUtil.convertBTCFromSatoshi(txAmount).toString(), getString(R.string.account_btc)));
                    } else {
                        mTvPendingTransaction.setText(String.format("%s %s %s", getString(R.string.prompt_sending), CommonUtil.convertBTCFromSatoshi(txAmount * -1).toString(), getString(R.string.account_btc)));
                    }
                }
            } else {
                mLayoutPendingTransaction.setVisibility(View.GONE);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        RxEventBus.get().unregister(EventTypeDef.BTC_ACCOUNT_SYNC, btcSyncStatus);
        RxEventBus.get().unregister(EventTypeDef.BTC_APP_KIT_INIT_SET_UP, btcAppkitSetup);
        RxEventBus.get().unregister(EventTypeDef.BTC_TRANSACTION_CHANGE, btcTransactionStatus);
    }
}
