package ch.bubendorf.locusaddon.gsakdatabase.util;

import android.database.sqlite.SQLiteDatabase;

public class CacheWrapper {

    public final String gcCode;
    public final float distance;
    public final SQLiteDatabase db;

    public CacheWrapper(final float dist, final String code, final SQLiteDatabase db) {
        this.distance = dist;
        this.gcCode = code;
        this.db = db;
    }
}
