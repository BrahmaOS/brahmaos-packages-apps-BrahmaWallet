package io.brahmaos.wallet.brahmawallet.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import io.brahmaos.wallet.util.BLog;


/**
 * BrahmaWallet database helper
 */
public class WalletDBHelper extends SQLiteOpenHelper {

    private static final String TAG = WalletDBHelper.class.getName();

    public WalletDBHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        BLog.v(TAG, "onCreate");
        TokenDao.getInstance().init(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        BLog.w(TAG, "upgrading from version " + oldVersion + " to " + newVersion);

        TokenDao.getInstance().upgrade(db, oldVersion, newVersion);
    }
}
