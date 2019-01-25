package io.brahmaos.wallet.brahmawallet.ui.account;

import android.app.ProgressDialog;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.common.base.Splitter;

import org.bitcoinj.kits.WalletAppKit;

import java.util.List;

import brahmaos.util.DataCryptoUtils;
import io.brahmaos.wallet.brahmawallet.R;
import io.brahmaos.wallet.brahmawallet.common.IntentParam;
import io.brahmaos.wallet.brahmawallet.common.ReqCode;
import io.brahmaos.wallet.brahmawallet.db.entity.AccountEntity;
import io.brahmaos.wallet.brahmawallet.service.BrahmaWeb3jService;
import io.brahmaos.wallet.brahmawallet.service.BtcAccountManager;
import io.brahmaos.wallet.brahmawallet.service.ImageManager;
import io.brahmaos.wallet.brahmawallet.service.MainService;
import io.brahmaos.wallet.brahmawallet.ui.base.BaseActivity;
import io.brahmaos.wallet.brahmawallet.view.CustomProgressDialog;
import io.brahmaos.wallet.brahmawallet.viewmodel.AccountViewModel;
import io.brahmaos.wallet.util.BLog;
import io.brahmaos.wallet.util.CommonUtil;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class BtcAccountDetailActivity extends BaseActivity {

    @Override
    protected String tag() {
        return BtcAccountDetailActivity.class.getName();
    }

    // UI references.
    private ImageView ivAccountAvatar;
    private RelativeLayout layoutAccountName;
    private TextView tvAccountName;
    private RelativeLayout layoutAccountAddress;
    private TextView tvAccountAddress;
    private RelativeLayout layoutAccountAddressQRCode;
    private TextView tvChangePassword;
    private TextView tvExportMnemonics;

    private String accountAddress;
    private AccountEntity account;
    private CustomProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_btc_account_detail);
        ivAccountAvatar = findViewById(R.id.iv_account_avatar);
        layoutAccountName = findViewById(R.id.layout_account_name);
        tvAccountName = findViewById(R.id.tv_account_name);
        layoutAccountAddress = findViewById(R.id.layout_account_address);
        tvAccountAddress = findViewById(R.id.tv_account_address);
        layoutAccountAddressQRCode = findViewById(R.id.layout_account_address_qrcode);
        tvChangePassword = findViewById(R.id.tv_change_password);
        tvExportMnemonics = findViewById(R.id.tv_export_mnemonics);
        showNavBackBtn();
        accountAddress = getIntent().getStringExtra(IntentParam.PARAM_ACCOUNT_ADDRESS);
        BLog.d(tag(), "the account address is: " + accountAddress);
        if (accountAddress == null) {
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        progressDialog = new CustomProgressDialog(this, R.style.CustomProgressDialogStyle, "");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setCancelable(false);

        account = MainService.getInstance().getBitcoinAccountByAddress(accountAddress);
        initAccountInfo(account);
    }

    private void initAccountInfo(AccountEntity account) {
        ImageManager.showAccountAvatar(this, ivAccountAvatar, account);
        tvAccountName.setText(account.getName());

        tvAccountAddress.setText(CommonUtil.generateSimpleAddress(BtcAccountManager.getInstance().getBtcCurrentReceiveAddress(accountAddress)));

        layoutAccountName.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChangeAccountNameActivity.class);
            intent.putExtra(IntentParam.PARAM_ACCOUNT_INFO, account);
            startActivityForResult(intent, ReqCode.CHANGE_ACCOUNT_NAME);
        });

        layoutAccountAddress.setOnClickListener(v -> {
            Intent intent = new Intent(BtcAccountDetailActivity.this, BtcAddressQrcodeActivity.class);
            intent.putExtra(IntentParam.PARAM_ACCOUNT_ADDRESS, account.getAddress());
            startActivity(intent);
        });

        layoutAccountAddressQRCode.setOnClickListener(v -> {
            Intent intent = new Intent(BtcAccountDetailActivity.this, BtcAddressQrcodeActivity.class);
            intent.putExtra(IntentParam.PARAM_ACCOUNT_ADDRESS, account.getAddress());
            startActivity(intent);
        });

        tvChangePassword.setOnClickListener(v -> {
            Intent intent = new Intent(this, BtcAccountChangePasswordActivity.class);
            intent.putExtra(IntentParam.PARAM_ACCOUNT_ADDRESS, account.getAddress());
            startActivity(intent);
        });
        tvExportMnemonics.setOnClickListener(v -> {
            final View dialogView = getLayoutInflater().inflate(R.layout.dialog_account_password, null);
            final EditText etPassword = dialogView.findViewById(R.id.et_password);
            AlertDialog passwordDialog = new AlertDialog.Builder(this, R.style.Theme_AppCompat_Light_Dialog_Alert_Self)
                    .setView(dialogView)
                    .setCancelable(true)
                    .setPositiveButton(R.string.confirm, (dialog, which) -> {
                        dialog.cancel();
                        String password = etPassword.getText().toString();
                        exportMnemonics(password);
                    })
                    .create();
            passwordDialog.setOnShowListener(dialog -> {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(etPassword, InputMethodManager.SHOW_IMPLICIT);
            });
            passwordDialog.show();
        });
    }

    private void exportMnemonics(String password) {
        BLog.d(tag(), "the encrypt mnemonics is: " + account.getCryptoMnemonics());
        String mnemonicsCode = DataCryptoUtils.aes128Decrypt(account.getCryptoMnemonics(), password);
        if (mnemonicsCode != null) {
            List<String> mnemonicsCodes = Splitter.on(" ").splitToList(mnemonicsCode);
            if (mnemonicsCodes.size() == 0 || mnemonicsCodes.size() % 3 > 0) {
                showPasswordErrorDialog();
            } else {
                final View dialogView = getLayoutInflater().inflate(R.layout.dialog_export_mnemonics, null);
                TextView tvKeystore = dialogView.findViewById(R.id.tv_dialog_mnemonics);
                tvKeystore.setText(mnemonicsCode);
                AlertDialog privateKeyDialog = new AlertDialog.Builder(this)
                        .setView(dialogView)
                        .setCancelable(false)
                        .setNegativeButton(R.string.cancel, ((dialog, which) -> {
                            dialog.cancel();
                        }))
                        .setPositiveButton(R.string.copy, (dialog, which) -> {
                            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                            cm.setText(mnemonicsCode);
                            showLongToast(R.string.tip_success_copy);
                        })
                        .create();
                privateKeyDialog.show();
            }
        } else {
            showPasswordErrorDialog();
        }
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
