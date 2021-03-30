package net.kuratkoo.locusaddon.gsakdatabase;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Toast;

import net.kuratkoo.locusaddon.gsakdatabase.util.Gsak;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import menion.android.locus.addon.publiclib.DisplayData;
import menion.android.locus.addon.publiclib.LocusIntents;
import menion.android.locus.addon.publiclib.geoData.Point;
import menion.android.locus.addon.publiclib.geoData.PointGeocachingData;
import menion.android.locus.addon.publiclib.geoData.PointGeocachingDataWaypoint;
import menion.android.locus.addon.publiclib.geoData.PointsData;
import menion.android.locus.addon.publiclib.utils.RequiredVersionMissingException;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;
import static java.lang.Float.parseFloat;
import static java.lang.Integer.parseInt;

/**
 * LoadActivity
 * @author Radim -kuratkoo- Vaculik <kuratkoo@gmail.com>
 */
public class LoadActivity extends Activity implements DialogInterface.OnDismissListener {

    private static final String TAG = "LoadActivity";
    private ProgressDialog progress;
    private ArrayList<PointsData> data;
    private File fd;
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
                    loadGCCodes(db, gcCodes, alreadyLoaded, curr);
                    if (isCancelled()) {
                        return null;
                    }
                }
                if (db2 != null) {
                    loadGCCodes(db2, gcCodes, alreadyLoaded, curr);
                    if (isCancelled()) {
                        return null;
                    }
                }
                if (db3 != null) {
                    loadGCCodes(db3, gcCodes, alreadyLoaded, curr);
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

                PointsData pd = new PointsData("GSAK data");
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
                    Cursor c = pair.db.rawQuery("SELECT * FROM CachesAll WHERE Code = ?", new String[]{gcCode});
                    c.moveToNext();
                    Location loc = new Location(TAG);
                    loc.setLatitude(c.getDouble(c.getColumnIndex("Latitude")));
                    loc.setLongitude(c.getDouble(c.getColumnIndex("Longitude")));
                    Point p = new Point(c.getString(c.getColumnIndex("Name")), loc);

                    PointGeocachingData gcData = new PointGeocachingData();
                    gcData.cacheID = c.getString(c.getColumnIndex("Code"));
                    gcData.name = c.getString(c.getColumnIndex("Name"));
                    gcData.owner = c.getString(c.getColumnIndex("OwnerName"));
                    gcData.placedBy = c.getString(c.getColumnIndex("PlacedBy"));
                    gcData.difficulty = c.getFloat(c.getColumnIndex("Difficulty"));
                    gcData.terrain = c.getFloat(c.getColumnIndex("Terrain"));
                    gcData.country = c.getString(c.getColumnIndex("Country"));
                    gcData.state = c.getString(c.getColumnIndex("State"));
                    gcData.container = Gsak.convertContainer(c.getString(c.getColumnIndex("Container")));
                    gcData.type = Gsak.convertCacheType(c.getString(c.getColumnIndex("CacheType")));
                    gcData.available = Gsak.isAvailable(c.getString(c.getColumnIndex("Status")));
                    gcData.archived = Gsak.isArchived(c.getString(c.getColumnIndex("Status")));
                    gcData.found = Gsak.isFound(c.getInt(c.getColumnIndex("Found")));
                    gcData.premiumOnly = Gsak.isPremium(c.getInt(c.getColumnIndex("IsPremium")));
                    gcData.computed = Gsak.isCorrected(c.getInt(c.getColumnIndex("HasCorrected")));

                    @SuppressLint("SimpleDateFormat") SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                    Date date = new Date();
                    gcData.exported = dateFormat.format(date);

                    String lastUpdated = c.getString(c.getColumnIndex("LastUserDate"));
                    if (lastUpdated.length() == 10) {
                        gcData.lastUpdated = lastUpdated + "T";
                    }
                    gcData.hidden = c.getString(c.getColumnIndex("PlacedDate")) + "T";

                    c.close();

                    /* Add waypoints to Geocache */
                    ArrayList<PointGeocachingDataWaypoint> pgdws = new ArrayList<>();

                    Cursor wp = pair.db.rawQuery("SELECT * FROM WayAll WHERE cParent = ?", new String[]{gcData.cacheID});
                    while (wp.moveToNext()) {
                        if (this.isCancelled()) {
                            wp.close();
                            return null;
                        }
                        PointGeocachingDataWaypoint pgdw = new PointGeocachingDataWaypoint();
                        pgdw.lat = wp.getDouble(wp.getColumnIndex("cLat"));
                        pgdw.lon = wp.getDouble(wp.getColumnIndex("cLon"));
                        pgdw.name = wp.getString(wp.getColumnIndex("cName"));
                        pgdw.type = Gsak.convertWaypointType(wp.getString(wp.getColumnIndex("cType")));
                        pgdw.description = wp.getString(wp.getColumnIndex("cComment"));
                        pgdw.code = wp.getString(wp.getColumnIndex("cCode"));
                        pgdws.add(pgdw);
                    }
                    wp.close();
                    gcData.waypoints = pgdws;

                    p.setGeocachingData(gcData);
                    p.setExtraOnDisplay("net.kuratkoo.locusaddon.gsakdatabase", "net.kuratkoo.locusaddon.gsakdatabase.DetailActivity", "cacheId", gcData.cacheID);
                    pd.addPoint(p);
                }

                data = new ArrayList<>();
                data.add(pd);

                if (isCancelled()) {
                    return null;
                }
                return null;
            } catch (Exception e) {
                return e;
            }
        }

        private void loadGCCodes(SQLiteDatabase database, List<Pair> gcCodes, Set<String> alreadyLoaded, Location curr) {
            String sql = buildCacheSQL();

            float radiusMeter = parseFloat(getDefaultSharedPreferences(LoadActivity.this).getString("radius", "25")) * 1000;
            float radiusLatLon = radiusMeter / 1000 / 70;
            float radiusNorthSouth = 360f / (40007863 / radiusMeter);
            float radiusEastWest = 360f / (40075017 / radiusMeter) / (float)Math.cos(curr.getLatitude() / 180 * Math.PI);
            String[] cond = new String[]{
                    String.valueOf(curr.getLatitude() - radiusNorthSouth),
                    String.valueOf(curr.getLatitude() + radiusNorthSouth),
                    String.valueOf(curr.getLongitude() - radiusEastWest),
                    String.valueOf(curr.getLongitude() + radiusEastWest)
            };
            /* Load GC codes */
            Location loc = new Location(TAG);
            Cursor c = database.rawQuery(sql, cond);
            while (c.moveToNext()) {
                if (isCancelled()) {
                    c.close();
                    return;
                }
                String code = c.getString(c.getColumnIndex("Code"));
                if (!alreadyLoaded.contains(code)) {
                    alreadyLoaded.add(code);
                    loc.setLatitude(c.getDouble(c.getColumnIndex("Latitude")));
                    loc.setLongitude(c.getDouble(c.getColumnIndex("Longitude")));
                    if (loc.distanceTo(curr) <= radiusMeter) {
                        gcCodes.add(new Pair(loc.distanceTo(curr), code, database));
                    }
                }
            }
            c.close();
        }

        private String buildCacheSQL() {
            StringBuilder sql = new StringBuilder(256);
            sql.append("SELECT Latitude, Longitude, Code FROM Caches WHERE (status = 'A'");

            // Disable geocaches
            SharedPreferences sharedPreferences = getDefaultSharedPreferences(LoadActivity.this);
            if (sharedPreferences.getBoolean("disable", false)) {
                sql.append(" OR status = 'T'");
            }
            // Archived geocaches
            if (sharedPreferences.getBoolean("archive", false)) {
                sql.append(" OR status = 'X'");
            }
            sql.append(") ");

            // Found and not Found
            boolean found = sharedPreferences.getBoolean("found", false);
            boolean notfound = sharedPreferences.getBoolean("notfound", true);
            if (found || notfound) {
                sql.append(" AND ( 1=0 ");
                if (found) {
                    sql.append(" OR Found = 1");
                }
                if (notfound) {
                    sql.append(" OR Found = 0");
                }
                sql.append(" ) ");
            }

            if (!sharedPreferences.getBoolean("own", false)) {
                sql.append(" AND PlacedBy != '");
                sql.append(sharedPreferences.getString("nick", ""));
                sql.append("'");
            }

            List<String> geocacheTypes = Gsak.geocacheTypesFromFilter(sharedPreferences);
            boolean first = true;
            StringBuilder sqlType = new StringBuilder(256);
            for (String geocacheType : geocacheTypes) {
                if (first) {
                    sqlType.append(geocacheType);
                    first = false;
                } else {
                    sqlType.append(" OR ").append(geocacheType);
                }
            }
            if (sqlType.length() > 0) {
                sql.append(" AND (");
                sql.append(sqlType);
                sql.append(")");
            }

            sql.append(" AND CAST(Latitude AS REAL) > ? AND CAST(Latitude AS REAL) < ? AND CAST(Longitude AS REAL) > ? AND CAST(Longitude AS REAL) < ?");
            return sql.toString();
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

            String filePath = fd.getParent() + File.separator + "data.locus";

            try {
                DisplayData.sendDataFile(LoadActivity.this,
                        data,
                        filePath,
                        getDefaultSharedPreferences(LoadActivity.this).getBoolean("import", true));
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
        fd = new File(sharedPreferences.getString("db", ""));
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

        Intent fromIntent = getIntent();
        if (LocusIntents.isIntentOnPointAction(fromIntent)) {
            point = LocusIntents.handleIntentOnPointAction(fromIntent);
        } else if (LocusIntents.isIntentMainFunction(fromIntent)) {
            LocusIntents.handleIntentMainFunction(fromIntent, new LocusIntents.OnIntentMainFunction() {

                public void onLocationReceived(boolean gpsEnabled, Location locGps, Location locMapCenter) {
                    point = new Point("Map center", locMapCenter);
                }

                public void onFailed() {
                }
            });
        }
        loadAsyncTask = new LoadAsyncTask();
        loadAsyncTask.execute(point);
    }

    private static class Pair {

        private final String gcCode;
        private final float distance;
        private final SQLiteDatabase db;

        public Pair(final float dist, final String code, final SQLiteDatabase db) {
            this.distance = dist;
            this.gcCode = code;
            this.db = db;
        }
    }
}
