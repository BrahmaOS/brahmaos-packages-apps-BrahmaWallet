package io.brahmaos.wallet.brahmawallet.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import io.brahmaos.wallet.brahmawallet.R;
import io.brahmaos.wallet.brahmawallet.common.BrahmaConfig;
import io.brahmaos.wallet.brahmawallet.common.BrahmaConst;
import io.brahmaos.wallet.brahmawallet.common.IntentParam;
import io.brahmaos.wallet.brahmawallet.db.entity.AccountEntity;
import io.brahmaos.wallet.brahmawallet.db.entity.TokenEntity;
import io.brahmaos.wallet.brahmawallet.event.EventTypeDef;
import io.brahmaos.wallet.brahmawallet.model.AccountAssets;
import io.brahmaos.wallet.brahmawallet.model.BitcoinDownloadProgress;
import io.brahmaos.wallet.brahmawallet.model.CryptoCurrency;
import io.brahmaos.wallet.brahmawallet.model.VersionInfo;
import io.brahmaos.wallet.brahmawallet.service.ImageManager;
import io.brahmaos.wallet.brahmawallet.service.MainService;
import io.brahmaos.wallet.brahmawallet.service.TokenService;
import io.brahmaos.wallet.brahmawallet.service.VersionUpgradeService;
import io.brahmaos.wallet.brahmawallet.ui.account.AccountsActivity;
import io.brahmaos.wallet.brahmawallet.ui.account.CreateBtcAccountActivity;
import io.brahmaos.wallet.brahmawallet.ui.base.BaseActivity;
import io.brahmaos.wallet.brahmawallet.ui.setting.AboutActivity;
import io.brahmaos.wallet.brahmawallet.ui.setting.CelestialBodyIntroActivity;
import io.brahmaos.wallet.brahmawallet.ui.setting.HelpActivity;
import io.brahmaos.wallet.brahmawallet.ui.setting.SettingsActivity;
import io.brahmaos.wallet.brahmawallet.ui.token.TokensActivity;
import io.brahmaos.wallet.brahmawallet.ui.transfer.BtcTransferActivity;
import io.brahmaos.wallet.brahmawallet.ui.transfer.InstantExchangeActivity;
import io.brahmaos.wallet.brahmawallet.ui.transfer.EthTransferActivity;
import io.brahmaos.wallet.util.BLog;
import io.brahmaos.wallet.util.CommonUtil;
import io.brahmaos.wallet.util.PermissionUtil;
import io.brahmaos.wallet.util.RxEventBus;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.Observable;

public class MainActivity extends BaseActivity
        implements NavigationView.OnNavigationItemSelectedListener, VersionUpgradeService.INewVerNotify {
    @Override
    protected String tag() {
        return MainActivity.class.getName();
    }

    public static int REQ_CODE_TRANSFER = 10;

    private LinearLayout layoutHeader;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView tvApproEqual;
    private TextView tvTestNetwork;
    private TextView tvCurrencyUnit;
    private TextView tvTotalAssets;
    private ImageView ivAssetsVisible;
    private TextView tvTokenCategories;
    private RecyclerView recyclerViewAssets;
    private DrawerLayout drawer;
    private NavigationView navigationView;

    private List<AccountEntity> cacheAccounts = new ArrayList<>();
    private List<TokenEntity> cacheTokens = new ArrayList<>();
    private List<AccountAssets> cacheAssets = new ArrayList<>();
    private List<CryptoCurrency> cacheCryptoCurrencies = new ArrayList<>();
    private VersionInfo newVersionInfo;

    private BitcoinDownloadProgress bitcoinDownloadProgress;
    private Observable<BitcoinDownloadProgress> btcSyncStatus;
    private Observable<Boolean> accountAssetsCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        BLog.i(tag(), "MainActivity onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drawer);
        viewBind();

        // get all account assets callback
        accountAssetsCallback = RxEventBus.get().register(EventTypeDef.LOAD_ACCOUNT_ASSETS, Boolean.class);
        accountAssetsCallback.onBackpressureBuffer()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Boolean>() {
                    @Override
                    public void onNext(Boolean flag) {
                        cacheAssets = MainService.getInstance().getAccountAssetsList();
                        showAssetsCurrency();
                    }

                    @Override
                    public void onCompleted() {
                        cacheAssets = MainService.getInstance().getAccountAssetsList();
                        showAssetsCurrency();
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        Log.i(tag(), e.toString());
                        cacheAssets = MainService.getInstance().getAccountAssetsList();
                        showAssetsCurrency();
                    }
                });

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
                            MainService.getInstance().getBtcAssets();
                        } else {
                            recyclerViewAssets.getAdapter().notifyDataSetChanged();
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

        VersionUpgradeService.getInstance().checkVersion(this, true, this);
        TokenService.getInstance().getTokensLatestVersion();

        initView();
        initData();
    }

    private void viewBind() {
        layoutHeader = findViewById(R.id.layout_header);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        tvApproEqual = findViewById(R.id.tv_appro_equal);
        tvTestNetwork = findViewById(R.id.tv_test_network);
        tvCurrencyUnit = findViewById(R.id.tv_money_unit);
        tvTotalAssets = findViewById(R.id.tv_total_assets);
        ivAssetsVisible = findViewById(R.id.iv_assets_visibility);
        tvTokenCategories = findViewById(R.id.tv_assets_categories_num);
        recyclerViewAssets = findViewById(R.id.assets_recycler);
        drawer = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
    }

    private void initView() {
        DisplayMetrics display = this.getResources().getDisplayMetrics();

        int statusBarHeight = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = getResources().getDimensionPixelSize(resourceId);
        }
        int toolbarHeight = getResources().getDimensionPixelSize(R.dimen.height_toolbar);

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) layoutHeader.getLayoutParams();
        params.width = display.widthPixels;
        params.height = ((int) (display.heightPixels * BrahmaConst.MAIN_PAGE_HEADER_RATIO) - statusBarHeight - toolbarHeight);
        layoutHeader.setLayoutParams(params);

        swipeRefreshLayout.setColorSchemeResources(R.color.master);
        swipeRefreshLayout.setRefreshing(true);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);

        swipeRefreshLayout.setOnRefreshListener(() -> {
            // get the latest assets
            getAllAssets();
            // get Currencies
            getCryptoCurrents();
        });
        ImageView ivCelestialBody = navigationView.getHeaderView(0).findViewById(R.id.iv_celestial_body);
        ivCelestialBody.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CelestialBodyIntroActivity.class);
            startActivity(intent);
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerViewAssets.setLayoutManager(layoutManager);
        recyclerViewAssets.setAdapter(new AssetsRecyclerAdapter());

        // Solve the sliding lag problem
        recyclerViewAssets.setHasFixedSize(true);
        recyclerViewAssets.setNestedScrollingEnabled(false);

        tvCurrencyUnit.setText(BrahmaConfig.getInstance().getCurrencyUnit());

        ImageView ivChooseToken = findViewById(R.id.iv_choose_token);
        ivChooseToken.setOnClickListener(v -> {
            Intent intent = new Intent(this, TokensActivity.class);
            startActivity(intent);
        });

        ivAssetsVisible.setOnClickListener(v -> {
            if (BrahmaConfig.getInstance().isAssetsVisible()) {
                BrahmaConfig.getInstance().setAssetsVisible(false);
            } else {
                BrahmaConfig.getInstance().setAssetsVisible(true);
            }
            showAssetsCurrency();
        });

        changeNetwork();
    }

    private void changeNetwork() {
        String networkUrl = BrahmaConfig.getInstance().getNetworkUrl();
        String networkName = CommonUtil.generateNetworkName(networkUrl);
        if (networkName.equals("Mainnet") || networkName.length() < 1) {
            tvTestNetwork.setVisibility(View.GONE);
        } else {
            tvTestNetwork.setVisibility(View.VISIBLE);
            tvTestNetwork.setText(networkName);
        }
    }

    private void initData() {
        cacheTokens.addAll(MainService.getInstance().getAllChosenTokens());
        cacheAccounts = MainService.getInstance().getAllAccounts();
        recyclerViewAssets.getAdapter().notifyDataSetChanged();
        Log.d(tag(), "the accounts is:" + cacheAccounts.toString());
        // fetch crypto currents
        getCryptoCurrents();
        // fetch account token amount
        getAllAssets();
    }

    private void getAllAssets() {
        MainService.getInstance().loadTotalAccountAssets();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        BLog.e(tag(), "onNewIntent");
        boolean changeNetworkFlag = intent.getBooleanExtra(IntentParam.FLAG_CHANGE_NETWORK, false);
        boolean changeLanguageFlag = intent.getBooleanExtra(IntentParam.FLAG_CHANGE_LANGUAGE, false);
        boolean changeCurrencyUnit = intent.getBooleanExtra(IntentParam.FLAG_CHANGE_CURRENCY_UNIT, false);
        // change network type
        if (changeNetworkFlag) {
            MainService.getInstance().setAccountAssetsList(new ArrayList<>());
            swipeRefreshLayout.setRefreshing(true);
            getAllAssets();
            changeNetwork();
        }
        // change language; if change language, then recreate the activity to reload the resource.
        if (changeLanguageFlag) {
            this.recreate();
        }
        // change currency unit
        if (changeCurrencyUnit) {
            tvCurrencyUnit.setText(BrahmaConfig.getInstance().getCurrencyUnit());
            showAssetsCurrency();
            recyclerViewAssets.getAdapter().notifyDataSetChanged();
        }
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!checkChosenToken()) {
            cacheTokens.clear();
            cacheTokens.addAll(MainService.getInstance().getAllChosenTokens());

            swipeRefreshLayout.setRefreshing(true);
            // get the latest assets
            getAllAssets();
            // get Currencies
            getCryptoCurrents();
        }
    }

    // Judge the change of chosen token
    private boolean checkChosenToken() {
        List<TokenEntity> chosenTokenEntity = MainService.getInstance().getAllChosenTokens();
        if (cacheTokens.size() != chosenTokenEntity.size()) {
            return false;
        } else {
            for (TokenEntity token : chosenTokenEntity) {
                if (!cacheTokens.contains(token)) {
                    return false;
                }
            }
            return true;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.fragment_wallet, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //noinspection SimplifiableIfStatement
        if (item.getItemId() == R.id.menu_instant_exchange) {
            if (cacheAccounts.size() > 0) {
                Intent intent = new Intent(this, InstantExchangeActivity.class);
                startActivity(intent);
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_accounts) {
            Intent intent = new Intent(this, AccountsActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_help) {
            Intent intent = new Intent(this, HelpActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_info) {
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
        }

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void alreadyLatest() {

    }

    @Override
    public void confirmUpdate(VersionInfo newVer) {
        newVersionInfo = newVer;
    }

    @Override
    public void cancelUpdate(VersionInfo newVer) {

    }

    @Override
    public void handleExternalStoragePermission() {
        VersionUpgradeService.getInstance().downloadApkFile(this, newVersionInfo, this);
    }

    private void getCryptoCurrents() {
        String symbols;
        if (cacheTokens != null && cacheTokens.size() > 0) {
            StringBuilder stringBuilder = new StringBuilder();
            for (TokenEntity token : cacheTokens) {
                stringBuilder.append(token.getShortName()).append(",");
            }
            symbols = stringBuilder.toString();
        } else {
            symbols = "ETH,BRM";
        }
        MainService.getInstance().fetchCurrenciesFromNet(symbols)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<List<CryptoCurrency>>() {

                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        throwable.printStackTrace();
                        cacheCryptoCurrencies = MainService.getInstance().getCryptoCurrencies();
                        showAssetsCurrency();
                    }

                    @Override
                    public void onNext(List<CryptoCurrency> apr) {
                        cacheCryptoCurrencies = MainService.getInstance().getCryptoCurrencies();
                        showAssetsCurrency();
                    }
                });
    }

    /**
     * Display the number of tokens and the corresponding legal currency value
     */
    private void showAssetsCurrency() {
        int ethAccountCount = 0;
        int btcAccountCount = 0;
        BLog.d(tag(), "the accounts is: " + cacheAccounts.toString());
        for (AccountEntity account : cacheAccounts) {
            if (account.getType() == BrahmaConst.BTC_ACCOUNT_TYPE) {
                btcAccountCount++;
            } else if (account.getType() == BrahmaConst.ETH_ACCOUNT_TYPE) {
                ethAccountCount++;
            }
        }
        int ethTokenCount = cacheTokens.size() - 1;
        int totalCount = ethAccountCount * ethTokenCount + btcAccountCount;
        if (cacheAssets.size() == totalCount) {
            recyclerViewAssets.getAdapter().notifyDataSetChanged();
            BLog.d(tag(), "the cache assets is: " + cacheAssets.toString());
            BigDecimal totalValue = BigDecimal.ZERO;
            BLog.d(tag(), cacheAssets.toString());
            for (AccountAssets accountAssets : cacheAssets) {
                if (accountAssets.getBalance().compareTo(BigInteger.ZERO) > 0 && cacheCryptoCurrencies != null) {
                    for (CryptoCurrency cryptoCurrency : cacheCryptoCurrencies) {
                        if (CommonUtil.cryptoCurrencyCompareToken(cryptoCurrency, accountAssets.getTokenEntity())) {
                            double tokenPrice = cryptoCurrency.getPriceCny();
                            if (BrahmaConfig.getInstance().getCurrencyUnit().equals(BrahmaConst.UNIT_PRICE_USD)) {
                                tokenPrice = cryptoCurrency.getPriceUsd();
                            }
                            BigDecimal value = new BigDecimal(tokenPrice)
                                    .multiply(CommonUtil.convertUnit(accountAssets.getTokenEntity().getName(),
                                            accountAssets.getBalance()));
                            BLog.d(tag(), accountAssets.getAccountEntity().getName() + "--" +
                                    accountAssets.getTokenEntity().getName() + "'s value is :" +
                                    value.toString());
                            totalValue = totalValue.add(value);
                            break;
                        }
                    }
                }
            }
            swipeRefreshLayout.setRefreshing(false);
            if (BrahmaConfig.getInstance().isAssetsVisible()) {
                tvTotalAssets.setText(String.valueOf(totalValue.setScale(2, BigDecimal.ROUND_HALF_UP)));
                tvApproEqual.setText(R.string.asymptotic);
                ivAssetsVisible.setImageResource(R.drawable.ic_open_eye);
            } else {
                tvTotalAssets.setText("******");
                tvApproEqual.setText("");
                ivAssetsVisible.setImageResource(R.drawable.ic_close_eye);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQ_CODE_TRANSFER) {
            if (resultCode == RESULT_OK) {
                BLog.i(tag(), "transfer success");
                // get the latest assets
                getAllAssets();
                swipeRefreshLayout.setRefreshing(true);
            }
        } else if (requestCode == PermissionUtil.CODE_EXTERNAL_STORAGE) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                handleExternalStoragePermission();
            } else {
                PermissionUtil.openSettingActivity(this, getString(R.string.tip_external_storage_permission));
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        RxEventBus.get().unregister(EventTypeDef.LOAD_ACCOUNT_ASSETS, accountAssetsCallback);
        RxEventBus.get().unregister(EventTypeDef.BTC_ACCOUNT_SYNC, btcSyncStatus);
    }

    /**
     * list item account
     */
    private class AssetsRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View rootView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_assets, parent, false);
            return new AssetsRecyclerAdapter.ItemViewHolder(rootView);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof AssetsRecyclerAdapter.ItemViewHolder) {
                AssetsRecyclerAdapter.ItemViewHolder itemViewHolder = (AssetsRecyclerAdapter.ItemViewHolder) holder;
                TokenEntity tokenEntity = cacheTokens.get(position);
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
                if (tokenEntity.getName().toLowerCase().equals(BrahmaConst.BITCOIN)) {
                    List<AccountEntity> btcAccounts = new ArrayList<>();
                    for (AccountEntity accountEntity : cacheAccounts) {
                        if (accountEntity.getType() == BrahmaConst.BTC_ACCOUNT_TYPE) {
                            btcAccounts.add(accountEntity);
                        }
                    }
                    if (btcAccounts.size() > 0) {
                        Intent intent = new Intent(MainActivity.this, BtcTransferActivity.class);
                        intent.putExtra(IntentParam.PARAM_TOKEN_INFO, tokenEntity);
                        startActivityForResult(intent, REQ_CODE_TRANSFER);
                    } else {
                        AlertDialog passwordDialog = new AlertDialog.Builder(MainActivity.this)
                                .setMessage(R.string.tip_no_btc_account)
                                .setCancelable(true)
                                .setPositiveButton(R.string.create, (dialog, which) -> {
                                    dialog.cancel();
                                    Intent intent = new Intent(MainActivity.this, CreateBtcAccountActivity.class);
                                    startActivity(intent);
                                })
                                .create();
                        passwordDialog.show();
                    }

                } else {
                    Intent intent = new Intent(MainActivity.this, EthTransferActivity.class);
                    intent.putExtra(IntentParam.PARAM_TOKEN_INFO, tokenEntity);
                    startActivityForResult(intent, REQ_CODE_TRANSFER);
                }
            });
            holder.tvTokenName.setText(tokenEntity.getShortName());
            holder.tvTokenFullName.setText(tokenEntity.getName());
            holder.tvTokenPrice.setText("0");
            ImageManager.showTokenIcon(MainActivity.this, holder.ivTokenIcon,
                    tokenEntity.getName(), tokenEntity.getAddress());
            BigInteger tokenCount = BigInteger.ZERO;

            for (AccountAssets accountAssets : cacheAssets) {
                if (accountAssets.getTokenEntity().getAddress().toLowerCase().equals(tokenEntity.getAddress().toLowerCase())) {
                    tokenCount = tokenCount.add(accountAssets.getBalance());
                }
            }
            BigDecimal tokenValue = BigDecimal.ZERO;
            if (cacheCryptoCurrencies != null && cacheCryptoCurrencies.size() > 0) {
                for (CryptoCurrency cryptoCurrency : cacheCryptoCurrencies) {
                    if (CommonUtil.cryptoCurrencyCompareToken(cryptoCurrency, tokenEntity)) {
                        double tokenPrice = cryptoCurrency.getPriceUsd();
                        if (BrahmaConfig.getInstance().getCurrencyUnit().equals(BrahmaConst.UNIT_PRICE_CNY)) {
                            tokenPrice = cryptoCurrency.getPriceCny();
                            Glide.with(MainActivity.this)
                                    .load(R.drawable.currency_cny)
                                    .into(holder.ivTokenPrice);
                            Glide.with(MainActivity.this)
                                    .load(R.drawable.currency_cny)
                                    .into(holder.ivTokenAssets);
                        } else {
                            Glide.with(MainActivity.this)
                                    .load(R.drawable.currency_usd)
                                    .into(holder.ivTokenPrice);
                            Glide.with(MainActivity.this)
                                    .load(R.drawable.currency_usd)
                                    .into(holder.ivTokenAssets);
                        }
                        tokenValue = CommonUtil.convertUnit(tokenEntity.getName(), tokenCount).multiply(new BigDecimal(tokenPrice));
                        holder.tvTokenPrice.setText(String.valueOf(new BigDecimal(tokenPrice).setScale(3, BigDecimal.ROUND_HALF_UP)));
                        break;
                    }
                }
            }
            if (BrahmaConfig.getInstance().isAssetsVisible()) {
                holder.tvTokenApproEqual.setText(R.string.asymptotic);
                holder.tvTokenAccount.setText(String.valueOf(CommonUtil.convertUnit(tokenEntity.getName(), tokenCount)));
                holder.tvTokenAssetsCount.setText(String.valueOf(tokenValue.setScale(2, BigDecimal.ROUND_HALF_UP)));
            } else {
                holder.tvTokenApproEqual.setText("");
                holder.tvTokenAccount.setText("****");
                holder.tvTokenAssetsCount.setText("********");
            }
            if (tokenEntity.getName().toLowerCase().equals(BrahmaConst.BITCOIN)) {
                if (bitcoinDownloadProgress != null) {
                    if (bitcoinDownloadProgress.isDownloaded()) {
                        holder.ivBtcSync.setVisibility(View.GONE);
                        holder.tvBtcSyncStatus.setVisibility(View.GONE);
                        holder.tvTokenAccount.setVisibility(View.VISIBLE);
                    } else {
                        holder.ivBtcSync.setVisibility(View.VISIBLE);
                        holder.tvBtcSyncStatus.setVisibility(View.VISIBLE);
                        holder.tvTokenAccount.setVisibility(View.GONE);
                        Animation rotate = AnimationUtils.loadAnimation(MainActivity.this, R.anim.sync_rotate);
                        if (rotate != null) {
                            holder.ivBtcSync.startAnimation(rotate);
                        }
                        int progress = 1;
                        if ((int) bitcoinDownloadProgress.getProgressPercentage() > progress) {
                            progress = (int) bitcoinDownloadProgress.getProgressPercentage();
                        }
                        holder.tvBtcSyncStatus.setText(String.format(Locale.US, "%s %d%%",
                                getResources().getString(R.string.sync), progress));
                    }
                } else {
                    holder.ivBtcSync.setVisibility(View.GONE);
                    holder.tvBtcSyncStatus.setVisibility(View.GONE);
                    holder.tvTokenAccount.setVisibility(View.VISIBLE);
                }
            } else {
                holder.ivBtcSync.setVisibility(View.GONE);
                holder.tvBtcSyncStatus.setVisibility(View.GONE);
                holder.tvTokenAccount.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public int getItemCount() {
            return cacheTokens.size();
        }

        class ItemViewHolder extends RecyclerView.ViewHolder {

            LinearLayout layoutAssets;
            ImageView ivTokenIcon;
            TextView tvTokenName;
            TextView tvTokenFullName;
            TextView tvTokenPrice;
            TextView tvTokenAccount;
            TextView tvTokenApproEqual;
            TextView tvTokenAssetsCount;
            ImageView ivTokenPrice;
            ImageView ivTokenAssets;
            TextView tvBtcSyncStatus;
            ImageView ivBtcSync;

            ItemViewHolder(View itemView) {
                super(itemView);
                layoutAssets = itemView.findViewById(R.id.layout_assets);
                ivTokenIcon = itemView.findViewById(R.id.iv_token_icon);
                tvTokenName = itemView.findViewById(R.id.tv_token_name);
                tvTokenAccount = itemView.findViewById(R.id.tv_token_count);
                tvTokenApproEqual = itemView.findViewById(R.id.tv_token_appro_equal);
                tvTokenAssetsCount = itemView.findViewById(R.id.tv_token_assets_count);
                tvTokenFullName = itemView.findViewById(R.id.tv_token_full_name);
                tvTokenPrice = itemView.findViewById(R.id.tv_token_price);
                ivTokenPrice = itemView.findViewById(R.id.iv_currency_unit);
                ivTokenAssets = itemView.findViewById(R.id.iv_currency_amount);
                ivBtcSync = itemView.findViewById(R.id.iv_btc_sync);
                tvBtcSyncStatus = itemView.findViewById(R.id.tv_btc_sync);
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (drawer.isDrawerOpen(GravityCompat.START)) {
                drawer.closeDrawer(GravityCompat.START);
            } else {
                finish();
                timerExit.schedule(timerTask, 500);
            }
        }
        return false;
    }
    private Timer timerExit = new Timer();
    private TimerTask timerTask = new TimerTask() {
        @Override
        public void run() {
            System.exit(0);
        }
    };
}
