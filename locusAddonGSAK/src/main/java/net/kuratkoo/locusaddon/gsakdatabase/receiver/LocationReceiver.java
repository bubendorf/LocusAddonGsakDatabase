package net.kuratkoo.locusaddon.gsakdatabase.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.util.Log;

import locus.api.android.ActionBasics;
import locus.api.android.features.periodicUpdates.UpdateContainer;
import locus.api.android.objects.LocusVersion;
import locus.api.android.utils.LocusUtils;
import locus.api.android.utils.exceptions.RequiredVersionMissingException;
import locus.api.objects.extra.Location;

/**
 * LocationReceiver
 * @author Radim -kuratkoo- Vaculik <kuratkoo@gmail.com>
 */
public class LocationReceiver extends BroadcastReceiver {

    private static final long MIN_INTERVALL = 5 * 1000L;
    private static final long MIN_DISTANCE = 100;

    private static LocusVersion locusVersion;
    private static long lastUpdate;
    private static Location lastMapCenter;

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }
//        Log.i("LocationReceiver", "LocationReceiver");
        if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean("livemap", false)) {
            return;
        }

        if (System.currentTimeMillis() >= lastUpdate + MIN_INTERVALL) {
            final UpdateContainer updateContainer = getContent(context);
            if (updateContainer != null && updateContainer.isMapVisible()) {
            //Log.i("LocationReceiver", "LocationReceiver");
                if (lastMapCenter == null || lastMapCenter.distanceTo(updateContainer.getLocMapCenter()) >= MIN_DISTANCE) {
                    lastMapCenter = updateContainer.getLocMapCenter();
                    lastUpdate = System.currentTimeMillis();

                    final PointLoader pointLoader = PointLoader.getInstance();
                    pointLoader.setContext(context);
                    pointLoader.run(updateContainer.getLocMapCenter());
                }
            }
        }
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
