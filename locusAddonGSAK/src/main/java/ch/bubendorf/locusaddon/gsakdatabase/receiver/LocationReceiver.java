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
 */
public class LocationReceiver extends BroadcastReceiver {

    private static final long MIN_INTERVALL = 2 * 1000L;
    private static final long MIN_DISTANCE = 100;
    public static final int OVERSCAN_PERCENT = 20;

    private static LocusVersion locusVersion;
    private static long lastUpdate;
    private static Location lastMapCenter;

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }
        if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean("livemap", false)) {
            return;
        }

        if (System.currentTimeMillis() >= lastUpdate + MIN_INTERVALL) {
            final UpdateContainer updateContainer = getContent(context);
            if (updateContainer != null && updateContainer.isMapVisible()) {
                if (lastMapCenter == null) {
                    update(context, updateContainer);
                } else {
                    if (lastMapCenter.distanceTo(updateContainer.getLocMapCenter()) >= MIN_DISTANCE) {
                        final double height = updateContainer.getMapTopLeft().getLatitude() - updateContainer.getMapBottomRight().getLatitude();
                        final double width = updateContainer.getMapBottomRight().getLongitude() - updateContainer.getMapTopLeft().getLongitude();

                        final double heightCut = height * OVERSCAN_PERCENT / 100;
                        final double widthCut = width * OVERSCAN_PERCENT / 100;
                        final double heightChange = Math.abs(lastMapCenter.getLatitude() - updateContainer.getLocMapCenter().getLatitude());
                        final double widthChange = Math.abs(lastMapCenter.getLongitude() - updateContainer.getLocMapCenter().getLongitude());

                        if (heightChange >= heightCut || widthChange >= widthCut) {
                            update(context, updateContainer);
                        }
                    }
                }
            }
        }
    }

    private void update(final Context context, final UpdateContainer updateContainer) {
        final PointLoader pointLoader = PointLoader.getInstance();
        pointLoader.setContext(context);

        // We need the permission to access the file system. Check and ask for the permission if necessary
        PermissionActivity.checkPermission(context, this::goOn, updateContainer);
    }

    private void goOn(final UpdateContainer updateContainer) {
        lastMapCenter = updateContainer.getLocMapCenter();
        lastUpdate = System.currentTimeMillis();

        final PointLoader pointLoader = PointLoader.getInstance();

        pointLoader.run(updateContainer.getLocMapCenter(), updateContainer.getMapTopLeft(), updateContainer.getMapBottomRight());
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
