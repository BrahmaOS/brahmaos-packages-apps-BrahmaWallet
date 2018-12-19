package io.brahmaos.wallet.brahmawallet.ui.account;

import android.animation.ObjectAnimator;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.Locale;

import io.brahmaos.wallet.brahmawallet.R;
import io.brahmaos.wallet.brahmawallet.common.IntentParam;
import io.brahmaos.wallet.brahmawallet.db.entity.AccountEntity;
import io.brahmaos.wallet.brahmawallet.event.EventTypeDef;
import io.brahmaos.wallet.brahmawallet.service.BtcAccountManager;
import io.brahmaos.wallet.brahmawallet.service.ImageManager;
import io.brahmaos.wallet.brahmawallet.service.MainService;
import io.brahmaos.wallet.brahmawallet.ui.base.BaseActivity;
import io.brahmaos.wallet.brahmawallet.viewmodel.AccountViewModel;
import io.brahmaos.wallet.util.BitcoinPaymentURI;
import io.brahmaos.wallet.util.CommonUtil;
import io.brahmaos.wallet.util.QRCodeUtil;
import io.brahmaos.wallet.util.RxEventBus;
import rx.Observable;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class BtcAddressQrcodeActivity extends BaseActivity {

    private ImageView ivAccountAvatar;
    private TextView tvAccountName;
    private TextView tvMainAddress;
    private TextView tvChildAddress;
    private TextView tvAddressType;
    private LinearLayout layoutAccountAddress;
    private TextView tvAccountAddress;
    private ImageView ivAddressCode;
    private TextView tvReceiveAmount;
    private TextView tvInputAmount;

    private String accountAddress;
    private String currentAddress;
    private double amount;
    private AccountEntity account;
    private Observable<Boolean> btcAppkitSetup;

    @Override
    protected String tag() {
        return BtcAddressQrcodeActivity.class.getName();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_btc_address_qrcode);
        showNavBackBtn();

        ivAccountAvatar = findViewById(R.id.iv_account_avatar);
        tvAccountName = findViewById(R.id.tv_account_name);
        tvMainAddress = findViewById(R.id.tv_main_address);
        tvChildAddress = findViewById(R.id.tv_child_address);
        tvAddressType = findViewById(R.id.tv_address_type);
        layoutAccountAddress = findViewById(R.id.layout_account_address);
        tvAccountAddress = findViewById(R.id.tv_account_address);
        ivAddressCode = findViewById(R.id.iv_address_code);
        tvReceiveAmount = findViewById(R.id.tv_btc_receive_amount);
        tvInputAmount = findViewById(R.id.tv_input_amount);

        accountAddress = getIntent().getStringExtra(IntentParam.PARAM_ACCOUNT_ADDRESS);
        if (accountAddress == null || accountAddress.length() <= 0) {
            finish();
        }

        btcAppkitSetup = RxEventBus.get().register(EventTypeDef.BTC_APP_KIT_INIT_SET_UP, Boolean.class);
        btcAppkitSetup.observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Boolean>() {
                    @Override
                    public void onNext(Boolean flag) {
                        if (flag) {
                            initView();
                        }
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

        account = MainService.getInstance().getBitcoinAccountByAddress(accountAddress);
        if (account == null) {
            finish();
        } else {
            initView();
        }
    }

    private void initView() {
        ImageManager.showAccountAvatar(this, ivAccountAvatar, account);
        tvAccountName.setText(account.getName());
        final String mainAddress = accountAddress;
        final String childAddress = BtcAccountManager.getInstance().getBtcCurrentReceiveAddress(accountAddress);
        currentAddress = mainAddress;
        tvAccountAddress.setText(CommonUtil.generateSimpleAddress(currentAddress));

        layoutAccountAddress.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clipData = ClipData.newPlainText("text", currentAddress);
            if (cm != null) {
                cm.setPrimaryClip(clipData);
                showLongToast(R.string.tip_success_copy);
            }
        });

        showQRCode();

        tvMainAddress.setOnClickListener(v -> {
            ObjectAnimator.ofFloat(tvAddressType, "translationX", 0).start();
            tvAddressType.setText(R.string.btc_main_address);
            currentAddress = mainAddress;
            tvAccountAddress.setText(CommonUtil.generateSimpleAddress(currentAddress));
            showQRCode();
        });
        tvChildAddress.setOnClickListener(v -> {
            ObjectAnimator.ofFloat(tvAddressType, "translationX", tvAddressType.getWidth()).start();
            tvAddressType.setText(R.string.btc_child_address);
            currentAddress = childAddress;
            tvAccountAddress.setText(CommonUtil.generateSimpleAddress(currentAddress));
            showQRCode();
        });

        tvInputAmount.setOnClickListener(v -> {
            final View dialogView = getLayoutInflater().inflate(R.layout.dialog_transfer_btc_amount, null);
            final EditText etAmount = dialogView.findViewById(R.id.et_btc_amount);

            AlertDialog passwordDialog = new AlertDialog.Builder(this, R.style.Theme_AppCompat_Light_Dialog_Alert_Self)
                    .setView(dialogView)
                    .setCancelable(true)
                    .setPositiveButton(R.string.confirm, (dialog, which) -> {
                        dialog.cancel();
                        try {
                            amount = Double.valueOf(etAmount.getText().toString());
                            if (amount > 0) {
                                String text = String.format(Locale.getDefault(), "%s %s %s",
                                        getString(R.string.prompt_receive),
                                        etAmount.getText().toString(),
                                        getString(R.string.account_btc));
                                tvReceiveAmount.setText(text);
                            } else {
                                tvReceiveAmount.setText(R.string.transfer_btc);
                            }
                        } catch (Exception e) {
                            e.fillInStackTrace();
                        }
                        showQRCode();
                    })
                    .create();
            passwordDialog.setOnShowListener(dialog -> {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(etAmount, InputMethodManager.SHOW_IMPLICIT);
            });

            passwordDialog.show();
        });
    }

    private void showQRCode() {
        BitcoinPaymentURI.Builder receiveUriBuilder = new BitcoinPaymentURI.Builder();
        String receiveUri = receiveUriBuilder
                .address(currentAddress)
                .amount(amount)
                .build()
                .getURI();
        System.out.println(receiveUri);

        Observable<Bitmap> observable = Observable.create(e -> {
            int width = ivAddressCode.getWidth();
            if (width <= 0) {
                width = (CommonUtil.getScreenWidth(BtcAddressQrcodeActivity.this) - CommonUtil.dip2px(BtcAddressQrcodeActivity.this, 80)) * 55 / 100;
            }
            Bitmap bitmap = QRCodeUtil.createQRImage(receiveUri, width, width, null);
            e.onNext(bitmap);
            e.onCompleted();
        });
        observable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Bitmap>() {
                    @Override
                    public void onNext(Bitmap bitmap) {
                        int width = ivAddressCode.getWidth();
                        if (width > 0) {
                            ivAddressCode.getLayoutParams().height = width;
                            ivAddressCode.requestLayout();
                        }
                        RequestOptions options = RequestOptions.placeholderOf(R.drawable.btc_address_bg);
                        Glide.with(BtcAddressQrcodeActivity.this)
                                .load(bitmap)
                                .apply(options)
                                .into(ivAddressCode);
                    }

                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }
                });
        ;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        RxEventBus.get().unregister(EventTypeDef.BTC_APP_KIT_INIT_SET_UP, btcAppkitSetup);
    }
}
