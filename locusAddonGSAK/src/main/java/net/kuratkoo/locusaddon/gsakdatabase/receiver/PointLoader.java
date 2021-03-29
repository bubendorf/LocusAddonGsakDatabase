package net.kuratkoo.locusaddon.gsakdatabase.receiver;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import net.kuratkoo.locusaddon.gsakdatabase.util.Gsak;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import menion.android.locus.addon.publiclib.DisplayData;
import menion.android.locus.addon.publiclib.PeriodicUpdate;
import menion.android.locus.addon.publiclib.PeriodicUpdate.UpdateContainer;
import menion.android.locus.addon.publiclib.geoData.Point;
import menion.android.locus.addon.publiclib.geoData.PointGeocachingData;
import menion.android.locus.addon.publiclib.geoData.PointsData;
import menion.android.locus.addon.publiclib.utils.RequiredVersionMissingException;

/**
 * PointLoader
 * @author Radim -kuratkoo- Vaculik <kuratkoo@gmail.com>
 */
public class PointLoader {

    private static final String TAG = "PointLoader";
    private static PointLoader mInstance;
    private Context context;
    private Intent intent;
    private MapLoadAsyncTask mapLoadAsyncTask;

    public static PointLoader getInstance() {
        if (mInstance == null) {
            mInstance = new PointLoader();
        }
        return mInstance;
    }

    private PointLoader() {
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public void setIntent(Intent intent) {
        this.intent = intent;
    }

    public void run() {
        PeriodicUpdate pu = PeriodicUpdate.getInstance();
        pu.setLocNotificationLimit(50.0);
        pu.onReceive(context, intent, new PeriodicUpdate.OnUpdate() {

            public void onUpdate(UpdateContainer update) {
                if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("livemap", false)
                        && !PreferenceManager.getDefaultSharedPreferences(context).getString("db", "").equals("")) {
                    if ((update.newMapCenter || update.newZoomLevel) && update.mapVisible) {
                        if (mapLoadAsyncTask == null || mapLoadAsyncTask.getStatus() == AsyncTask.Status.FINISHED) {
                            mapLoadAsyncTask = new MapLoadAsyncTask();
                            mapLoadAsyncTask.execute(update);
                        } else {
                            mapLoadAsyncTask.cancel(true);
                            mapLoadAsyncTask = new MapLoadAsyncTask();
                            mapLoadAsyncTask.execute(update);
                        }
                    }
                }
            }

            public void onIncorrectData() {
            }
        });
    }

    private class MapLoadAsyncTask extends AsyncTask<UpdateContainer, Integer, Exception> {

        private PointsData pd;
        private SQLiteDatabase db;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            db = SQLiteDatabase.openDatabase(PreferenceManager.getDefaultSharedPreferences(context).getString("db", ""), null, SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        }

        @Override
        protected Exception doInBackground(UpdateContainer... updateSet) {
            try {
                if (this.isCancelled()) {
                    return null;
                }

                UpdateContainer update = updateSet[0];
                pd = new PointsData("GSAK live data");

                String[] cond = new String[]{
                    String.valueOf(update.mapBottomRight.getLatitude()),
                    String.valueOf(update.mapTopLeft.getLatitude()),
                    String.valueOf(update.mapTopLeft.getLongitude()),
                    String.valueOf(update.mapBottomRight.getLongitude())
                };

                String sql = "SELECT Code, Name, Latitude, Longitude, CacheType, HasCorrected, PlacedBy, Status, Found FROM Caches WHERE (status = \"A\"";
                // Disable geocaches
                if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("disable", false)) {
                    sql = sql + " OR status = \"T\"";
                }

                // Archived geocaches
                if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("archive", false)) {
                    sql = sql + " OR status = \"X\"";
                }

                sql = sql + ") ";

                if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean("found", false)) {
                    sql = sql + " AND Found = 0";
                }

                if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean("own", false)) {
                    sql = sql + " AND PlacedBy != \"" + PreferenceManager.getDefaultSharedPreferences(context).getString("nick", "") + "\"";
                }

                List<String> geocacheTypes = Gsak.geocacheTypesFromFilter(PreferenceManager.getDefaultSharedPreferences(context));
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

                Cursor c = db.rawQuery(sql, cond);

                if (this.isCancelled()) {
                    c.close();
                    return null;
                }

                while (c.moveToNext()) {
                    if (this.isCancelled()) {
                        c.close();
                        return null;
                    }
                    Location loc = new Location(TAG);
                    loc.setLatitude(c.getDouble(c.getColumnIndex("Latitude")));
                    loc.setLongitude(c.getDouble(c.getColumnIndex("Longitude")));
                    Point p = new Point(c.getString(c.getColumnIndex("Name")), loc);

                    PointGeocachingData gcData = new PointGeocachingData();
                    gcData.cacheID = c.getString(c.getColumnIndex("Code"));
                    gcData.name = c.getString(c.getColumnIndex("Name"));
                    gcData.owner = c.getString(c.getColumnIndex("PlacedBy"));
                    gcData.type = Gsak.convertCacheType(c.getString(c.getColumnIndex("CacheType")));
                    gcData.available = Gsak.isAvailable(c.getString(c.getColumnIndex("Status")));
                    gcData.archived = Gsak.isArchived(c.getString(c.getColumnIndex("Status")));
                    gcData.computed = Gsak.isCorrected(c.getInt(c.getColumnIndex("HasCorrected")));
                    gcData.found = Gsak.isFound(c.getInt(c.getColumnIndex("Found")));

                    p.setGeocachingData(gcData);
                    p.setExtraOnDisplay("net.kuratkoo.locusaddon.gsakdatabase", "net.kuratkoo.locusaddon.gsakdatabase.DetailActivity", "cacheId", gcData.cacheID);
                    pd.addPoint(p);
                }
                c.close();
            } catch (Exception ex) {
                return ex;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Exception exception) {
            super.onPostExecute(exception);
            db.close();
            if (exception != null) {
                Log.w(TAG, exception);
                Toast.makeText(context, "Error: " + exception.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }

            try {
                File fd = new File(PreferenceManager.getDefaultSharedPreferences(context).getString("db", ""));
                String filePath = fd.getParent() + "livemap.locus";

                ArrayList<PointsData> data = new ArrayList<>();
                data.add(pd);
                DisplayData.sendDataFileSilent(context, data, filePath);
            } catch (RequiredVersionMissingException rvme) {
                Toast.makeText(context, "Error: " + rvme.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            db.close();
        }
    }
}
