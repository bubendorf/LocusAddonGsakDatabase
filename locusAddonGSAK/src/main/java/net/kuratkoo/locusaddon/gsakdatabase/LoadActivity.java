package net.kuratkoo.locusaddon.gsakdatabase;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Toast;

import net.kuratkoo.locusaddon.gsakdatabase.util.Gsak;
import net.kuratkoo.locusaddon.gsakdatabase.util.GsakReader;
import net.kuratkoo.locusaddon.gsakdatabase.util.Pair;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import locus.api.android.ActionDisplayPoints;
import locus.api.android.ActionDisplayVarious;
import locus.api.android.objects.LocusVersion;
import locus.api.android.objects.PackPoints;
import locus.api.android.utils.IntentHelper;
import locus.api.android.utils.exceptions.RequiredVersionMissingException;
import locus.api.objects.extra.Location;
import locus.api.objects.geoData.Point;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;
import static java.lang.Integer.parseInt;

/**
 * LoadActivity
 * @author Radim -kuratkoo- Vaculik <kuratkoo@gmail.com>
 */
public class LoadActivity extends Activity implements DialogInterface.OnDismissListener {

    private static final String TAG = "LoadActivity";
    private ProgressDialog progress;
    private PackPoints pd;
    private Point point;
    private LoadAsyncTask loadAsyncTask;

    public void onDismiss(DialogInterface arg0) {
        loadAsyncTask.cancel(true);
    }

    private class LoadAsyncTask extends AsyncTask<Point, Integer, Exception> {

        private SQLiteDatabase db;
        private SQLiteDatabase db2;
        private SQLiteDatabase db3;

        @Override
        protected void onPreExecute() {
            progress.show();

            SharedPreferences sharedPreferences = getDefaultSharedPreferences(LoadActivity.this);
            if (sharedPreferences.getBoolean("pref_use_db", true)) {
                db = SQLiteDatabase.openDatabase(sharedPreferences.getString("db", ""),
                        null, SQLiteDatabase.NO_LOCALIZED_COLLATORS + SQLiteDatabase.OPEN_READONLY);
            }
            if (sharedPreferences.getBoolean("pref_use_db2", false)) {
                db2 = SQLiteDatabase.openDatabase(sharedPreferences.getString("db2", ""),
                        null, SQLiteDatabase.NO_LOCALIZED_COLLATORS + SQLiteDatabase.OPEN_READONLY);
            }
            if (sharedPreferences.getBoolean("pref_use_db3", false)) {
                db3 = SQLiteDatabase.openDatabase(sharedPreferences.getString("db3", ""),
                        null, SQLiteDatabase.NO_LOCALIZED_COLLATORS + SQLiteDatabase.OPEN_READONLY);
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            progress.setMessage(getString(R.string.loading) + " " + values[0] + " " + getString(R.string.geocaches));
        }

        protected Exception doInBackground(Point... pointSet) {
            try {
                if (isCancelled()) {
                    return null;
                }

                List<Pair> gcCodes = new ArrayList<>(256);
                Set<String> alreadyLoaded = new HashSet<>(256);

                Location curr = pointSet[0].getLocation();
                if (db != null) {
                    GsakReader.loadGCCodes(LoadActivity.this, this, db, gcCodes, alreadyLoaded, curr);
                    if (isCancelled()) {
                        return null;
                    }
                }
                if (db2 != null) {
                    GsakReader.loadGCCodes(LoadActivity.this, this, db2, gcCodes, alreadyLoaded, curr);
                    if (isCancelled()) {
                        return null;
                    }
                }
                if (db3 != null) {
                    GsakReader.loadGCCodes(LoadActivity.this, this, db3, gcCodes, alreadyLoaded, curr);
                    if (isCancelled()) {
                        return null;
                    }
                }

                int count = 0;
                int limit = parseInt(getDefaultSharedPreferences(LoadActivity.this).getString("limit", "0"));

                if (limit > 0) {
                    Collections.sort(gcCodes, new Comparator<Pair>() {
                        public int compare(Pair p1, Pair p2) {
                            return Float.compare(p1.distance, p2.distance);
                        }
                    });
                }

                pd = new PackPoints("GSAK data");
                for (Pair pair : gcCodes) {
                    if (isCancelled()) {
                        return null;
                    }
                    if (limit > 0 && count >= limit) {
                            break;
                    }
                    String gcCode = pair.gcCode;
                    if (++count % 10 == 0) {
                        publishProgress(count);
                    }
                    SQLiteDatabase database = pair.db;
                    Point p = GsakReader.readGeocache(database, gcCode, false);
                    if (p != null) {
                        pd.addPoint(p);
                    }
                }

                if (isCancelled()) {
                    return null;
                }
                return null;
            } catch (Exception e) {
                return e;
            }
        }

        @Override
        protected void onPostExecute(Exception ex) {
            closeDatabases();
            progress.dismiss();

            if (ex != null) {
                Toast.makeText(LoadActivity.this, getString(R.string.unable_to_load_geocaches) + " (" + ex.getLocalizedMessage() + ")", Toast.LENGTH_LONG).show();
                LoadActivity.this.finish();
                return;
            }

//            String filePath = fd.getParent() + File.separator + "data.locus";

            try {
                ActionDisplayVarious.ExtraAction action = getDefaultSharedPreferences(LoadActivity.this).getBoolean("import", true) ?
                        ActionDisplayVarious.ExtraAction.IMPORT :
                        ActionDisplayVarious.ExtraAction.CENTER;
                ActionDisplayPoints.INSTANCE.sendPack(LoadActivity.this, pd, action);

/*                DisplayData.sendDataFile(LoadActivity.this,
                        data,
                        filePath,
                        getDefaultSharedPreferences(LoadActivity.this).getBoolean("import", true));*/
            } catch (OutOfMemoryError e) {
                AlertDialog.Builder ad = new AlertDialog.Builder(LoadActivity.this);
                ad.setIcon(android.R.drawable.ic_dialog_alert);
                ad.setTitle(R.string.error);
                ad.setMessage(R.string.out_of_memory);
                ad.setPositiveButton(android.R.string.ok, new OnClickListener() {

                    public void onClick(DialogInterface di, int arg1) {
                        di.dismiss();
                    }
                });
                ad.show();
            } catch (RequiredVersionMissingException rvme) {
                Toast.makeText(LoadActivity.this, "Error: " + rvme.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        }

        private void closeDatabases() {
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
            progress.dismiss();
            Toast.makeText(LoadActivity.this, R.string.canceled, Toast.LENGTH_LONG).show();
            LoadActivity.this.finish();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        progress = new ProgressDialog(this);
        progress.setMessage(getString(R.string.loading_dots));
        progress.setIcon(android.R.drawable.ic_dialog_info);
        progress.setTitle(getString(R.string.loading));
        progress.setOnDismissListener(this);

        SharedPreferences sharedPreferences = getDefaultSharedPreferences(LoadActivity.this);
        File fd = new File(sharedPreferences.getString("db", ""));
        if (!Gsak.isGsakDatabase(fd)) {
            Toast.makeText(LoadActivity.this, R.string.no_db_file, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        if (sharedPreferences.getBoolean("pref_use_db2", false) &&
                !Gsak.isGsakDatabase(new File(sharedPreferences.getString("db2", "")))) {
            Toast.makeText(LoadActivity.this, R.string.no_db_file, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        if (sharedPreferences.getBoolean("pref_use_db3", false) &&
                !Gsak.isGsakDatabase(new File(sharedPreferences.getString("db3", "")))) {
            Toast.makeText(LoadActivity.this, R.string.no_db_file, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        try {
        Intent fromIntent = getIntent();
        if (IntentHelper.INSTANCE.isIntentPointTools(fromIntent)) {
            point = IntentHelper.INSTANCE.getPointFromIntent(this, fromIntent);
        } else if (IntentHelper.INSTANCE.isIntentMainFunctionGc(fromIntent)) {
            IntentHelper.INSTANCE.handleIntentMainFunctionGc(LoadActivity.this, fromIntent, new IntentHelper.OnIntentReceived() {
                @Override
                public void onReceived(@NotNull LocusVersion locusVersion, @Nullable Location gpsLocation, @Nullable Location mapCenterlocation) {
                    if (mapCenterlocation != null) {
                        point = new Point("Map center", mapCenterlocation);
                    }
                }

                @Override
                public void onFailed() {
                }
            });
        }
        if (point != null) {
            loadAsyncTask = new LoadAsyncTask();
            loadAsyncTask.execute(point);
        }
        } catch (RequiredVersionMissingException rvme) {
            Toast.makeText(LoadActivity.this, "Error: " + rvme.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }


}
