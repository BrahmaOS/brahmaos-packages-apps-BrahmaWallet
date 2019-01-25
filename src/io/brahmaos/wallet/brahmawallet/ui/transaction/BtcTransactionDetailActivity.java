package io.brahmaos.wallet.brahmawallet.ui.transaction;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.Utils;

import java.util.Date;
import java.util.Map;

import brahmaos.content.TransactionDetails;
import io.brahmaos.wallet.brahmawallet.R;
import io.brahmaos.wallet.brahmawallet.common.BrahmaConfig;
import io.brahmaos.wallet.brahmawallet.common.IntentParam;
import io.brahmaos.wallet.brahmawallet.db.entity.AccountEntity;
import io.brahmaos.wallet.brahmawallet.service.BtcAccountManager;
import io.brahmaos.wallet.brahmawallet.ui.base.BaseActivity;
import io.brahmaos.wallet.util.BLog;
import io.brahmaos.wallet.util.CommonUtil;

public class BtcTransactionDetailActivity extends BaseActivity {
    @Override
    protected String tag() {
        return BtcTransactionDetailActivity.class.getName();
    }

    // UI references.
    TextView mTvBtcAmount;
    TextView mTvTxHash;
    TextView mTvTxFirstConfirmedBlock;
    TextView mTvConfirmations;
    TextView mTvTxDatetime;
    LinearLayout mLayoutTransactionInput;
    LinearLayout mLayoutTransactionOutput;
    TextView mTvTransactionFee;
    TextView mTvTransactionFeeUnit;
    LinearLayout mLayoutCopyBlockchainUrl;

    private TransactionDetails mTransaction;
    private AccountEntity mAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_btc_transaction_detail);
        mTvBtcAmount = findViewById(R.id.tv_transaction_amount);
        mTvTxHash = findViewById(R.id.tv_transaction_hash);
        mTvTxFirstConfirmedBlock = findViewById(R.id.tv_transaction_block_height);
        mTvConfirmations = findViewById(R.id.tv_confirmations);
        mTvTxDatetime = findViewById(R.id.tv_transaction_date);
        mLayoutTransactionInput = findViewById(R.id.layout_input_transaction);
        mLayoutTransactionOutput = findViewById(R.id.layout_output_transaction);
        mTvTransactionFee = findViewById(R.id.tv_transaction_fee);
        mTvTransactionFeeUnit = findViewById(R.id.tv_transaction_fee_unit);
        mLayoutCopyBlockchainUrl = findViewById(R.id.layout_copy_blockchain_url);
        showNavBackBtn();
        mAccount = (AccountEntity) getIntent().getSerializableExtra(IntentParam.PARAM_ACCOUNT_INFO);
        if (mAccount == null) {
            finish();
        }
        String transactionHash = getIntent().getStringExtra(IntentParam.PARAM_TX_HASH);
        if (transactionHash == null) {
            finish();
        }
        mTransaction = (TransactionDetails) getIntent().getParcelableExtra(IntentParam.PARAM_BITCOIN_TRANSACTION);
        initView();
    }

    private void initView() {
        String sendAmount = String.valueOf(CommonUtil.convertBTCFromSatoshi(mTransaction.amount));
        try {
            String txHash = mTransaction.hash;
            mTvBtcAmount.setText(String.format("%s %s", sendAmount, getString(R.string.account_btc)));
            mTvTxHash.setText(txHash);
            BLog.d(tag(), "the transaction is:" + mTransaction.toString());
            mTvTxFirstConfirmedBlock.setText(String.valueOf(mTransaction.confirmBlockHeight));
            mTvConfirmations.setText(String.valueOf(mTransaction.depthInBlocks));
            mTvTxDatetime.setText(new Date(mTransaction.updateTime).toString());
            long fee = mTransaction.fee;
            int size = mTransaction.bytesLength;
            if (size > 0) {
                mTvTransactionFee.setText(new StringBuilder().append(CommonUtil.convertBTCFromSatoshi(fee)).append(" BTC for ").append(size).append(" bytes"));
                mTvTransactionFeeUnit.setText(new StringBuilder().append(fee / size).append(" sat/byte"));
            }

            if (mTransaction.inputs != null && mTransaction.inputs.size() > 0) {
                for (Map<String, Long> input : mTransaction.inputs) {
                    final ItemView itemView = new ItemView();
                    itemView.layoutItem = LayoutInflater.from(this).inflate(R.layout.item_transaction, null);
                    itemView.tvAddress = itemView.layoutItem.findViewById(R.id.tv_address);
                    itemView.tvAmount = itemView.layoutItem.findViewById(R.id.tv_amount);

                    if (input.values().iterator().hasNext()) {
                        itemView.tvAmount.setText(String.valueOf(CommonUtil.convertBTCFromSatoshi(input.values().iterator().next())));
                    }
                    if (input.keySet().iterator().hasNext()) {
                        itemView.tvAddress.setText(input.keySet().iterator().next());
                    }

                    mLayoutTransactionInput.addView(itemView.layoutItem);
                }
            }

            if (mTransaction.outputs != null && mTransaction.outputs.size() > 0) {
                for (Map<String, Long> output : mTransaction.outputs) {
                    final ItemView itemView = new ItemView();
                    itemView.layoutItem = LayoutInflater.from(this).inflate(R.layout.item_transaction, null);
                    itemView.tvAddress = itemView.layoutItem.findViewById(R.id.tv_address);
                    itemView.tvAmount = itemView.layoutItem.findViewById(R.id.tv_amount);

                    if (output.values().iterator().hasNext()) {
                        itemView.tvAmount.setText(String.valueOf(CommonUtil.convertBTCFromSatoshi(output.values().iterator().next())));
                    }
                    if (output.keySet().iterator().hasNext()) {
                        itemView.tvAddress.setText(output.keySet().iterator().next());
                    }

                    mLayoutTransactionOutput.addView(itemView.layoutItem);
                }
            }

            mLayoutCopyBlockchainUrl.setOnClickListener(v -> {
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clipData = ClipData.newPlainText("text", BrahmaConfig.getInstance().getBlochchainTxDetailUrl(txHash));
                if (cm != null) {
                    cm.setPrimaryClip(clipData);
                    showLongToast(R.string.tip_success_copy);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class ItemView {
        View layoutItem;
        TextView tvAmount;
        TextView tvAddress;
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
            Intent intent = new Intent(BtcTransactionDetailActivity.this, BlockchainTxDetailActivity.class);
            intent.putExtra(IntentParam.PARAM_TX_HASH, mTransaction.hash);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
