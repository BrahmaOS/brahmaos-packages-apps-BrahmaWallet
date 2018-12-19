package io.brahmaos.wallet.brahmawallet.ui.token;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import io.brahmaos.wallet.brahmawallet.R;
import io.brahmaos.wallet.brahmawallet.db.entity.AllTokenEntity;
import io.brahmaos.wallet.brahmawallet.db.entity.TokenEntity;
import io.brahmaos.wallet.brahmawallet.service.ImageManager;
import io.brahmaos.wallet.brahmawallet.service.MainService;
import io.brahmaos.wallet.brahmawallet.service.TokenService;
import io.brahmaos.wallet.brahmawallet.ui.base.BaseActivity;
import io.brahmaos.wallet.util.BLog;
import io.brahmaos.wallet.util.CommonUtil;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class TokenSearchActivity extends BaseActivity {

    @Override
    protected String tag() {
        return TokenSearchActivity.class.getName();
    }

    // UI references.
    private RecyclerView recyclerViewTokens;
    private LinearLayout layoutNoResult;
    private LinearLayout layoutDefault;

    private List<TokenEntity> chooseTokes = null;
    private List<AllTokenEntity> allTokens = new ArrayList<>();
    private String currentData = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_token_search);
        showNavBackBtn();
        recyclerViewTokens = findViewById(R.id.tokens_recycler);
        layoutNoResult = findViewById(R.id.layout_no_result);
        layoutDefault = findViewById(R.id.layout_default);

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setTitle("");
        }

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerViewTokens.setLayoutManager(layoutManager);
        recyclerViewTokens.setAdapter(new TokenRecyclerAdapter());

        chooseTokes = MainService.getInstance().getAllChosenTokens();
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_search_token, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();

        int magId = getResources().getIdentifier("android:id/search_mag_icon",null, null);
        ImageView magImage = (ImageView) searchView.findViewById(magId);
        magImage.setLayoutParams(new LinearLayout.LayoutParams(0, 0));

        int searchPlateId = getResources().getIdentifier("android:id/search_plate",null, null);
        View searchPlateView = searchView.findViewById(searchPlateId);
        searchPlateView.setBackground(null);

        int submitViewId = getResources().getIdentifier("android:id/submit_area",null, null);
        View submitVie = searchView.findViewById(submitViewId);
        submitVie.setBackground(null);

        magImage.setLayoutParams(new LinearLayout.LayoutParams(0, 0));

        searchView.setIconifiedByDefault(false);
        searchView.onActionViewExpanded();
        searchView.setMaxWidth(30000);
        searchView.setQueryHint(getString(R.string.prompt_search_token));

        searchView.setOnCloseListener(() -> {
            recyclerViewTokens.setVisibility(View.GONE);
            layoutNoResult.setVisibility(View.GONE);
            layoutDefault.setVisibility(View.VISIBLE);
            return false;
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                if (s.length() > 0) {
                    queryTokens(s);
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }

        });

        return super.onCreateOptionsMenu(menu);
    }

    private void queryTokens(String param) {
        List<AllTokenEntity> allTokenEntities = TokenService.getInstance().queryToken(param);
        if (allTokenEntities.size() <= 0) {
            recyclerViewTokens.setVisibility(View.GONE);
            layoutNoResult.setVisibility(View.VISIBLE);
            layoutDefault.setVisibility(View.GONE);
        } else {
            recyclerViewTokens.setVisibility(View.VISIBLE);
            layoutNoResult.setVisibility(View.GONE);
            layoutDefault.setVisibility(View.GONE);
            allTokens = allTokenEntities;
            // When change database, don't need refresh page
            recyclerViewTokens.getAdapter().notifyDataSetChanged();
        }

    }

    /**
     * list item account
     */
    private class TokenRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View rootView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_token_search, parent, false);
            rootView.setOnClickListener(v -> {

            });
            return new TokenRecyclerAdapter.ItemViewHolder(rootView);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof TokenRecyclerAdapter.ItemViewHolder) {
                TokenRecyclerAdapter.ItemViewHolder itemViewHolder = (TokenRecyclerAdapter.ItemViewHolder) holder;
                AllTokenEntity tokenEntity = allTokens.get(position);
                setData(itemViewHolder, tokenEntity);
            }
        }

        /**
         * set account view
         */
        private void setData(TokenRecyclerAdapter.ItemViewHolder holder, final AllTokenEntity token) {
            if (token == null) {
                return ;
            }

            holder.tvTokenShoreName.setText(token.getShortName());
            holder.tvTokenAddress.setText(CommonUtil.generateSimpleAddress(token.getAddress()));
            holder.tvTokenName.setText(token.getName());
            // BRM and ETH cannot be cancelled
            if (token.getShortName().equals("ETH")) {
                holder.tvTokenAddress.setVisibility(View.GONE);
                holder.switchToken.setVisibility(View.GONE);
                ImageManager.showTokenIcon(TokenSearchActivity.this, holder.ivTokenAvatar, R.drawable.icon_eth);
            } else if (token.getShortName().equals("BRM")) {
                holder.tvTokenAddress.setVisibility(View.VISIBLE);
                holder.switchToken.setVisibility(View.GONE);
                ImageManager.showTokenIcon(TokenSearchActivity.this, holder.ivTokenAvatar, R.drawable.icon_brm);
            } else {
                holder.tvTokenAddress.setVisibility(View.VISIBLE);
                holder.switchToken.setVisibility(View.VISIBLE);
                ImageManager.showTokenIcon(TokenSearchActivity.this, holder.ivTokenAvatar,
                        token.getName(), token.getAddress());

                // Determine if the token is selected
                boolean checked = false;
                if (chooseTokes != null && chooseTokes.size() > 0) {
                    for (TokenEntity tokenEntity : chooseTokes) {
                        if (tokenEntity.getAddress().equals(token.getAddress())) {
                            checked = true;
                            break;
                        }
                    }
                }
                TokenEntity currentToken = new TokenEntity();
                currentToken.setAddress(token.getAddress());
                currentToken.setName(token.getName());
                currentToken.setShortName(token.getShortName());
                currentToken.setAvatar(token.getAvatar());
                holder.switchToken.setOnCheckedChangeListener(null);
                holder.switchToken.setChecked(checked);
                holder.switchToken.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        TokenService.getInstance().checkTokenEntity(currentToken);
                    } else {
                        TokenService.getInstance().unCheckTokenEntity(currentToken);
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            return allTokens.size();
        }

        class ItemViewHolder extends RecyclerView.ViewHolder {

            ImageView ivTokenAvatar;
            TextView tvTokenShoreName;
            TextView tvTokenName;
            TextView tvTokenAddress;
            Switch switchToken;

            ItemViewHolder(View itemView) {
                super(itemView);
                ivTokenAvatar = itemView.findViewById(R.id.iv_token_icon);
                tvTokenShoreName = itemView.findViewById(R.id.tv_token_short_name);
                tvTokenName = itemView.findViewById(R.id.tv_token_name);
                tvTokenAddress = itemView.findViewById(R.id.tv_token_address);
                switchToken = itemView.findViewById(R.id.switch_token);
            }
        }
    }
}
