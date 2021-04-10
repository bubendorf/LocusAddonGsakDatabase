package net.kuratkoo.locusaddon.gsakdatabase.util;

import android.os.AsyncTask;

import locus.api.objects.extra.Location;

public abstract class GeocacheAsyncTask extends AsyncTask<Location, Integer, Exception> {

    @Override
    public void onProgressUpdate(final Integer... values) {
        // Empty
    }

    public final void myPublishProgress(final Integer... values) {
        super.publishProgress(values);
    }
}
