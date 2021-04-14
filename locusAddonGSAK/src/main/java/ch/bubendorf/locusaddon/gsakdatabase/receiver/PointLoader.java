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

package ch.bubendorf.locusaddon.gsakdatabase.receiver;

import android.content.Context;
import android.os.AsyncTask;

import locus.api.objects.extra.Location;

/**
 * PointLoader
 *
 * @author Radim -kuratkoo- Vaculik <kuratkoo@gmail.com>
 * @author Markus Bubendorf <gsakforlocus@bubendorf.net>
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
