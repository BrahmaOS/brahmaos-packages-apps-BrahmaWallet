package io.brahmaos.wallet.brahmawallet.ui.transaction;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.widget.NestedScrollView;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import brahmaos.content.TransactionDetails;
import io.brahmaos.wallet.brahmawallet.R;
import io.brahmaos.wallet.brahmawallet.common.IntentParam;
import io.brahmaos.wallet.brahmawallet.db.entity.AccountEntity;
import io.brahmaos.wallet.brahmawallet.service.BtcAccountManager;
import io.brahmaos.wallet.brahmawallet.ui.base.BaseActivity;
import io.brahmaos.wallet.util.CommonUtil;

public class BtcTransactionsActivity extends BaseActivity {
    @Override
    protected String tag() {
        return BtcTransactionsActivity.class.getName();
    }

    // UI references.
    SwipeRefreshLayout swipeRefreshLayout;
    NestedScrollView nestedScrollView;

    RecyclerView recyclerViewTransactions;
    LinearLayout layoutNoTransactions;

    private AccountEntity mAccount;
    private List<TransactionDetails> mTransactions = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_btc_transactions);

        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        nestedScrollView = findViewById(R.id.sv_content);
        recyclerViewTransactions = findViewById(R.id.transactions_recycler);
        layoutNoTransactions = findViewById(R.id.layout_no_transactions);

        showNavBackBtn();
        mAccount = (AccountEntity) getIntent().getSerializableExtra(IntentParam.PARAM_ACCOUNT_INFO);

        if (mAccount == null) {
            finish();
        }
        initView();
        getBtcTransactions();
    }

    private void initView() {
        swipeRefreshLayout.setColorSchemeResources(R.color.master);
        swipeRefreshLayout.setOnRefreshListener(this::getBtcTransactions);

        swipeRefreshLayout.setRefreshing(true);
        recyclerViewTransactions.setVisibility(View.GONE);
        layoutNoTransactions.setVisibility(View.GONE);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setSmoothScrollbarEnabled(true);
        layoutManager.setAutoMeasureEnabled(true);
        recyclerViewTransactions.setLayoutManager(layoutManager);
        recyclerViewTransactions.setAdapter(new TransactionRecyclerAdapter());
        recyclerViewTransactions.setVisibility(View.GONE);
        // Solve the sliding lag problem
        recyclerViewTransactions.setHasFixedSize(true);
        recyclerViewTransactions.setNestedScrollingEnabled(false);

        // Solve the sliding lag problem
        recyclerViewTransactions.setHasFixedSize(true);
        recyclerViewTransactions.setNestedScrollingEnabled(false);
    }

    private void getBtcTransactions() {
        mTransactions = BtcAccountManager.getInstance().getTransactionsByTime(mAccount.getAddress());
        swipeRefreshLayout.setRefreshing(false);
        if (mTransactions != null && mTransactions.size() > 0) {
            recyclerViewTransactions.setVisibility(View.VISIBLE);
            recyclerViewTransactions.getAdapter().notifyDataSetChanged();
        } else {
            layoutNoTransactions.setVisibility(View.VISIBLE);
        }
    }

    /**
     * list item account
     */
    private class TransactionRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View rootView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_btc_transaction, parent, false);
            return new TransactionRecyclerAdapter.ItemViewHolder(rootView);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof TransactionRecyclerAdapter.ItemViewHolder) {
                TransactionRecyclerAdapter.ItemViewHolder itemViewHolder = (TransactionRecyclerAdapter.ItemViewHolder) holder;
                TransactionDetails transaction = mTransactions.get(position);
                setAssetsData(itemViewHolder, transaction);
            }
        }

        /*
         * set assets view
         */
        private void setAssetsData(TransactionRecyclerAdapter.ItemViewHolder holder, final TransactionDetails transaction) {
            if (transaction == null) {
                return;
            }
            holder.layoutTransaction.setOnClickListener(v -> {
                Intent intent = new Intent(BtcTransactionsActivity.this, BtcTransactionDetailActivity.class);
                intent.putExtra(IntentParam.PARAM_ACCOUNT_INFO, mAccount);
                intent.putExtra(IntentParam.PARAM_TX_HASH, transaction.hash);
                intent.putExtra(IntentParam.PARAM_BITCOIN_TRANSACTION, transaction);
                startActivity(intent);
            });
            holder.tvTxTime.setText(CommonUtil.timestampToDate(transaction.updateTime / 1000, null));
            int depthInBlocks = transaction.depthInBlocks;
            if (depthInBlocks >= BtcAccountManager.MIN_CONFIRM_BLOCK_HEIGHT) {
                holder.tvTxSendStatus.setText(R.string.transaction_confirmed);
            } else {
                holder.tvTxSendStatus.setText(String.format(Locale.getDefault(), "%s %d/%d", getString(R.string.transaction_confirming), depthInBlocks, BtcAccountManager.MIN_CONFIRM_BLOCK_HEIGHT));
                holder.tvTxSendStatus.setTextColor(getResources().getColor(R.color.master));
            }
            String sendAmount = String.valueOf(CommonUtil.convertBTCFromSatoshi(transaction.amount));
            holder.tvTxAmount.setText(sendAmount);
            if (transaction.amount < 0) {
                Glide.with(BtcTransactionsActivity.this)
                        .load(R.drawable.icon_send)
                        .into(holder.ivTxStatusIcon);
                holder.tvTxAmount.setTextColor(getResources().getColor(R.color.tx_send));
            } else {
                Glide.with(BtcTransactionsActivity.this)
                        .load(R.drawable.icon_receive)
                        .into(holder.ivTxStatusIcon);
                holder.tvTxAmount.setTextColor(getResources().getColor(R.color.tx_receive));
            }
        }

        @Override
        public int getItemCount() {
            return mTransactions.size();
        }

        class ItemViewHolder extends RecyclerView.ViewHolder {
            LinearLayout layoutTransaction;
            ImageView ivTxStatusIcon;
            TextView tvTxTime;
            TextView tvTxSendStatus;
            TextView tvTxAmount;

            ItemViewHolder(View itemView) {
                super(itemView);
                layoutTransaction = itemView.findViewById(R.id.layout_transaction_item);
                ivTxStatusIcon = itemView.findViewById(R.id.iv_transaction_status_icon);
                tvTxTime = itemView.findViewById(R.id.tv_transaction_time);
                tvTxSendStatus = itemView.findViewById(R.id.tv_confirm_status);
                tvTxAmount = itemView.findViewById(R.id.tv_transactions_amount);
            }
        }
    }
}
