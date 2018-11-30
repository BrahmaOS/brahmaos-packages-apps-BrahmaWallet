package io.brahmaos.wallet.brahmawallet.ui.account;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import java.util.List;

import brahmaos.content.WalletData;
import io.brahmaos.wallet.brahmawallet.R;
import io.brahmaos.wallet.brahmawallet.db.entity.AccountEntity;
import io.brahmaos.wallet.brahmawallet.event.EventTypeDef;
import io.brahmaos.wallet.brahmawallet.service.EthAccountManager;
import io.brahmaos.wallet.brahmawallet.service.MainService;
import io.brahmaos.wallet.brahmawallet.ui.base.BaseActivity;
import io.brahmaos.wallet.brahmawallet.ui.setting.PrivacyPolicyActivity;
import io.brahmaos.wallet.brahmawallet.ui.setting.ServiceTermsActivity;
import io.brahmaos.wallet.brahmawallet.view.CustomProgressDialog;
import io.brahmaos.wallet.util.BLog;
import io.brahmaos.wallet.util.RxEventBus;
import rx.Completable;
import rx.CompletableSubscriber;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * A create eth account screen
 */
public class CreateEthAccountActivity extends BaseActivity {

    // UI references.
    private EditText etAccountName;
    private EditText etPassword;
    private EditText etRepeatPassword;
    private Button btnCreateAccount;
    private CheckBox checkBoxReadProtocol;
    private View formCreateAccount;
    private TextView tvServiceAgreement;
    private TextView tvPrivacyPolicy;

    private CustomProgressDialog customProgressDialog;
    private List<AccountEntity> accounts;

    @Override
    protected String tag() {
        return CreateEthAccountActivity.class.getName();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);
        etAccountName = findViewById(R.id.et_account_name);
        etPassword = findViewById(R.id.et_password);
        etRepeatPassword = findViewById(R.id.et_repeat_password);
        btnCreateAccount = findViewById(R.id.btn_create_account);
        checkBoxReadProtocol = findViewById(R.id.checkbox_read_protocol);
        formCreateAccount = findViewById(R.id.layout_create_account_form);
        tvServiceAgreement = findViewById(R.id.service_tv);
        tvPrivacyPolicy = findViewById(R.id.privacy_policy_tv);

        showNavBackBtn();
        accounts = MainService.getInstance().getEthereumAccounts();

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            ActionBar ab = getSupportActionBar();
            if (ab != null) {
                ab.setTitle(getString(R.string.action_create_eth_account));
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        btnCreateAccount.setOnClickListener(view -> createAccount());

        checkBoxReadProtocol.setOnCheckedChangeListener((buttonView, isChecked) -> btnCreateAccount.setEnabled(isChecked));

        tvServiceAgreement.setOnClickListener(v -> {
            Intent intent = new Intent(CreateEthAccountActivity.this, ServiceTermsActivity.class);
            startActivity(intent);
        });

        tvPrivacyPolicy.setOnClickListener(v -> {
            Intent intent = new Intent(CreateEthAccountActivity.this, PrivacyPolicyActivity.class);
            startActivity(intent);
        });
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid name, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void createAccount() {
        btnCreateAccount.setEnabled(false);
        // Reset errors.
        etAccountName.setError(null);
        etPassword.setError(null);
        etRepeatPassword.setError(null);

        // Store values at the time of the create account.
        String name = etAccountName.getText().toString();
        String password = etPassword.getText().toString();
        String repeatPassword = etRepeatPassword.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid account name.
        if (TextUtils.isEmpty(name)) {
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
        if (!cancel && (TextUtils.isEmpty(password) || !isPasswordValid(password))) {
            etPassword.setError(getString(R.string.error_invalid_password));
            focusView = etPassword;
            cancel = true;
        }

        if (!cancel && !password.equals(repeatPassword)) {
            etPassword.setError(getString(R.string.error_incorrect_password));
            focusView = etPassword;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
            btnCreateAccount.setEnabled(true);
            return;
        }
        customProgressDialog = new CustomProgressDialog(this, R.style.CustomProgressDialogStyle, getString(R.string.progress_create_account));
        customProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        customProgressDialog.setCancelable(false);
        customProgressDialog.show();
        try {
            EthAccountManager.getInstance().createEthAccount(name, password)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<WalletData>() {

                        @Override
                        public void onCompleted() {
                            if (customProgressDialog != null) {
                                customProgressDialog.dismiss();
                            }
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            if (customProgressDialog != null) {
                                customProgressDialog.dismiss();
                            }
                            throwable.printStackTrace();
                            BLog.d(tag(), "create eth account error");
                        }

                        @Override
                        public void onNext(WalletData walletData) {
                            if (customProgressDialog != null) {
                                customProgressDialog.dismiss();
                            }
                            if (walletData == null) {
                                showLongToast(R.string.error_create_account);
                            } else {
                                RxEventBus.get().post(EventTypeDef.CHANGE_ETH_ACCOUNT, false);
                                /*Intent intent = new Intent(CreateEthAccountActivity.this, AccountBackupActivity.class);
                                startActivity(intent);*/
                                finish();
                            }
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
            BLog.e(tag(), e.getMessage());
            customProgressDialog.cancel();
            btnCreateAccount.setEnabled(true);
            showLongToast(R.string.error_create_account);
        }
    }

    private boolean isPasswordValid(String password) {
        return password.length() > 4;
    }
}

