package net.kuratkoo.locusaddon.gsakdatabase;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Toast;

import net.kuratkoo.locusaddon.gsakdatabase.util.Gsak;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import menion.android.locus.addon.publiclib.DisplayData;
import menion.android.locus.addon.publiclib.LocusIntents;
import menion.android.locus.addon.publiclib.geoData.Point;
import menion.android.locus.addon.publiclib.geoData.PointGeocachingData;
import menion.android.locus.addon.publiclib.geoData.PointGeocachingDataWaypoint;
import menion.android.locus.addon.publiclib.geoData.PointsData;
import menion.android.locus.addon.publiclib.utils.RequiredVersionMissingException;

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

        @Override
        protected void onPreExecute() {
            progress.show();
            db = SQLiteDatabase.openDatabase(
                    PreferenceManager.getDefaultSharedPreferences(LoadActivity.this).getString("db", ""),
                    null, SQLiteDatabase.NO_LOCALIZED_COLLATORS + SQLiteDatabase.OPEN_READONLY);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            progress.setMessage(getString(R.string.loading) + " " + values[0] + " " + getString(R.string.geocaches));
        }

        protected Exception doInBackground(Point... pointSet) {
            try {
                if (this.isCancelled()) {
                    return null;
                }

                Point pp = pointSet[0];
                Location curr = pp.getLocation();
                PointsData pd = new PointsData("GSAK data");
                float radius = Float.parseFloat(PreferenceManager.getDefaultSharedPreferences(LoadActivity.this).getString("radius", "1")) / 70;

                String[] cond = new String[]{
                    String.valueOf(curr.getLatitude() - radius),
                    String.valueOf(curr.getLatitude() + radius),
                    String.valueOf(curr.getLongitude() - radius),
                    String.valueOf(curr.getLongitude() + radius)
                };
                Cursor c;
                String sql = "SELECT Latitude, Longitude, Code, PlacedBy FROM Caches WHERE (status = \"A\"";

                // Disable geocaches
                if (PreferenceManager.getDefaultSharedPreferences(LoadActivity.this).getBoolean("disable", false)) {
                    sql = sql + " OR status = \"T\"";
                }

                // Archived geocaches
                if (PreferenceManager.getDefaultSharedPreferences(LoadActivity.this).getBoolean("archive", false)) {
                    sql = sql + " OR status = \"X\"";
                }

                sql = sql + ") ";

                if (!PreferenceManager.getDefaultSharedPreferences(LoadActivity.this).getBoolean("found", false)) {
                    sql = sql + " AND Found = 0";
                }

                if (!PreferenceManager.getDefaultSharedPreferences(LoadActivity.this).getBoolean("own", false)) {
                    sql = sql + " AND PlacedBy != \"" + PreferenceManager.getDefaultSharedPreferences(LoadActivity.this).getString("nick", "") + "\"";
                }

                List<String> geocacheTypes = Gsak.geocacheTypesFromFilter(PreferenceManager.getDefaultSharedPreferences(LoadActivity.this));
                boolean first = true;
                StringBuilder sqlType = new StringBuilder();
                for (String geocacheType : geocacheTypes) {
                    if (first) {
                        sqlType.append(geocacheType);
                        first = false;
                    } else {
                        sqlType.append(" OR ").append(geocacheType);
                    }
                }
                if (!sqlType.toString().equals("")) {
                    sql += " AND (" + sqlType + ")";
                }

                sql += " AND CAST(Latitude AS REAL) > ? AND CAST(Latitude AS REAL) < ? AND CAST(Longitude AS REAL) > ? AND CAST(Longitude AS REAL) < ?";

                c = db.rawQuery(sql, cond);

                /* Load GC codes */
                List<Pair> gcCodes = new ArrayList<>();
                while (c.moveToNext()) {
                    if (this.isCancelled()) {
                        c.close();
                        return null;
                    }
                    Location loc = new Location(TAG);
                    loc.setLatitude(c.getDouble(c.getColumnIndex("Latitude")));
                    loc.setLongitude(c.getDouble(c.getColumnIndex("Longitude")));
                    if (loc.distanceTo(curr) < Float.parseFloat(PreferenceManager.getDefaultSharedPreferences(LoadActivity.this).getString("radius", "1")) * 1000) {
                        gcCodes.add(new Pair(loc.distanceTo(curr), c.getString(c.getColumnIndex("Code"))));
                    }
                }
                c.close();

                int count = 0;
                int limit = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(LoadActivity.this).getString("limit", "0"));

                if (limit > 0) {
                    Collections.sort(gcCodes, new Comparator<Pair>() {

                        public int compare(Pair p1, Pair p2) {
                            return p1.distance.compareTo(p2.distance);
                        }
                    });
                }

                for (Pair pair : gcCodes) {
                    if (this.isCancelled()) {
                        return null;
                    }
                    if (limit > 0) {
                        if (count >= limit) {
                            break;
                        }
                    }
                    String gcCode = pair.gcCode;
                    if (++count % 10 == 0) {
                        publishProgress(count);
                    }
                    c = db.rawQuery("SELECT * FROM CachesAll WHERE Code = ?", new String[]{gcCode});
                    c.moveToNext();
                    Location loc = new Location(TAG);
                    loc.setLatitude(c.getDouble(c.getColumnIndex("Latitude")));
                    loc.setLongitude(c.getDouble(c.getColumnIndex("Longitude")));
                    Point p = new Point(c.getString(c.getColumnIndex("Name")), loc);

                    PointGeocachingData gcData = new PointGeocachingData();
                    gcData.cacheID = c.getString(c.getColumnIndex("Code"));
                    gcData.name = c.getString(c.getColumnIndex("Name"));
                    gcData.owner = c.getString(c.getColumnIndex("PlacedBy"));
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
                    gcData.premiumOnly = Gsak.isPremium(c.getInt(c.getColumnIndex("Found")));
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

                    Cursor wp = db.rawQuery("SELECT * FROM WayAll WHERE cParent = ?", new String[]{gcData.cacheID});
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

                if (this.isCancelled()) {
                    return null;
                }
                return null;
            } catch (Exception e) {
                return e;
            }
        }

        @Override
        protected void onPostExecute(Exception ex) {
            db.close();
            db = null;
            progress.dismiss();

            if (ex != null) {
                Toast.makeText(LoadActivity.this, getString(R.string.unable_to_load_geocaches) + " (" + ex.getLocalizedMessage() + ")", Toast.LENGTH_LONG).show();
                LoadActivity.this.finish();
                return;
            }

            String filePath = fd.getParent() + "data.locus";

            try {
                DisplayData.sendDataFile(LoadActivity.this,
                        data,
                        filePath,
                        PreferenceManager.getDefaultSharedPreferences(LoadActivity.this).getBoolean("import", true));
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

        @Override
        protected void onCancelled() {
            super.onCancelled();
            db.close();
            db = null;
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

        fd = new File(PreferenceManager.getDefaultSharedPreferences(LoadActivity.this).getString("db", ""));
        if (!Gsak.isGsakDatabase(fd)) {
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
        private final Float distance;

        public Pair(Float f, String s) {
            this.distance = f;
            this.gcCode = s;
        }
    }
}
