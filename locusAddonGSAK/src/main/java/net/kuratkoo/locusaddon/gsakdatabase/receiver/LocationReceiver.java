package net.kuratkoo.locusaddon.gsakdatabase.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * LocationReceiver
 * @author Radim -kuratkoo- Vaculik <kuratkoo@gmail.com>
 */
public class LocationReceiver extends BroadcastReceiver {

    private static final String TAG = "LocusAddonGsakDatabase|LocationReceiver";

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }

        PointLoader pl = PointLoader.getInstance();
        pl.setContext(context);
        pl.setIntent(intent);
        pl.run();
    }
}
