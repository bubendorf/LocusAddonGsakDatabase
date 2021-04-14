/*
 * DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
 * Version 2, December 2004
 *
 * Copyright (C) 2004 Sam Hocevar <sam@hocevar.net>
 *
 * Everyone is permitted to copy and distribute verbatim or modified
 * copies of this license document, and changing it is allowed as long
 * as the name is changed.
 *
 * DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
 * TERMS AND CONDITIONS FOR COPYING, DISTRIBUTION AND MODIFICATION
 *
 * 0. You just DO WHAT THE FUCK YOU WANT TO.
 */

package ch.bubendorf.locusaddon.gsakdatabase.util;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;

import locus.api.android.objects.PackPoints;
import locus.api.objects.extra.Location;

/**
 * @author Markus Bubendorf <gsakforlocus@bubendorf.net>
 */
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
