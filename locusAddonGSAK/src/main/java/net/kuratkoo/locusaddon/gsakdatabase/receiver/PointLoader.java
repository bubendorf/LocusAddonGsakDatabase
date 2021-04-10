package net.kuratkoo.locusaddon.gsakdatabase.receiver;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import net.kuratkoo.locusaddon.gsakdatabase.util.GeocacheAsyncTask;
import net.kuratkoo.locusaddon.gsakdatabase.util.GsakReader;
import net.kuratkoo.locusaddon.gsakdatabase.util.Pair;

import java.util.List;

import locus.api.android.ActionDisplayPoints;
import locus.api.android.objects.PackPoints;
import locus.api.objects.extra.Location;

/**
 * PointLoader
 * @author Radim -kuratkoo- Vaculik <kuratkoo@gmail.com>
 */
public class PointLoader {

    private static final String TAG = "PointLoader";
    private static PointLoader instance;

    private Context context;
    private MapLoadAsyncTask mapLoadAsyncTask;

    public static PointLoader getInstance() {
        if (instance == null) {
            instance = new PointLoader();
        }
        return instance;
    }

    private PointLoader() {
    }

    public void setContext(final Context context) {
        this.context = context;
    }

    public void run(final Location center) {
        Log.d(TAG, "run(" + center + ")");
        if (mapLoadAsyncTask != null && mapLoadAsyncTask.getStatus() != AsyncTask.Status.FINISHED) {
            mapLoadAsyncTask.cancel(true);
        }
        mapLoadAsyncTask = new MapLoadAsyncTask();
        mapLoadAsyncTask.execute(center);
    }

    private class MapLoadAsyncTask extends GeocacheAsyncTask {
        private SQLiteDatabase db;
        private SQLiteDatabase db2;
        private SQLiteDatabase db3;

        private PackPoints packPoints;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            db = GsakReader.openDatabase(context, "db");
            db2 = GsakReader.openDatabase(context, "db2");
            db3 = GsakReader.openDatabase(context, "db3");
        }

        @Override
        protected Exception doInBackground(final Location... locations) {
            try {
                if (isCancelled()) {
                    return null;
                }

                final List<Pair> gcCodes = GsakReader.readGCCodes(PointLoader.this.context, this, db, db2, db3, locations[0]);
                packPoints = GsakReader.readGeocaches(this, gcCodes);
                return null;
            } catch (final Exception e) {
                return e;
            }
        }

        @Override
        protected void onPostExecute(final Exception exception) {
            super.onPostExecute(exception);
            closeDatabases();

            if (exception != null) {
//                Log.w(TAG, exception);
                Toast.makeText(context, "Error: " + exception.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                return;
            }

            try {
                //final ActionDisplayVarious.ExtraAction action = ActionDisplayVarious.ExtraAction.CENTER;
                ActionDisplayPoints.INSTANCE.sendPackSilent(PointLoader.this.context, packPoints, false);

                /*Intent intent = new Intent();
                intent.setAction(LocusConst.ACTION_DISPLAY_DATA_SILENTLY);
                intent.putExtra(LocusConst.INTENT_EXTRA_POINTS_DATA, packPoints.getAsBytes());

                context.startActivity(intent);*/

            } catch (final Exception e) {
                Toast.makeText(PointLoader.this.context, "Error: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }

            /*

            try {
                File fd = new File(PreferenceManager.getDefaultSharedPreferences(context).getString("db", ""));
                String filePath = fd.getParent() + File.separator + "livemap.locus";

                ArrayList<PointsData> data = new ArrayList<>();
                data.add(pd);
                DisplayData.sendDataFileSilent(context, data, filePath);
            } catch (RequiredVersionMissingException rvme) {
                Toast.makeText(context, "Error: " + rvme.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }*/
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
        }
    }
}
