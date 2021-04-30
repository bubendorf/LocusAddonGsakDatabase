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
import android.preference.PreferenceManager;
import android.util.Log;

import ch.bubendorf.locusaddon.gsakdatabase.PermissionActivity;
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

    private static final String TAG = "LocationReceiver";

    private static final long MIN_INTERVALL = 2 * 1000L;
    public static final int MOVE_PERCENT = 20;

    private static LocusVersion locusVersion;
    private static long lastUpdate;
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
        Log.i(TAG, "update start");
        // We need the permission to access the file system. Check and ask for the permission if necessary
        PermissionActivity.checkPermission(context, this::goOn, updateContainer, true);
        lastMapCenter = updateContainer.getLocMapCenter();
        lastUpdate = System.currentTimeMillis();
        Log.i(TAG, "update ende");
    }

    private void goOn(final Context context, final UpdateContainer updateContainer) {
        Log.i(TAG, "goOn start");
        lastMapCenter = updateContainer.getLocMapCenter();
        lastUpdate = System.currentTimeMillis();

        final PointLoader pointLoader = PointLoader.getInstance();
        pointLoader.run(context, updateContainer.getLocMapCenter(), updateContainer.getMapTopLeft(), updateContainer.getMapBottomRight());
        Log.i(TAG, "goOn ende");
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
