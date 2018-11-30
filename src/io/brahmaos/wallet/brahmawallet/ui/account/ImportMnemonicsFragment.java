package io.brahmaos.wallet.brahmawallet.ui.account;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.web3j.crypto.WalletFile;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.ObjectMapperFactory;

import java.util.List;

import brahmaos.content.WalletData;
import io.brahmaos.wallet.brahmawallet.R;
import io.brahmaos.wallet.brahmawallet.db.entity.AccountEntity;
import io.brahmaos.wallet.brahmawallet.event.EventTypeDef;
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

public class ImportMnemonicsFragment extends Fragment {
    protected String tag() {
        return ImportMnemonicsFragment.class.getName();
    }

    public static final String ARG_PAGE = "MNEMONIC_PAGE";
    private List<AccountEntity> accounts;

    private View parentView;
    private EditText etMnemonic;
    private EditText etAccountName;
    private EditText etPassword;
    private EditText etRepeatPassword;
    private Button btnImportAccount;
    private CheckBox checkBoxReadProtocol;
    private CustomProgressDialog customProgressDialog;
    private TextView tvService;
    private TextView tvPrivacyPolicy;
    private Spinner spinner;

    public static ImportMnemonicsFragment newInstance(int page) {
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE, page);
        ImportMnemonicsFragment pageFragment = new ImportMnemonicsFragment();
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
            parentView = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_import_mnemonics, container, false);
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

        etMnemonic = parentView.findViewById(R.id.et_mnemonic);
        etAccountName = parentView.findViewById(R.id.et_account_name);
        etPassword = parentView.findViewById(R.id.et_password);
        etRepeatPassword = parentView.findViewById(R.id.et_repeat_password);
        btnImportAccount = parentView.findViewById(R.id.btn_import_mnemonics);
        checkBoxReadProtocol= parentView.findViewById(R.id.checkbox_read_protocol);
        checkBoxReadProtocol.setOnCheckedChangeListener((buttonView, isChecked) -> btnImportAccount.setEnabled(isChecked));
        btnImportAccount.setOnClickListener(view -> importMnemonicAccount());
        spinner = parentView.findViewById(R.id.spinner);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(), R.array.mnemonic_path, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

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

    private void importMnemonicAccount() {
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
            customProgressDialog = new CustomProgressDialog(getActivity(),
                    R.style.CustomProgressDialogStyle,
                    getString(R.string.progress_import_account));
            customProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            customProgressDialog.setCancelable(false);
            customProgressDialog.show();

            EthAccountManager.getInstance().restoreEthAccountWithMnemonics(name, password, mnemonics)
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
                            Toast.makeText(getContext(), R.string.error_import_private_key, Toast.LENGTH_LONG).show();
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
                                RxEventBus.get().post(EventTypeDef.CHANGE_ETH_ACCOUNT, false);
                                getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
                                Toast.makeText(getContext(), R.string.success_import_account, Toast.LENGTH_LONG).show();
                                Intent intent = new Intent();
                                getActivity().setResult(Activity.RESULT_OK, intent);
                                getActivity().finish();
                            } else {
                                Toast.makeText(getContext(), R.string.error_import_private_key, Toast.LENGTH_LONG).show();
                                etMnemonic.requestFocus();
                                btnImportAccount.setEnabled(true);
                            }
                        }
                    });

        } else {
            Toast.makeText(getContext(), R.string.error_mnemonics_length, Toast.LENGTH_LONG).show();
            etMnemonic.requestFocus();
            btnImportAccount.setEnabled(true);
        }
    }
}
