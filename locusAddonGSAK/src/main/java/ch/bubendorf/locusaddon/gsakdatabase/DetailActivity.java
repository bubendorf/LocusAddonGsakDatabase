package ch.bubendorf.locusaddon.gsakdatabase;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Toast;

import androidx.annotation.Nullable;

import ch.bubendorf.locusaddon.gsakdatabase.util.Gsak;
import ch.bubendorf.locusaddon.gsakdatabase.util.GsakReader;

import java.io.File;
import java.text.ParseException;

import locus.api.android.utils.LocusUtils;
import locus.api.objects.geoData.Point;

/**
 * DetailActivity
 *
 * @author Radim -kuratkoo- Vaculik <kuratkoo@gmail.com>
 */
public class DetailActivity extends Activity {

//    private static final String TAG = "DetailActivity";

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // We need the permission to access the file system. Check and ask for the permission if necessary
        PermissionActivity.checkPermission(this, this::goOn, null);
    }

    private void goOn(final Context context, final Void data) {
        final Intent intent = getIntent();

        final File fd = new File(PreferenceManager.getDefaultSharedPreferences(this).getString("db", ""));
        if (!Gsak.isGsakDatabase(fd)) {
            Toast.makeText(this, R.string.no_db_file, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (intent.hasExtra("cacheId")) {
            final String value = intent.getStringExtra("cacheId");

            try {
                Point point = readGeocacheFromDatabase("db", value);
                if (point == null) {
                    point = readGeocacheFromDatabase("db2", value);
                    if (point == null) {
                        point = readGeocacheFromDatabase("db3", value);
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
                Toast.makeText(this, getText(R.string.unable_to_load_detail) + " " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            } finally {
                finish();
            }
        }
    }

    @Nullable
    private Point readGeocacheFromDatabase(final String dbId, final String gcCode) throws ParseException {
        final String dbPath = PreferenceManager.getDefaultSharedPreferences(this).getString(dbId, "");
        if (dbPath.length() == 0) {
            return null;
        }
        final SQLiteDatabase database = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.NO_LOCALIZED_COLLATORS + SQLiteDatabase.OPEN_READONLY);
        final Point p = GsakReader.readGeocache(database, gcCode, true);
        database.close();
        return p;
    }
}
