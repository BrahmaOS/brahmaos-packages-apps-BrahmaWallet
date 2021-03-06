package io.brahmaos.wallet.brahmawallet.ui.account;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.web3j.crypto.WalletFile;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.ObjectMapperFactory;

import java.io.IOException;
import java.util.List;

import brahmaos.content.WalletData;
import io.brahmaos.wallet.brahmawallet.R;
import io.brahmaos.wallet.brahmawallet.db.entity.AccountEntity;
import io.brahmaos.wallet.brahmawallet.event.EventTypeDef;
import io.brahmaos.wallet.brahmawallet.service.BrahmaWeb3jService;
import io.brahmaos.wallet.brahmawallet.service.EthAccountManager;
import io.brahmaos.wallet.brahmawallet.service.MainService;
import io.brahmaos.wallet.brahmawallet.ui.setting.PrivacyPolicyActivity;
import io.brahmaos.wallet.brahmawallet.ui.setting.ServiceTermsActivity;
import io.brahmaos.wallet.brahmawallet.view.CustomProgressDialog;
import io.brahmaos.wallet.util.BLog;
import io.brahmaos.wallet.util.CommonUtil;
import io.brahmaos.wallet.util.RxEventBus;
import rx.Completable;
import rx.CompletableSubscriber;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class ImportOfficialFragment extends Fragment {
    protected String tag() {
        return ImportOfficialFragment.class.getName();
    }

    public static final String ARG_PAGE = "OFFICIAL_KEYSTORE_PAGE";
    private List<AccountEntity> accounts;

    private View parentView;
    private EditText etKeystore;
    private EditText etAccountName;
    private EditText etPassword;
    private Button btnImportAccount;
    private CheckBox checkBoxReadProtocol;
    private CustomProgressDialog customProgressDialog;
    private TextView tvService;
    private TextView tvPrivacyPolicy;

    public static ImportOfficialFragment newInstance(int page) {
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE, page);
        ImportOfficialFragment pageFragment = new ImportOfficialFragment();
        pageFragment.setArguments(args);
        return pageFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        BLog.d(tag(), "onCreateView");
        if (parentView == null) {
            parentView = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_import_official, container, false);
            initView();
        } else {
            ViewGroup parent = (ViewGroup)parentView.getParent();
            if (parent != null) {
                parent.removeView(parentView);
            }
        }
        return parentView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        accounts = MainService.getInstance().getEthereumAccounts();
    }

    private void initView() {

        etKeystore = parentView.findViewById(R.id.et_official_json);
        etAccountName = parentView.findViewById(R.id.et_account_name);
        etPassword = parentView.findViewById(R.id.et_password);
        btnImportAccount = parentView.findViewById(R.id.btn_import_keystore);
        checkBoxReadProtocol= parentView.findViewById(R.id.checkbox_read_protocol);
        checkBoxReadProtocol.setOnCheckedChangeListener((buttonView, isChecked) -> btnImportAccount.setEnabled(isChecked));
        btnImportAccount.setOnClickListener(view -> importOfficialAccount());

        tvService = parentView.findViewById(R.id.service_tv);
        tvService.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ServiceTermsActivity.class);
            startActivity(intent);
        });

        tvPrivacyPolicy = parentView.findViewById(R.id.privacy_policy_tv);
        tvPrivacyPolicy.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), PrivacyPolicyActivity.class);
            startActivity(intent);
        });
    }

    private void importOfficialAccount() {
        btnImportAccount.setEnabled(false);
        // Reset errors.
        etAccountName.setError(null);
        etPassword.setError(null);

        // Store values at the time of the create account.
        String officialKeystore = CommonUtil.parseAccountContent(etKeystore.getText().toString().trim());
        String name = etAccountName.getText().toString().trim();
        String password = etPassword.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid keystore.
        if (TextUtils.isEmpty(officialKeystore)) {
            focusView = etKeystore;
            Toast.makeText(getActivity(), R.string.error_field_required, Toast.LENGTH_LONG).show();
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

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
            btnImportAccount.setEnabled(true);
            return;
        }
        BLog.i(tag(), "the password is:" + password);
        try {
            customProgressDialog = new CustomProgressDialog(getActivity(),
                    R.style.CustomProgressDialogStyle,
                    getString(R.string.progress_import_account));
            customProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            customProgressDialog.setCancelable(false);
            customProgressDialog.show();
            EthAccountManager.getInstance().restoreEthAccountWithKeystore(name, password, officialKeystore)
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
                            Toast.makeText(getContext(), R.string.error_import_keystore, Toast.LENGTH_LONG).show();
                            etKeystore.requestFocus();
                            btnImportAccount.setEnabled(true);
                        }

                        @Override
                        public void onNext(WalletData walletData) {
                            if (customProgressDialog != null) {
                                customProgressDialog.cancel();
                            }

                            if (walletData != null) {
                                // hide soft input board
                                RxEventBus.get().post(EventTypeDef.CHANGE_ETH_ACCOUNT, false);
                                getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
                                Toast.makeText(getContext(), R.string.success_import_account, Toast.LENGTH_LONG).show();
                                Intent intent = new Intent();
                                getActivity().setResult(Activity.RESULT_OK, intent);
                                getActivity().finish();
                            } else {
                                Toast.makeText(getContext(), R.string.error_import_keystore, Toast.LENGTH_LONG).show();
                                etKeystore.requestFocus();
                                btnImportAccount.setEnabled(true);
                            }
                        }
                    });

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), R.string.error_keystore, Toast.LENGTH_LONG).show();
            btnImportAccount.setEnabled(true);
            etKeystore.requestFocus();
            if (customProgressDialog != null) {
                customProgressDialog.cancel();
            }
        }
    }
}
