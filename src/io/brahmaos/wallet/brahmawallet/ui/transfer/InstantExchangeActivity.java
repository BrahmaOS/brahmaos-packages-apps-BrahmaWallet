package io.brahmaos.wallet.brahmawallet.ui.transfer;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.CipherException;
import org.web3j.utils.Convert;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import io.brahmaos.wallet.brahmawallet.BuildConfig;
import io.brahmaos.wallet.brahmawallet.R;
import io.brahmaos.wallet.brahmawallet.common.BrahmaConfig;
import io.brahmaos.wallet.brahmawallet.common.BrahmaConst;
import io.brahmaos.wallet.brahmawallet.common.IntentParam;
import io.brahmaos.wallet.brahmawallet.db.entity.AccountEntity;
import io.brahmaos.wallet.brahmawallet.db.entity.TokenEntity;
import io.brahmaos.wallet.brahmawallet.model.AccountAssets;
import io.brahmaos.wallet.brahmawallet.model.KyberToken;
import io.brahmaos.wallet.brahmawallet.service.BrahmaWeb3jService;
import io.brahmaos.wallet.brahmawallet.service.EthAccountManager;
import io.brahmaos.wallet.brahmawallet.service.ImageManager;
import io.brahmaos.wallet.brahmawallet.service.MainService;
import io.brahmaos.wallet.brahmawallet.service.TokenService;
import io.brahmaos.wallet.brahmawallet.ui.base.BaseActivity;
import io.brahmaos.wallet.brahmawallet.ui.setting.HelpActivity;
import io.brahmaos.wallet.brahmawallet.view.CustomProgressDialog;
import io.brahmaos.wallet.brahmawallet.view.CustomStatusView;
import io.brahmaos.wallet.util.BLog;
import io.brahmaos.wallet.util.CommonUtil;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class InstantExchangeActivity extends BaseActivity {

    // UI references.
    private ImageView ivAccountAvatar;
    private TextView tvAccountName;
    private TextView tvAccountAddress;
    private TextView tvChangeAccount;

    private TextView tvEthBalance;
    private RelativeLayout layoutErc20Token;
    private TextView tvErc20TokenName;
    private TextView tvErc20TokenBalance;

    private TextView tvRateSendTokenNum;
    private TextView tvRateSendTokenName;
    private TextView tvRateReceiveTokenNum;
    private TextView tvRateReceiveTokenName;

    private EditText etSendTokenNum;
    private ImageView ivSendTokenIcon;
    private TextView tvSendTokenName;
    private LinearLayout layoutSendToken;

    private EditText etReceiveTokenNum;
    private ImageView ivReceiveTokenIcon;
    private TextView tvReceiveTokenName;
    private LinearLayout layoutReceiveToken;

    private EditText etGasPrice;
    private EditText etGasLimit;
    private Button btnShowTransfer;

    private AccountEntity mAccount;
    private List<AccountEntity> mAccounts = new ArrayList<>();
    private List<AccountAssets> mAccountAssetsList = new ArrayList<>();
    private List<KyberToken> mKyberTokens = new ArrayList<>();
    private List<KyberToken> sendChooseKyberTokens = new ArrayList<>();
    private List<KyberToken> receiveChooseKyberTokens = new ArrayList<>();
    private BottomSheetDialog sendDialog;
    private SearchView mSendSearchView;
    private RecyclerView sendTokensRecyclerView;
    private BottomSheetDialog receiveDialog;
    private SearchView mReceiveSearchView;
    private RecyclerView receiveTokensRecyclerView;

    private BigInteger currentRate = BigInteger.ONE;
    private BigInteger slippageRate = BigInteger.ZERO;
    private KyberToken sendToken;
    private KyberToken receiveToken;
    private KyberToken ethToken = new KyberToken();
    private KyberToken kncToken = new KyberToken();
    private CustomProgressDialog customProgressDialog;

    @Override
    protected String tag() {
        return InstantExchangeActivity.class.getName();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_instant_exchange);
        showNavBackBtn();

        mAccount = (AccountEntity) getIntent().getSerializableExtra(IntentParam.PARAM_ACCOUNT_INFO);
        initView();
        initData();
    }

    private void initView() {
        ivAccountAvatar = findViewById(R.id.iv_account_avatar);
        tvAccountName = findViewById(R.id.tv_account_name);
        tvAccountAddress = findViewById(R.id.tv_account_address);
        tvChangeAccount = findViewById(R.id.tv_change_account);

        tvEthBalance = findViewById(R.id.tv_eth_balance);
        layoutErc20Token = findViewById(R.id.layout_erc20_token_balance);
        tvErc20TokenName = findViewById(R.id.tv_erc20_token_name);
        tvErc20TokenBalance = findViewById(R.id.tv_erc20_token_balance);

        tvRateSendTokenNum = findViewById(R.id.tv_rate_send_token_num);
        tvRateSendTokenName = findViewById(R.id.tv_rate_send_token_name);
        tvRateReceiveTokenNum = findViewById(R.id.tv_rate_receive_token_num);
        tvRateReceiveTokenName = findViewById(R.id.tv_rate_receive_token_name);

        etSendTokenNum = findViewById(R.id.et_send_token_num);
        ivSendTokenIcon = findViewById(R.id.iv_send_token_icon);
        tvSendTokenName = findViewById(R.id.tv_send_token_name);
        layoutSendToken = findViewById(R.id.layout_send_token);

        etReceiveTokenNum = findViewById(R.id.et_receive_token_num);
        ivReceiveTokenIcon = findViewById(R.id.iv_receive_token_icon);
        tvReceiveTokenName = findViewById(R.id.tv_receive_token_name);
        layoutReceiveToken = findViewById(R.id.layout_receive_token);

        etGasPrice = findViewById(R.id.et_gas_price);
        etGasLimit = findViewById(R.id.et_gas_limit);
        btnShowTransfer = findViewById(R.id.btn_show_transfer_info);

        mAccountAssetsList = MainService.getInstance().getAccountAssetsList();

        etGasPrice.setText(String.valueOf(BrahmaConst.DEFAULT_GAS_PRICE));
        etGasLimit.setText(String.valueOf(BrahmaConst.DEFAULT_GAS_LIMIT));
        getGasPrice();

        mAccounts = MainService.getInstance().getEthereumAccounts();
        if (mAccounts == null || mAccounts.size() <=0) {
            finish();
        }
        if (mAccount == null || mAccount.getAddress().length() == 0) {
            mAccount = mAccounts.get(0);
        }
        if (mAccounts.size() > 1) {
            tvChangeAccount.setVisibility(View.VISIBLE);
        }
        tvChangeAccount.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_account_list, null);
            builder.setView(dialogView);
            builder.setCancelable(true);
            final AlertDialog alertDialog = builder.create();
            alertDialog.show();

            LinearLayout layoutAccountList = dialogView.findViewById(R.id.layout_accounts);

            for (final AccountEntity account : mAccounts) {
                final AccountItemView accountItemView = new AccountItemView();
                accountItemView.layoutAccountItem = LayoutInflater.from(this).inflate(R.layout.dialog_list_item_account, null);
                accountItemView.ivAccountAvatar = accountItemView.layoutAccountItem.findViewById(R.id.iv_account_avatar);
                accountItemView.tvAccountName = accountItemView.layoutAccountItem.findViewById(R.id.tv_account_name);
                accountItemView.tvAccountAddress = accountItemView.layoutAccountItem.findViewById(R.id.tv_account_address);
                accountItemView.layoutDivider = accountItemView.layoutAccountItem.findViewById(R.id.layout_divider);

                accountItemView.tvAccountName.setText(account.getName());
                ImageManager.showAccountAvatar(this, accountItemView.ivAccountAvatar, account);
                accountItemView.tvAccountAddress.setText(CommonUtil.generateSimpleAddress(account.getAddress()));

                accountItemView.layoutAccountItem.setOnClickListener(v1 -> {
                    alertDialog.cancel();
                    mAccount = account;
                    showAccountInfo(account);
                });

                if (mAccounts.indexOf(account) == mAccounts.size() - 1) {
                    accountItemView.layoutDivider.setVisibility(View.GONE);
                }

                layoutAccountList.addView(accountItemView.layoutAccountItem);
            }
        });

        etSendTokenNum.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                BLog.e(tag(), s.toString() + "----start :" + start + "-----befor:" + before + "-------count:" + count);
                if (s.length() > 0) {
                    try {
                        BigDecimal sendNum = new BigDecimal(Float.parseFloat(s.toString()));
                        BigDecimal receiveNum = Convert.fromWei(sendNum.multiply(new BigDecimal(currentRate)), Convert.Unit.ETHER);
                        if (receiveNum.compareTo(new BigDecimal(1)) > 0) {
                            etReceiveTokenNum.setText(String.valueOf(receiveNum.setScale(4, BigDecimal.ROUND_HALF_UP)));
                        } else {
                            etReceiveTokenNum.setText(String.valueOf(receiveNum.setScale(8, BigDecimal.ROUND_HALF_UP)));
                        }
                    } catch (NumberFormatException e) {
                        etSendTokenNum.setText("");
                    }
                }
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) { }
            @Override
            public void afterTextChanged(Editable s) { }
        });

        layoutSendToken.setOnClickListener(v -> {
            showSendKyberTokens();
        });

        layoutReceiveToken.setOnClickListener(v -> {
            showReceiveKyberTokens();
        });

        btnShowTransfer.setOnClickListener(v -> {
            if (sendToken.getName().toLowerCase().equals(BrahmaConst.ETHEREUM)) {
                showTransferInfo();
            } else {
                checkAllowanceAmount();
            }
        });

        sendDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_dialog_kyber_tokens, null);
        sendDialog.setContentView(view);
        mSendSearchView = view.findViewById(R.id.view_search);
        mSendSearchView.onActionViewExpanded();
        mSendSearchView.setMaxWidth(30000);
        mSendSearchView.setQueryHint(getString(R.string.choose_send_kyber_tokens));
        setUnderLineTransparent(mSendSearchView);
        sendTokensRecyclerView = view.findViewById(R.id.tokens_recycler);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        sendTokensRecyclerView.setLayoutManager(layoutManager);
        sendTokensRecyclerView.setAdapter(new SendKyberTokenRecyclerAdapter());
        mSendSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                sendChooseKyberTokens = searchKyberToken(mKyberTokens, query);
                sendTokensRecyclerView.getAdapter().notifyDataSetChanged();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText == null || newText.length() <= 0) {
                    copySendKyberTokens();
                    sendChooseKyberTokens.add(0, ethToken);
                    sendTokensRecyclerView.getAdapter().notifyDataSetChanged();
                }
                return false;
            }
        });

        receiveDialog = new BottomSheetDialog(this);
        View receiveView = getLayoutInflater().inflate(R.layout.bottom_sheet_dialog_kyber_tokens, null);
        receiveDialog.setContentView(receiveView);
        mReceiveSearchView = receiveView.findViewById(R.id.view_search);
        mReceiveSearchView.onActionViewExpanded();
        mReceiveSearchView.setMaxWidth(30000);
        mReceiveSearchView.setQueryHint(getString(R.string.choose_receive_kyber_tokens));
        setUnderLineTransparent(mReceiveSearchView);
        receiveTokensRecyclerView = receiveView.findViewById(R.id.tokens_recycler);
        LinearLayoutManager recevieLayoutManager = new LinearLayoutManager(this);
        receiveTokensRecyclerView.setLayoutManager(recevieLayoutManager);
        receiveTokensRecyclerView.setAdapter(new ReceiveKyberTokenRecyclerAdapter());
        mReceiveSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                receiveChooseKyberTokens = searchKyberToken(mKyberTokens, query);
                receiveTokensRecyclerView.getAdapter().notifyDataSetChanged();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText == null || newText.length() <= 0) {
                    copyReceiveKyberTokens();
                    receiveTokensRecyclerView.getAdapter().notifyDataSetChanged();
                }
                return false;
            }
        });

        customProgressDialog = new CustomProgressDialog(this, R.style.CustomProgressDialogStyle, getString(R.string.progress_get_rate));
        customProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        customProgressDialog.setCancelable(false);
    }

    private void initData() {
        // init eth token
        ethToken.setSymbol("ETH");
        ethToken.setName("ethereum");
        ethToken.setContractAddress("0xeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee");

        // init knc token
        kncToken.setSymbol("KNC");
        kncToken.setName("Kyber Network");
        if (BuildConfig.TEST_FLAG) {
            kncToken.setContractAddress("0x4E470dc7321E84CA96FcAEDD0C8aBCebbAEB68C6");
        } else {
            kncToken.setContractAddress("0xdd974d5c2e2928dea5f71b9825b8b646686bd200");
        }

        initSendToken(ethToken);
        showAccountInfo(mAccount);

        mKyberTokens = TokenService.getInstance().getKyberTokenList();
        if (mKyberTokens.size() <= 0) {
            TokenService.getInstance().getKyberTokens()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<List<KyberToken>>() {
                        @Override
                        public void onCompleted() {
                        }
                        @Override
                        public void onError(Throwable throwable) {
                            throwable.printStackTrace();
                        }
                        @Override
                        public void onNext(List<KyberToken> apr) {
                            mKyberTokens = TokenService.getInstance().getKyberTokenList();
                            copyKyberTokens();
                            sendChooseKyberTokens.add(0, ethToken);
                            sendTokensRecyclerView.getAdapter().notifyDataSetChanged();

                            receiveChooseKyberTokens = mKyberTokens;
                            receiveTokensRecyclerView.getAdapter().notifyDataSetChanged();
                            BLog.i(tag(), "onNext");
                        }
                    });
        } else {
            copyKyberTokens();
            sendChooseKyberTokens.add(0, ethToken);
        }
    }

    private void showAccountBalance() {
        for (AccountAssets assets : mAccountAssetsList) {
            if (assets.getAccountEntity().getAddress().toLowerCase().equals(mAccount.getAddress().toLowerCase())) {
                if (assets.getTokenEntity().getName().toLowerCase().equals(BrahmaConst.ETHEREUM.toLowerCase())) {
                    tvEthBalance.setText(String.valueOf(CommonUtil.getAccountFromWei(assets.getBalance())));
                }
            }
        }
    }

    private void initSendToken(KyberToken token) {
        sendToken = token;
        tvSendTokenName.setText(token.getSymbol());
        TokenEntity tokenEntity = TokenService.getInstance().queryTokenByAddress(token.getContractAddress().toLowerCase());
        ImageManager.showTokenIcon(InstantExchangeActivity.this, ivSendTokenIcon,
                token.getName(), tokenEntity.getAvatar());
        tvRateSendTokenName.setText(token.getSymbol());
        if (token.getSymbol().equals("ETH")) {
            initReceiveToken(kncToken);
        } else {
            initReceiveToken(ethToken);
        }
        if (mAccount != null) {
            showErc20TokenBalance();
        }
    }

    private void initReceiveToken(KyberToken token) {
        receiveToken = token;
        tvReceiveTokenName.setText(token.getSymbol());
        TokenEntity tokenEntity = TokenService.getInstance().queryTokenByAddress(token.getContractAddress().toLowerCase());
        ImageManager.showTokenIcon(InstantExchangeActivity.this, ivReceiveTokenIcon,
                token.getName(), tokenEntity.getAvatar());

        tvRateReceiveTokenName.setText(token.getSymbol());
        tvRateReceiveTokenNum.setText("");
        getExpectedRate();
    }

    private void copySendKyberTokens() {
        sendChooseKyberTokens = new ArrayList<>();
        sendChooseKyberTokens.addAll(mKyberTokens);
    }

    private void copyReceiveKyberTokens() {
        receiveChooseKyberTokens = new ArrayList<>();
        receiveChooseKyberTokens.addAll(mKyberTokens);
    }

    private void copyKyberTokens() {
        sendChooseKyberTokens = new ArrayList<>();
        sendChooseKyberTokens.addAll(mKyberTokens);
    }

    private void showErc20TokenBalance() {
        if (sendToken.getName().toLowerCase().equals(BrahmaConst.ETHEREUM)) {
            layoutErc20Token.setVisibility(View.GONE);
        } else {
            layoutErc20Token.setVisibility(View.VISIBLE);
        }
        tvErc20TokenName.setText(sendToken.getSymbol());
        TokenEntity tokenEntity = new TokenEntity();
        tokenEntity.setAddress(sendToken.getContractAddress());
        tokenEntity.setShortName(sendToken.getSymbol());

        EthAccountManager.getInstance().getEthereumBalanceByAddress(mAccount, tokenEntity)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<AccountAssets>() {
                    @Override
                    public void onNext(AccountAssets accountAssets) {
                        tvErc20TokenBalance.setText(String.valueOf(CommonUtil.getAccountFromWei(accountAssets.getBalance())));
                    }

                    @Override
                    public void onError(Throwable e) {
                        BLog.e("get erc20 token balance", e.getMessage());
                    }

                    @Override
                    public void onCompleted() {
                    }
                });
    }

    public void getGasPrice() {
        EthAccountManager.getInstance()
                .getEthereumGasPrice()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onNext(String gasPrice) {
                        BLog.d(tag(), "the gas price is: " + gasPrice);
                        BigDecimal gasPriceGwei = new BigDecimal(gasPrice);
                        etGasPrice.setText(String.valueOf(gasPriceGwei));
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onCompleted() {

                    }
                });
    }

    /*
     *  Get the real-time rate and
     *  update the receive amount based on the send amount
     */
    private void getExpectedRate() {
        if (customProgressDialog != null) {
            customProgressDialog.setProgressMessage(getString(R.string.progress_get_rate));
            customProgressDialog.show();
        }
        EthAccountManager.getInstance()
                .getExpectedRate(sendToken.getContractAddress(), receiveToken.getContractAddress())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<List<String>>() {
                    @Override
                    public void onNext(List<String> expectedRates) {
                        if (expectedRates != null && expectedRates.size() > 1) {
                            String expectedRate = expectedRates.get(0);
                            currentRate = new BigInteger(expectedRate);
                            BigDecimal readableRate = Convert.fromWei(new BigDecimal(expectedRate), Convert.Unit.ETHER);
                            if (readableRate.compareTo(new BigDecimal(1)) > 0) {
                                tvRateReceiveTokenNum.setText(String.valueOf(readableRate.setScale(4, BigDecimal.ROUND_HALF_UP)));
                            } else {
                                tvRateReceiveTokenNum.setText(String.valueOf(readableRate.setScale(8, BigDecimal.ROUND_HALF_UP)));
                            }
                            String slippageRateStr = expectedRates.get(1);
                            slippageRate = new BigInteger(slippageRateStr);

                            try {
                                BigDecimal sendNum = new BigDecimal(Float.parseFloat(etSendTokenNum.getText().toString()));
                                BigDecimal receiveNum = Convert.fromWei(sendNum.multiply(new BigDecimal(currentRate)), Convert.Unit.ETHER);
                                if (receiveNum.compareTo(new BigDecimal(1)) > 0) {
                                    etReceiveTokenNum.setText(String.valueOf(receiveNum.setScale(4, BigDecimal.ROUND_HALF_UP)));
                                } else {
                                    etReceiveTokenNum.setText(String.valueOf(receiveNum.setScale(8, BigDecimal.ROUND_HALF_UP)));
                                }
                            } catch (Exception e) {
                                e.getMessage();
                                etReceiveTokenNum.setText("");
                            }
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onCompleted() {
                        if (customProgressDialog != null) {
                            customProgressDialog.cancel();
                        }
                    }
                });
    }

    private class AccountItemView {
        View layoutAccountItem;
        ImageView ivAccountAvatar;
        TextView tvAccountName;
        TextView tvAccountAddress;
        LinearLayout layoutDivider;
    }

    private void showAccountInfo(AccountEntity account) {
        if (account != null) {
            ImageManager.showAccountAvatar(this, ivAccountAvatar, account);
            tvAccountName.setText(account.getName());
            tvAccountAddress.setText(CommonUtil.generateSimpleAddress(account.getAddress()));
            showAccountBalance();
            showErc20TokenBalance();
        }
    }

    // Show all kyber tokens
    public void showSendKyberTokens() {
        InputMethodManager imm = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            if (etSendTokenNum.isFocusable()) {
                imm.hideSoftInputFromWindow(etSendTokenNum.getWindowToken(), 0);
            } else if (etReceiveTokenNum.isFocusable()) {
                imm.hideSoftInputFromWindow(etReceiveTokenNum.getWindowToken(), 0);
            }
        }

        sendDialog.show();
        sendTokensRecyclerView.getAdapter().notifyDataSetChanged();
    }

    // show recevie kyber token
    private void showReceiveKyberTokens() {
        InputMethodManager imm = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            if (etSendTokenNum.isFocusable()) {
                imm.hideSoftInputFromWindow(etSendTokenNum.getWindowToken(), 0);
            } else if (etReceiveTokenNum.isFocusable()) {
                imm.hideSoftInputFromWindow(etReceiveTokenNum.getWindowToken(), 0);
            }
        }

        if (sendToken.getName().toLowerCase().equals(BrahmaConst.ETHEREUM)) {
            receiveChooseKyberTokens = mKyberTokens;
            receiveDialog.show();
            receiveTokensRecyclerView.getAdapter().notifyDataSetChanged();
        }
    }

    private class SendKyberTokenRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View rootView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_kyber_token, parent, false);
            rootView.setOnClickListener(v -> {

            });
            return new SendKyberTokenRecyclerAdapter.ItemViewHolder(rootView);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof SendKyberTokenRecyclerAdapter.ItemViewHolder) {
                SendKyberTokenRecyclerAdapter.ItemViewHolder itemViewHolder = (SendKyberTokenRecyclerAdapter.ItemViewHolder) holder;
                KyberToken kyberToken = sendChooseKyberTokens.get(position);
                setData(itemViewHolder, kyberToken);
            }
        }

        /**
         * set account view
         */
        private void setData(SendKyberTokenRecyclerAdapter.ItemViewHolder holder, final KyberToken token) {
            if (token == null) {
                return ;
            }

            holder.tvTokenShoreName.setText(token.getSymbol());
            holder.tvTokenAddress.setText(CommonUtil.generateSimpleAddress(token.getContractAddress()));
            holder.tvTokenName.setText(token.getName());
            // ETH cannot be cancelled
            if (token.getSymbol().equals("ETH")) {
                holder.tvTokenAddress.setVisibility(View.GONE);
                ImageManager.showTokenIcon(InstantExchangeActivity.this, holder.ivTokenAvatar, R.drawable.icon_eth);
            } else {
                holder.tvTokenAddress.setVisibility(View.VISIBLE);
                TokenEntity tokenEntity = TokenService.getInstance().queryTokenByAddress(token.getContractAddress().toLowerCase());
                ImageManager.showTokenIcon(InstantExchangeActivity.this, holder.ivTokenAvatar, token.getName(), tokenEntity.getAvatar());
            }
            holder.layoutKyberToken.setOnClickListener(v -> {
                if (!BuildConfig.TEST_FLAG) {
                    initSendToken(token);
                } else {
                    if (token.getSymbol().toLowerCase().equals("eth")) {
                        initSendToken(token);
                    } else {
                        initSendToken(kncToken);
                    }
                }
                sendDialog.cancel();
            });
        }

        @Override
        public int getItemCount() {
            return sendChooseKyberTokens.size();
        }

        class ItemViewHolder extends RecyclerView.ViewHolder {

            LinearLayout layoutKyberToken;
            ImageView ivTokenAvatar;
            TextView tvTokenShoreName;
            TextView tvTokenName;
            TextView tvTokenAddress;

            ItemViewHolder(View itemView) {
                super(itemView);
                layoutKyberToken = itemView.findViewById(R.id.layout_kyber_token);
                ivTokenAvatar = itemView.findViewById(R.id.iv_token_icon);
                tvTokenShoreName = itemView.findViewById(R.id.tv_token_short_name);
                tvTokenName = itemView.findViewById(R.id.tv_token_name);
                tvTokenAddress = itemView.findViewById(R.id.tv_token_address);
            }
        }
    }

    private class ReceiveKyberTokenRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View rootView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_kyber_token, parent, false);
            rootView.setOnClickListener(v -> {

            });
            return new ReceiveKyberTokenRecyclerAdapter.ItemViewHolder(rootView);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof ReceiveKyberTokenRecyclerAdapter.ItemViewHolder) {
                ReceiveKyberTokenRecyclerAdapter.ItemViewHolder itemViewHolder = (ReceiveKyberTokenRecyclerAdapter.ItemViewHolder) holder;
                KyberToken kyberToken = receiveChooseKyberTokens.get(position);
                setData(itemViewHolder, kyberToken);
            }
        }

        /**
         * set account view
         */
        private void setData(ReceiveKyberTokenRecyclerAdapter.ItemViewHolder holder, final KyberToken token) {
            if (token == null) {
                return ;
            }

            holder.tvTokenShoreName.setText(token.getSymbol());
            holder.tvTokenAddress.setText(CommonUtil.generateSimpleAddress(token.getContractAddress()));
            holder.tvTokenName.setText(token.getName());
            // ETH cannot be cancelled
            if (token.getSymbol().equals("ETH")) {
                holder.tvTokenAddress.setVisibility(View.GONE);
                ImageManager.showTokenIcon(InstantExchangeActivity.this, holder.ivTokenAvatar, R.drawable.icon_eth);
            } else {
                holder.tvTokenAddress.setVisibility(View.VISIBLE);
                TokenEntity tokenEntity = TokenService.getInstance().queryTokenByAddress(token.getContractAddress().toLowerCase());
                ImageManager.showTokenIcon(InstantExchangeActivity.this, holder.ivTokenAvatar, token.getName(), tokenEntity.getAvatar());
            }
            holder.layoutKyberToken.setOnClickListener(v -> {
                if (!BuildConfig.TEST_FLAG) {
                    initReceiveToken(token);
                } else {
                    initReceiveToken(kncToken);
                }
                receiveDialog.cancel();
            });
        }

        @Override
        public int getItemCount() {
            return receiveChooseKyberTokens.size();
        }

        class ItemViewHolder extends RecyclerView.ViewHolder {

            LinearLayout layoutKyberToken;
            ImageView ivTokenAvatar;
            TextView tvTokenShoreName;
            TextView tvTokenName;
            TextView tvTokenAddress;

            ItemViewHolder(View itemView) {
                super(itemView);
                layoutKyberToken = itemView.findViewById(R.id.layout_kyber_token);
                ivTokenAvatar = itemView.findViewById(R.id.iv_token_icon);
                tvTokenShoreName = itemView.findViewById(R.id.tv_token_short_name);
                tvTokenName = itemView.findViewById(R.id.tv_token_name);
                tvTokenAddress = itemView.findViewById(R.id.tv_token_address);
            }
        }
    }

    private void showTransferInfo() {
        String srcAmount = etSendTokenNum.getText().toString().trim();
        String maxDestAmount = etReceiveTokenNum.getText().toString().trim();
        BigDecimal amount = BigDecimal.ZERO;
        String gasPriceStr = etGasPrice.getText().toString().trim();
        String gasLimitStr = etGasLimit.getText().toString().trim();

        String tips = "";
        boolean cancel = false;

        if (!cancel && gasPriceStr.length() < 1) {
            tips = getString(R.string.tip_invalid_gas_price);
            cancel = true;
        }

        if (!cancel && gasLimitStr.length() < 1) {
            tips = getString(R.string.tip_invalid_gas_limit);
            cancel = true;
        }

        BigInteger totalBalance = BigInteger.ZERO;
        BigInteger ethTotalBalance = BigInteger.ZERO;
        for (AccountAssets assets : mAccountAssetsList) {
            if (assets.getAccountEntity().getAddress().equals(mAccount.getAddress())) {
                if (assets.getTokenEntity().getAddress().equals(sendToken.getContractAddress())) {
                    totalBalance = assets.getBalance();
                }
                if (assets.getTokenEntity().getName().toLowerCase().equals(BrahmaConst.ETHEREUM.toLowerCase())) {
                    ethTotalBalance = assets.getBalance();
                }
            }
        }

        if (cancel) {
            // dialog show tip
            AlertDialog dialogTip = new AlertDialog.Builder(this)
                    .setMessage(tips)
                    .setNegativeButton(R.string.ok, (dialog, which) -> dialog.cancel())
                    .create();
            dialogTip.show();
            return;
        }

        BigDecimal gasPrice = new BigDecimal(gasPriceStr);
        BigInteger gasLimit = new BigInteger(gasLimitStr);

        final BottomSheetDialog transferInfoDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_dialog_instant_transfer_info, null);
        transferInfoDialog.setContentView(view);
        transferInfoDialog.setCancelable(false);
        transferInfoDialog.show();

        ImageView ivCloseDialog = view.findViewById(R.id.iv_close_dialog);
        ivCloseDialog.setOnClickListener(v -> {
            transferInfoDialog.cancel();
        });

        TextView tvTitleSendTokenName = view.findViewById(R.id.tv_tilte_send_token_name);
        TextView tvTitleReceiveTokenName = view.findViewById(R.id.tv_title_receive_token_name);
        TextView tvSendTokenName = view.findViewById(R.id.tv_pay_token_name);
        TextView tvReceiveTokenName = view.findViewById(R.id.tv_receive_token_name);
        tvTitleSendTokenName.setText(sendToken.getSymbol());
        tvTitleReceiveTokenName.setText(receiveToken.getSymbol());
        tvSendTokenName.setText(sendToken.getSymbol());
        tvReceiveTokenName.setText(receiveToken.getSymbol());

        TextView tvSendTokenAmount = view.findViewById(R.id.tv_pay_token_amount);
        TextView tvReceiveTokenAmount = view.findViewById(R.id.tv_receive_token_amount);
        tvSendTokenAmount.setText(srcAmount);
        tvReceiveTokenAmount.setText(maxDestAmount);

        TextView tvFromAddress = view.findViewById(R.id.tv_pay_address);
        tvFromAddress.setText(mAccount.getAddress());

        TextView tvGasPrice = view.findViewById(R.id.tv_gas_price);
        tvGasPrice.setText(gasPriceStr);
        TextView tvGasLimit = view.findViewById(R.id.tv_gas_limit);
        tvGasLimit.setText(gasLimitStr);
        TextView tvGasValue = view.findViewById(R.id.tv_gas_value);
        BigDecimal gasValue = Convert.fromWei(Convert.toWei(new BigDecimal(gasLimit).multiply(gasPrice), Convert.Unit.GWEI), Convert.Unit.ETHER);
        tvGasValue.setText(gasValue.setScale(9, BigDecimal.ROUND_HALF_UP).toString());

        LinearLayout layoutTransferInfo = view.findViewById(R.id.layout_transfer_info);
        LinearLayout layoutTransferStatus = view.findViewById(R.id.layout_transfer_status);
        CustomStatusView customStatusView = view.findViewById(R.id.as_status);
        TextView tvTransferStatus = view.findViewById(R.id.tv_transfer_status);
        Button confirmBtn = view.findViewById(R.id.btn_commit_transfer);
        BigDecimal finalAmount = amount;
        confirmBtn.setOnClickListener(v -> {
            final View dialogView = getLayoutInflater().inflate(R.layout.dialog_account_password, null);

            AlertDialog passwordDialog = new AlertDialog.Builder(InstantExchangeActivity.this)
                    .setView(dialogView)
                    .setCancelable(false)
                    .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel())
                    .setPositiveButton(R.string.ok, (dialog, which) -> {
                        dialog.cancel();
                        // show transfer progress
                        layoutTransferStatus.setVisibility(View.VISIBLE);
                        tvTransferStatus.setText(R.string.progress_send_request);
                        customStatusView.loadLoading();
                        String password = ((EditText) dialogView.findViewById(R.id.et_password)).getText().toString();
                        EthAccountManager.getInstance().sendInstantExchangeTransfer(mAccount, sendToken,
                                receiveToken, new BigDecimal(srcAmount), new BigDecimal(maxDestAmount),
                                slippageRate, password, gasPrice, gasLimit)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(new Observer<String>() {
                                    @Override
                                    public void onNext(String hash) {
                                        if (hash != null && hash.length() > 0) {
                                            tvTransferStatus.setText(R.string.progress_transfer_success);
                                            BLog.i(tag(), "the transfer success");
                                            customStatusView.loadSuccess();
                                            new Handler().postDelayed(() -> {
                                                transferInfoDialog.cancel();
                                                Intent intent = new Intent();
                                                setResult(Activity.RESULT_OK, intent);
                                                finish();
                                            }, 1200);
                                        } else {
                                            customStatusView.loadFailure();
                                            tvTransferStatus.setText(R.string.progress_transfer_fail);
                                            new Handler().postDelayed(() -> {
                                                layoutTransferStatus.setVisibility(View.GONE);
                                                int resId = R.string.tip_error_transfer;
                                                new AlertDialog.Builder(InstantExchangeActivity.this)
                                                        .setMessage(resId)
                                                        .setNegativeButton(R.string.ok, (dialog1, which1) -> dialog1.cancel())
                                                        .create().show();
                                            }, 1500);

                                            BLog.i(tag(), "the transfer failed");
                                        }
                                    }

                                    @Override
                                    public void onError(Throwable e) {
                                        e.printStackTrace();
                                        customStatusView.loadFailure();
                                        tvTransferStatus.setText(R.string.progress_transfer_fail);
                                        new Handler().postDelayed(() -> {
                                            layoutTransferStatus.setVisibility(View.GONE);
                                            int resId = R.string.tip_error_transfer;
                                            if (e instanceof CipherException) {
                                                resId = R.string.tip_error_password;
                                            }
                                            new AlertDialog.Builder(InstantExchangeActivity.this)
                                                    .setMessage(resId)
                                                    .setNegativeButton(R.string.ok, (dialog1, which1) -> dialog1.cancel())
                                                    .create().show();
                                        }, 1500);

                                        BLog.i(tag(), "the transfer failed");
                                    }

                                    @Override
                                    public void onCompleted() {
                                    }
                                });
                    })
                    .create();
            passwordDialog.show();
        });
    }

    // check
    private void checkAllowanceAmount() {
        if (customProgressDialog != null) {
            customProgressDialog.setProgressMessage(getString(R.string.progress_get_allowance));
            customProgressDialog.show();
        }
        String srcAmount = etSendTokenNum.getText().toString().trim();
        EthAccountManager.getInstance()
                .getContractAllowance(mAccount, sendToken)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<BigInteger>() {
                    @Override
                    public void onNext(BigInteger allowance) {
                        if (allowance != null) {
                            BigInteger sendAmount = CommonUtil.convertWeiFromEther(new BigDecimal(srcAmount));
                            if (allowance.compareTo(sendAmount) >= 0) {
                                showTransferInfo();
                            } else {
                                showApprovalTipDialog();
                            }
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onCompleted() {
                        if (customProgressDialog != null) {
                            customProgressDialog.cancel();
                        }
                    }
                });
    }

    // show approval tip
    private void showApprovalTipDialog() {
        String sendAmount = etSendTokenNum.getText().toString().trim();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_contract_approve, null);
        EditText etApproveAmount = dialogView.findViewById(R.id.et_approve_amount);
        TextView tvHelp = dialogView.findViewById(R.id.tv_approve_reason);
        tvHelp.setOnClickListener(v -> {
            Intent intent = new Intent(InstantExchangeActivity.this, HelpActivity.class);
            startActivity(intent);
        });
        etApproveAmount.setText(sendAmount);
        AlertDialog alert = builder.setView(dialogView)
                            .setCancelable(false)
                            .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel())
                            .setPositiveButton(R.string.confirm, (dialog, which) -> {
                                String approveAmount = etApproveAmount.getText().toString().trim();
                                if (approveAmount.length() > 0) {
                                    dialog.cancel();
                                    showApprovalInfo(approveAmount);
                                }
                            })
                            .create();
        alert.show();
        Button negativeButton = alert.getButton(DialogInterface.BUTTON_NEGATIVE);
        negativeButton.setTextColor(getResources().getColor(R.color.color_disabled_text));
    }

    // show approve transaction info
    private void showApprovalInfo(String approveAmount) {
        String gasPriceStr = etGasPrice.getText().toString().trim();
        String gasLimitStr = etGasLimit.getText().toString().trim();
        BigDecimal gasPrice = new BigDecimal(gasPriceStr);
        BigInteger gasLimit = new BigInteger(gasLimitStr);

        final BottomSheetDialog transferInfoDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_dialog_approve_transfer_info, null);
        transferInfoDialog.setContentView(view);
        transferInfoDialog.setCancelable(false);
        View parent = (View) view.getParent();
        BottomSheetBehavior behavior = BottomSheetBehavior.from(parent);
        view.measure(0, 0);
        behavior.setPeekHeight(view.getMeasuredHeight());
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) parent.getLayoutParams();
        params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        parent.setLayoutParams(params);
        transferInfoDialog.show();

        ImageView ivCloseDialog = view.findViewById(R.id.iv_close_dialog);
        ivCloseDialog.setOnClickListener(v -> {
            transferInfoDialog.cancel();
        });

        TextView tvApprovalAmount = view.findViewById(R.id.tv_approval_token_amount);
        TextView tvApprovalTokenName = view.findViewById(R.id.tv_approval_token_name);
        tvApprovalAmount.setText(approveAmount);
        tvApprovalTokenName.setText(sendToken.getSymbol());

        TextView tvPayToAddress = view.findViewById(R.id.tv_pay_to_address);
        TextView tvPayByAddress = view.findViewById(R.id.tv_pay_by_address);
        tvPayToAddress.setText(sendToken.getContractAddress());
        tvPayByAddress.setText(mAccount.getAddress());

        TextView tvGasPrice = view.findViewById(R.id.tv_gas_price);
        tvGasPrice.setText(gasPriceStr);
        TextView tvGasLimit = view.findViewById(R.id.tv_gas_limit);
        tvGasLimit.setText(gasLimitStr);
        TextView tvGasValue = view.findViewById(R.id.tv_gas_value);
        BigDecimal gasValue = Convert.fromWei(Convert.toWei(new BigDecimal(gasLimit).multiply(gasPrice), Convert.Unit.GWEI), Convert.Unit.ETHER);
        tvGasValue.setText(String.valueOf(gasValue.setScale(9, BigDecimal.ROUND_HALF_UP)));

        LinearLayout layoutTransferStatus = view.findViewById(R.id.layout_transfer_status);
        CustomStatusView customStatusView = view.findViewById(R.id.as_status);
        TextView tvTransferStatus = view.findViewById(R.id.tv_transfer_status);
        Button confirmBtn = view.findViewById(R.id.btn_commit_transfer);
        confirmBtn.setOnClickListener(v -> {
            final View dialogView = getLayoutInflater().inflate(R.layout.dialog_account_password, null);

            AlertDialog passwordDialog = new AlertDialog.Builder(InstantExchangeActivity.this)
                    .setView(dialogView)
                    .setCancelable(false)
                    .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel())
                    .setPositiveButton(R.string.ok, (dialog, which) -> {
                        dialog.cancel();
                        // show transfer progress
                        layoutTransferStatus.setVisibility(View.VISIBLE);
                        customStatusView.loadLoading();
                        String password = ((EditText) dialogView.findViewById(R.id.et_password)).getText().toString();
                        EthAccountManager.getInstance().sendContractApproveTransfer(mAccount, sendToken,
                                new BigDecimal(approveAmount), password, gasPrice, gasLimit)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(new Observer<String>() {
                                    @Override
                                    public void onNext(String hash) {
                                        if (hash != null && hash.length() > 0) {
                                            tvTransferStatus.setText(R.string.progress_transfer_success);
                                            BLog.i(tag(), "the transfer success");
                                            customStatusView.loadSuccess();
                                            new Handler().postDelayed(() -> {
                                                transferInfoDialog.cancel();
                                                int resId = R.string.tip_approve_success;
                                                new AlertDialog.Builder(InstantExchangeActivity.this)
                                                        .setMessage(resId)
                                                        .setNegativeButton(R.string.ok, (dialog1, which1) -> dialog1.cancel())
                                                        .create().show();
                                            }, 1200);
                                        } else {
                                            customStatusView.loadFailure();
                                            tvTransferStatus.setText(R.string.progress_transfer_fail);
                                            new Handler().postDelayed(() -> {
                                                layoutTransferStatus.setVisibility(View.GONE);
                                                int resId = R.string.tip_error_transfer;
                                                new AlertDialog.Builder(InstantExchangeActivity.this)
                                                        .setMessage(resId)
                                                        .setNegativeButton(R.string.ok, (dialog1, which1) -> dialog1.cancel())
                                                        .create().show();
                                            }, 1500);
                                        }
                                    }

                                    @Override
                                    public void onError(Throwable e) {
                                        e.printStackTrace();
                                        customStatusView.loadFailure();
                                        tvTransferStatus.setText(R.string.progress_transfer_fail);
                                        new Handler().postDelayed(() -> {
                                            layoutTransferStatus.setVisibility(View.GONE);
                                            int resId = R.string.tip_error_transfer;
                                            if (e instanceof CipherException) {
                                                resId = R.string.tip_error_password;
                                            }
                                            new AlertDialog.Builder(InstantExchangeActivity.this)
                                                    .setMessage(resId)
                                                    .setNegativeButton(R.string.ok, (dialog1, which1) -> dialog1.cancel())
                                                    .create().show();
                                        }, 1500);

                                        BLog.i(tag(), "the transfer failed");
                                    }

                                    @Override
                                    public void onCompleted() {
                                    }
                                });
                    })
                    .create();
            passwordDialog.show();
        });
    }

    private void setUnderLineTransparent(SearchView searchView){
        try {
            Class<?> argClass = searchView.getClass();
            Field ownField = argClass.getDeclaredField("mSearchPlate");
            ownField.setAccessible(true);
            View mView = (View) ownField.get(searchView);
            mView.setBackgroundColor(Color.TRANSPARENT);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private List<KyberToken> searchKyberToken(List<KyberToken> kyberTokens, String searchText) {
        List<KyberToken> kyberTokenList = new ArrayList<>();
        for (KyberToken kyberToken : kyberTokens) {
            if (kyberToken.getName().toLowerCase().matches("(.*)" + searchText.toLowerCase() + "(.*)") ||
                    kyberToken.getSymbol().toLowerCase().matches("(.*)" + searchText.toLowerCase() + "(.*)")) {
                kyberTokenList.add(kyberToken);
            }
        }
        return kyberTokenList;
    }
}
