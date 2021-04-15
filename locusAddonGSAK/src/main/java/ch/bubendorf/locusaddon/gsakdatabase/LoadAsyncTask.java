package ch.bubendorf.locusaddon.gsakdatabase;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.widget.Toast;

import java.util.List;

import ch.bubendorf.locusaddon.gsakdatabase.util.CacheWrapper;
import ch.bubendorf.locusaddon.gsakdatabase.util.GeocacheAsyncTask;
import ch.bubendorf.locusaddon.gsakdatabase.util.GsakReader;
import locus.api.android.ActionDisplayPoints;
import locus.api.android.ActionDisplayVarious;
import locus.api.android.utils.exceptions.RequiredVersionMissingException;
import locus.api.objects.extra.Location;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;

public class LoadAsyncTask extends GeocacheAsyncTask implements DialogInterface.OnDismissListener {

    @SuppressLint("StaticFieldLeak")
    private final Activity activity;

    private final ProgressDialog progress;

    public LoadAsyncTask(final Activity activity) {
        this.activity = activity;

        progress = new ProgressDialog(activity);
        progress.setMessage(activity.getString(R.string.loading_dots));
        progress.setIcon(android.R.drawable.ic_dialog_info);
        progress.setTitle(activity.getString(R.string.loading));
        progress.setOnDismissListener(this);
    }

    @Override
    public void onDismiss(final DialogInterface arg0) {
        cancel(true);
    }

    @Override
    protected void onPreExecute() {
        progress.show();

        openDatabases(activity);
    }

    @Override
    public void onProgressUpdate(final Integer... values) {
        progress.setMessage(activity.getString(R.string.loading) + " " + values[0] + " " + activity.getString(R.string.geocaches));
    }

    protected Exception doInBackground(final Location... locations) {
        try {
            if (isCancelled()) {
                return null;
            }

//            myPublishProgress(0);
            final List<CacheWrapper> gcCodes = GsakReader.readGCCodes(activity, this,
                    db, db2, db3, locations[0], null, null);
            packPoints = GsakReader.readGeocaches(this, gcCodes);
            return null;
        } catch (final Exception e) {
            return e;
        }
    }

    @Override
    protected void onPostExecute(final Exception ex) {
        closeDatabases();
        progress.dismiss();

        if (ex != null) {
            Toast.makeText(activity, activity.getString(R.string.unable_to_load_geocaches) + " (" + ex.getLocalizedMessage() + ")", Toast.LENGTH_LONG).show();
            activity.finish();
            return;
        }

        if (packPoints != null && packPoints.getPoints().length > 0) {
            try {
                final ActionDisplayVarious.ExtraAction action = getDefaultSharedPreferences(activity).getBoolean("import", true) ?
                        ActionDisplayVarious.ExtraAction.IMPORT :
                        ActionDisplayVarious.ExtraAction.CENTER;
                ActionDisplayPoints.INSTANCE.sendPack(activity, packPoints, action);
            } catch (final OutOfMemoryError e) {
                final AlertDialog.Builder ad = new AlertDialog.Builder(activity);
                ad.setIcon(android.R.drawable.ic_dialog_alert);
                ad.setTitle(R.string.error);
                ad.setMessage(R.string.out_of_memory);
                ad.setPositiveButton(android.R.string.ok, (di, arg1) -> di.dismiss());
                ad.show();
            } catch (final RequiredVersionMissingException rvme) {
                Toast.makeText(activity, "Error: " + rvme.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        }
        activity.finish();
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        progress.dismiss();
        Toast.makeText(activity, R.string.canceled, Toast.LENGTH_LONG).show();
        activity.finish();
    }
}