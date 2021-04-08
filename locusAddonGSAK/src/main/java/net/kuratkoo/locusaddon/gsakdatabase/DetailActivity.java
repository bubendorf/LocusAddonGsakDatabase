package net.kuratkoo.locusaddon.gsakdatabase;

import android.app.Activity;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Toast;

import androidx.annotation.Nullable;

import net.kuratkoo.locusaddon.gsakdatabase.util.Gsak;
import net.kuratkoo.locusaddon.gsakdatabase.util.GsakReader;

import java.io.File;
import java.text.ParseException;

import locus.api.android.utils.LocusUtils;
import locus.api.objects.geoData.Point;

/**
 * DetailActivity
 * @author Radim -kuratkoo- Vaculik <kuratkoo@gmail.com>
 */
public class DetailActivity extends Activity {

//    private static final String TAG = "DetailActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();

        File fd = new File(PreferenceManager.getDefaultSharedPreferences(this).getString("db", ""));
        if (!Gsak.isGsakDatabase(fd)) {
            Toast.makeText(this, R.string.no_db_file, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (intent.hasExtra("cacheId")) {
            String value = intent.getStringExtra("cacheId");

            try {
                Point point = readGeocacheFromDatabase("db", value);
                if (point == null) {
                    point = readGeocacheFromDatabase("db2", value);
                    if (point == null) {
                        point = readGeocacheFromDatabase("db3", value);
                    }
                }
                // return data
                Intent retIntent = LocusUtils.INSTANCE.prepareResultExtraOnDisplayIntent(point, true);
                setResult(Activity.RESULT_OK, retIntent);
            } catch (Exception e) {
                Toast.makeText(this, getText(R.string.unable_to_load_detail) + " " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            } finally {
                finish();
            }
        }
    }

    @Nullable
    private Point readGeocacheFromDatabase(String dbId, String gcCode) throws ParseException {
        String dbPath = PreferenceManager.getDefaultSharedPreferences(this).getString(dbId, "");
        if (dbPath.length() == 0) {
            return null;
        }
        SQLiteDatabase database = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.NO_LOCALIZED_COLLATORS + SQLiteDatabase.OPEN_READONLY);
        Point p = GsakReader.readGeocache(database, gcCode, true);
        database.close();
        return p;
    }
}
