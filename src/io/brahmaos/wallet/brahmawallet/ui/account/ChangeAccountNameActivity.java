package io.brahmaos.wallet.brahmawallet.ui.account;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import java.util.List;

import brahmaos.app.WalletManager;
import brahmaos.content.BrahmaContext;
import io.brahmaos.wallet.brahmawallet.R;
import io.brahmaos.wallet.brahmawallet.common.BrahmaConst;
import io.brahmaos.wallet.brahmawallet.common.IntentParam;
import io.brahmaos.wallet.brahmawallet.db.entity.AccountEntity;
import io.brahmaos.wallet.brahmawallet.event.EventTypeDef;
import io.brahmaos.wallet.brahmawallet.service.MainService;
import io.brahmaos.wallet.brahmawallet.ui.base.BaseActivity;
import io.brahmaos.wallet.brahmawallet.view.CustomProgressDialog;
import io.brahmaos.wallet.util.BLog;
import io.brahmaos.wallet.util.RxEventBus;

public class ChangeAccountNameActivity extends BaseActivity {

    @Override
    protected String tag() {
        return ChangeAccountNameActivity.class.getName();
    }

    // UI references.
    private EditText etAccountName;

    private List<AccountEntity> accounts;
    private AccountEntity mAccount;
    private MenuItem menuSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_account_name);
        etAccountName = findViewById(R.id.et_account_name);

        showNavBackBtn();
        mAccount = (AccountEntity) getIntent().getSerializableExtra(IntentParam.PARAM_ACCOUNT_INFO);
        if (mAccount == null) {
            finish();
        }
        initView();
        initData();
    }

    private void initView() {
        etAccountName.setText(mAccount.getName());
        etAccountName.setSelection(mAccount.getName().length());
        etAccountName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                BLog.i(tag(), s.toString());
                if (s.toString().equals(mAccount.getName())) {
                    menuSave.setEnabled(false);
                } else {
                    menuSave.setEnabled(true);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private void initData() {
        accounts = MainService.getInstance().getAllAccounts();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_save, menu);
        menuSave = menu.findItem(R.id.action_save);
        menuSave.setEnabled(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //noinspection SimplifiableIfStatement
        if (item.getItemId() == R.id.action_save) {
            String name = etAccountName.getText().toString();
            if (TextUtils.isEmpty(name)) {
                etAccountName.setError(getString(R.string.error_field_required));
                return false;
            }

            if (accounts != null && accounts.size() > 0) {
                boolean cancel = false;
                for (AccountEntity accountEntity : accounts) {
                    if (accountEntity.getName().equals(name)) {
                        cancel = true;
                        break;
                    }
                }
                if (cancel) {
                    etAccountName.setError(getString(R.string.error_incorrect_name));
                    return false;
                }
            }

            CustomProgressDialog progressDialog = new CustomProgressDialog(this, R.style.CustomProgressDialogStyle, "");
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.show();

            WalletManager walletManager = (WalletManager) getSystemService(BrahmaContext.WALLET_SERVICE);

            int ret = walletManager.updateWalletNameForAddress(name, mAccount.getAddress());
            progressDialog.cancel();
            if (ret == WalletManager.CODE_NO_ERROR) {
                showLongToast(R.string.success_change_account_name);

                // Refresh accounts
                MainService.getInstance().loadAllAccounts();
                if (mAccount.getType() == BrahmaConst.ETH_ACCOUNT_TYPE) {
                    RxEventBus.get().post(EventTypeDef.CHANGE_ETH_ACCOUNT, true);
                } else if (mAccount.getType() == BrahmaConst.BTC_ACCOUNT_TYPE) {
                    RxEventBus.get().post(EventTypeDef.CHANGE_BTC_ACCOUNT, true);
                }
                Intent intent = this.getIntent();
                intent.putExtra(IntentParam.PARAM_ACCOUNT_NAME, name);
                setResult(RESULT_OK, intent);
                finish();
            } else {
                showLongToast(R.string.error_change_account_name);
            }
        }
        return super.onOptionsItemSelected(item);
    }

}
