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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

import ch.bubendorf.locusaddon.gsakdatabase.util.Gsak;
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
 * @author Markus Bubendorf <gsakforlocus@bubendorf.net>
 */
public class LoadActivity extends Activity {

    //    private static final String TAG = "LoadActivity";
    private Point point;
    //private int numberOfInstalledLocus = 0;
    //private LocusVersion locusVersion;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //numberOfInstalledLocus = LocusUtils.INSTANCE.getAvailableVersions(this).size()     ;

        // We need the permission to access the file system. Check and ask for the permission if necessary
        PermissionActivity.checkPermission(this, this::goOn, null, false);
    }

    private void goOn(final Context context, final Void data) {
        final SharedPreferences sharedPreferences = getDefaultSharedPreferences(LoadActivity.this);
        final String dbPath = sharedPreferences.getString("db", "");
        final File fd = new File(dbPath);
        final boolean pref_use_db = sharedPreferences.getBoolean("pref_use_db", false);
        if (pref_use_db && !Gsak.isReadableGsakDatabase(fd)) {
            final String text = getResources().getString(R.string.no_db_file);
            Toast.makeText(LoadActivity.this, text + " " + dbPath, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        final String db2Path = sharedPreferences.getString("db2", "");
        final boolean pref_use_db2 = sharedPreferences.getBoolean("pref_use_db2", false);
        if (pref_use_db2 && !Gsak.isReadableGsakDatabase(new File(db2Path))) {
            final String text = getResources().getString(R.string.no_db_file);
            Toast.makeText(LoadActivity.this, text + " " + db2Path, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        final String db3Path = sharedPreferences.getString("db3", "");
        final boolean pref_use_db3 = sharedPreferences.getBoolean("pref_use_db3", false);
        if (pref_use_db3 && !Gsak.isReadableGsakDatabase(new File(db3Path))) {
            final String text = getResources().getString(R.string.no_db_file);
            Toast.makeText(LoadActivity.this, text + " " + db3Path, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (!pref_use_db && !pref_use_db2 && !pref_use_db3) {
            final String text = getResources().getString(R.string.no_db_activated);
            Toast.makeText(LoadActivity.this, text, Toast.LENGTH_LONG).show();
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
                    public void onReceived(@NotNull final LocusVersion lv, @Nullable final Location gpsLocation, @Nullable final Location mapCenterlocation) {
                        if (mapCenterlocation != null) {
                            point = new Point("Map center", mapCenterlocation);
//                            locusVersion = lv;
                        }
                    }

                    @Override
                    public void onFailed() {
                    }
                });
            }
            if (point != null) {
                final LoadAsyncTask loadAsyncTask = new LoadAsyncTask(this);
                loadAsyncTask.execute(point.getLocation());
            }
        } catch (final RequiredVersionMissingException rvme) {
            Toast.makeText(LoadActivity.this, "Error: " + rvme.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }

    //public LocusVersion getLocusVersion() {
    //    return locusVersion;
    //}

    //public int getNumberOfInstalledLocus() {
    //    return numberOfInstalledLocus;
    //}
}
