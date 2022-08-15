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

import static android.widget.Toast.LENGTH_LONG;
import static androidx.preference.PreferenceManager.getDefaultSharedPreferences;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.Html;
import android.text.Spanned;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.hbisoft.pickit.PickiT;
import com.hbisoft.pickit.PickiTCallbacks;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import ch.bubendorf.locusaddon.gsakdatabase.lova.Lova;
import ch.bubendorf.locusaddon.gsakdatabase.util.ColumnMetaData;
import ch.bubendorf.locusaddon.gsakdatabase.util.Gsak;
import ch.bubendorf.locusaddon.gsakdatabase.util.GsakReader;
import ch.bubendorf.locusaddon.gsakdatabase.util.ToastUtil;
import locus.api.android.ActionFiles;

public class PreferenceFragment extends PreferenceFragmentCompat
        implements SharedPreferences.OnSharedPreferenceChangeListener,
        PreferenceFragmentCompat.OnPreferenceStartScreenCallback,
        PickiTCallbacks {

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
    private SwitchPreference center;

    private String rootKey;

    private PickiT pickiT;
    private String pickiTPath;

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

        pickiT = new PickiT(getContext(), this, getActivity());
    }

    @SuppressLint("RestrictedApi")
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
            center = preferenceScreen.findPreference("center");

            dbPick = preferenceScreen.findPreference("db_pick");
            assert dbPick != null;
            dbPick.setOnPreferenceClickListener(getOnDBPreferenceClickListener(0));
            final String dbPath = sharedPreferences.getString("db", "");
            dbPick.setSummary(editPreferenceSummary(dbPath, getText(R.string.pref_db_sum)));
            useDb.setEnabled(dbPath.length() > 0);

            db2Pick = preferenceScreen.findPreference("db2_pick");
            assert db2Pick != null;
            db2Pick.setOnPreferenceClickListener(getOnDBPreferenceClickListener(1));
            final String db2Path = sharedPreferences.getString("db2", "");
            db2Pick.setSummary(editPreferenceSummary(db2Path, getText(R.string.pref_db2_sum)));
            useDb2.setEnabled(db2Path.length() > 0);

            db3Pick = preferenceScreen.findPreference("db3_pick");
            assert db3Pick != null;
            db3Pick.setOnPreferenceClickListener(getOnDBPreferenceClickListener(2));
            final String db3Path = sharedPreferences.getString("db3", "");
            db3Pick.setSummary(editPreferenceSummary(db3Path, getText(R.string.pref_db3_sum)));
            useDb3.setEnabled(db3Path.length() > 0);

            nick = preferenceScreen.findPreference("nick");
            assert nick != null;
            nick.setSummary(editPreferenceSummary(nick.getText(), getText(R.string.pref_nick_sum)));
            own.setEnabled(nick.getText().trim().length() != 0);

            radius = preferenceScreen.findPreference("radius");
            assert radius != null;
            radius.setSummary(editPreferenceSummary(radius.getText() + " km", getText(R.string.pref_radius_sum)));

            logsCount = preferenceScreen.findPreference("logs_count");
            assert logsCount != null;
            logsCount.setSummary(editPreferenceSummary(logsCount.getText(), getText(R.string.pref_logs_sum)));

            limit = preferenceScreen.findPreference("limit");
            assert limit != null;
            limit.setSummary(editPreferenceSummary(limit.getText(), getText(R.string.pref_limit_sum)));

            if (!own.isEnabled()) {
                own.setSummary(Html.fromHtml(getString(R.string.pref_own_sum) + " <b>" + getString(R.string.pref_own_fill) + "</b>", 0));
            }

            Preference backupSettingsPref = preferenceScreen.findPreference("backupSettings");
            if (backupSettingsPref != null) {
                backupSettingsPref.setOnPreferenceClickListener(pref -> {
                    backupPreferences();
                    return true;
                });
            }

            Preference restoreSettingsPref = preferenceScreen.findPreference("restoreSettings");
            if (restoreSettingsPref != null) {
                restoreSettingsPref.setOnPreferenceClickListener(pref -> {
                    restorePreferences();
                    return true;
                });
            }
        }
    }

    private void populateColumnsPref() {
        final FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }
        final SharedPreferences sharedPreferences = getDefaultSharedPreferences(activity);
        final String path = sharedPreferences.getString("db", "");
        final String path2 = sharedPreferences.getString("db2", "");
        final String path3 = sharedPreferences.getString("db3", "");

        if (Gsak.isNotAGsakDatabase(path) && Gsak.isNotAGsakDatabase(path2) && Gsak.isNotAGsakDatabase(path3)) {
            // No paths set ==> Disable the Columns Preference
            final Preference columnsPref =  getPreferenceScreen().findPreference("pref_columns");
            if (columnsPref != null) {
                // Googles Pre-launch report complains otherwise
                columnsPref.setEnabled(false);
            }
            return;
        }

        if ("pref_details".equals(rootKey)) {
            // We need the permission to access the file system. Check and ask for the permission if necessary
            ReadPermissionActivity.checkPermission(activity, () -> new Lova<>(GsakReader::getAllColumns)
                    .onSuccess(this::populateColumnsPref)
                    .onError(this::showError)
                    .execute(activity), false);
        }
    }

    private void showError(final Exception e) {
        ToastUtil.show(requireActivity(), e.getLocalizedMessage(), 5);
    }

    private void populateColumnsPref(final Collection<ColumnMetaData> columnMetaDatas) {
        final PreferenceCategory columnsPref =  getPreferenceScreen().findPreference("pref_columns");

        assert columnsPref != null;
        columnsPref.setEnabled(true);
        columnsPref.removeAll();
        String currentTable = null;
        PreferenceCategory prefCategory = null;
        final Resources resources = getResources();
        for (final ColumnMetaData column : columnMetaDatas) {
            if (!column.getTableName().equals(currentTable)) {
                currentTable = column.getTableName();
                prefCategory = new PreferenceCategory(requireActivity());
                prefCategory.setTitle(currentTable);
                prefCategory.setIconSpaceReserved(false);
                columnsPref.addPreference(prefCategory);
            }

            final SwitchPreference checkBox = new SwitchPreference(requireActivity());
            checkBox.setTitle(GsakReader.deCamelize(column.getColumnName()));
            final String key = "column_" + column.getColumnName();
            checkBox.setKey(key);
            final int resId  = resources.getIdentifier(key, "string", requireActivity().getPackageName());
            if (resId != 0) {
                checkBox.setSummary(resId);
            } /*else {
                checkBox.setSummary(column.getType());
            }*/
            checkBox.setIconSpaceReserved(false);
            prefCategory.addPreference(checkBox);
        }
    }

    private Preference.OnPreferenceClickListener getOnDBPreferenceClickListener(final int requestCode) {
        return pref -> {
            Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
            chooseFile.setType("*/*");
            chooseFile.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
            chooseFile = Intent.createChooser(chooseFile, "Choose a file");
            startActivityForResult(chooseFile, requestCode);
            return true;
        };
    }

    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key) {
        if (key.equals("db")) {
            final String path = sharedPreferences.getString(key, "");
            dbPick.setSummary(editPreferenceSummary(path, getText(R.string.pref_db_sum)));
            useDb.setEnabled(path.length() > 0);
        }
        if (key.equals("db2")) {
            final String path = sharedPreferences.getString(key, "");
            db2Pick.setSummary(editPreferenceSummary(path, getText(R.string.pref_db2_sum)));
            useDb2.setEnabled(path.length() > 0);
        }
        if (key.equals("db3")) {
            final String path = sharedPreferences.getString(key, "");
            db3Pick.setSummary(editPreferenceSummary(path, getText(R.string.pref_db3_sum)));
            useDb3.setEnabled(path.length() > 0);
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
                ToastUtil.show(requireActivity(), getString(R.string.pref_logs_error), 5);
                value = "20";
                logsCount.setText(value);
            }
            logsCount.setSummary(editPreferenceSummary(value, getText(R.string.pref_logs_sum)));
        }

        if (key.equals("radius")) {
            String value = sharedPreferences.getString(key, "1");
            if (value.equals("") || !value.matches("[0-9]+") || value.equals("0") || value.equals("00")) {
                ToastUtil.show(requireActivity(), getString(R.string.pref_radius_error), 5);
                value = "1";
                radius.setText(value);
            }
            radius.setSummary(editPreferenceSummary(value + " km", getText(R.string.pref_radius_sum)));
        }

        if (key.equals("limit")) {
            String value = sharedPreferences.getString(key, "0");
            if (value.equals("") || !value.matches("[0-9]+")) {
                ToastUtil.show(requireActivity(), getString(R.string.pref_limit_error), 5);
                value = "100";
                limit.setText(value);
            }
            limit.setSummary(editPreferenceSummary(value, getText(R.string.pref_limit_sum)));
        }

        if (key.equals("import")) {
            final boolean value = sharedPreferences.getBoolean("import", true);
            center.setEnabled(!value);
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

                pickiT.getPath(data.getData(), Build.VERSION.SDK_INT);
                final String filename = pickiTPath;

                final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(requireActivity());
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
    public boolean onPreferenceStartScreen(@NonNull final PreferenceFragmentCompat caller, final PreferenceScreen preferenceScreen) {
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

    @Override
    public void PickiTonUriReturned() {

    }

    @Override
    public void PickiTonStartListener() {

    }

    @Override
    public void PickiTonProgressUpdate(int progress) {

    }

    @Override
    public void PickiTonCompleteListener(String path, boolean wasDriveFile, boolean wasUnknownProvider, boolean wasSuccessful, String Reason) {
        pickiTPath = wasSuccessful ? path : null;
    }

    @Override
    public void PickiTonMultipleCompleteListener(ArrayList<String> paths, boolean wasSuccessful, String Reason) {

    }

    private void backupPreferences() {
        // We need the permission to access the file system. Check and ask for the permission if necessary
        WritePermissionActivity.checkPermission(getContext(), this::backupPreferencesWithPermission, null, null, false);
    }

    private void backupPreferencesWithPermission(final Context context, final Void voidData) {
        final PreferenceScreen preferenceScreen = getPreferenceScreen();
        final SharedPreferences sharedPreferences = preferenceScreen.getSharedPreferences();

        Map<String, ?> map = sharedPreferences.getAll();

        String data = "This is the data!";

        if (isExternalStorageAvailable() && !isExternalStorageReadOnly()) {
             String filename = "preferences.txt";
             //String filepath = "GSAKForLocus";

            try {
                final File docPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
                final File filePath = new File(docPath, "GSAKForLocus");
                filePath.mkdirs();
                final File externalFile = new File(filePath, filename);
                final FileOutputStream fos = new FileOutputStream(externalFile);
                final OutputStreamWriter writer = new OutputStreamWriter(fos);
                writer.append("BlaBla");
                writer.close();
                fos.flush();
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void restorePreferences() {
        // We need the permission to access the file system. Check and ask for the permission if necessary
        WritePermissionActivity.checkPermission(getContext(), this::restorePreferencesWithPermission, null, null, false);
    }

    private void restorePreferencesWithPermission(final Context context, final Void voidData) {
        if (isExternalStorageAvailable() && !isExternalStorageReadOnly()) {
            String filename = "preferences.txt";
            //String filepath = "GSAKForLocus";

            try {
                final File docPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
                final File filePath = new File(docPath, "GSAKForLocus");
                filePath.mkdirs();
                final File externalFile = new File(filePath, filename);
                if (externalFile.exists()) {
                    // Read it
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static boolean isExternalStorageReadOnly() {
        String extStorageState = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED_READ_ONLY.equals(extStorageState);
    }

    private static boolean isExternalStorageAvailable() {
        String extStorageState = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(extStorageState);
    }
}
