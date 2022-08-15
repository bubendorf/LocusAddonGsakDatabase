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

import ch.bubendorf.locusaddon.gsakdatabase.util.Gsak;
import ch.bubendorf.locusaddon.gsakdatabase.util.ToastUtil;
import locus.api.android.objects.LocusVersion;
import locus.api.android.utils.IntentHelper;
import locus.api.android.utils.exceptions.RequiredVersionMissingException;
import locus.api.objects.extra.Location;
import locus.api.objects.geoData.Point;

import static androidx.preference.PreferenceManager.getDefaultSharedPreferences;

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
        ReadPermissionActivity.checkPermission(this, this::goOn, null, null, false);
    }

    private void goOn(final Context context, final Void data) {
        final SharedPreferences sharedPreferences = getDefaultSharedPreferences(LoadActivity.this);

        String error = Gsak.checkDatabase(LoadActivity.this, "db");
        if (error != null) {
            ToastUtil.show(LoadActivity.this, error, 5);
            finish();
            return;
        }

        error = Gsak.checkDatabase(LoadActivity.this, "db2");
        if (error != null) {
            ToastUtil.show(LoadActivity.this, error, 5);
            finish();
            return;
        }

        error = Gsak.checkDatabase(LoadActivity.this, "db3");
        if (error != null) {
            ToastUtil.show(LoadActivity.this, error, 5);
            finish();
            return;
        }

        final boolean pref_use_db = sharedPreferences.getBoolean("pref_use_db", false);
        final boolean pref_use_db2 = sharedPreferences.getBoolean("pref_use_db2", false);
        final boolean pref_use_db3 = sharedPreferences.getBoolean("pref_use_db3", false);
        if (!pref_use_db && !pref_use_db2 && !pref_use_db3) {
            final String text = getResources().getString(R.string.no_db_activated);
            ToastUtil.show(LoadActivity.this, text, 5);
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
            ToastUtil.show(LoadActivity.this, "Error: " + rvme.getLocalizedMessage(), 5);
        }
    }

    //public LocusVersion getLocusVersion() {
    //    return locusVersion;
    //}

    //public int getNumberOfInstalledLocus() {
    //    return numberOfInstalledLocus;
    //}
}
