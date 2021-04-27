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

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.Html;
import android.text.Spanned;
import android.widget.Toast;

import java.util.Collection;

import ch.bubendorf.locusaddon.gsakdatabase.lova.Lova;
import ch.bubendorf.locusaddon.gsakdatabase.util.ColumnMetaData;
import ch.bubendorf.locusaddon.gsakdatabase.util.Gsak;
import ch.bubendorf.locusaddon.gsakdatabase.util.GsakReader;
import locus.api.android.ActionFiles;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;

/**
 * MainActivity
 *
 * @author Radim -kuratkoo- Vaculik <kuratkoo@gmail.com>
 * @author Markus Bubendorf <gsakforlocus@bubendorf.net>
 */
public class PrefActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {

    private Preference dbPick;
    private Preference db2Pick;
    private Preference db3Pick;
    private EditTextPreference nick;
    private EditTextPreference logsCount;
    private EditTextPreference radius;
    private EditTextPreference limit;
    private CheckBoxPreference own;
    private CheckBoxPreference useDb;
    private CheckBoxPreference useDb2;
    private CheckBoxPreference useDb3;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showPreferences();
    }

    @Override
    public void onResume() {
        super.onResume();
        poulateColumnsPref();
    }


    private void showPreferences() {
        addPreferencesFromResource(R.xml.prefs);

        final PreferenceScreen preferenceScreen = getPreferenceScreen();
        final SharedPreferences sharedPreferences = preferenceScreen.getSharedPreferences();

        // Preselect some details column
        final SharedPreferences.Editor editor = sharedPreferences.edit();
        for (final String columnName : GsakReader.preselectList) {
            final String key = "column_" + columnName;
            editor.putBoolean(key, sharedPreferences.getBoolean(key, true));
        }
        editor.apply();

//        poulateColumnsPref();

        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        own = (CheckBoxPreference) preferenceScreen.findPreference("own");
        useDb = (CheckBoxPreference) preferenceScreen.findPreference("pref_use_db");
        useDb2 = (CheckBoxPreference) preferenceScreen.findPreference("pref_use_db2");
        useDb3 = (CheckBoxPreference) preferenceScreen.findPreference("pref_use_db3");

        dbPick = preferenceScreen.findPreference("db_pick");
        dbPick.setOnPreferenceClickListener(getOnDBPreferenceClickListener(0));
        final String dbPath = PreferenceManager.getDefaultSharedPreferences(this).getString("db", "");
        dbPick.setSummary(editPreferenceSummary(dbPath, getText(R.string.pref_db_sum)));
        useDb.setEnabled(dbPath.length() > 0);

        db2Pick = preferenceScreen.findPreference("db2_pick");
        db2Pick.setOnPreferenceClickListener(getOnDBPreferenceClickListener(1));
        final String db2Path = PreferenceManager.getDefaultSharedPreferences(this).getString("db2", "");
        db2Pick.setSummary(editPreferenceSummary(db2Path, getText(R.string.pref_db2_sum)));
        useDb2.setEnabled(db2Path.length() > 0);

        db3Pick = preferenceScreen.findPreference("db3_pick");
        db3Pick.setOnPreferenceClickListener(getOnDBPreferenceClickListener(2));
        final String db3Path = PreferenceManager.getDefaultSharedPreferences(this).getString("db3", "");
        db3Pick.setSummary(editPreferenceSummary(db3Path, getText(R.string.pref_db3_sum)));
        useDb3.setEnabled(db3Path.length() > 0);

        nick = (EditTextPreference) preferenceScreen.findPreference("nick");
        nick.setSummary(editPreferenceSummary(nick.getText(), getText(R.string.pref_nick_sum)));
        own.setEnabled(nick.getText().trim().length() != 0);

        radius = (EditTextPreference) preferenceScreen.findPreference("radius");
        radius.setSummary(editPreferenceSummary(radius.getText() + " km", getText(R.string.pref_radius_sum)));

        logsCount = (EditTextPreference) preferenceScreen.findPreference("logs_count");
        logsCount.setSummary(editPreferenceSummary(logsCount.getText(), getText(R.string.pref_logs_sum)));

        limit = (EditTextPreference) preferenceScreen.findPreference("limit");
        limit.setSummary(editPreferenceSummary(limit.getText(), getText(R.string.pref_limit_sum)));

        if (!own.isEnabled()) {
            own.setSummary(Html.fromHtml(getString(R.string.pref_own_sum) + " <b>" + getString(R.string.pref_own_fill) + "</b>"));
        }
    }

    private void poulateColumnsPref() {
        final SharedPreferences sharedPreferences = getDefaultSharedPreferences(this);
        final String path = sharedPreferences.getString("db", "");
        final String path2 = sharedPreferences.getString("db2", "");
        final String path3 = sharedPreferences.getString("db3", "");

        if (!Gsak.isGsakDatabase(path) && !Gsak.isGsakDatabase(path2) && !Gsak.isGsakDatabase(path3)) {
            // No paths set ==> Disable the Columns Preference
            final PreferenceScreen columnsPref = (PreferenceScreen) getPreferenceScreen().findPreference("pref_columns");
            columnsPref.setEnabled(false);
            return;
        }

        // We need the permission to access the file system. Check and ask for the permission if necessary
        PermissionActivity.checkPermission(this, () -> {
            new Lova<>(GsakReader::getAllColumns)
                    .onSuccess(this::poulateColumnsPref)
                    //.onError(this::displayError)
                    .execute(this);
        }, false);
    }

    private void poulateColumnsPref(final Collection<ColumnMetaData> columnNames) {
        final PreferenceScreen columnsPref = (PreferenceScreen) getPreferenceScreen().findPreference("pref_columns");

        columnsPref.setEnabled(true);
        columnsPref.removeAll();
        for (final ColumnMetaData column : columnNames) {
            final CheckBoxPreference checkBox = new CheckBoxPreference(this);
            checkBox.setTitle(GsakReader.deCamelize(column.getName()));
            final String key = "column_" + column.getName();
            checkBox.setKey(key);
            //checkBox.setChecked(false);
            columnsPref.addPreference(checkBox);
        }
    }

    private Preference.OnPreferenceClickListener getOnDBPreferenceClickListener(final int requestCode) {
        return pref -> {
            try {
                ActionFiles.INSTANCE.actionPickFile(PrefActivity.this, requestCode, getText(R.string.pref_db_pick_title).toString(), new String[]{".db3"});
            } catch (final ActivityNotFoundException anfe) {
                Toast.makeText(PrefActivity.this, "Error: " + anfe.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
            return true;
        };
    }

    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key) {
        if (key.equals("db")) {
            final String path = sharedPreferences.getString(key, "");
            dbPick.setSummary(editPreferenceSummary(path, getText(R.string.pref_db_sum)));
            useDb.setEnabled(path != null && path.length() > 0);
        }
        if (key.equals("db2")) {
            final String path = sharedPreferences.getString(key, "");
            db2Pick.setSummary(editPreferenceSummary(path, getText(R.string.pref_db2_sum)));
            useDb2.setEnabled(path != null && path.length() > 0);
        }
        if (key.equals("db3")) {
            final String path = sharedPreferences.getString(key, "");
            db3Pick.setSummary(editPreferenceSummary(path, getText(R.string.pref_db3_sum)));
            useDb3.setEnabled(path != null && path.length() > 0);
        }

        if (key.equals("nick")) {
            nick.setSummary(editPreferenceSummary(sharedPreferences.getString(key, ""), getText(R.string.pref_nick_sum)));
            if (nick.getText().trim().length() == 0) {
                own.setEnabled(false);
                own.setSummary(Html.fromHtml(getString(R.string.pref_own_sum) + " <b>" + getString(R.string.pref_own_fill) + "</b>"));
            } else {
                own.setEnabled(true);
                own.setSummary(getString(R.string.pref_own_sum));
            }
        }

        if (key.equals("logs_count")) {
            String value = sharedPreferences.getString(key, "20");
            if (value.equals("") || !value.matches("[0-9]+")) {
                Toast.makeText(this, getString(R.string.pref_logs_error), Toast.LENGTH_LONG).show();
                value = "20";
                logsCount.setText(value);
            }
            logsCount.setSummary(editPreferenceSummary(value, getText(R.string.pref_logs_sum)));
        }

        if (key.equals("radius")) {
            String value = sharedPreferences.getString(key, "1");
            if (value.equals("") || !value.matches("[0-9]+") || value.equals("0") || value.equals("00")) {
                Toast.makeText(this, getString(R.string.pref_radius_error), Toast.LENGTH_LONG).show();
                value = "1";
                radius.setText(value);
            }
            radius.setSummary(editPreferenceSummary(value + " km", getText(R.string.pref_radius_sum)));
        }

        if (key.equals("limit")) {
            String value = sharedPreferences.getString(key, "0");
            if (value.equals("") || !value.matches("[0-9]+")) {
                Toast.makeText(this, getString(R.string.pref_limit_error), Toast.LENGTH_LONG).show();
                value = "100";
                limit.setText(value);
            }
            limit.setSummary(editPreferenceSummary(value, getText(R.string.pref_limit_sum)));
        }
    }
    private Spanned editPreferenceSummary(final String value, final CharSequence summary) {
        if (!value.equals("")) {
            return Html.fromHtml("<font color=\"#FF8000\"><b>(" + value + ")</b></font><br/> " + summary);
        } else {
            return Html.fromHtml(summary.toString());
        }
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (requestCode >= 0 && requestCode <= 2) {
            if (resultCode == RESULT_OK && data != null) {
                final String filename = data.getData().toString().replace("file://", "");
                final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
                final SharedPreferences.Editor editor = sharedPref.edit();
                if (requestCode == 0) {
                    editor.putString("db", filename);
                    dbPick.setSummary(editPreferenceSummary(filename, getText(R.string.pref_db_sum)));
                    useDb.setChecked(filename != null && filename.length() > 0);
                    useDb.setEnabled(filename != null && filename.length() > 0);
                } else if (requestCode == 1) {
                    editor.putString("db2", filename);
                    db2Pick.setSummary(editPreferenceSummary(filename, getText(R.string.pref_db2_sum)));
                    useDb2.setChecked(filename != null && filename.length() > 0);
                    useDb2.setEnabled(filename != null && filename.length() > 0);
                } else {
                    editor.putString("db3", filename);
                    db3Pick.setSummary(editPreferenceSummary(filename, getText(R.string.pref_db3_sum)));
                    useDb3.setChecked(filename != null && filename.length() > 0);
                    useDb3.setEnabled(filename != null && filename.length() > 0);
                }
                editor.apply();

                poulateColumnsPref();
            }
        }
    }
}
