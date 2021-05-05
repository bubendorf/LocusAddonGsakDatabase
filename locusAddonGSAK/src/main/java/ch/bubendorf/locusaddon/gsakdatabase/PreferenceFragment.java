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
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.view.View;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

import ch.bubendorf.locusaddon.gsakdatabase.lova.Lova;
import ch.bubendorf.locusaddon.gsakdatabase.util.ColumnMetaData;
import ch.bubendorf.locusaddon.gsakdatabase.util.Gsak;
import ch.bubendorf.locusaddon.gsakdatabase.util.GsakReader;
import locus.api.android.ActionFiles;

import static androidx.preference.PreferenceManager.getDefaultSharedPreferences;

public class PreferenceFragment extends PreferenceFragmentCompat
        implements SharedPreferences.OnSharedPreferenceChangeListener,
        PreferenceFragmentCompat.OnPreferenceStartScreenCallback  {

    private Preference dbPick;
    private Preference db2Pick;
    private Preference db3Pick;
    private EditTextPreference nick;
    private EditTextPreference logsCount;
    private EditTextPreference radius;
    private EditTextPreference limit;
    private SwitchPreference own;
    private SwitchPreference useDb;
    private SwitchPreference useDb2;
    private SwitchPreference useDb3;

    private String rootKey;

    /*@Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showPreferences();
    }*/

    @Override
    public void onResume() {
        super.onResume();
        populateColumnsPref();
    }

    @Override
    public void onViewCreated(@NotNull final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set the default background in the view so as to avoid transparency
        //view.setBackgroundColor(ContextCompat.getColor(getContext(), android.R.color.background_dark));
        view.setBackgroundColor(Color.DKGRAY);
    }

    @Override
    public Fragment getCallbackFragment() {
        return this;
    }

    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        this.rootKey = rootKey;

        setPreferencesFromResource(R.xml.prefs, rootKey);

        final PreferenceScreen preferenceScreen = getPreferenceScreen();
        final SharedPreferences sharedPreferences = preferenceScreen.getSharedPreferences();

        if (rootKey == null) {
            // Preselect some details column
            final SharedPreferences.Editor editor = sharedPreferences.edit();
            for (final String columnName : GsakReader.preselectList) {
                final String key = "column_" + columnName;
                editor.putBoolean(key, sharedPreferences.getBoolean(key, true));
            }
            editor.apply();

            sharedPreferences.registerOnSharedPreferenceChangeListener(this);

            own = preferenceScreen.findPreference("own");
            useDb = preferenceScreen.findPreference("pref_use_db");
            useDb2 = preferenceScreen.findPreference("pref_use_db2");
            useDb3 = preferenceScreen.findPreference("pref_use_db3");

            dbPick = preferenceScreen.findPreference("db_pick");
            dbPick.setOnPreferenceClickListener(getOnDBPreferenceClickListener(0));
            final String dbPath = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("db", "");
            dbPick.setSummary(editPreferenceSummary(dbPath, getText(R.string.pref_db_sum)));
            useDb.setEnabled(dbPath.length() > 0);

            db2Pick = preferenceScreen.findPreference("db2_pick");
            db2Pick.setOnPreferenceClickListener(getOnDBPreferenceClickListener(1));
            final String db2Path = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("db2", "");
            db2Pick.setSummary(editPreferenceSummary(db2Path, getText(R.string.pref_db2_sum)));
            useDb2.setEnabled(db2Path.length() > 0);

            db3Pick = preferenceScreen.findPreference("db3_pick");
            db3Pick.setOnPreferenceClickListener(getOnDBPreferenceClickListener(2));
            final String db3Path = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("db3", "");
            db3Pick.setSummary(editPreferenceSummary(db3Path, getText(R.string.pref_db3_sum)));
            useDb3.setEnabled(db3Path.length() > 0);

            nick = preferenceScreen.findPreference("nick");
            nick.setSummary(editPreferenceSummary(nick.getText(), getText(R.string.pref_nick_sum)));
            own.setEnabled(nick.getText().trim().length() != 0);

            radius = preferenceScreen.findPreference("radius");
            radius.setSummary(editPreferenceSummary(radius.getText() + " km", getText(R.string.pref_radius_sum)));

            logsCount = preferenceScreen.findPreference("logs_count");
            logsCount.setSummary(editPreferenceSummary(logsCount.getText(), getText(R.string.pref_logs_sum)));

            limit = preferenceScreen.findPreference("limit");
            limit.setSummary(editPreferenceSummary(limit.getText(), getText(R.string.pref_limit_sum)));

            if (!own.isEnabled()) {
                own.setSummary(Html.fromHtml(getString(R.string.pref_own_sum) + " <b>" + getString(R.string.pref_own_fill) + "</b>", 0));
            }
        }
    }

    private void populateColumnsPref() {
        final SharedPreferences sharedPreferences = getDefaultSharedPreferences(getActivity());
        final String path = sharedPreferences.getString("db", "");
        final String path2 = sharedPreferences.getString("db2", "");
        final String path3 = sharedPreferences.getString("db3", "");

        if (!Gsak.isGsakDatabase(path) && !Gsak.isGsakDatabase(path2) && !Gsak.isGsakDatabase(path3)) {
            // No paths set ==> Disable the Columns Preference
            final PreferenceCategory columnsPref =  getPreferenceScreen().findPreference("pref_columns");
            columnsPref.setEnabled(false);
            return;
        }

        if ("pref_details".equals(rootKey)) {
            // We need the permission to access the file system. Check and ask for the permission if necessary
            PermissionActivity.checkPermission(getActivity(), () -> new Lova<>(GsakReader::getAllColumns)
                    .onSuccess(this::populateColumnsPref)
                    .onError(this::showError)
                    .execute(getActivity()), false);
        }
    }

    private void showError(final Exception e) {
        Toast.makeText(getActivity(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
    }

    private void populateColumnsPref(final Collection<ColumnMetaData> columnMetaDatas) {
        final PreferenceCategory columnsPref =  getPreferenceScreen().findPreference("pref_columns");

        columnsPref.setEnabled(true);
        columnsPref.removeAll();
        String currentTable = null;
        PreferenceCategory prefCategory = null;
        final Resources resources = getResources();
        for (final ColumnMetaData column : columnMetaDatas) {
            if (!column.getTableName().equals(currentTable)) {
                currentTable = column.getTableName();
                prefCategory = new PreferenceCategory(getActivity());
                prefCategory.setTitle(currentTable);
                prefCategory.setIconSpaceReserved(false);
                columnsPref.addPreference(prefCategory);
            }

            final SwitchPreference checkBox = new SwitchPreference(getActivity());
            checkBox.setTitle(GsakReader.deCamelize(column.getColumnName()));
            final String key = "column_" + column.getColumnName();
            checkBox.setKey(key);
            final int resId  = resources.getIdentifier(key, "string", getContext().getPackageName());
            if (resId != 0) {
                checkBox.setSummary(resId);
            }
            checkBox.setIconSpaceReserved(false);
            prefCategory.addPreference(checkBox);
        }
    }

    private Preference.OnPreferenceClickListener getOnDBPreferenceClickListener(final int requestCode) {
        return pref -> {
            try {
                ActionFiles.INSTANCE.actionPickFile(getActivity(), requestCode, getText(R.string.pref_db_pick_title).toString(), new String[]{".db3"});
            } catch (final ActivityNotFoundException anfe) {
                Toast.makeText(getActivity(), "Error: " + anfe.getLocalizedMessage(), Toast.LENGTH_LONG).show();
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
                own.setSummary(Html.fromHtml(getString(R.string.pref_own_sum) + " <b>" + getString(R.string.pref_own_fill) + "</b>" ,0));
            } else {
                own.setEnabled(true);
                own.setSummary(getString(R.string.pref_own_sum));
            }
        }

        if (key.equals("logs_count")) {
            String value = sharedPreferences.getString(key, "20");
            if (value.equals("") || !value.matches("[0-9]+")) {
                Toast.makeText(getActivity(), getString(R.string.pref_logs_error), Toast.LENGTH_LONG).show();
                value = "20";
                logsCount.setText(value);
            }
            logsCount.setSummary(editPreferenceSummary(value, getText(R.string.pref_logs_sum)));
        }

        if (key.equals("radius")) {
            String value = sharedPreferences.getString(key, "1");
            if (value.equals("") || !value.matches("[0-9]+") || value.equals("0") || value.equals("00")) {
                Toast.makeText(getActivity(), getString(R.string.pref_radius_error), Toast.LENGTH_LONG).show();
                value = "1";
                radius.setText(value);
            }
            radius.setSummary(editPreferenceSummary(value + " km", getText(R.string.pref_radius_sum)));
        }

        if (key.equals("limit")) {
            String value = sharedPreferences.getString(key, "0");
            if (value.equals("") || !value.matches("[0-9]+")) {
                Toast.makeText(getActivity(), getString(R.string.pref_limit_error), Toast.LENGTH_LONG).show();
                value = "100";
                limit.setText(value);
            }
            limit.setSummary(editPreferenceSummary(value, getText(R.string.pref_limit_sum)));
        }
    }
    private Spanned editPreferenceSummary(final String value, final CharSequence summary) {
        if (!value.equals("")) {
            return Html.fromHtml("<font color=\"#FF8000\"><b>(" + value + ")</b></font><br/> " + summary, 0);
        } else {
            return Html.fromHtml(summary.toString(), 0);
        }
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode >= 0 && requestCode <= 2) {
            if (resultCode == Activity.RESULT_OK && data != null) {

                final String filename = Uri.parse(data.getData().toString()).getPath();
                final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
                final SharedPreferences.Editor editor = sharedPref.edit();
                if (requestCode == 0) {
                    editor.putString("db", filename);
                    dbPick.setSummary(editPreferenceSummary(filename, getText(R.string.pref_db_sum)));
                    useDb.setChecked(filename.length() > 0);
                    useDb.setEnabled(filename.length() > 0);
                } else if (requestCode == 1) {
                    editor.putString("db2", filename);
                    db2Pick.setSummary(editPreferenceSummary(filename, getText(R.string.pref_db2_sum)));
                    useDb2.setChecked(filename.length() > 0);
                    useDb2.setEnabled(filename.length() > 0);
                } else {
                    editor.putString("db3", filename);
                    db3Pick.setSummary(editPreferenceSummary(filename, getText(R.string.pref_db3_sum)));
                    useDb3.setChecked(filename.length() > 0);
                    useDb3.setEnabled(filename.length() > 0);
                }
                editor.apply();

                populateColumnsPref();
            }
        }
    }

    @Override
    public boolean onPreferenceStartScreen(final PreferenceFragmentCompat caller, final PreferenceScreen preferenceScreen) {
        final FragmentTransaction ft = getParentFragmentManager().beginTransaction();
        final PreferenceFragment fragment = new PreferenceFragment();
        final Bundle args = new Bundle();
        args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, preferenceScreen.getKey());
        fragment.setArguments(args);
        ft.add(android.R.id.content, fragment, preferenceScreen.getKey());
        ft.addToBackStack(preferenceScreen.getKey());
        ft.commit();
        return true;
    }
}
