package com.openclaw.assistant;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class MemoryManager extends SQLiteOpenHelper {
    private static final String TAG = "MemoryManager";
    private static final String DATABASE_NAME = "mate_memory.db";
    private static final int DATABASE_VERSION = 1;

    public MemoryManager(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Table for basic facts (Key-Value)
        db.execSQL("CREATE TABLE facts (id INTEGER PRIMARY KEY AUTOINCREMENT, key TEXT UNIQUE, value TEXT)");
        
        // Table for conversation history (Simplified for now)
        db.execSQL("CREATE TABLE conversations (id INTEGER PRIMARY KEY AUTOINCREMENT, role TEXT, content TEXT, timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)");
        
        // Table for price history (Bargain Hunter)
        db.execSQL("CREATE TABLE price_history (id INTEGER PRIMARY KEY AUTOINCREMENT, item_name TEXT, price REAL, currency TEXT, app_source TEXT, timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS facts");
        db.execSQL("DROP TABLE IF EXISTS conversations");
        db.execSQL("DROP TABLE IF EXISTS price_history");
        onCreate(db);
    }

    public void storeFact(String key, String value) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("key", key);
        values.put("value", value);
        db.insertWithOnConflict("facts", null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public String getFact(String key) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query("facts", new String[]{"value"}, "key=?", new String[]{key}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            String value = cursor.getString(0);
            cursor.close();
            return value;
        }
        return null;
    }

    public void logPrice(String itemName, double price, String currency, String appSource) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("item_name", itemName);
        values.put("price", price);
        values.put("currency", currency);
        values.put("app_source", appSource);
        db.insert("price_history", null, values);
        Log.d(TAG, "Logged price for " + itemName + ": " + currency + price);
    }
}
