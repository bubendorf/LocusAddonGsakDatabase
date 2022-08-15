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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

import ch.bubendorf.locusaddon.gsakdatabase.PermissionActivity;
import ch.bubendorf.locusaddon.gsakdatabase.R;
import ch.bubendorf.locusaddon.gsakdatabase.ReadPermissionActivity;
import ch.bubendorf.locusaddon.gsakdatabase.util.ToastUtil;
import locus.api.android.ActionBasics;
import locus.api.android.features.periodicUpdates.UpdateContainer;
import locus.api.android.objects.LocusVersion;
import locus.api.android.utils.LocusUtils;
import locus.api.android.utils.exceptions.RequiredVersionMissingException;
import locus.api.objects.extra.Location;

/**
 * LocationReceiver
 *
 * @author Radim -kuratkoo- Vaculik <kuratkoo@gmail.com>
 * @author Markus Bubendorf <gsakforlocus@bubendorf.net>
 */
public class LocationReceiver extends BroadcastReceiver {

    //private static final String TAG = "LocationReceiver";

    private static final long MIN_INTERVALL = 1200L;
    public static final int MOVE_PERCENT = 10;

    private static LocusVersion locusVersion;
    private static long lastUpdate;
    private static long lastNoPermissionComplain;
    private static long lastNoActiveDBComplain;
    private static Location lastMapCenter;

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }
        if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean("livemap", false)) {
            // Live Map is switched off in GSAK for Locus ==> Do nothing
            return;
        }

        if (System.currentTimeMillis() >= lastUpdate + MIN_INTERVALL) {
            final UpdateContainer updateContainer = getContent(context);
            if (updateContainer != null && updateContainer.isMapVisible() && !updateContainer.isUserTouching()) {
                if (lastMapCenter == null) {
                    update(context, updateContainer);
                } else {
                    final Location bottomRight = updateContainer.getMapBottomRight();
                    final Location topLeft = updateContainer.getMapTopLeft();
                    final Location center = updateContainer.getLocMapCenter();
                    if (bottomRight != null && topLeft != null && center != null) {
                        final double latFromTo = topLeft.getLatitude() - bottomRight.getLatitude();
                        final double lonFromTo = bottomRight.getLongitude() - topLeft.getLongitude();

                        final double latCut = latFromTo * MOVE_PERCENT / 100;
                        final double lonCut = lonFromTo * MOVE_PERCENT / 100;
                        final double latChange = Math.abs(lastMapCenter.getLatitude() - center.getLatitude());
                        final double lonChange = Math.abs(lastMapCenter.getLongitude() - center.getLongitude());

                        if (latChange >= latCut || lonChange >= lonCut) {
                            update(context, updateContainer);
                        }
                    }
                }
            }
        }
    }

    private void update(final Context context, final UpdateContainer updateContainer) {
        //Log.d(TAG, "update start");
        lastMapCenter = updateContainer.getLocMapCenter();
        lastUpdate = System.currentTimeMillis();

        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        final boolean pref_use_db = sharedPreferences.getBoolean("pref_use_db", false);
        final boolean pref_use_db2 = sharedPreferences.getBoolean("pref_use_db2", false);
        final boolean pref_use_db3 = sharedPreferences.getBoolean("pref_use_db3", false);

        if (!pref_use_db && !pref_use_db2 && !pref_use_db3) {
            if (System.currentTimeMillis() >= lastNoActiveDBComplain + 60 * 1000L) {
                // Only show the info once every 60 seconds.
                final String text = context.getResources().getString(R.string.no_db_activated);
                ToastUtil.show(context, text, 5);
                lastNoActiveDBComplain = System.currentTimeMillis();
            }
        } else {
            // We need the permission to access the file system. Check and ask for the permission if necessary
            ReadPermissionActivity.checkPermission(context, this::goOn, this::noPermission, updateContainer, true);
        }
        //Log.d(TAG, "update ende");
    }

    private void noPermission(final Context context, final UpdateContainer updateContainer) {
        //Log.d(TAG, "noPermission");
        if (System.currentTimeMillis() >= lastNoPermissionComplain + 60 * 1000L) {
            // Only show the info once every 60 seconds
            ToastUtil.show(context, context.getString(R.string.no_permission), 5);
            lastNoPermissionComplain = System.currentTimeMillis();
        }
    }

    private void goOn(final Context context, final UpdateContainer updateContainer) {
        //Log.d(TAG, "goOn start");
        final PointLoader pointLoader = PointLoader.getInstance();
        pointLoader.run(context, updateContainer.getLocMapCenter(), updateContainer.getMapTopLeft(), updateContainer.getMapBottomRight());
        //Log.d(TAG, "goOn ende");
    }

    private UpdateContainer getContent(final Context context) {
        if (locusVersion == null) {
            locusVersion = LocusUtils.INSTANCE.getActiveVersion(context, 3);
        }
        if (locusVersion != null) {
            try {
                return ActionBasics.INSTANCE.getUpdateContainer(context, locusVersion);
            } catch (final RequiredVersionMissingException e) {
                Log.e("LocationReceiver", e.getError());
                return null;
            }
        }
        return null;
    }


}
