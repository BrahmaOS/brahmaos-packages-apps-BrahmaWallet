package io.brahmaos.wallet.brahmawallet.ui.account;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import io.brahmaos.wallet.brahmawallet.R;
import io.brahmaos.wallet.brahmawallet.common.BrahmaConfig;
import io.brahmaos.wallet.brahmawallet.common.BrahmaConst;
import io.brahmaos.wallet.brahmawallet.common.IntentParam;
import io.brahmaos.wallet.brahmawallet.db.entity.AccountEntity;
import io.brahmaos.wallet.brahmawallet.db.entity.TokenEntity;
import io.brahmaos.wallet.brahmawallet.event.EventTypeDef;
import io.brahmaos.wallet.brahmawallet.model.AccountAssets;
import io.brahmaos.wallet.brahmawallet.model.CryptoCurrency;
import io.brahmaos.wallet.brahmawallet.service.ImageManager;
import io.brahmaos.wallet.brahmawallet.service.MainService;
import io.brahmaos.wallet.brahmawallet.ui.base.BaseActivity;
import io.brahmaos.wallet.brahmawallet.ui.transaction.EthTransactionsActivity;
import io.brahmaos.wallet.brahmawallet.ui.transaction.TransactionsActivity;
import io.brahmaos.wallet.brahmawallet.view.CustomProgressDialog;
import io.brahmaos.wallet.util.CommonUtil;

import io.brahmaos.wallet.util.RxEventBus;
import rx.Completable;
import rx.CompletableSubscriber;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class EthAccountAssetsActivity extends BaseActivity {

    @Override
    protected String tag() {
        return EthAccountAssetsActivity.class.getName();
    }

    public static final int REQ_CODE_TRANSFER = 10;

    // UI references.
    private RelativeLayout mLayoutAccountInfo;
    private RecyclerView recyclerViewAssets;
    private ImageView ivAccountAvatar;
    private TextView tvAccountName;
    private TextView tvAccountAddress;
    private TextView tvTotalAssets;
    private TextView tvCurrencyUnit;
    private AppBarLayout appBarLayout;
    private CollapsingToolbarLayout collapsingToolbarLayout;
    private ImageView ivAccountBg;

    private String accountAddress;
    private AccountEntity account;
    private List<TokenEntity> tokenEntities = new ArrayList<>();
    private List<AccountAssets> accountAssetsList = new ArrayList<>();
    private List<CryptoCurrency> cryptoCurrencies = new ArrayList<>();
    private CustomProgressDialog customProgressDialog;

    private Observable<Boolean> mUpdateAccountCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_eth_assets);
        showNavBackBtn();
        initView();
        accountAddress = getIntent().getStringExtra(IntentParam.PARAM_ACCOUNT_ADDRESS);
        if (accountAddress == null || accountAddress.length() <= 1) {
            finish();
        }
        String currencyUnit = BrahmaConfig.getInstance().getCurrencyUnit();
        if (currencyUnit != null) {
            tvCurrencyUnit.setText(currencyUnit);
        } else {
            tvCurrencyUnit.setText(BrahmaConst.UNIT_PRICE_CNY);
        }
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerViewAssets.setLayoutManager(layoutManager);
        recyclerViewAssets.setAdapter(new AssetsRecyclerAdapter());

        // Solve the sliding lag problem
        recyclerViewAssets.setHasFixedSize(true);
        recyclerViewAssets.setNestedScrollingEnabled(false);
        cryptoCurrencies = MainService.getInstance().getCryptoCurrencies();
        accountAssetsList = MainService.getInstance().getAccountAssetsList();

        appBarLayout.setExpanded(true);

        // used to update account info
        mUpdateAccountCallback = RxEventBus.get().register(EventTypeDef.CHANGE_ETH_ACCOUNT, Boolean.class);
        mUpdateAccountCallback.onBackpressureBuffer()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Boolean>() {
                    @Override
                    public void onNext(Boolean flag) {
                        account = MainService.getInstance().getEthereumAccountByAddress(accountAddress);
                        initData();
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

    private void initView() {
        mLayoutAccountInfo = findViewById(R.id.layout_account_info);
        recyclerViewAssets = findViewById(R.id.assets_recycler);
        ivAccountAvatar = findViewById(R.id.iv_account_avatar);
        tvAccountName = findViewById(R.id.tv_account_name);
        tvAccountAddress = findViewById(R.id.tv_account_address);
        tvTotalAssets = findViewById(R.id.tv_total_assets);
        tvCurrencyUnit = findViewById(R.id.tv_currency_unit);
        appBarLayout = findViewById(R.id.layout_app_bar);
        collapsingToolbarLayout = findViewById(R.id.layout_collapsing_toolbar);
        ivAccountBg = findViewById(R.id.iv_account_bg);
    }

    @Override
    protected void onStart() {
        super.onStart();
        account = MainService.getInstance().getEthereumAccountByAddress(accountAddress);
        initData();
        initAssets();
        tokenEntities = MainService.getInstance().getAllChosenTokens();;
        recyclerViewAssets.getAdapter().notifyDataSetChanged();
    }

    private void initData() {
        ImageManager.showAccountAvatar(EthAccountAssetsActivity.this, ivAccountAvatar, account);
        ImageManager.showAccountBackground(EthAccountAssetsActivity.this, ivAccountBg, account);
        tvAccountName.setText(account.getName());
        tvAccountAddress.setText(CommonUtil.generateSimpleAddress(account.getAddress()));

        mLayoutAccountInfo.setOnClickListener(v -> {
            Intent intent = new Intent(this, EthAccountDetailActivity.class);
            intent.putExtra(IntentParam.PARAM_ACCOUNT_ADDRESS, account.getAddress());
            startActivity(intent);
        });

        appBarLayout.addOnOffsetChangedListener((appBarLayout, verticalOffset) -> {
            if (Math.abs(verticalOffset) >= appBarLayout.getTotalScrollRange() / 2) {
                collapsingToolbarLayout.setTitle(account.getName());
            } else {
                collapsingToolbarLayout.setTitle("");
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_qrcode, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_qrcode) {
            if (account != null) {
                Intent intent = new Intent(EthAccountAssetsActivity.this, AddressQrcodeActivity.class);
                intent.putExtra(IntentParam.PARAM_ACCOUNT_INFO, account);
                startActivity(intent);
            }
            return true;
        } else if (id == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void initAssets() {
        if (accountAssetsList == null) {
            accountAssetsList = new ArrayList<>();
        }
        BigDecimal totalValue = BigDecimal.ZERO;
        for (AccountAssets assets : accountAssetsList) {
            if (assets.getAccountEntity().getAddress().equals(account.getAddress())) {
                if (assets.getBalance().compareTo(BigInteger.ZERO) > 0) {
                    for (CryptoCurrency cryptoCurrency : cryptoCurrencies) {
                        if (CommonUtil.cryptoCurrencyCompareToken(cryptoCurrency, assets.getTokenEntity())) {
                            double tokenPrice = cryptoCurrency.getPriceCny();
                            if (BrahmaConfig.getInstance().getCurrencyUnit().equals(BrahmaConst.UNIT_PRICE_USD)) {
                                tokenPrice = cryptoCurrency.getPriceUsd();
                            }
                            BigDecimal value = new BigDecimal(tokenPrice)
                                    .multiply(CommonUtil.getAccountFromWei(assets.getBalance()));
                            totalValue = totalValue.add(value);
                            break;
                        }
                    }
                }
            }
        }
        tvTotalAssets.setText(String.valueOf(totalValue.setScale(2, BigDecimal.ROUND_HALF_UP)));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        RxEventBus.get().unregister(EventTypeDef.CHANGE_ETH_ACCOUNT, mUpdateAccountCallback);
    }

    /**
     * list item currency
     */
    private class AssetsRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View rootView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_assets, parent, false);
            rootView.setOnClickListener(v -> {

            });
            return new AssetsRecyclerAdapter.ItemViewHolder(rootView);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof AssetsRecyclerAdapter.ItemViewHolder) {
                AssetsRecyclerAdapter.ItemViewHolder itemViewHolder = (AssetsRecyclerAdapter.ItemViewHolder) holder;
                TokenEntity tokenEntity = tokenEntities.get(position);
                setAssetsData(itemViewHolder, tokenEntity);
            }
        }

        /**
         * set assets view
         */
        private void setAssetsData(AssetsRecyclerAdapter.ItemViewHolder holder, final TokenEntity tokenEntity) {
            if (tokenEntity == null) {
                return;
            }

            holder.layoutAssets.setOnClickListener(v -> {
                if (tokenEntity.getName().toLowerCase().equals(BrahmaConst.ETHEREUM)) {
                    Intent intent = new Intent(EthAccountAssetsActivity.this, EthTransactionsActivity.class);
                    intent.putExtra(IntentParam.PARAM_ACCOUNT_INFO, account);
                    intent.putExtra(IntentParam.PARAM_TOKEN_INFO, tokenEntity);
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(EthAccountAssetsActivity.this, TransactionsActivity.class);
                    intent.putExtra(IntentParam.PARAM_ACCOUNT_INFO, account);
                    intent.putExtra(IntentParam.PARAM_TOKEN_INFO, tokenEntity);
                    startActivity(intent);
                }
            });

            holder.tvTokenName.setText(tokenEntity.getShortName());
            holder.tvTokenFullName.setText(tokenEntity.getName());
            ImageManager.showTokenIcon(EthAccountAssetsActivity.this, holder.ivTokenIcon,
                    tokenEntity.getName(), tokenEntity.getAddress());
            BigInteger tokenCount = BigInteger.ZERO;
            for (AccountAssets accountAssets : accountAssetsList) {
                if (accountAssets.getTokenEntity().getAddress().equals(tokenEntity.getAddress()) &&
                        accountAssets.getAccountEntity().getAddress().equals(account.getAddress())) {
                    tokenCount = tokenCount.add(accountAssets.getBalance());
                }
            }
            holder.tvTokenAccount.setText(String.valueOf(CommonUtil.getAccountFromWei(tokenCount)));
            BigDecimal tokenValue = BigDecimal.ZERO;
            for (CryptoCurrency cryptoCurrency : cryptoCurrencies) {
                if (CommonUtil.cryptoCurrencyCompareToken(cryptoCurrency, tokenEntity)) {
                    double tokenPrice = cryptoCurrency.getPriceUsd();
                    if (BrahmaConfig.getInstance().getCurrencyUnit().equals(BrahmaConst.UNIT_PRICE_CNY)) {
                        tokenPrice = cryptoCurrency.getPriceCny();
                        Glide.with(EthAccountAssetsActivity.this)
                                .load(R.drawable.currency_cny)
                                .into(holder.ivTokenPrice);
                        Glide.with(EthAccountAssetsActivity.this)
                                .load(R.drawable.currency_cny)
                                .into(holder.ivTokenAssets);
                    } else {
                        Glide.with(EthAccountAssetsActivity.this)
                                .load(R.drawable.currency_usd)
                                .into(holder.ivTokenPrice);
                        Glide.with(EthAccountAssetsActivity.this)
                                .load(R.drawable.currency_usd)
                                .into(holder.ivTokenAssets);
                    }
                    tokenValue = CommonUtil.getAccountFromWei(tokenCount).multiply(new BigDecimal(tokenPrice));
                    holder.tvTokenPrice.setText(String.valueOf(new BigDecimal(tokenPrice).setScale(3, BigDecimal.ROUND_HALF_UP)));
                    break;
                }
            }
            holder.tvTokenAssetsCount.setText(String.valueOf(tokenValue.setScale(2, BigDecimal.ROUND_HALF_UP)));
        }

        @Override
        public int getItemCount() {
            return tokenEntities.size();
        }

        class ItemViewHolder extends RecyclerView.ViewHolder {

            LinearLayout layoutAssets;
            ImageView ivTokenIcon;
            TextView tvTokenName;
            TextView tvTokenFullName;
            TextView tvTokenPrice;
            TextView tvTokenAccount;
            TextView tvTokenAssetsCount;
            ImageView ivTokenPrice;
            ImageView ivTokenAssets;

            ItemViewHolder(View itemView) {
                super(itemView);
                layoutAssets = itemView.findViewById(R.id.layout_assets);
                ivTokenIcon = itemView.findViewById(R.id.iv_token_icon);
                tvTokenName = itemView.findViewById(R.id.tv_token_name);
                tvTokenAccount = itemView.findViewById(R.id.tv_token_count);
                tvTokenAssetsCount = itemView.findViewById(R.id.tv_token_assets_count);
                tvTokenFullName = itemView.findViewById(R.id.tv_token_full_name);
                tvTokenPrice = itemView.findViewById(R.id.tv_token_price);
                ivTokenPrice = itemView.findViewById(R.id.iv_currency_unit);
                ivTokenAssets = itemView.findViewById(R.id.iv_currency_amount);
            }
        }
    }

}
