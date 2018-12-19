package io.brahmaos.wallet.brahmawallet.ui.account;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import com.google.common.base.Splitter;

import java.util.List;

import brahmaos.app.WalletManager;
import io.brahmaos.wallet.brahmawallet.R;
import io.brahmaos.wallet.brahmawallet.common.IntentParam;
import io.brahmaos.wallet.brahmawallet.db.entity.AccountEntity;
import io.brahmaos.wallet.brahmawallet.service.BtcAccountManager;
import io.brahmaos.wallet.brahmawallet.service.MainService;
import io.brahmaos.wallet.brahmawallet.ui.base.BaseActivity;
import io.brahmaos.wallet.brahmawallet.view.CustomProgressDialog;
import io.brahmaos.wallet.brahmawallet.viewmodel.AccountViewModel;
import io.brahmaos.wallet.util.BLog;
import io.brahmaos.wallet.util.CommonUtil;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * A login screen that offers login via email/password.
 */
public class BtcAccountChangePasswordActivity extends BaseActivity {

    // UI references.
    EditText etCurrentPassword;
    EditText etNewPassword;
    EditText etRepeatPassword;

    private String accountAddress;
    private AccountEntity account;
    private CustomProgressDialog progressDialog;

    @Override
    protected String tag() {
        return BtcAccountChangePasswordActivity.class.getName();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_btc_account_change_password);
        showNavBackBtn();
        etCurrentPassword = findViewById(R.id.et_current_password);
        etNewPassword = findViewById(R.id.et_new_password);
        etRepeatPassword = findViewById(R.id.et_repeat_new_password);
        accountAddress = getIntent().getStringExtra(IntentParam.PARAM_ACCOUNT_ADDRESS);
        if (accountAddress == null || accountAddress.length() <= 0) {
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        account = MainService.getInstance().getBitcoinAccountByAddress(accountAddress);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_save, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //noinspection SimplifiableIfStatement
        if (item.getItemId() == R.id.action_save) {
            String currentPassword = etCurrentPassword.getText().toString();
            String newPassword = etNewPassword.getText().toString();
            String repeatPassword = etRepeatPassword.getText().toString();

            if (!CommonUtil.isPasswordValid(newPassword)) {
                etNewPassword.setError(getString(R.string.error_invalid_password));
                etNewPassword.requestFocus();
                return false;
            }

            if (!newPassword.equals(repeatPassword)) {
                etNewPassword.setError(getString(R.string.error_incorrect_password));
                etNewPassword.requestFocus();
                return false;
            }
            BtcAccountManager.getInstance()
                    .updateAccountPassword(accountAddress, currentPassword, newPassword)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<Integer>() {
                        @Override
                        public void onNext(Integer ret) {
                            if (progressDialog != null) {
                                progressDialog.cancel();
                            }
                            if (ret == WalletManager.CODE_NO_ERROR) {
                                MainService.getInstance().loadAllAccounts();
                                account = MainService.getInstance().getBitcoinAccountByAddress(accountAddress);
                                showLongToast(R.string.success_change_password);
                                finish();
                            } else if (ret == WalletManager.CODE_ERROR_PASSWORD) {
                                etCurrentPassword.setError(getString(R.string.error_current_password));
                                etCurrentPassword.requestFocus();
                            } else {
                                showLongToast(R.string.error_change_password);
                            }
                        }

                        @Override
                        public void onError(Throwable e) {
                            e.printStackTrace();
                            progressDialog.cancel();
                            showLongToast(R.string.error_change_password);
                        }

                        @Override
                        public void onCompleted() {

                        }
                    });
        }
        return super.onOptionsItemSelected(item);
    }
}

