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

import android.annotation.SuppressLint;
import android.content.Context;
import android.widget.Toast;

import java.text.ParseException;
import java.util.List;

import ch.bubendorf.locusaddon.gsakdatabase.util.CacheWrapper;
import ch.bubendorf.locusaddon.gsakdatabase.util.GeocacheAsyncTask;
import ch.bubendorf.locusaddon.gsakdatabase.util.GsakReader;
import ch.bubendorf.locusaddon.gsakdatabase.util.ToastUtil;
import locus.api.android.ActionDisplayPoints;
import locus.api.objects.extra.Location;

/**
 * @author Markus Bubendorf <gsakforlocus@bubendorf.net>
 */
public class PointLoaderAsyncTask extends GeocacheAsyncTask {

    @SuppressLint("StaticFieldLeak")
    private final Context context;

    public PointLoaderAsyncTask(final Context context) {
        this.context = context;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        openDatabases(context);
    }

    @Override
    protected Void doInBackground(final Location... locations) {
        if (isCancelled()) {
            return null;
        }

        final List<CacheWrapper> gcCodes = GsakReader.readGCCodes(context, this,
                db, db2, db3, locations[0], locations[1], locations[2]);

        try {
            packPoints = GsakReader.readGeocaches(context,this, gcCodes);
        } catch (final ParseException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    @Override
    protected void onPostExecute(final Void v) {
        super.onPostExecute(v);
        closeDatabases();

        if (packPoints != null && packPoints.getPoints().length > 0) {
            try {
                ActionDisplayPoints.INSTANCE.sendPackSilent(context, packPoints, false);
            } catch (final Exception e) {
                ToastUtil.show(context, "Error: " + e.getLocalizedMessage(), 5);
            }
        }
    }

}
