package ch.bubendorf.locusaddon.gsakdatabase.receiver;

import android.content.Context;
import android.os.AsyncTask;

import locus.api.objects.extra.Location;

/**
 * PointLoader
 *
 * @author Radim -kuratkoo- Vaculik <kuratkoo@gmail.com>
 */
public class PointLoader {

    // private static final String TAG = "PointLoader";
    private static PointLoader instance;

    private PointLoaderAsyncTask pointLoaderAsyncTask;

    public static PointLoader getInstance() {
        if (instance == null) {
            instance = new PointLoader();
        }
        return instance;
    }

    private PointLoader() {
    }

    public void run(final Context context, final Location center, final Location topLeft, final Location bottomRight) {
//        Log.d(TAG, "run(" + center + ")");
        if (pointLoaderAsyncTask != null && pointLoaderAsyncTask.getStatus() != AsyncTask.Status.FINISHED) {
            pointLoaderAsyncTask.cancel(true);
        }
        pointLoaderAsyncTask = new PointLoaderAsyncTask(context);
        pointLoaderAsyncTask.execute(center, topLeft, bottomRight);
    }

}
