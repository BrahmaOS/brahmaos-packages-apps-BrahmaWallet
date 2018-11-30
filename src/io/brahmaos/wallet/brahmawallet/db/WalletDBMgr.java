package io.brahmaos.wallet.brahmawallet.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import io.brahmaos.wallet.util.BLog;


/**
 * BrahmaWallet database manager
 */
public class WalletDBMgr {

    private static final String TAG = WalletDBMgr.class.getName();

    private SQLiteDatabase dbInstance;
    private Context context;
    private WalletDBHelper dbHelper;

    private static WalletDBMgr instance = new WalletDBMgr();
    public static WalletDBMgr getInstance() {
        return instance;
    }

    /**
     * init
     */
    public boolean init(Context ctx) {
        context = ctx;
        this.dbHelper = new WalletDBHelper(context,
                WalletDBConst.DB_NAME,
                null,
                WalletDBConst.DB_VERSION);
        return open();
    }

    public SQLiteDatabase getDBInstance() {
        if (dbInstance == null || !dbInstance.isOpen()) {
            dbInstance = dbHelper.getWritableDatabase();
        }

        return dbInstance;
    }

    public boolean open() {
        BLog.d(TAG, "open");

        try {
            dbInstance = dbHelper.getWritableDatabase();
        } catch (SQLiteException ex) {
            dbInstance = dbHelper.getReadableDatabase();
        }

        return true;
    }

    public void close() {
        BLog.d(TAG, "close");
        getDBInstance().close();
    }
}
