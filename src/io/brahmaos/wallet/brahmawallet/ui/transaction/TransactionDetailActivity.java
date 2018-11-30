package io.brahmaos.wallet.brahmawallet.ui.transaction;

import android.arch.lifecycle.ViewModelProviders;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.NestedScrollView;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
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

import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.brahmaos.wallet.brahmawallet.R;
import io.brahmaos.wallet.brahmawallet.common.BrahmaConfig;
import io.brahmaos.wallet.brahmawallet.common.IntentParam;
import io.brahmaos.wallet.brahmawallet.db.entity.AccountEntity;
import io.brahmaos.wallet.brahmawallet.db.entity.ContactEntity;
import io.brahmaos.wallet.brahmawallet.db.entity.TokenEntity;
import io.brahmaos.wallet.brahmawallet.model.EthTransaction;
import io.brahmaos.wallet.brahmawallet.model.TokenTransaction;
import io.brahmaos.wallet.brahmawallet.service.ImageManager;
import io.brahmaos.wallet.brahmawallet.service.TransactionService;
import io.brahmaos.wallet.brahmawallet.ui.base.BaseActivity;
import io.brahmaos.wallet.brahmawallet.ui.transfer.TransferActivity;
import io.brahmaos.wallet.util.BLog;
import io.brahmaos.wallet.util.CommonUtil;
import me.yokeyword.indexablerv.IndexableLayout;
import me.yokeyword.indexablerv.SimpleHeaderAdapter;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class TransactionDetailActivity extends BaseActivity {
    @Override
    protected String tag() {
        return TransactionDetailActivity.class.getName();
    }

    public static final int REQ_CODE_TRANSFER = 10;

    // UI references.
    private ImageView ivSendAccountAvatar;
    private TextView tvSendAccountName;
    private TextView tvSendAccountAddress;
    private ImageView ivReceiveAccountAvatar;
    private TextView tvReceiveAccountName;
    private TextView tvReceiveAccountAddress;

    private TextView tvTxHash;
    private TextView tvBlockHeight;
    private TextView tvTxTime;
    private RelativeLayout layoutTokenTransfered;
    private TextView tvTokenTransfered;
    private LinearLayout layoutTokenTransferedDivider;
    private TextView tvTransactionValue;
    private TextView tvTxGasValue;
    private TextView tvTxGasUsed;
    private TextView tvTxGasPrice;
    private LinearLayout layoutCopyEtherscanUrl;

    private EthTransaction mEthTx;
    private TokenTransaction mTokenTx;
    private String fromAddress;
    private String toAddress;
    private String txHash;

    // Determine whether to traverse
    private boolean contactFlag = false;
    private boolean accountFlag = false;

    private boolean fromFlag = false;
    private boolean toFlag = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_detail);
        ivSendAccountAvatar = findViewById(R.id.iv_send_account_avatar);
        tvSendAccountName = findViewById(R.id.tv_send_account_name);
        tvSendAccountAddress = findViewById(R.id.tv_send_account_address);
        ivReceiveAccountAvatar = findViewById(R.id.iv_receive_account_avatar);
        tvReceiveAccountName = findViewById(R.id.tv_receive_account_name);
        tvReceiveAccountAddress = findViewById(R.id.tv_receive_account_address);
        tvTxHash = findViewById(R.id.tv_transaction_tx_hash);
        tvBlockHeight = findViewById(R.id.tv_transaction_block_height);
        tvTxTime = findViewById(R.id.tv_transaction_time);
        layoutTokenTransfered = findViewById(R.id.layout_transaction_token_transfered);
        tvTokenTransfered = findViewById(R.id.tv_transaction_token_transfered);
        layoutTokenTransferedDivider = findViewById(R.id.laytou_divider_token_transfered);
        tvTransactionValue = findViewById(R.id.tv_transaction_value);
        tvTxGasValue = findViewById(R.id.tv_gas_value);
        tvTxGasUsed = findViewById(R.id.tv_gas_used);
        tvTxGasPrice = findViewById(R.id.tv_gas_price);
        layoutCopyEtherscanUrl = findViewById(R.id.layout_copy_etherscan_url);
        showNavBackBtn();

        mEthTx = (EthTransaction) getIntent().getSerializableExtra(IntentParam.PARAM_ETH_TX);
        mTokenTx = (TokenTransaction) getIntent().getSerializableExtra(IntentParam.PARAM_TOKEN_TX);

        if (mEthTx == null && mTokenTx == null) {
            finish();
        }

        if (mEthTx != null) {
            fromAddress = mEthTx.getFromAddress();
            toAddress = mEthTx.getToAddress();
            txHash = mEthTx.getHash();
            initEthData();
        } else {
            fromAddress = mTokenTx.getFromAddress();
            toAddress = mTokenTx.getToAddress();
            txHash = mTokenTx.getEthTransaction().getHash();
            initTokenData();
        }
        initHeader();

        layoutCopyEtherscanUrl.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clipData = ClipData.newPlainText("text", BrahmaConfig.getInstance().getEtherscanTxDetailUrl(txHash));
            if (cm != null) {
                cm.setPrimaryClip(clipData);
                showLongToast(R.string.tip_success_copy);
            }
        });
    }

    private void initHeader() {
        tvSendAccountAddress.setText(fromAddress);
        tvReceiveAccountAddress.setText(toAddress);
    }

    private void judgeAddressStatus() {
        if (accountFlag && contactFlag) {
            if (!fromFlag) {
                tvSendAccountName.setVisibility(View.GONE);
                ivSendAccountAvatar.setBackgroundResource(0);
                Glide.with(this)
                        .load(R.drawable.ic_person_add)
                        .into(ivSendAccountAvatar);
                ivSendAccountAvatar.setOnClickListener(v -> {

                });
            }
            if (!toFlag) {
                tvReceiveAccountName.setVisibility(View.GONE);
                ivReceiveAccountAvatar.setBackgroundResource(0);
                Glide.with(this)
                        .load(R.drawable.ic_person_add)
                        .into(ivReceiveAccountAvatar);
                ivReceiveAccountAvatar.setOnClickListener(v -> {

                });
            }
        }
    }

    private void initEthData() {
        tvTxHash.setText(CommonUtil.generateSimpleAddress(mEthTx.getHash()));
        tvBlockHeight.setText(String.valueOf(mEthTx.getBlockHeight()));
        layoutTokenTransfered.setVisibility(View.GONE);
        layoutTokenTransferedDivider.setVisibility(View.GONE);
        tvTransactionValue.setText(String.valueOf(CommonUtil.getAccountFromWei(mEthTx.getValue())));
        tvTxGasUsed.setText(String.valueOf(mEthTx.getGasUsed()));
        tvTxGasPrice.setText(String.valueOf(Convert.fromWei(new BigDecimal(mEthTx.getGasPrice()), Convert.Unit.GWEI).setScale(3, BigDecimal.ROUND_HALF_UP)));
        BigDecimal gasValue = Convert.fromWei(new BigDecimal(mEthTx.getGasUsed()).multiply(new BigDecimal(mEthTx.getGasPrice())), Convert.Unit.ETHER);
        tvTxGasValue.setText(String.valueOf(gasValue.setScale(9, BigDecimal.ROUND_HALF_UP)));
        tvTxTime.setText(CommonUtil.timestampToDate(mEthTx.getTxTime(), null));
    }

    private void initTokenData() {
        tvTxHash.setText(CommonUtil.generateSimpleAddress(mTokenTx.getEthTransaction().getHash()));
        tvBlockHeight.setText(String.valueOf(mTokenTx.getEthTransaction().getBlockHeight()));
        layoutTokenTransfered.setVisibility(View.VISIBLE);
        layoutTokenTransferedDivider.setVisibility(View.VISIBLE);
        tvTokenTransfered.setText(String.valueOf(CommonUtil.getAccountFromWei(mTokenTx.getValue())));
        tvTransactionValue.setText(String.valueOf(CommonUtil.getAccountFromWei(mTokenTx.getEthTransaction().getValue())));
        tvTxGasUsed.setText(String.valueOf(mTokenTx.getEthTransaction().getGasUsed()));
        tvTxGasPrice.setText(String.valueOf(Convert.fromWei(new BigDecimal(mTokenTx.getEthTransaction().getGasPrice()), Convert.Unit.GWEI).setScale(3, BigDecimal.ROUND_HALF_UP)));
        BigDecimal gasValue = Convert.fromWei(new BigDecimal(mTokenTx.getEthTransaction().getGasUsed()).multiply(new BigDecimal(mTokenTx.getEthTransaction().getGasPrice())), Convert.Unit.ETHER);
        tvTxGasValue.setText(String.valueOf(gasValue.setScale(9, BigDecimal.ROUND_HALF_UP)));
        tvTxTime.setText(CommonUtil.timestampToDate(mTokenTx.getEthTransaction().getTxTime(), null));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_tx_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_tx_detail) {
            Intent intent = new Intent(TransactionDetailActivity.this, EtherscanTxDetailActivity.class);
            intent.putExtra(IntentParam.PARAM_TX_HASH, txHash);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
