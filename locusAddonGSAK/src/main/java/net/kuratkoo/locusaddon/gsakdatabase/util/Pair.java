package net.kuratkoo.locusaddon.gsakdatabase.util;

import android.database.sqlite.SQLiteDatabase;

public class Pair {

    public final String gcCode;
    public final float distance;
    public final SQLiteDatabase db;

    public Pair(final float dist, final String code, final SQLiteDatabase db) {
        this.distance = dist;
        this.gcCode = code;
        this.db = db;
    }
}
