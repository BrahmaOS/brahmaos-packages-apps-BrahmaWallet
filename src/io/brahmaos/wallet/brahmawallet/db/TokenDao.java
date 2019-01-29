package io.brahmaos.wallet.brahmawallet.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import io.brahmaos.wallet.brahmawallet.R;
import io.brahmaos.wallet.brahmawallet.common.BrahmaConst;
import io.brahmaos.wallet.brahmawallet.db.entity.AllTokenEntity;
import io.brahmaos.wallet.brahmawallet.db.entity.TokenEntity;
import io.brahmaos.wallet.util.BLog;


/**
 * Token
 */
public class TokenDao {

    // ALL TOKEN LIST
    public static final String TABLE_G_ALL_TOKEN = "T_WALLET_ALL_TOKEN";
    public static final String COL_G_ALL_TOKEN_ID = "_id";
    public static final String COL_G_ALL_TOKEN_NAME = "name";
    public static final String COL_G_ALL_TOKEN_SHORT_NAME = "short_name";
    public static final String COL_G_ALL_TOKEN_ADDRESS = "address";
    public static final String COL_G_ALL_TOKEN_AVATAR = "avatar";
    public static final String COL_G_ALL_TOKEN_SHOW_FLAG = "show_flag";
    public static final String COL_G_ALL_TOKEN_CODE = "code";

    public static final String SQL_TABLE_G_ALL_TOKEN = "CREATE TABLE "
            + TABLE_G_ALL_TOKEN + " ("
            + COL_G_ALL_TOKEN_ID + " integer primary key autoincrement, "
            + COL_G_ALL_TOKEN_NAME + " varchar(128) not null, "
            + COL_G_ALL_TOKEN_SHORT_NAME + " varchar(128) not null, "
            + COL_G_ALL_TOKEN_ADDRESS + " text, "
            + COL_G_ALL_TOKEN_AVATAR + " text, "
            + COL_G_ALL_TOKEN_SHOW_FLAG + " int, "
            + COL_G_ALL_TOKEN_CODE + " int);";

    // CHOSEN TOKEN LIST
    public static final String TABLE_G_CHOSEN_TOKEN = "T_WALLET_CHOSEN_TOKEN";
    public static final String COL_G_CHOSEN_TOKEN_ID = "_id";
    public static final String COL_G_CHOSEN_TOKEN_NAME = "name";
    public static final String COL_G_CHOSEN_TOKEN_SHORT_NAME = "short_name";
    public static final String COL_G_CHOSEN_TOKEN_ADDRESS = "address";
    public static final String COL_G_CHOSEN_TOKEN_AVATAR = "avatar";
    public static final String COL_G_CHOSEN_TOKEN_CODE = "code";

    public static final String SQL_TABLE_G_CHOSEN_TOKEN = "CREATE TABLE "
            + TABLE_G_CHOSEN_TOKEN + " ("
            + COL_G_CHOSEN_TOKEN_ID + " integer primary key autoincrement, "
            + COL_G_CHOSEN_TOKEN_NAME + " varchar(128) not null, "
            + COL_G_CHOSEN_TOKEN_SHORT_NAME + " varchar(128) not null, "
            + COL_G_CHOSEN_TOKEN_ADDRESS + " text, "
            + COL_G_CHOSEN_TOKEN_AVATAR + " text, "
            + COL_G_CHOSEN_TOKEN_CODE + " int);";

    public String tag() {
        return TokenDao.class.getName();
    }

    private static TokenDao instance = new TokenDao();
    public static TokenDao getInstance() {
        return instance;
    }

    public void init(SQLiteDatabase db) {
        db.execSQL(SQL_TABLE_G_ALL_TOKEN);
        db.execSQL(SQL_TABLE_G_CHOSEN_TOKEN);
        db.execSQL("INSERT INTO " + TABLE_G_CHOSEN_TOKEN + " (name, address, short_name, avatar, code) " +
                "values (\"BrahmaOS\", \"0xd7732e3783b0047aa251928960063f863ad022d8\", \"BRM\", "
                + String.valueOf(R.drawable.icon_brm) + "," + String.valueOf(BrahmaConst.COIN_CODE_BRM) + ")");
        db.execSQL("INSERT INTO " + TABLE_G_CHOSEN_TOKEN + " (name, address, short_name, avatar, code) " +
                "values (\"Ethereum\", \"\", \"ETH\", "
                + String.valueOf(R.drawable.icon_eth) + "," + String.valueOf(BrahmaConst.COIN_CODE_ETH) + ")");
        db.execSQL("INSERT INTO " + TABLE_G_CHOSEN_TOKEN + " (name, address, short_name, avatar, code) " +
                "values (\"Bitcoin\", \"btc\", \"BTC\", \"\"," + String.valueOf(BrahmaConst.COIN_CODE_BTC) + ")");
    }

    /**
     * Load all tokens
     *
     * @return all tokens
     */
    public List<AllTokenEntity> getAllTokens() {
        SQLiteDatabase dbInstance = WalletDBMgr.getInstance().getDBInstance();
        String sql = "select * from " + TABLE_G_ALL_TOKEN;
        Cursor cursor = dbInstance.rawQuery(sql, null);
        BLog.d(tag(), "loadAllTokens - " + sql);
        List<AllTokenEntity> allTokenEntityList = new ArrayList<>();
        while (cursor != null && cursor.moveToNext()) {
            AllTokenEntity tokenEntity = new AllTokenEntity();
            tokenEntity.setId(cursor.getInt(cursor.getColumnIndex(COL_G_ALL_TOKEN_ID)));
            tokenEntity.setName(cursor.getString(cursor.getColumnIndex(COL_G_ALL_TOKEN_NAME)));
            tokenEntity.setShortName(cursor.getString(cursor.getColumnIndex(COL_G_ALL_TOKEN_SHORT_NAME)));
            tokenEntity.setAddress(cursor.getString(cursor.getColumnIndex(COL_G_ALL_TOKEN_ADDRESS)));
            tokenEntity.setAvatar(cursor.getString(cursor.getColumnIndex(COL_G_ALL_TOKEN_AVATAR)));
            tokenEntity.setShowFlag(cursor.getInt(cursor.getColumnIndex(COL_G_ALL_TOKEN_SHOW_FLAG)));
            tokenEntity.setCode(cursor.getInt(cursor.getColumnIndex(COL_G_ALL_TOKEN_CODE)));
            allTokenEntityList.add(tokenEntity);
        }

        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }

        return allTokenEntityList;
    }

    /**
     * Load all tokens with show flag
     *
     * @return all tokens
     */
    public List<AllTokenEntity> getAllTokens(int showFlag) {
        SQLiteDatabase dbInstance = WalletDBMgr.getInstance().getDBInstance();
        String sql = "select * from " + TABLE_G_ALL_TOKEN
                + " where " + COL_G_ALL_TOKEN_SHOW_FLAG + " = " + showFlag;
        Cursor cursor = dbInstance.rawQuery(sql, null);
        BLog.d(tag(), "loadAllTokens - " + sql);
        List<AllTokenEntity> allTokenEntityList = new ArrayList<>();
        while (cursor != null && cursor.moveToNext()) {
            AllTokenEntity tokenEntity = new AllTokenEntity();
            tokenEntity.setId(cursor.getInt(cursor.getColumnIndex(COL_G_ALL_TOKEN_ID)));
            tokenEntity.setName(cursor.getString(cursor.getColumnIndex(COL_G_ALL_TOKEN_NAME)));
            tokenEntity.setShortName(cursor.getString(cursor.getColumnIndex(COL_G_ALL_TOKEN_SHORT_NAME)));
            tokenEntity.setAddress(cursor.getString(cursor.getColumnIndex(COL_G_ALL_TOKEN_ADDRESS)));
            tokenEntity.setAvatar(cursor.getString(cursor.getColumnIndex(COL_G_ALL_TOKEN_AVATAR)));
            tokenEntity.setShowFlag(cursor.getInt(cursor.getColumnIndex(COL_G_ALL_TOKEN_SHOW_FLAG)));
            tokenEntity.setCode(cursor.getInt(cursor.getColumnIndex(COL_G_ALL_TOKEN_CODE)));
            allTokenEntityList.add(tokenEntity);
        }

        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }

        return allTokenEntityList;
    }

    public void insertAllTokens(List<AllTokenEntity> allTokenEntities) {
        if (allTokenEntities != null) {
            SQLiteDatabase dbInstance = WalletDBMgr.getInstance().getDBInstance();
            ContentValues newValues;
            for (AllTokenEntity token : allTokenEntities) {
                newValues = new ContentValues();
                newValues.put(COL_G_ALL_TOKEN_NAME, token.getName());
                newValues.put(COL_G_ALL_TOKEN_SHORT_NAME, token.getShortName());
                newValues.put(COL_G_ALL_TOKEN_ADDRESS, token.getAddress());
                newValues.put(COL_G_ALL_TOKEN_AVATAR, token.getAvatar());
                newValues.put(COL_G_ALL_TOKEN_SHOW_FLAG, token.getShowFlag());
                newValues.put(COL_G_ALL_TOKEN_CODE, token.getCode());
                dbInstance.insert(TABLE_G_ALL_TOKEN, null, newValues);
            }
        }
    }

    public void deleteAllTokens() {
        SQLiteDatabase dbInstance = WalletDBMgr.getInstance().getDBInstance();
        String where = "1 = ?";
        String[] selectArgs = { String.valueOf(1) };
        dbInstance.delete(TABLE_G_ALL_TOKEN, where, selectArgs);
    }

    /**
     * query all tokens
     *
     * @return query all tokens
     */
    public List<AllTokenEntity> queryAllTokens(String param) {
        SQLiteDatabase dbInstance = WalletDBMgr.getInstance().getDBInstance();
        String sql = "select * from " + TABLE_G_ALL_TOKEN
                + " where " + COL_G_ALL_TOKEN_NAME + " like '%" + param
                + "%' or " + COL_G_ALL_TOKEN_SHORT_NAME + " like '%" + param + "%'";
        Cursor cursor = dbInstance.rawQuery(sql, null);
        BLog.d(tag(), "queryAllTokens - " + sql);
        List<AllTokenEntity> allTokenEntityList = new ArrayList<>();
        while (cursor != null && cursor.moveToNext()) {
            AllTokenEntity tokenEntity = new AllTokenEntity();
            tokenEntity.setId(cursor.getInt(cursor.getColumnIndex(COL_G_ALL_TOKEN_ID)));
            tokenEntity.setName(cursor.getString(cursor.getColumnIndex(COL_G_ALL_TOKEN_NAME)));
            tokenEntity.setShortName(cursor.getString(cursor.getColumnIndex(COL_G_ALL_TOKEN_SHORT_NAME)));
            tokenEntity.setAddress(cursor.getString(cursor.getColumnIndex(COL_G_ALL_TOKEN_ADDRESS)));
            tokenEntity.setAvatar(cursor.getString(cursor.getColumnIndex(COL_G_ALL_TOKEN_AVATAR)));
            tokenEntity.setShowFlag(cursor.getInt(cursor.getColumnIndex(COL_G_ALL_TOKEN_SHOW_FLAG)));
            tokenEntity.setCode(cursor.getInt(cursor.getColumnIndex(COL_G_ALL_TOKEN_CODE)));
            allTokenEntityList.add(tokenEntity);
        }
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }

        return allTokenEntityList;
    }

    /**
     * query token detail
     */
    public AllTokenEntity queryAllTokenEntityByCode(int code) {
        SQLiteDatabase dbInstance = WalletDBMgr.getInstance().getDBInstance();
        String sql = "select * from " + TABLE_G_ALL_TOKEN
                + " where " + COL_G_ALL_TOKEN_CODE + "=" + code;
        Cursor cursor = dbInstance.rawQuery(sql, null);
        BLog.d(tag(), "queryAllTokenEntityByCode - " + sql);
        AllTokenEntity tokenEntity = new AllTokenEntity();
        while (cursor != null && cursor.moveToNext()) {
            tokenEntity.setId(cursor.getInt(cursor.getColumnIndex(COL_G_ALL_TOKEN_ID)));
            tokenEntity.setName(cursor.getString(cursor.getColumnIndex(COL_G_ALL_TOKEN_NAME)));
            tokenEntity.setShortName(cursor.getString(cursor.getColumnIndex(COL_G_ALL_TOKEN_SHORT_NAME)));
            tokenEntity.setAddress(cursor.getString(cursor.getColumnIndex(COL_G_ALL_TOKEN_ADDRESS)));
            tokenEntity.setAvatar(cursor.getString(cursor.getColumnIndex(COL_G_ALL_TOKEN_AVATAR)));
            tokenEntity.setShowFlag(cursor.getInt(cursor.getColumnIndex(COL_G_ALL_TOKEN_SHOW_FLAG)));
            tokenEntity.setCode(cursor.getInt(cursor.getColumnIndex(COL_G_ALL_TOKEN_CODE)));
            break;
        }
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }

        return tokenEntity;
    }

    /**
     * Load all chosen tokens
     *
     * @return all chosen tokens
     */
    public List<TokenEntity> loadChosenTokens() {
        SQLiteDatabase dbInstance = WalletDBMgr.getInstance().getDBInstance();
        String sql = "select * from " + TABLE_G_CHOSEN_TOKEN;
        Cursor cursor = dbInstance.rawQuery(sql, null);
        BLog.d(tag(), "loadChosenTokens - " + sql);
        List<TokenEntity> allTokenEntityList = new ArrayList<>();
        while (cursor != null && cursor.moveToNext()) {
            TokenEntity tokenEntity = new TokenEntity();
            tokenEntity.setId(cursor.getInt(cursor.getColumnIndex(COL_G_CHOSEN_TOKEN_ID)));
            tokenEntity.setName(cursor.getString(cursor.getColumnIndex(COL_G_CHOSEN_TOKEN_NAME)));
            tokenEntity.setShortName(cursor.getString(cursor.getColumnIndex(COL_G_CHOSEN_TOKEN_SHORT_NAME)));
            tokenEntity.setAddress(cursor.getString(cursor.getColumnIndex(COL_G_CHOSEN_TOKEN_ADDRESS)));
            tokenEntity.setAvatar(cursor.getString(cursor.getColumnIndex(COL_G_CHOSEN_TOKEN_AVATAR)));
            tokenEntity.setCode(cursor.getInt(cursor.getColumnIndex(COL_G_CHOSEN_TOKEN_CODE)));
            allTokenEntityList.add(tokenEntity);
        }

        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }

        return allTokenEntityList;
    }

    /**
     * Query chosen tokens
     *
     * @return all chosen tokens
     */
    public TokenEntity queryChosenTokenByCode(int code) {
        SQLiteDatabase dbInstance = WalletDBMgr.getInstance().getDBInstance();
        String sql = "select * from " + TABLE_G_CHOSEN_TOKEN + " where " + COL_G_CHOSEN_TOKEN_CODE + " = " + code;
        Cursor cursor = dbInstance.rawQuery(sql, null);
        BLog.d(tag(), "queryChosenTokenByCode - " + sql);
        TokenEntity tokenEntity = new TokenEntity();
        while (cursor != null && cursor.moveToNext()) {
            tokenEntity.setId(cursor.getInt(cursor.getColumnIndex(COL_G_CHOSEN_TOKEN_ID)));
            tokenEntity.setName(cursor.getString(cursor.getColumnIndex(COL_G_CHOSEN_TOKEN_NAME)));
            tokenEntity.setShortName(cursor.getString(cursor.getColumnIndex(COL_G_CHOSEN_TOKEN_SHORT_NAME)));
            tokenEntity.setAddress(cursor.getString(cursor.getColumnIndex(COL_G_CHOSEN_TOKEN_ADDRESS)));
            tokenEntity.setAvatar(cursor.getString(cursor.getColumnIndex(COL_G_CHOSEN_TOKEN_AVATAR)));
            tokenEntity.setCode(cursor.getInt(cursor.getColumnIndex(COL_G_CHOSEN_TOKEN_CODE)));
            break;
        }

        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }

        return tokenEntity;
    }

    /**
     * Query chosen tokens
     *
     * @return all chosen tokens
     */
    public TokenEntity queryTokenByAddress(String address) {
        SQLiteDatabase dbInstance = WalletDBMgr.getInstance().getDBInstance();
        String sql = "select * from " + TABLE_G_ALL_TOKEN + " where lower(" + COL_G_CHOSEN_TOKEN_ADDRESS + ") = '" + address + "'";
        Cursor cursor = dbInstance.rawQuery(sql, null);
        BLog.d(tag(), "queryChosenTokenByAddress - " + sql);
        TokenEntity tokenEntity = new TokenEntity();
        while (cursor != null && cursor.moveToNext()) {
            tokenEntity.setId(cursor.getInt(cursor.getColumnIndex(COL_G_CHOSEN_TOKEN_ID)));
            tokenEntity.setName(cursor.getString(cursor.getColumnIndex(COL_G_CHOSEN_TOKEN_NAME)));
            tokenEntity.setShortName(cursor.getString(cursor.getColumnIndex(COL_G_CHOSEN_TOKEN_SHORT_NAME)));
            tokenEntity.setAddress(cursor.getString(cursor.getColumnIndex(COL_G_CHOSEN_TOKEN_ADDRESS)));
            tokenEntity.setAvatar(cursor.getString(cursor.getColumnIndex(COL_G_CHOSEN_TOKEN_AVATAR)));
            tokenEntity.setCode(cursor.getInt(cursor.getColumnIndex(COL_G_CHOSEN_TOKEN_CODE)));
            break;
        }

        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }

        return tokenEntity;
    }

    public void insertChosenToken(TokenEntity token) {
        if (token != null) {
            SQLiteDatabase dbInstance = WalletDBMgr.getInstance().getDBInstance();
            ContentValues newValues = new ContentValues();
            newValues.put(COL_G_CHOSEN_TOKEN_NAME, token.getName());
            newValues.put(COL_G_CHOSEN_TOKEN_SHORT_NAME, token.getShortName());
            newValues.put(COL_G_CHOSEN_TOKEN_ADDRESS, token.getAddress());
            newValues.put(COL_G_CHOSEN_TOKEN_AVATAR, token.getAvatar());
            newValues.put(COL_G_CHOSEN_TOKEN_CODE, token.getCode());
            dbInstance.insert(TABLE_G_CHOSEN_TOKEN, null, newValues);
        }
    }

    public void deleteChosenToken(String address) {
        SQLiteDatabase dbInstance = WalletDBMgr.getInstance().getDBInstance();
        String where = "LOWER(" + COL_G_CHOSEN_TOKEN_ADDRESS + ") = ?";
        String[] selectArgs = { address.toLowerCase() };
        dbInstance.delete(TABLE_G_CHOSEN_TOKEN, where, selectArgs);
    }

    public void upgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        int upgradeVersion  = oldVersion;
        BLog.w(tag(), "TokenDao update");
        if (upgradeVersion != newVersion) {
            // Drop tables
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_G_ALL_TOKEN);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_G_CHOSEN_TOKEN);

            // Create tables
            init(db);
        }
    }
}
