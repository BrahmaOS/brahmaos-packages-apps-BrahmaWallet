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
import android.widget.RelativeLayout;
import android.widget.TextView;

import brahmaos.app.WalletManager;
import io.brahmaos.wallet.brahmawallet.R;
import io.brahmaos.wallet.brahmawallet.common.BrahmaConst;
import io.brahmaos.wallet.brahmawallet.common.IntentParam;
import io.brahmaos.wallet.brahmawallet.common.ReqCode;
import io.brahmaos.wallet.brahmawallet.db.entity.AccountEntity;
import io.brahmaos.wallet.brahmawallet.event.EventTypeDef;
import io.brahmaos.wallet.brahmawallet.service.BrahmaWeb3jService;
import io.brahmaos.wallet.brahmawallet.service.EthAccountManager;
import io.brahmaos.wallet.brahmawallet.service.ImageManager;
import io.brahmaos.wallet.brahmawallet.service.MainService;
import io.brahmaos.wallet.brahmawallet.ui.base.BaseActivity;
import io.brahmaos.wallet.brahmawallet.view.CustomProgressDialog;
import io.brahmaos.wallet.util.BLog;
import io.brahmaos.wallet.util.CommonUtil;
import io.brahmaos.wallet.util.RxEventBus;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class EthAccountDetailActivity extends BaseActivity {

    @Override
    protected String tag() {
        return EthAccountDetailActivity.class.getName();
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

        account = MainService.getInstance().getEthereumAccountByAddress(accountAddress);
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
            intent.putExtra(IntentParam.PARAM_ACCOUNT_INFO, account);
            startActivityForResult(intent, ReqCode.CHANGE_ACCOUNT_NAME);
        });

        layoutAccountAddress.setOnClickListener(v -> {
            Intent intent = new Intent(EthAccountDetailActivity.this, AddressQrcodeActivity.class);
            intent.putExtra(IntentParam.PARAM_ACCOUNT_INFO, account);
            startActivity(intent);
        });

        layoutAccountAddressQRCode.setOnClickListener(v -> {
            Intent intent = new Intent(EthAccountDetailActivity.this, AddressQrcodeActivity.class);
            intent.putExtra(IntentParam.PARAM_ACCOUNT_INFO, account);
            startActivity(intent);
        });

        tvChangePassword.setOnClickListener(v -> {
            Intent intent = new Intent(this, AccountChangePasswordActivity.class);
            intent.putExtra(IntentParam.PARAM_ACCOUNT_INFO, account);
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
        if (account.isDefault()) {
            tvDeleteAccount.setVisibility(View.GONE);
        }
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
        EthAccountManager.getInstance()
                .getKeystoreByPassword(account.getAddress(), password)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onNext(String keystore) {
                        if (progressDialog != null) {
                            progressDialog.cancel();
                        }
                        if (keystore != null && keystore.length() > 0) {
                            Intent intent = new Intent(EthAccountDetailActivity.this, BackupKeystoreActivity.class);
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
        EthAccountManager.getInstance()
                .getPrivateKeyByPassword(account.getAddress(), password)
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
        EthAccountManager.getInstance()
                .getPrivateKeyByPassword(account.getAddress(), password)
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
                        if (progressDialog != null) {
                            progressDialog.cancel();
                        }
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
        EthAccountManager.getInstance().deleteEthereumAccount(account.getAddress())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onNext(Integer ret) {
                        if (progressDialog != null) {
                            progressDialog.cancel();
                        }
                        if (ret == WalletManager.CODE_NO_ERROR) {
                            showLongToast(R.string.success_delete_account);
                            MainService.getInstance().loadAllAccounts();
                            RxEventBus.get().post(EventTypeDef.CHANGE_ETH_ACCOUNT);
                            finish();
                        } else {
                            showLongToast(R.string.error_delete_account);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        if (progressDialog != null) {
                            progressDialog.cancel();
                        }
                        showLongToast(R.string.error_delete_account);
                    }

                    @Override
                    public void onCompleted() {
                        if (progressDialog != null) {
                            progressDialog.cancel();
                        }
                    }
                });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        BLog.d(tag(), "requestCode: " + requestCode + "  ;resultCode" + resultCode);
        if (requestCode == ReqCode.CHANGE_ACCOUNT_NAME) {
            if (resultCode == RESULT_OK) {
                account.setName(data.getStringExtra(IntentParam.PARAM_ACCOUNT_NAME));
                tvAccountName.setText(account.getName());
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
}
