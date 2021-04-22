package ch.bubendorf.locusaddon.gsakdatabase;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.util.Collections;
import java.util.List;

import ch.bubendorf.locusaddon.gsakdatabase.util.CacheWrapper;
import ch.bubendorf.locusaddon.gsakdatabase.util.GeocacheAsyncTask;
import ch.bubendorf.locusaddon.gsakdatabase.util.GsakReader;
import locus.api.android.ActionDisplayPoints;
import locus.api.android.ActionDisplayVarious;
import locus.api.android.objects.LocusVersion;
import locus.api.android.utils.LocusUtils;
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
            packPoints = GsakReader.readGeocaches(activity,this, gcCodes);
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
                final int size = packPoints.getAsBytes().length;
                Log.i("LoadAsyncTask", "Size of data: " + size);
                final ActionDisplayVarious.ExtraAction action = getDefaultSharedPreferences(activity).getBoolean("import", true) ?
                        ActionDisplayVarious.ExtraAction.IMPORT :
                        ActionDisplayVarious.ExtraAction.CENTER;
                if (size > 524288) {
                    final LocusVersion activeVersion = LocusUtils.INSTANCE.getActiveVersion(activity, 3);
                    final File file = getCacheFile(activity);
                    final Uri uri = FileProvider.getUriForFile(activity, activity.getString(R.string.file_provider_authority), file);
                    ActionDisplayPoints.INSTANCE.sendPacksFile(activity, activeVersion,
                            Collections.singletonList(packPoints), file, uri, action);
                } else {
                    ActionDisplayPoints.INSTANCE.sendPack(activity, packPoints, action);
                }
            } catch (final OutOfMemoryError e) {
                final AlertDialog.Builder alertDialog = new AlertDialog.Builder(activity);
                alertDialog.setIcon(android.R.drawable.ic_dialog_alert);
                alertDialog.setTitle(R.string.error);
                alertDialog.setMessage(R.string.out_of_memory);
                alertDialog.setPositiveButton(android.R.string.ok, (dialogInterface, arg1) -> dialogInterface.dismiss());
                alertDialog.show();
            } catch (final RequiredVersionMissingException rvme) {
                Toast.makeText(activity, "Error: " + rvme.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            } catch (final Exception e) {
                Toast.makeText(activity, "Error: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                Log.e("LoadAsyncTask", e.getMessage(), e);
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

    private File getCacheFile(final Context ctx) {
        // get filepath
        final File dir = new File(ctx.getCacheDir(), "shared");
        dir.mkdirs();

        // return generated file
        return new File(dir, "locus.data");
    }
}