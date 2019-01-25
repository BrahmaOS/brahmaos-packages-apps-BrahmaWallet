package io.brahmaos.wallet.brahmawallet.ui.transfer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.web3j.crypto.CipherException;
import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import io.brahmaos.wallet.brahmawallet.R;
import io.brahmaos.wallet.brahmawallet.common.BrahmaConst;
import io.brahmaos.wallet.brahmawallet.common.IntentParam;
import io.brahmaos.wallet.brahmawallet.common.ReqCode;
import io.brahmaos.wallet.brahmawallet.db.entity.AccountEntity;
import io.brahmaos.wallet.brahmawallet.db.entity.TokenEntity;
import io.brahmaos.wallet.brahmawallet.model.AccountAssets;
import io.brahmaos.wallet.brahmawallet.service.BrahmaWeb3jService;
import io.brahmaos.wallet.brahmawallet.service.EthAccountManager;
import io.brahmaos.wallet.brahmawallet.service.ImageManager;
import io.brahmaos.wallet.brahmawallet.service.MainService;
import io.brahmaos.wallet.brahmawallet.ui.base.BaseActivity;
import io.brahmaos.wallet.brahmawallet.ui.common.barcode.CaptureActivity;
import io.brahmaos.wallet.brahmawallet.ui.common.barcode.Intents;
import io.brahmaos.wallet.brahmawallet.view.CustomStatusView;
import io.brahmaos.wallet.util.BLog;
import io.brahmaos.wallet.util.CommonUtil;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class EthTransferActivity extends BaseActivity {

    @Override
    protected String tag() {
        return EthTransferActivity.class.getName();
    }

    // UI references.
    private ImageView ivAccountAvatar;
    private TextView tvAccountName;
    private TextView tvAccountAddress;
    private TextView tvChangeAccount;

    private TextView tvEthBalance;
    private RelativeLayout layoutSendTokenBalance;
    private TextView tvSendTokenName;
    private TextView tvSendTokenBalance;

    private Button btnShowTransfer;
    private EditText etReceiverAddress;
    private EditText etAmount;
    private TextInputLayout layoutRemarkInput;
    private EditText etRemark;
    private EditText etGasPrice;
    private EditText etGasLimit;
    private ImageView ivContacts;

    private AccountEntity mAccount;
    private TokenEntity mToken;
    private List<AccountEntity> mAccounts = new ArrayList<>();
    private List<AccountAssets> mAccountAssetsList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer);
        showNavBackBtn();
        mAccount = (AccountEntity) getIntent().getSerializableExtra(IntentParam.PARAM_ACCOUNT_INFO);
        mToken = (TokenEntity) getIntent().getSerializableExtra(IntentParam.PARAM_TOKEN_INFO);

        if (mToken == null) {
            finish();
        }
        initView();
        initData();
    }

    private void initView() {
        ivAccountAvatar = findViewById(R.id.iv_account_avatar);
        tvAccountName = findViewById(R.id.tv_account_name);
        tvAccountAddress = findViewById(R.id.tv_account_address);
        tvChangeAccount = findViewById(R.id.tv_change_account);

        tvEthBalance = findViewById(R.id.tv_eth_balance);
        layoutSendTokenBalance = findViewById(R.id.layout_send_token_balance);
        tvSendTokenName = findViewById(R.id.tv_send_token_name);
        tvSendTokenBalance = findViewById(R.id.tv_send_token_balance);

        btnShowTransfer = findViewById(R.id.btn_show_transfer_info);
        etReceiverAddress = findViewById(R.id.et_receiver_address);
        etAmount = findViewById(R.id.et_amount);
        layoutRemarkInput = findViewById(R.id.layout_text_input_remark);
        etRemark = findViewById(R.id.et_remark);
        etGasPrice = findViewById(R.id.et_gas_price);
        etGasLimit = findViewById(R.id.et_gas_limit);
        ivContacts = findViewById(R.id.iv_contacts);
    }

    private void initData() {
        String tokenShortName = mToken.getShortName();
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            ActionBar ab = getSupportActionBar();
            if (ab != null) {
                ab.setTitle(tokenShortName + getString(R.string.blank_space) +
                        getString(R.string.action_transfer));
            }
        }

        if (mToken.getName().toLowerCase().equals(BrahmaConst.ETHEREUM)) {
            layoutSendTokenBalance.setVisibility(View.GONE);
            layoutRemarkInput.setVisibility(View.VISIBLE);
        } else {
            layoutSendTokenBalance.setVisibility(View.VISIBLE);
            tvSendTokenName.setText(mToken.getShortName());
            layoutRemarkInput.setVisibility(View.GONE);
        }

        mAccountAssetsList = MainService.getInstance().getAccountAssetsList();

        mAccounts = MainService.getInstance().getEthereumAccounts();
        if (mAccounts == null || mAccounts.size() <= 0) {
            finish();
        }

        if (mAccounts.size() > 1) {
            tvChangeAccount.setVisibility(View.VISIBLE);
        } else {
            tvChangeAccount.setVisibility(View.GONE);
        }

        if (mAccount == null || mAccount.getAddress().length() <= 0) {
            for (AccountEntity accountEntity : mAccounts) {
                if (accountEntity.isDefault()) {
                    mAccount = accountEntity;
                    break;
                }
            }
        }
        showAccountInfo(mAccount);

        tvChangeAccount.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_account_list, null);
            builder.setView(dialogView);
            builder.setCancelable(true);
            final AlertDialog alertDialog = builder.create();
            alertDialog.show();

            LinearLayout layoutAccountList = dialogView.findViewById(R.id.layout_accounts);

            for (final AccountEntity account : mAccounts) {
                final AccountItemView accountItemView = new AccountItemView();
                accountItemView.layoutAccountItem = LayoutInflater.from(this).inflate(R.layout.dialog_list_item_account, null);
                accountItemView.ivAccountAvatar = accountItemView.layoutAccountItem.findViewById(R.id.iv_account_avatar);
                accountItemView.tvAccountName = accountItemView.layoutAccountItem.findViewById(R.id.tv_account_name);
                accountItemView.tvAccountAddress = accountItemView.layoutAccountItem.findViewById(R.id.tv_account_address);
                accountItemView.layoutDivider = accountItemView.layoutAccountItem.findViewById(R.id.layout_divider);

                accountItemView.tvAccountName.setText(account.getName());
                ImageManager.showAccountAvatar(this, accountItemView.ivAccountAvatar, account);
                accountItemView.tvAccountAddress.setText(CommonUtil.generateSimpleAddress(account.getAddress()));

                accountItemView.layoutAccountItem.setOnClickListener(v1 -> {
                    alertDialog.cancel();
                    mAccount = account;
                    showAccountInfo(account);
                });

                if (mAccounts.indexOf(account) == mAccounts.size() - 1) {
                    accountItemView.layoutDivider.setVisibility(View.GONE);
                }

                layoutAccountList.addView(accountItemView.layoutAccountItem);
            }
        });

        etGasPrice.setText(String.valueOf(BrahmaConst.DEFAULT_GAS_PRICE));
        etGasLimit.setText(String.valueOf(BrahmaConst.DEFAULT_GAS_LIMIT));
        btnShowTransfer.setOnClickListener(v -> showTransferInfo());
        getGasPrice();

        ivContacts.setVisibility(View.GONE);
        ivContacts.setOnClickListener(v -> {

        });
    }

    public void getGasPrice() {
        EthAccountManager.getInstance()
                .getEthereumGasPrice()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onNext(String gasPrice) {
                        BLog.d(tag(), "the gas price is: " + gasPrice);
                        BigDecimal gasPriceGwei = new BigDecimal(gasPrice);
                        etGasPrice.setText(String.valueOf(gasPriceGwei));
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onCompleted() {

                    }
                });
    }

    private void showAccountBalance() {
        for (AccountAssets assets : mAccountAssetsList) {
            if (assets.getAccountEntity().getAddress().toLowerCase().equals(mAccount.getAddress().toLowerCase())) {
                if (assets.getTokenEntity().getAddress().toLowerCase().equals(mToken.getAddress().toLowerCase())) {
                    tvSendTokenBalance.setText(String.valueOf(CommonUtil.getAccountFromWei(assets.getBalance())));
                }
                if (assets.getTokenEntity().getName().toLowerCase().equals(BrahmaConst.ETHEREUM.toLowerCase())) {
                    tvEthBalance.setText(String.valueOf(CommonUtil.getAccountFromWei(assets.getBalance())));
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_transfer, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_scan) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                requestCameraScanPermission();
            } else {
                scanAddressCode();
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void scanAddressCode() {
        Intent intent = new Intent(this, CaptureActivity.class);
        intent.putExtra(Intents.Scan.PROMPT_MESSAGE, "");
        startActivityForResult(intent, ReqCode.SCAN_QR_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        BLog.d(tag(), "requestCode: " + requestCode + "  ;resultCode" + resultCode);
        if (requestCode == ReqCode.SCAN_QR_CODE) {
            if (resultCode == RESULT_OK) {
                if (data != null) {
                    String qrCode = data.getStringExtra(Intents.Scan.RESULT);
                    if (qrCode != null && qrCode.length() > 0) {
                        etReceiverAddress.setText(qrCode);
                    } else {
                        showLongToast(R.string.tip_scan_code_failed);
                    }
                }
            }
        } else if (requestCode == ReqCode.CHOOSE_TRANSFER_CONTACT) {
            if (resultCode == RESULT_OK) {
                if (data != null) {
                    String address = data.getStringExtra(IntentParam.PARAM_CONTACT_ADDRESS);
                    if (address != null && address.length() > 0) {
                        etReceiverAddress.setText(address);
                    }
                }
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void handleCameraScanPermission() {
        scanAddressCode();
    }

    private void showAccountInfo(AccountEntity account) {
        if (account != null) {
            ImageManager.showAccountAvatar(this, ivAccountAvatar, account);
            tvAccountName.setText(account.getName());
            tvAccountAddress.setText(CommonUtil.generateSimpleAddress(account.getAddress()));
            showAccountBalance();
        }
    }

    private class AccountItemView {
        View layoutAccountItem;
        ImageView ivAccountAvatar;
        TextView tvAccountName;
        TextView tvAccountAddress;
        LinearLayout layoutDivider;
    }

    private void showTransferInfo() {
        String receiverAddress = etReceiverAddress.getText().toString().trim();
        String transferAmount = etAmount.getText().toString().trim();
        String remark = etRemark.getText().toString().trim();
        BigDecimal amount = BigDecimal.ZERO;
        String gasPriceStr = etGasPrice.getText().toString().trim();
        String gasLimitStr = etGasLimit.getText().toString().trim();

        String tips = "";
        boolean cancel = false;
        if (!BrahmaWeb3jService.getInstance().isValidAddress(receiverAddress)) {
            tips = getString(R.string.tip_error_address);
            cancel = true;
        }
        if (!cancel && receiverAddress.equals(mAccount.getAddress())) {
            tips = getString(R.string.tip_same_address);
            cancel = true;
        }

        if (!cancel && gasPriceStr.length() < 1) {
            tips = getString(R.string.tip_invalid_gas_price);
            cancel = true;
        }

        if (!cancel && gasLimitStr.length() < 1) {
            tips = getString(R.string.tip_invalid_gas_limit);
            cancel = true;
        }

        BigInteger totalBalance = BigInteger.ZERO;
        BigInteger ethTotalBalance = BigInteger.ZERO;
        for (AccountAssets assets : mAccountAssetsList) {
            if (assets.getAccountEntity().getAddress().equals(mAccount.getAddress())) {
                if (assets.getTokenEntity().getAddress().equals(mToken.getAddress())) {
                    totalBalance = assets.getBalance();
                }
                if (assets.getTokenEntity().getName().toLowerCase().equals(BrahmaConst.ETHEREUM.toLowerCase())) {
                    ethTotalBalance = assets.getBalance();
                }
            }
        }

        if (!cancel) {
            if (transferAmount.length() < 1) {
                tips = getString(R.string.tip_invalid_amount);
                cancel = true;
            } else {
                amount = new BigDecimal(transferAmount);
            }
        }

        if (!cancel && (amount.compareTo(BigDecimal.ZERO) <= 0 ||
                CommonUtil.convertWeiFromEther(amount).compareTo(totalBalance) > 0)) {
            tips = getString(R.string.tip_invalid_amount);
            cancel = true;
        }

        if (!cancel && ethTotalBalance.compareTo(BigInteger.ZERO) <= 0) {
            tips = getString(R.string.tip_insufficient_eth);
            cancel = true;
        }

        // check the ether is enough
        if (!cancel && mToken.getName().toLowerCase().equals(BrahmaConst.ETHEREUM)) {
            totalBalance = ethTotalBalance;
            if (CommonUtil.convertWeiFromEther(amount.add(BrahmaConst.DEFAULT_FEE)).compareTo(totalBalance) > 0) {
                tips = getString(R.string.tip_insufficient_eth);
                cancel = true;
            }
        }

        if (cancel) {
            // dialog show tip
            AlertDialog dialogTip = new AlertDialog.Builder(this)
                    .setMessage(tips)
                    .setNegativeButton(R.string.ok, (dialog, which) -> dialog.cancel())
                    .create();
            dialogTip.show();
            return;
        }

        BigDecimal gasPrice = new BigDecimal(gasPriceStr);
        BigInteger gasLimit = new BigInteger(gasLimitStr);

        final BottomSheetDialog transferInfoDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_dialog_transfer_info, null);
        transferInfoDialog.setContentView(view);
        transferInfoDialog.setCancelable(false);
        transferInfoDialog.show();

        ImageView ivCloseDialog = view.findViewById(R.id.iv_close_dialog);
        ivCloseDialog.setOnClickListener(v -> transferInfoDialog.cancel());

        TextView tvDialogPayToAddress = view.findViewById(R.id.tv_pay_to_address);
        tvDialogPayToAddress.setText(receiverAddress);

        TextView tvDialogPayByAddress = view.findViewById(R.id.tv_pay_by_address);
        tvDialogPayByAddress.setText(CommonUtil.generateSimpleAddress(mAccount.getAddress()));

        TextView tvGasPrice = view.findViewById(R.id.tv_gas_price);
        tvGasPrice.setText(gasPriceStr);
        TextView tvGasLimit = view.findViewById(R.id.tv_gas_limit);
        tvGasLimit.setText(gasLimitStr);
        TextView tvGasValue = view.findViewById(R.id.tv_gas_value);
        BigDecimal gasValue = Convert.fromWei(Convert.toWei(new BigDecimal(gasLimit).multiply(gasPrice), Convert.Unit.GWEI), Convert.Unit.ETHER);
        tvGasValue.setText(String.valueOf(gasValue.setScale(9, BigDecimal.ROUND_HALF_UP)));

        TextView tvTransferAmount = view.findViewById(R.id.tv_dialog_transfer_amount);
        tvTransferAmount.setText(String.valueOf(amount));

        TextView tvTransferToken = view.findViewById(R.id.tv_dialog_transfer_token);
        tvTransferToken.setText(mToken.getShortName());

        LinearLayout layoutTransferStatus = view.findViewById(R.id.layout_transfer_status);
        CustomStatusView customStatusView = view.findViewById(R.id.as_status);
        TextView tvTransferStatus = view.findViewById(R.id.tv_transfer_status);
        Button confirmBtn = view.findViewById(R.id.btn_commit_transfer);
        BigDecimal finalAmount = amount;
        confirmBtn.setOnClickListener(v -> {
            final View dialogView = getLayoutInflater().inflate(R.layout.dialog_account_password, null);

            AlertDialog passwordDialog = new AlertDialog.Builder(EthTransferActivity.this)
                    .setView(dialogView)
                    .setCancelable(false)
                    .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel())
                    .setPositiveButton(R.string.ok, (dialog, which) -> {
                        dialog.cancel();
                        // show transfer progress
                        layoutTransferStatus.setVisibility(View.VISIBLE);
                        customStatusView.loadLoading();
                        String password = ((EditText) dialogView.findViewById(R.id.et_password)).getText().toString();

                        tvTransferStatus.setText(R.string.progress_send_request);
                        EthAccountManager.getInstance().sendTransfer(mAccount, mToken, password, receiverAddress,
                                finalAmount, gasPrice, gasLimit, remark)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(new Observer<String>() {
                                    @Override
                                    public void onNext(String txHash) {
                                        if (txHash != null && txHash.length() > 0) {
                                            BLog.d(tag(), "the transfer hash is: " + txHash);
                                            tvTransferStatus.setText(R.string.progress_transfer_success);
                                            BLog.i(tag(), "the transfer success");
                                            customStatusView.loadSuccess();
                                            new Handler().postDelayed(() -> {
                                                transferInfoDialog.cancel();
                                                // Eth transfer is a real-time arrival, and token transfer may take longer,
                                                // so there is no need to refresh
                                                finish();
                                            }, 1200);
                                        } else {
                                            customStatusView.loadFailure();
                                            tvTransferStatus.setText(R.string.progress_transfer_fail);
                                            new Handler().postDelayed(() -> {
                                                layoutTransferStatus.setVisibility(View.GONE);
                                                int resId = R.string.tip_error_transfer;
                                                new AlertDialog.Builder(EthTransferActivity.this)
                                                        .setMessage(resId)
                                                        .setNegativeButton(R.string.ok, (dialog1, which1) -> dialog1.cancel())
                                                        .create().show();
                                            }, 1500);

                                            BLog.i(tag(), "the transfer failed");
                                        }
                                    }

                                    @Override
                                    public void onError(Throwable e) {
                                        e.printStackTrace();
                                        customStatusView.loadFailure();
                                        tvTransferStatus.setText(R.string.progress_transfer_fail);
                                        new Handler().postDelayed(() -> {
                                            layoutTransferStatus.setVisibility(View.GONE);
                                            int resId = R.string.tip_error_transfer;
                                            if (e instanceof CipherException) {
                                                resId = R.string.tip_error_password;
                                            }
                                            new AlertDialog.Builder(EthTransferActivity.this)
                                                    .setMessage(resId)
                                                    .setNegativeButton(R.string.ok, (dialog1, which1) -> dialog1.cancel())
                                                    .create().show();
                                        }, 1500);

                                        BLog.i(tag(), "the transfer failed");
                                    }

                                    @Override
                                    public void onCompleted() {
                                    }
                                });
                        })
                    .create();
            passwordDialog.show();
        });
    }

}
