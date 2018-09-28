package io.brahmaos.wallet.brahmawallet.ui.transaction;

import android.content.Intent;
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
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.brahmaos.wallet.brahmawallet.R;
import io.brahmaos.wallet.brahmawallet.common.IntentParam;
import io.brahmaos.wallet.brahmawallet.db.entity.AccountEntity;
import io.brahmaos.wallet.brahmawallet.db.entity.TokenEntity;
import io.brahmaos.wallet.brahmawallet.model.TokenTransaction;
import io.brahmaos.wallet.brahmawallet.service.ImageManager;
import io.brahmaos.wallet.brahmawallet.service.TransactionService;
import io.brahmaos.wallet.brahmawallet.ui.base.BaseActivity;
import io.brahmaos.wallet.brahmawallet.ui.transfer.TransferActivity;
import io.brahmaos.wallet.util.BLog;
import io.brahmaos.wallet.util.CommonUtil;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class TransactionsActivity extends BaseActivity {
    @Override
    protected String tag() {
        return TransactionsActivity.class.getName();
    }

    public static final int REQ_CODE_TRANSFER = 10;

    // UI references.
    private SwipeRefreshLayout swipeRefreshLayout;
    private NestedScrollView nestedScrollView;
    private ImageView ivAccountAvatar;
    private TextView tvAccountName;
    private TextView tvAccountAddress;
    private RecyclerView recyclerViewTransactions;
    private LinearLayout layoutNoTransactions;
    private FloatingActionButton fab;

    private AccountEntity mAccount;
    private TokenEntity mToken;
    private List<TokenTransaction> mTokenTransactions = new ArrayList<>();
    private boolean loadMoreFinished = false;
    private int page = 0;
    private int count = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transactions);
        showNavBackBtn();
        mAccount = (AccountEntity) getIntent().getSerializableExtra(IntentParam.PARAM_ACCOUNT_INFO);
        mToken = (TokenEntity) getIntent().getSerializableExtra(IntentParam.PARAM_TOKEN_INFO);

        if (mAccount == null || mToken == null) {
            finish();
        }
        initView();
        initData();
        getTokenTxList();
    }

    private void initView() {
        // UI references.
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        nestedScrollView = findViewById(R.id.sv_content);
        ivAccountAvatar = findViewById(R.id.iv_account_avatar);
        tvAccountName = findViewById(R.id.tv_account_name);
        tvAccountAddress = findViewById(R.id.tv_account_address);
        recyclerViewTransactions = findViewById(R.id.transactions_recycler);
        layoutNoTransactions = findViewById(R.id.layout_no_transactions);
        fab = findViewById(R.id.fab);
    }

    private void initData() {
        swipeRefreshLayout.setColorSchemeResources(R.color.master);
        swipeRefreshLayout.setOnRefreshListener(this::getLatestTokenTxList);

        String tokenShortName = mToken.getShortName();
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            ActionBar ab = getSupportActionBar();
            if (ab != null) {
                ab.setTitle(tokenShortName + getString(R.string.blank_space) +
                        getString(R.string.title_transactions));
            }
        }

        showAccountInfo(mAccount);
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

        nestedScrollView.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
            @Override
            public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                if (scrollY == (v.getChildAt(0).getMeasuredHeight() - v.getMeasuredHeight())) {
                    getTokenTxList();
                }
            }
        });
        // Solve the sliding lag problem
        recyclerViewTransactions.setHasFixedSize(true);
        recyclerViewTransactions.setNestedScrollingEnabled(false);

        fab.setOnClickListener(v -> {
            Intent intent = new Intent(this, TransferActivity.class);
            intent.putExtra(IntentParam.PARAM_ACCOUNT_INFO, mAccount);
            intent.putExtra(IntentParam.PARAM_TOKEN_INFO, mToken);
            startActivityForResult(intent, REQ_CODE_TRANSFER);
        });
    }

    private void showAccountInfo(AccountEntity account) {
        if (account != null) {
            ImageManager.showAccountAvatar(this, ivAccountAvatar, account);
            tvAccountName.setText(account.getName());
            tvAccountAddress.setText(CommonUtil.generateSimpleAddress(account.getAddress()));
        }
    }

    private void getTokenTxList() {
        TransactionService.getInstance().getTokenTransactions(mToken.getAddress().toLowerCase(), mAccount.getAddress().toLowerCase(), page, count)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<List<TokenTransaction>>() {

                    @Override
                    public void onCompleted() {
                        swipeRefreshLayout.setRefreshing(false);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        throwable.printStackTrace();
                    }

                    @Override
                    public void onNext(List<TokenTransaction> apr) {
                        handleTokenTxList(apr);
                    }
                });
    }

    private void getLatestTokenTxList() {
        TransactionService.getInstance().getTokenTransactions(mToken.getAddress().toLowerCase(),
                mAccount.getAddress().toLowerCase(), 0, count)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<List<TokenTransaction>>() {

                    @Override
                    public void onCompleted() {
                        swipeRefreshLayout.setRefreshing(false);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        throwable.printStackTrace();
                    }

                    @Override
                    public void onNext(List<TokenTransaction> apr) {
                        if (apr == null || apr.size() == 0) {
                            return;
                        }
                        for (TokenTransaction txNew : apr) {
                            for (TokenTransaction txLocal : mTokenTransactions) {
                                if (txLocal.getEthTransaction().getHash().equals(txNew.getEthTransaction().getHash())) {
                                    mTokenTransactions.remove(txLocal);
                                    break;
                                }
                            }
                            mTokenTransactions.add(txNew);
                        }
                        Collections.sort(mTokenTransactions);
                        recyclerViewTransactions.getAdapter().notifyDataSetChanged();
                    }
                });
    }

    private void handleTokenTxList(List<TokenTransaction> ethTxList) {
        if (page == 0) {
            if (ethTxList != null && ethTxList.size() > 0) {
                if (ethTxList.size() < count) {
                    loadMoreFinished = true;
                }
                mTokenTransactions = ethTxList;
                recyclerViewTransactions.setVisibility(View.VISIBLE);
                recyclerViewTransactions.getAdapter().notifyDataSetChanged();
            } else {
                layoutNoTransactions.setVisibility(View.VISIBLE);
            }
        } else {
            if (ethTxList == null) {
                loadMoreFinished = true;
                return;
            } else if (ethTxList.size() < count) {
                loadMoreFinished = true;
            }
            for (TokenTransaction txNew : ethTxList) {
                for (TokenTransaction txLocal : mTokenTransactions) {
                    if (txLocal.getEthTransaction().getHash().equals(txNew.getEthTransaction().getHash())) {
                        mTokenTransactions.remove(txLocal);
                        break;
                    }
                }
                mTokenTransactions.add(txNew);
            }
            Collections.sort(mTokenTransactions);
            recyclerViewTransactions.getAdapter().notifyDataSetChanged();
        }
        page += 1;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_tx_record, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_tx_record) {
            Intent intent = new Intent(TransactionsActivity.this, EtherscanTxsActivity.class);
            intent.putExtra(IntentParam.PARAM_ACCOUNT_INFO, mAccount);
            intent.putExtra(IntentParam.PARAM_TOKEN_INFO, mToken);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQ_CODE_TRANSFER) {
            if (resultCode == RESULT_OK) {
                BLog.i(tag(), "transfer success");
                swipeRefreshLayout.setRefreshing(true);
                getLatestTokenTxList();
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }


    /**
     * list item account
     */
    private class TransactionRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int TYPE_FOOTER = 1;
        private static final int TYPE_CONTENT = 2;

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == TYPE_FOOTER) {
                View rootView = LayoutInflater.from(parent.getContext()).inflate(R.layout.load_more, parent, false);
                return new FootViewHolder(rootView);
            } else {
                View rootView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_transaction, parent, false);
                return new TransactionRecyclerAdapter.ItemViewHolder(rootView);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof TransactionRecyclerAdapter.ItemViewHolder) {
                TransactionRecyclerAdapter.ItemViewHolder itemViewHolder = (TransactionRecyclerAdapter.ItemViewHolder) holder;
                TokenTransaction tokenTransaction = mTokenTransactions.get(position);
                setAssetsData(itemViewHolder, tokenTransaction);
            }
        }

        /*
         * set assets view
         */
        private void setAssetsData(TransactionRecyclerAdapter.ItemViewHolder holder, final TokenTransaction tokenTransaction) {
            if (tokenTransaction == null) {
                return;
            }
            holder.layoutTransaction.setOnClickListener(v -> {
                Intent intent = new Intent(TransactionsActivity.this, TransactionDetailActivity.class);
                intent.putExtra(IntentParam.PARAM_TOKEN_TX, tokenTransaction);
                startActivity(intent);
            });
            holder.tvTxTime.setText(CommonUtil.timestampToDate(tokenTransaction.getEthTransaction().getTxTime(), null));
            holder.tvTxSenderAddress.setText(CommonUtil.generateSimpleAddress(tokenTransaction.getFromAddress()));
            holder.tvTxReceiverAddress.setText(CommonUtil.generateSimpleAddress(tokenTransaction.getToAddress()));
            if (tokenTransaction.getFromAddress().toLowerCase().equals(mAccount.getAddress().toLowerCase())) {
                Glide.with(TransactionsActivity.this)
                        .load(R.drawable.icon_send)
                        .into(holder.ivTxStatusIcon);
                Glide.with(TransactionsActivity.this)
                        .load(R.drawable.icon_send_arrow)
                        .into(holder.ivTxArrow);
                String sendAmount = "- " + String.valueOf(CommonUtil.getAccountFromWei(tokenTransaction.getValue()));
                holder.tvTxAmount.setText(sendAmount);
                holder.tvTxAmount.setTextColor(getResources().getColor(R.color.tx_send));
            } else {
                Glide.with(TransactionsActivity.this)
                        .load(R.drawable.icon_receive)
                        .into(holder.ivTxStatusIcon);
                Glide.with(TransactionsActivity.this)
                        .load(R.drawable.icon_receive_arrow)
                        .into(holder.ivTxArrow);
                String sendAmount = "+ " + String.valueOf(CommonUtil.getAccountFromWei(tokenTransaction.getValue()));
                holder.tvTxAmount.setText(sendAmount);
                holder.tvTxAmount.setTextColor(getResources().getColor(R.color.tx_receive));
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (!loadMoreFinished && position + 1 == getItemCount()) {
                return TYPE_FOOTER;
            } else {
                return TYPE_CONTENT;
            }
        }

        @Override
        public int getItemCount() {
            if (loadMoreFinished) {
                return mTokenTransactions.size();
            }
            return mTokenTransactions.size() + 1;
        }

        class ItemViewHolder extends RecyclerView.ViewHolder {

            LinearLayout layoutTransaction;
            ImageView ivTxStatusIcon;
            TextView tvTxTime;
            TextView tvTxSenderAddress;
            ImageView ivTxArrow;
            TextView tvTxReceiverAddress;
            TextView tvTxAmount;

            ItemViewHolder(View itemView) {
                super(itemView);
                layoutTransaction = itemView.findViewById(R.id.layout_transaction_item);
                ivTxStatusIcon = itemView.findViewById(R.id.iv_transaction_status_icon);
                tvTxTime = itemView.findViewById(R.id.tv_transaction_time);
                tvTxSenderAddress = itemView.findViewById(R.id.tv_sender_address);
                ivTxArrow = itemView.findViewById(R.id.iv_transaction_arrow);
                tvTxReceiverAddress = itemView.findViewById(R.id.tv_receiver_address);
                tvTxAmount = itemView.findViewById(R.id.tv_transactions_amount);
            }
        }

        class FootViewHolder extends RecyclerView.ViewHolder {

            FootViewHolder(View itemView) {
                super(itemView);
            }
        }
    }
}