package io.brahmaos.wallet.brahmawallet.ui.account;

import android.app.ProgressDialog;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import io.brahmaos.wallet.brahmawallet.R;
import io.brahmaos.wallet.brahmawallet.common.BrahmaConst;
import io.brahmaos.wallet.brahmawallet.common.IntentParam;
import io.brahmaos.wallet.brahmawallet.db.entity.AccountEntity;
import io.brahmaos.wallet.brahmawallet.service.BrahmaWeb3jService;
import io.brahmaos.wallet.brahmawallet.service.ImageManager;
import io.brahmaos.wallet.brahmawallet.service.MainService;
import io.brahmaos.wallet.brahmawallet.ui.base.BaseActivity;
import io.brahmaos.wallet.brahmawallet.view.CustomProgressDialog;
import io.brahmaos.wallet.util.BLog;
import io.brahmaos.wallet.util.CommonUtil;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class AccountDetailActivity extends BaseActivity {

    @Override
    protected String tag() {
        return AccountDetailActivity.class.getName();
    }

    // UI references.
    private ImageView ivAccountAvatar;
    private RelativeLayout layoutAccountName;
    private TextView tvAccountName;
    private RelativeLayout layoutAccountAddress;
    private TextView tvAccountAddress;
    private RelativeLayout layoutAccountAddressQRCode;
    private TextView tvChangePassword;
    private TextView tvExportPrivateKey;
    private TextView tvExportKeystore;
    private TextView tvDeleteAccount;

    private String accountAddress;
    private AccountEntity account;
    private CustomProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_detail);
        showNavBackBtn();
        initView();
        accountAddress = getIntent().getStringExtra(IntentParam.PARAM_ACCOUNT_ADDRESS);
        if (accountAddress == null || accountAddress.length() <= 0) {
            finish();
        }
    }

    private void initView() {
        ivAccountAvatar = findViewById(R.id.iv_account_avatar);
        layoutAccountName = findViewById(R.id.layout_account_name);
        tvAccountName = findViewById(R.id.tv_account_name);
        layoutAccountAddress = findViewById(R.id.layout_account_address);
        tvAccountAddress = findViewById(R.id.tv_account_address);
        layoutAccountAddressQRCode = findViewById(R.id.layout_account_address_qrcode);
        tvChangePassword = findViewById(R.id.tv_change_password);
        tvExportPrivateKey = findViewById(R.id.tv_export_private_key);
        tvExportKeystore = findViewById(R.id.tv_export_keystore);
        tvDeleteAccount = findViewById(R.id.tv_delete_account);
    }

    @Override
    protected void onStart() {
        super.onStart();

        progressDialog = new CustomProgressDialog(this, R.style.CustomProgressDialogStyle, "");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setCancelable(false);

        account = MainService.getInstance().getAccountByAddress(accountAddress);
        initAccountInfo(account);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

    private void initAccountInfo(AccountEntity account) {
        ImageManager.showAccountAvatar(this, ivAccountAvatar, account);
        tvAccountName.setText(account.getName());
        tvAccountAddress.setText(CommonUtil.generateSimpleAddress(account.getAddress()));
        if (account.getId() == BrahmaConst.DEFAULT_WALLET_ACCOUNT_ID) {
            tvChangePassword.setVisibility(View.GONE);
            tvDeleteAccount.setVisibility(View.GONE);
        }
        layoutAccountName.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChangeAccountNameActivity.class);
            intent.putExtra(IntentParam.PARAM_ACCOUNT_ID, account.getId());
            intent.putExtra(IntentParam.PARAM_ACCOUNT_NAME, account.getName());
            startActivity(intent);
        });

        layoutAccountAddress.setOnClickListener(v -> {
            Intent intent = new Intent(AccountDetailActivity.this, AddressQrcodeActivity.class);
            intent.putExtra(IntentParam.PARAM_ACCOUNT_INFO, account);
            startActivity(intent);
        });

        layoutAccountAddressQRCode.setOnClickListener(v -> {
            Intent intent = new Intent(AccountDetailActivity.this, AddressQrcodeActivity.class);
            intent.putExtra(IntentParam.PARAM_ACCOUNT_INFO, account);
            startActivity(intent);
        });

        tvChangePassword.setOnClickListener(v -> {
            Intent intent = new Intent(this, AccountChangePasswordActivity.class);
            intent.putExtra(IntentParam.PARAM_ACCOUNT_ID, account.getId());
            startActivity(intent);
        });
        tvExportKeystore.setOnClickListener(v -> {
            final View dialogView = getLayoutInflater().inflate(R.layout.dialog_account_password, null);
            AlertDialog passwordDialog = new AlertDialog.Builder(this)
                    .setView(dialogView)
                    .setCancelable(true)
                    .setPositiveButton(R.string.confirm, (dialog, which) -> {
                        dialog.cancel();
                        String password = ((EditText) dialogView.findViewById(R.id.et_password)).getText().toString();
                        exportKeystore(password);
                    })
                    .create();
            passwordDialog.show();
        });
        tvExportPrivateKey.setOnClickListener(v -> {
            final View dialogView = getLayoutInflater().inflate(R.layout.dialog_account_password, null);
            AlertDialog passwordDialog = new AlertDialog.Builder(this)
                    .setView(dialogView)
                    .setCancelable(true)
                    .setPositiveButton(R.string.confirm, (dialog, which) -> {
                        dialog.cancel();
                        dialog.dismiss();
                        String password = ((EditText) dialogView.findViewById(R.id.et_password)).getText().toString();
                        exportPrivateKey(password);
                    })
                    .create();
            passwordDialog.show();
        });
        tvDeleteAccount.setOnClickListener(v -> {
            final View dialogView = getLayoutInflater().inflate(R.layout.dialog_account_password, null);
            AlertDialog passwordDialog = new AlertDialog.Builder(this)
                    .setView(dialogView)
                    .setCancelable(true)
                    .setPositiveButton(R.string.confirm, (dialog, which) -> {
                        dialog.cancel();
                        String password = ((EditText) dialogView.findViewById(R.id.et_password)).getText().toString();
                        prepareDeleteAccount(password);
                    })
                    .create();
            passwordDialog.show();
        });
    }

    private void exportKeystore(String password) {
        if (progressDialog != null) {
            progressDialog.cancel();
        }
        BLog.d(tag(), "password-----------" + password);
        progressDialog.show();
        MainService.getInstance()
                .getKeystoreByPassword(account.getId(), password)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onNext(String keystore) {
                        if (progressDialog != null) {
                            progressDialog.cancel();
                        }
                        if (keystore != null && keystore.length() > 0) {
                            //showKeystoreDialog(keystore);
                            Intent intent = new Intent(AccountDetailActivity.this, BackupKeystoreActivity.class);
                            intent.putExtra(IntentParam.PARAM_OFFICIAL_KEYSTORE, keystore);
                            startActivity(intent);
                        } else {
                            showPasswordErrorDialog();;
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        if (progressDialog != null) {
                            progressDialog.cancel();
                        }
                        showPasswordErrorDialog();
                    }

                    @Override
                    public void onCompleted() {
                        if (progressDialog != null) {
                            progressDialog.cancel();
                        }
                    }
                });
    }

    private void exportPrivateKey(String password) {
        if (progressDialog != null) {
            progressDialog.cancel();
        }
        progressDialog.show();
        BLog.d(tag(), "password-----------" + password);
        MainService.getInstance()
                .getPrivateKeyByPassword(account.getId(), password)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onNext(String privateKey) {
                        BLog.d(tag(), "privateKey-----------" + privateKey);
                        if (progressDialog != null) {
                            progressDialog.cancel();
                        }
                        if (privateKey != null && BrahmaWeb3jService.getInstance().isValidPrivateKey(privateKey)) {
                            showPrivateKeyDialog(privateKey);
                        } else {
                            showPasswordErrorDialog();;
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        if (progressDialog != null) {
                            progressDialog.cancel();
                        }
                        showPasswordErrorDialog();
                    }

                    @Override
                    public void onCompleted() {
                        if (progressDialog != null) {
                            progressDialog.cancel();
                        }
                    }
                });
    }

    private void showPasswordErrorDialog() {
        AlertDialog errorDialog = new AlertDialog.Builder(this)
                .setMessage(R.string.error_current_password)
                .setCancelable(true)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    dialog.cancel();
                })
                .create();
        errorDialog.show();
    }

    private void showKeystoreDialog(String keystore) {
        final View dialogView = getLayoutInflater().inflate(R.layout.dialog_export_keystore, null);
        TextView tvKeystore = dialogView.findViewById(R.id.tv_dialog_keystore);
        tvKeystore.setText(keystore);
        AlertDialog keystoreDialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .setNegativeButton(R.string.cancel, ((dialog, which) -> {
                    dialog.cancel();
                }))
                .setPositiveButton(R.string.copy, (dialog, which) -> {
                    ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    cm.setText(keystore);
                    showLongToast(R.string.tip_success_copy);
                })
                .create();
        keystoreDialog.show();
    }

    private void showPrivateKeyDialog(String privateKey) {
        final View dialogView = getLayoutInflater().inflate(R.layout.dialog_export_private_key, null);
        TextView tvKeystore = dialogView.findViewById(R.id.tv_dialog_private_key);
        tvKeystore.setText(privateKey);
        AlertDialog privateKeyDialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .setNegativeButton(R.string.cancel, ((dialog, which) -> {
                    dialog.cancel();
                }))
                .setPositiveButton(R.string.copy, (dialog, which) -> {
                    ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    cm.setText(privateKey);
                    showLongToast(R.string.tip_success_copy);
                })
                .create();
        privateKeyDialog.show();
    }

    /**
     * Verify the correctness of the password and
     * allow the user to confirm again whether to delete the account
     * @param password
     */
    private void prepareDeleteAccount(String password) {
        progressDialog.show();
        BrahmaWeb3jService.getInstance()
                .getPrivateKeyByPassword(account.getFilename(), password)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onNext(String privateKey) {
                        if (progressDialog != null) {
                            progressDialog.cancel();
                        }
                        if (privateKey != null && BrahmaWeb3jService.getInstance().isValidPrivateKey(privateKey)) {
                            showConfirmDeleteAccountDialog();
                        } else {
                            showPasswordErrorDialog();;
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        if (progressDialog != null) {
                            progressDialog.cancel();
                        }
                        showPasswordErrorDialog();
                    }

                    @Override
                    public void onCompleted() {

                    }
                });
    }

    private void showConfirmDeleteAccountDialog() {
        AlertDialog deleteDialog = new AlertDialog.Builder(this)
                .setMessage(R.string.delete_account_tip)
                .setCancelable(false)
                .setNegativeButton(R.string.cancel, ((dialog, which) -> {
                    dialog.cancel();
                }))
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    deleteAccount();
                })
                .create();
        deleteDialog.show();
    }

    private void deleteAccount() {
        if (progressDialog != null) {
            progressDialog.show();
        }
        MainService.getInstance().deleteAccountByPassword(account.getId())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {
                            progressDialog.cancel();
                            showLongToast(R.string.success_delete_account);
                            finish();
                        },
                        throwable -> {
                            BLog.e(tag(), "Unable to delete account", throwable);
                            progressDialog.cancel();
                            showLongToast(R.string.error_delete_account);
                        });;
    }

}
