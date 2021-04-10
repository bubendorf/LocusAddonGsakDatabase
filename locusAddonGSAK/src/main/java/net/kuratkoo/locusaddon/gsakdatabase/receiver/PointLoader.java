package net.kuratkoo.locusaddon.gsakdatabase.receiver;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import net.kuratkoo.locusaddon.gsakdatabase.util.GeocacheAsyncTask;
import net.kuratkoo.locusaddon.gsakdatabase.util.GsakReader;
import net.kuratkoo.locusaddon.gsakdatabase.util.Pair;

import java.util.List;

import locus.api.android.ActionDisplayPoints;
import locus.api.objects.extra.Location;

/**
 * PointLoader
 *
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

    public void run(final Location center, final Location topLeft, final Location bottomRight) {
        Log.d(TAG, "run(" + center + ")");
        if (mapLoadAsyncTask != null && mapLoadAsyncTask.getStatus() != AsyncTask.Status.FINISHED) {
            mapLoadAsyncTask.cancel(true);
        }
        mapLoadAsyncTask = new MapLoadAsyncTask();
        mapLoadAsyncTask.execute(center, topLeft, bottomRight);
    }

    private class MapLoadAsyncTask extends GeocacheAsyncTask {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            openDatabases(context);
        }

        @Override
        protected Exception doInBackground(final Location... locations) {
            try {
                if (isCancelled()) {
                    return null;
                }

                final List<Pair> gcCodes = GsakReader.readGCCodes(PointLoader.this.context, this,
                        db, db2, db3, locations[0], locations[1], locations[2]);
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
                ActionDisplayPoints.INSTANCE.sendPackSilent(PointLoader.this.context, packPoints, false);
            } catch (final Exception e) {
                Toast.makeText(PointLoader.this.context, "Error: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        }

    }
}
