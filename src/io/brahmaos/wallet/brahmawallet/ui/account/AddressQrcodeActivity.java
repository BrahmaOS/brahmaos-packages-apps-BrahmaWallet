package io.brahmaos.wallet.brahmawallet.ui.account;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import io.brahmaos.wallet.brahmawallet.R;
import io.brahmaos.wallet.brahmawallet.common.IntentParam;
import io.brahmaos.wallet.brahmawallet.db.entity.AccountEntity;
import io.brahmaos.wallet.brahmawallet.ui.base.BaseActivity;
import io.brahmaos.wallet.util.QRCodeUtil;

public class AddressQrcodeActivity extends BaseActivity {

    private AccountEntity account;
    private ImageView ivAddressCode;
    private TextView tvAccountAddress;
    private Button btnCopyAddress;
    @Override
    protected String tag() {
        return AddressQrcodeActivity.class.getName();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_address_qrcode);
        ivAddressCode = findViewById(R.id.iv_address_code);
        tvAccountAddress = findViewById(R.id.tv_account_address);
        btnCopyAddress = findViewById(R.id.btn_copy_address);
        showNavBackBtn();
        account = (AccountEntity) getIntent().getSerializableExtra(IntentParam.PARAM_ACCOUNT_INFO);
        if (account == null) {
            finish();
        }
        initView();
    }

    private void initView() {
        tvAccountAddress.setText(account.getAddress());

        btnCopyAddress.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clipData = ClipData.newPlainText("text",account.getAddress());
            if (cm != null) {
                cm.setPrimaryClip(clipData);
                showLongToast(R.string.tip_success_copy);
            }
        });

        new Thread(() -> {
            Bitmap bitmap = QRCodeUtil.createQRImage(account.getAddress(), 200, 200, null);

            if (bitmap != null) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(AddressQrcodeActivity.this)
                                .load(bitmap)
                                .into(ivAddressCode);
                    }
                });
            }
        }).start();
    }
}
