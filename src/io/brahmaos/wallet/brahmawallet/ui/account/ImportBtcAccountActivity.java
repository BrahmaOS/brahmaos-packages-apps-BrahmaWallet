package io.brahmaos.wallet.brahmawallet.ui.account;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import brahmaos.content.WalletData;
import io.brahmaos.wallet.brahmawallet.R;
import io.brahmaos.wallet.brahmawallet.db.entity.AccountEntity;
import io.brahmaos.wallet.brahmawallet.event.EventTypeDef;
import io.brahmaos.wallet.brahmawallet.service.BtcAccountManager;
import io.brahmaos.wallet.brahmawallet.service.MainService;
import io.brahmaos.wallet.brahmawallet.ui.base.BaseActivity;
import io.brahmaos.wallet.brahmawallet.ui.setting.PrivacyPolicyActivity;
import io.brahmaos.wallet.brahmawallet.ui.setting.ServiceTermsActivity;
import io.brahmaos.wallet.brahmawallet.view.CustomProgressDialog;
import io.brahmaos.wallet.util.CommonUtil;
import io.brahmaos.wallet.util.RxEventBus;
import rx.Observable;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class ImportBtcAccountActivity extends BaseActivity {
    @Override
    protected String tag() {
        return ImportBtcAccountActivity.class.getName();
    }

    public static final int REQ_IMPORT_ACCOUNT = 20;
    private List<AccountEntity> accounts;

    // UI references.
    private EditText etMnemonic;
    private EditText etAccountName;
    private EditText etPassword;
    private EditText etRepeatPassword;
    private Button btnImportAccount;
    private CheckBox checkBoxReadProtocol;
    private TextView tvService;
    private TextView tvPrivacyPolicy;

    private CustomProgressDialog customProgressDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restore_btc_account);
        etMnemonic = findViewById(R.id.et_mnemonic);
        etAccountName = findViewById(R.id.et_account_name);
        etPassword = findViewById(R.id.et_password);
        etRepeatPassword = findViewById(R.id.et_repeat_password);
        btnImportAccount = findViewById(R.id.btn_import_mnemonics);
        checkBoxReadProtocol = findViewById(R.id.checkbox_read_protocol);
        tvService = findViewById(R.id.service_tv);
        tvPrivacyPolicy = findViewById(R.id.privacy_policy_tv);

        showNavBackBtn();
        initView();
        initData();
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            ActionBar ab = getSupportActionBar();
            if (ab != null) {
                ab.setTitle(getString(R.string.action_restore_btc_account));
            }
        }
    }

    private void initView() {
        checkBoxReadProtocol.setOnCheckedChangeListener((buttonView, isChecked) -> btnImportAccount.setEnabled(isChecked));

        tvService.setOnClickListener(v -> {
            Intent intent = new Intent(this, ServiceTermsActivity.class);
            startActivity(intent);
        });

        tvPrivacyPolicy.setOnClickListener(v -> {
            Intent intent = new Intent(this, PrivacyPolicyActivity.class);
            startActivity(intent);
        });

        btnImportAccount.setOnClickListener(view -> restoreAccount());
    }

    private void initData() {
        accounts = MainService.getInstance().getBitcoinAccounts();
    }

    private void restoreAccount() {
        btnImportAccount.setEnabled(false);
        // Reset errors.
        etAccountName.setError(null);
        etPassword.setError(null);
        etRepeatPassword.setError(null);

        // Store values at the time of the create account.
        String mnemonics = etMnemonic.getText().toString().trim();
        String name = etAccountName.getText().toString().trim();
        String password = etPassword.getText().toString();
        String repeatPassword = etRepeatPassword.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid mnemonics.
        if (TextUtils.isEmpty(mnemonics)) {
            focusView = etMnemonic;
            Toast.makeText(this, R.string.error_field_required, Toast.LENGTH_LONG).show();
            cancel = true;
        }

        // Check for a valid account name.
        if (!cancel && TextUtils.isEmpty(name)) {
            etAccountName.setError(getString(R.string.error_field_required));
            focusView = etAccountName;
            cancel = true;
        }

        if (!cancel && accounts != null && accounts.size() > 0) {
            for (AccountEntity accountEntity : accounts) {
                if (accountEntity.getName().equals(name)) {
                    cancel = true;
                    break;
                }
            }
            if (cancel) {
                etAccountName.setError(getString(R.string.error_incorrect_name));
                focusView = etAccountName;
            }
        }

        // Check for a valid password, if the user entered one.
        if (!cancel && !password.equals(repeatPassword)) {
            etPassword.setError(getString(R.string.error_incorrect_password));
            focusView = etPassword;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
            btnImportAccount.setEnabled(true);
            return;
        }

        // check the private key valid
        if (CommonUtil.isValidMnemonics(mnemonics)) {
            customProgressDialog = new CustomProgressDialog(this,
                    R.style.CustomProgressDialogStyle,
                    getString(R.string.progress_import_account));
            customProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            customProgressDialog.setCancelable(false);
            customProgressDialog.show();
            BtcAccountManager.getInstance().importBtcAccountWithMnemonics(name, password, mnemonics)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<WalletData>() {

                        @Override
                        public void onCompleted() {
                            if (customProgressDialog != null) {
                                customProgressDialog.cancel();
                            }
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            if (customProgressDialog != null) {
                                customProgressDialog.cancel();
                            }
                            throwable.printStackTrace();
                            showLongToast(R.string.error_mnemonics);
                            etMnemonic.requestFocus();
                            btnImportAccount.setEnabled(true);
                        }

                        @Override
                        public void onNext(WalletData walletData) {
                            if (customProgressDialog != null) {
                                customProgressDialog.cancel();
                            }

                            if (walletData != null) {
                                // hide soft input board
                                RxEventBus.get().post(EventTypeDef.CHANGE_BTC_ACCOUNT, false);
                                getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
                                showLongToast(R.string.success_import_account);
                                Intent intent = new Intent();
                                setResult(Activity.RESULT_OK, intent);
                                finish();
                            } else {
                                showLongToast(R.string.error_mnemonics);
                                etMnemonic.requestFocus();
                                btnImportAccount.setEnabled(true);
                            }
                        }
                    });

        } else {
            showLongToast(R.string.error_mnemonics);
            etMnemonic.requestFocus();
            btnImportAccount.setEnabled(true);
        }
    }
}
