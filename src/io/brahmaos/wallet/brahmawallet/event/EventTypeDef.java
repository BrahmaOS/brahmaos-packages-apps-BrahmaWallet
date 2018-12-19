package io.brahmaos.wallet.brahmawallet.event;

/**
 * Rxbus event defintion
 */
public class EventTypeDef {
    public static final String LOAD_ACCOUNT_ASSETS = "load_all_account_assets";
    public static final String ACCOUNT_ASSETS_TRANSFER = "account_transfer_success";
    public static final String ACCOUNT_ASSETS_CHANGE = "account_assets_change";
    public static final String BTC_ACCOUNT_SYNC = "btc_account_sync";
    public static final String BTC_APP_KIT_INIT_SET_UP = "btc_app_kit_set_up";
    public static final String BTC_TRANSACTION_CHANGE = "btc_transaction_status_change";
    public static final String BTC_TRANSACTION_BROADCAST_COMPLETE = "btc_transaction_broadcast_complete";
    public static final String CREATE_ETH_ACCOUNT = "create_eth_account";
    public static final String CHANGE_ETH_ACCOUNT = "change_eth_account";
    public static final String CHANGE_BTC_ACCOUNT = "change_btc_account";
}
