package io.brahmaos.wallet.brahmawallet.ui.account;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
import io.brahmaos.wallet.brahmawallet.event.EventTypeDef;
import io.brahmaos.wallet.brahmawallet.model.AccountAssets;
import io.brahmaos.wallet.brahmawallet.model.CryptoCurrency;
import io.brahmaos.wallet.brahmawallet.service.ImageManager;
import io.brahmaos.wallet.brahmawallet.service.MainService;
import io.brahmaos.wallet.brahmawallet.view.CornerFlagView;
import io.brahmaos.wallet.brahmawallet.view.CustomProgressDialog;
import io.brahmaos.wallet.util.BLog;
import io.brahmaos.wallet.util.CommonUtil;
import io.brahmaos.wallet.util.RxEventBus;

import rx.Completable;
import rx.CompletableSubscriber;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class EthAccountFragment extends Fragment {
    protected String tag() {
        return EthAccountFragment.class.getName();
    }

    public static final String ARG_PAGE = "ETHEREUM_ACCOUNT_PAGE";

    // UI references.
    private View parentView;
    RecyclerView recyclerViewAccounts;
    private CustomProgressDialog progressDialog;

    private List<AccountEntity> accounts = new ArrayList<>();
    private List<AccountAssets> accountAssetsList = new ArrayList<>();
    private List<CryptoCurrency> cryptoCurrencies = new ArrayList<>();
    private Observable<Boolean> newEthAccountCallback;

    public static EthAccountFragment newInstance(int page) {
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE, page);
        EthAccountFragment pageFragment = new EthAccountFragment();
        pageFragment.setArguments(args);
        return pageFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // used to receive btc blocks sync progress
        newEthAccountCallback = RxEventBus.get().register(EventTypeDef.CHANGE_ETH_ACCOUNT, Boolean.class);
        newEthAccountCallback.onBackpressureBuffer()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Boolean>() {
                    @Override
                    public void onNext(Boolean flag) {
                        accounts = MainService.getInstance().getEthereumAccounts();
                        BLog.d(tag(), accounts.toString());
                        recyclerViewAccounts.getAdapter().notifyDataSetChanged();
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
    public void onDestroy() {
        super.onDestroy();
        RxEventBus.get().unregister(EventTypeDef.CHANGE_ETH_ACCOUNT, newEthAccountCallback);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        BLog.d(tag(), "onCreateView");
        if (parentView == null) {
            parentView = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_accounts_eth, container, false);
            initView();
        } else {
            ViewGroup parent = (ViewGroup)parentView.getParent();
            if (parent != null) {
                parent.removeView(parentView);
            }
        }
        return parentView;
    }

    private void initView() {
        recyclerViewAccounts = parentView.findViewById(R.id.accounts_recycler);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerViewAccounts.setLayoutManager(layoutManager);
        recyclerViewAccounts.setAdapter(new AccountRecyclerAdapter());
        accountAssetsList = MainService.getInstance().getAccountAssetsList();
        cryptoCurrencies = MainService.getInstance().getCryptoCurrencies();
        accounts = MainService.getInstance().getEthereumAccounts();
        recyclerViewAccounts.getAdapter().notifyDataSetChanged();
    }

    /**
     * list item account
     */
    private class AccountRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View rootView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_account_eth, parent, false);
            rootView.setOnClickListener(v -> {
                int position = recyclerViewAccounts.getChildAdapterPosition(v);
                AccountEntity account = accounts.get(position);
                Intent intent = new Intent(getActivity(), EthAccountAssetsActivity.class);
                intent.putExtra(IntentParam.PARAM_ACCOUNT_ADDRESS, account.getAddress());
                startActivity(intent);
            });
            return new AccountRecyclerAdapter.ItemViewHolder(rootView);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof AccountRecyclerAdapter.ItemViewHolder) {
                AccountRecyclerAdapter.ItemViewHolder itemViewHolder = (AccountRecyclerAdapter.ItemViewHolder) holder;
                AccountEntity accountEntity = accounts.get(position);
                setData(itemViewHolder, accountEntity);
            }
        }

        /**
         * set account view
         */
        private void setData(AccountRecyclerAdapter.ItemViewHolder holder, final AccountEntity account) {
            if (account == null) {
                return ;
            }
            ImageManager.showAccountAvatar(getActivity(), holder.ivAccountAvatar, account);
            int colorId = ImageManager.showAccountBackground(getActivity(), holder.ivAccountBg, account);

            if (BrahmaConfig.getInstance().getCurrencyUnit().equals(BrahmaConst.UNIT_PRICE_CNY)) {
                Glide.with(EthAccountFragment.this)
                        .load(R.drawable.currency_cny_white)
                        .into(holder.ivCurrencyUnit);
            } else {
                Glide.with(EthAccountFragment.this)
                        .load(R.drawable.currency_usd_white)
                        .into(holder.ivCurrencyUnit);
            }

            if (account.isDefault()) {
                holder.ivCornerFlag.setVisibility(View.VISIBLE);
                holder.ivCornerFlag.setTextColor(colorId);
            } else {
                holder.ivCornerFlag.setVisibility(View.GONE);
            }

            holder.tvAccountName.setText(account.getName());
            holder.tvAccountAddress.setText(CommonUtil.generateSimpleAddress(account.getAddress()));
            BigDecimal totalAssets = BigDecimal.ZERO;
            for (AccountAssets assets : accountAssetsList) {
                if (assets.getAccountEntity().getAddress().equals(account.getAddress()) &&
                        assets.getBalance().compareTo(BigInteger.ZERO) > 0) {
                    for (CryptoCurrency currency : cryptoCurrencies) {
                        if (CommonUtil.cryptoCurrencyCompareToken(currency, assets.getTokenEntity())) {
                            double tokenPrice = currency.getPriceCny();
                            if (BrahmaConfig.getInstance().getCurrencyUnit().equals(BrahmaConst.UNIT_PRICE_USD)) {
                                tokenPrice = currency.getPriceUsd();
                            }
                            BigDecimal tokenValue = new BigDecimal(tokenPrice).multiply(CommonUtil.getAccountFromWei(assets.getBalance()));
                            totalAssets = totalAssets.add(tokenValue);
                        }
                    }
                }
            }
            holder.tvTotalAssets.setText(String.valueOf(totalAssets.setScale(2, BigDecimal.ROUND_HALF_UP)));
        }

        @Override
        public int getItemCount() {
            return accounts.size();
        }

        class ItemViewHolder extends RecyclerView.ViewHolder {
            CornerFlagView ivCornerFlag;
            ImageView ivAccountBg;
            ImageView ivAccountAvatar;
            TextView tvAccountName;
            TextView tvAccountAddress;
            TextView tvTotalAssetsDesc;
            TextView tvTotalAssets;
            ImageView ivCurrencyUnit;

            ItemViewHolder(View itemView) {
                super(itemView);
                ivCornerFlag = itemView.findViewById(R.id.iv_corner_flag);
                ivAccountBg = itemView.findViewById(R.id.iv_account_bg);
                ivAccountAvatar = itemView.findViewById(R.id.iv_account_avatar);
                tvAccountName = itemView.findViewById(R.id.tv_account_name);
                tvAccountAddress = itemView.findViewById(R.id.tv_account_address);
                tvTotalAssetsDesc = itemView.findViewById(R.id.tv_total_assets_desc);
                tvTotalAssets = itemView.findViewById(R.id.tv_total_assets);
                ivCurrencyUnit = itemView.findViewById(R.id.iv_currency_amount);
            }
        }
    }
}
