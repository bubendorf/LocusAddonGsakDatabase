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

package ch.bubendorf.locusaddon.gsakdatabase;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.text.ParseException;
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

import static androidx.preference.PreferenceManager.getDefaultSharedPreferences;
import static locus.api.android.ActionDisplayVarious.ExtraAction.CENTER;
import static locus.api.android.ActionDisplayVarious.ExtraAction.IMPORT;
import static locus.api.android.ActionDisplayVarious.ExtraAction.NONE;

import org.acra.ACRA;

public class LoadAsyncTask extends GeocacheAsyncTask implements DialogInterface.OnDismissListener {

    @SuppressLint("StaticFieldLeak")
    private final LoadActivity activity;

    private final ProgressDialog progress;

    private int step;

    public LoadAsyncTask(final LoadActivity activity) {
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

        //ACRA.getErrorReporter().handleException(new RuntimeException("ACRA TEST"));
        /*if (1 == 1) {
            throw new RuntimeException("ACRA Test");
        }*/
    }

    @Override
    public void onProgressUpdate(final Integer... values) {
        if (step == 1) {
            progress.setMessage(activity.getString(R.string.loading) + " " + values[0] + " / " + values[1] + " " + activity.getString(R.string.databases));
        } else if (step == 2) {
            progress.setMessage(activity.getString(R.string.loading) + " " + values[0] + " / " + values[1] + " " + activity.getString(R.string.geocaches));
        } else {
            progress.setMessage("");
        }
    }

    protected Void doInBackground(final Location... locations) {
        if (isCancelled()) {
            return null;
        }

//            myPublishProgress(0);
        step = 1;
        final List<CacheWrapper> gcCodes = GsakReader.readGCCodes(activity, this,
                db, db2, db3, locations[0], null, null);

//        ACRA.getErrorReporter().handleException(new RuntimeException("ACRA doInBackground"));

        step = 2;
        try {
            packPoints = GsakReader.readGeocaches(activity, this, gcCodes);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        return null;
    }

    @Override
    protected void onPostExecute(final Void v) {
        super.onPostExecute(v);

        closeDatabases();
        progress.dismiss();

        if (packPoints != null && packPoints.getPoints().length > 0) {
            try {
//                Log.i("LoadAsyncTask");
                boolean importAction = getDefaultSharedPreferences(activity).getBoolean("import", true);
                boolean centerAction = getDefaultSharedPreferences(activity).getBoolean("center", true);
                final ActionDisplayVarious.ExtraAction action = importAction ?
                        IMPORT :
                        centerAction ? CENTER : NONE;
                if (packPoints.getPoints().length > 700 /*|| activity.getNumberOfInstalledLocus() > 1*/) {
                    // Use the 'Storage' method and send the PackPoints to one specific Locus
                    final LocusVersion activeVersion = /*activity.getLocusVersion() != null ?
                            activity.getLocusVersion() :*/
                            LocusUtils.INSTANCE.getActiveVersion(activity, 3);
                    if (activeVersion != null) {
                        final File file = getCacheFile(activity);
                        final Uri uri = FileProvider.getUriForFile(activity, activity.getString(R.string.file_provider_authority), file);
                        ActionDisplayPoints.INSTANCE.sendPacksFile(activity, activeVersion,
                                Collections.singletonList(packPoints), file, uri, action);
                    }
                } else {
                    // Use the 'Memory' method and send the PackPoints to the active(?) Locus
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
        } else {
            Toast.makeText(activity, activity.getString(R.string.no_geocaches_loaded), Toast.LENGTH_LONG).show();
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
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();

        // return generated file
        return new File(dir, "locus.data");
    }
}