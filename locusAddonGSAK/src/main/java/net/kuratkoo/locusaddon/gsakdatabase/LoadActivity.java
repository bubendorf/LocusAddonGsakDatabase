package net.kuratkoo.locusaddon.gsakdatabase;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import net.kuratkoo.locusaddon.gsakdatabase.util.GeocacheAsyncTask;
import net.kuratkoo.locusaddon.gsakdatabase.util.Gsak;
import net.kuratkoo.locusaddon.gsakdatabase.util.GsakReader;
import net.kuratkoo.locusaddon.gsakdatabase.util.Pair;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;

import locus.api.android.ActionDisplayPoints;
import locus.api.android.ActionDisplayVarious;
import locus.api.android.objects.LocusVersion;
import locus.api.android.utils.IntentHelper;
import locus.api.android.utils.exceptions.RequiredVersionMissingException;
import locus.api.objects.extra.Location;
import locus.api.objects.geoData.Point;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;

/**
 * LoadActivity
 *
 * @author Radim -kuratkoo- Vaculik <kuratkoo@gmail.com>
 */
public class LoadActivity extends Activity implements DialogInterface.OnDismissListener {

    private static final String TAG = "LoadActivity";
    private ProgressDialog progress;
    private Point point;
    private LoadAsyncTask loadAsyncTask;

    public void onDismiss(final DialogInterface arg0) {
        loadAsyncTask.cancel(true);
    }

    private class LoadAsyncTask extends GeocacheAsyncTask {

        @Override
        protected void onPreExecute() {
            progress.show();

            openDatabases(LoadActivity.this);
        }

        @Override
        public void onProgressUpdate(final Integer... values) {
            progress.setMessage(getString(R.string.loading) + " " + values[0] + " " + getString(R.string.geocaches));
        }

        protected Exception doInBackground(final Location... locations) {
            try {
                if (isCancelled()) {
                    return null;
                }

                final List<Pair> gcCodes = GsakReader.readGCCodes(LoadActivity.this, this,
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
                Toast.makeText(LoadActivity.this, getString(R.string.unable_to_load_geocaches) + " (" + ex.getLocalizedMessage() + ")", Toast.LENGTH_LONG).show();
                LoadActivity.this.finish();
                return;
            }

//            String filePath = fd.getParent() + File.separator + "data.locus";

            try {
                final ActionDisplayVarious.ExtraAction action = getDefaultSharedPreferences(LoadActivity.this).getBoolean("import", true) ?
                        ActionDisplayVarious.ExtraAction.IMPORT :
                        ActionDisplayVarious.ExtraAction.CENTER;
                ActionDisplayPoints.INSTANCE.sendPack(LoadActivity.this, packPoints, action);
            } catch (final OutOfMemoryError e) {
                final AlertDialog.Builder ad = new AlertDialog.Builder(LoadActivity.this);
                ad.setIcon(android.R.drawable.ic_dialog_alert);
                ad.setTitle(R.string.error);
                ad.setMessage(R.string.out_of_memory);
                ad.setPositiveButton(android.R.string.ok, new OnClickListener() {

                    public void onClick(final DialogInterface di, final int arg1) {
                        di.dismiss();
                    }
                });
                ad.show();
            } catch (final RequiredVersionMissingException rvme) {
                Toast.makeText(LoadActivity.this, "Error: " + rvme.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            progress.dismiss();
            Toast.makeText(LoadActivity.this, R.string.canceled, Toast.LENGTH_LONG).show();
            LoadActivity.this.finish();
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        progress = new ProgressDialog(this);
        progress.setMessage(getString(R.string.loading_dots));
        progress.setIcon(android.R.drawable.ic_dialog_info);
        progress.setTitle(getString(R.string.loading));
        progress.setOnDismissListener(this);

        final SharedPreferences sharedPreferences = getDefaultSharedPreferences(LoadActivity.this);
        final String dbPath = sharedPreferences.getString("db", "");
        final File fd = new File(dbPath);
        if (sharedPreferences.getBoolean("pref_use_db", true) &&
                !Gsak.isGsakDatabase(fd)) {
            final String text = getResources().getString(R.string.no_db_file);
            Toast.makeText(LoadActivity.this, text + " " + dbPath, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        final String db2Path = sharedPreferences.getString("db2", "");
        if (sharedPreferences.getBoolean("pref_use_db2", false) &&
                !Gsak.isGsakDatabase(new File(db2Path))) {
            final String text = getResources().getString(R.string.no_db_file);
            Toast.makeText(LoadActivity.this, text + " " + db2Path, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        final String db3Path = sharedPreferences.getString("db3", "");
        if (sharedPreferences.getBoolean("pref_use_db3", false) &&
                !Gsak.isGsakDatabase(new File(db3Path))) {
            final String text = getResources().getString(R.string.no_db_file);
            Toast.makeText(LoadActivity.this, text + " " + db3Path, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        try {
            final Intent fromIntent = getIntent();
            if (IntentHelper.INSTANCE.isIntentPointTools(fromIntent)) {
                point = IntentHelper.INSTANCE.getPointFromIntent(this, fromIntent);
            } else if (IntentHelper.INSTANCE.isIntentMainFunctionGc(fromIntent)) {
                IntentHelper.INSTANCE.handleIntentMainFunctionGc(LoadActivity.this, fromIntent, new IntentHelper.OnIntentReceived() {
                    @Override
                    public void onReceived(@NotNull final LocusVersion locusVersion, @Nullable final Location gpsLocation, @Nullable final Location mapCenterlocation) {
                        if (mapCenterlocation != null) {
                            point = new Point("Map center", mapCenterlocation);
                        }
                    }

                    @Override
                    public void onFailed() {
                    }
                });
            }
            if (point != null) {
                loadAsyncTask = new LoadAsyncTask();
                loadAsyncTask.execute(point.getLocation());
            }
        } catch (final RequiredVersionMissingException rvme) {
            Toast.makeText(LoadActivity.this, "Error: " + rvme.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }


}
