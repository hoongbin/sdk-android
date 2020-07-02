
package com.betadata.collect.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.betadata.collect.BetaDataLog;

import org.json.JSONArray;
import org.json.JSONObject;


public class OldBDatabaseHelper extends SQLiteOpenHelper {
    public OldBDatabaseHelper(Context context, String dbName) {
        super(context, dbName, null, 4);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public JSONArray getAllEvents() {
        final JSONArray arr = new JSONArray();
        Cursor c = null;
        try {
            final SQLiteDatabase db = getReadableDatabase();
            c = db.rawQuery("SELECT * FROM " + DbParams.TABLE_EVENTS +
                    " ORDER BY " + DbParams.KEY_CREATED_AT, null);
            while (c.moveToNext()) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("created_at", c.getString(c.getColumnIndex("created_at")));
                jsonObject.put("data", c.getString(c.getColumnIndex("data")));
                arr.put(jsonObject);
            }
        } catch (Exception e) {
            BetaDataLog.printStackTrace(e);
        } finally {
            close();
            if (c != null) {
                c.close();
            }
        }

        return arr;
    }
}
