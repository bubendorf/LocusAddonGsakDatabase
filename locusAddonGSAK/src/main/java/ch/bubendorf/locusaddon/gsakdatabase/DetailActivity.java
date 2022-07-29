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
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import java.text.ParseException;

import ch.bubendorf.locusaddon.gsakdatabase.util.Gsak;
import ch.bubendorf.locusaddon.gsakdatabase.util.GsakReader;
import ch.bubendorf.locusaddon.gsakdatabase.util.ToastUtil;
import locus.api.android.utils.LocusUtils;
import locus.api.objects.geoData.Point;

/**
 * DetailActivity
 *
 * @author Radim -kuratkoo- Vaculik <kuratkoo@gmail.com>
 * @author Markus Bubendorf <gsakforlocus@bubendorf.net>
 */
public class DetailActivity extends Activity {

//    private static final String TAG = "DetailActivity";

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // We need the permission to access the file system. Check and ask for the permission if necessary
        PermissionActivity.checkPermission(this, this::goOn, null, null, false);
    }

    private void goOn(final Context context, final Void data) {
        final Intent intent = getIntent();

        if (intent.hasExtra("cacheId")) {
            final String value = intent.getStringExtra("cacheId");

            try {
                Point point = readGeocacheFromDatabase(this, "db", value);
                if (point == null) {
                    point = readGeocacheFromDatabase(this, "db2", value);
                    if (point == null) {
                        point = readGeocacheFromDatabase(this, "db3", value);
                    }
                }
                if (point != null) {
                    // return data
                    final Intent retIntent = LocusUtils.INSTANCE.prepareResultExtraOnDisplayIntent(point, true);
                    setResult(Activity.RESULT_OK, retIntent);
                } else {
                    setResult(Activity.RESULT_CANCELED);
                }
            } catch (final Exception e) {
                ToastUtil.show(this, getText(R.string.unable_to_load_detail) + " " + e.getLocalizedMessage(), 5);
            } finally {
                finish();
            }
        }
    }

    @Nullable
    private Point readGeocacheFromDatabase(final Context context, final String dbId, final String gcCode) throws ParseException {
        final String dbPath = PreferenceManager.getDefaultSharedPreferences(this).getString(dbId, "");
        if (dbPath.length() == 0 || !Gsak.isReadableGsakDatabase(dbPath)) {
            return null;
        }
        final SQLiteDatabase database = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.NO_LOCALIZED_COLLATORS + SQLiteDatabase.OPEN_READONLY);
        final String logLimit = PreferenceManager.getDefaultSharedPreferences(this).getString("logs_count", "20");
        final Point p = GsakReader.readGeocache(context, database, gcCode, true, logLimit);
        database.close();
        return p;
    }
}
