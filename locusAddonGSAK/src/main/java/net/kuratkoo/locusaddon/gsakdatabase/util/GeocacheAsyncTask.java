package net.kuratkoo.locusaddon.gsakdatabase.util;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;

import locus.api.android.objects.PackPoints;
import locus.api.objects.extra.Location;

public abstract class GeocacheAsyncTask extends AsyncTask<Location, Integer, Exception> {

    protected SQLiteDatabase db;
    protected SQLiteDatabase db2;
    protected SQLiteDatabase db3;

    protected PackPoints packPoints;

    @Override
    public void onProgressUpdate(final Integer... values) {
        // Empty
    }

    public final void myPublishProgress(final int value) {
        super.publishProgress(value);
    }

    protected void openDatabases(final Context context) {
        db = GsakReader.openDatabase(context, "db");
        db2 = GsakReader.openDatabase(context, "db2");
        db3 = GsakReader.openDatabase(context, "db3");
    }

    protected void closeDatabases() {
        if (db != null) {
            db.close();
            db = null;
        }
        if (db2 != null) {
            db2.close();
            db2 = null;
        }
        if (db3 != null) {
            db3.close();
            db3 = null;
        }
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        closeDatabases();
    }
}
