package net.kuratkoo.locusaddon.gsakdatabase;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;
import menion.android.locus.addon.publiclib.LocusUtils;

/**
 * MainActivity
 * @author Radim -kuratkoo- Vaculik <kuratkoo@gmail.com>
 */
public class MainActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {

    private Preference dbPick;
    private EditTextPreference nick;
    private EditTextPreference logsCount;
    private EditTextPreference radius;
    private EditTextPreference limit;
    private CheckBoxPreference own;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.prefs);
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        own = (CheckBoxPreference) getPreferenceScreen().findPreference("own");

        dbPick = getPreferenceScreen().findPreference("db_pick");
        dbPick.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

            public boolean onPreferenceClick(Preference pref) {
                try {
                    LocusUtils.intentPickFile(MainActivity.this, 0, getText(R.string.pref_db_pick_title).toString(), new String[]{".db3"});
                } catch (ActivityNotFoundException anfe) {
                    Toast.makeText(MainActivity.this, "Error: " + anfe.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }
                return true;
            }
        });

        dbPick.setSummary(editPreferenceSummary(PreferenceManager.getDefaultSharedPreferences(this).getString("db", ""), getText(R.string.pref_db_sum)));

        nick = (EditTextPreference) getPreferenceScreen().findPreference("nick");
        nick.setSummary(editPreferenceSummary(nick.getText(), getText(R.string.pref_nick_sum)));
        own.setEnabled(nick.getText().trim().length() != 0);

        radius = (EditTextPreference) getPreferenceScreen().findPreference("radius");
        radius.setSummary(editPreferenceSummary(radius.getText() + " km", getText(R.string.pref_radius_sum)));

        logsCount = (EditTextPreference) getPreferenceScreen().findPreference("logs_count");
        logsCount.setSummary(editPreferenceSummary(logsCount.getText(), getText(R.string.pref_logs_sum)));

        limit = (EditTextPreference) getPreferenceScreen().findPreference("limit");
        if (limit.getText().equals("0")) {
            limit.setSummary(editPreferenceSummary(getString(R.string.pref_limit_nolimit), getText(R.string.pref_limit_sum)));
        } else {
            limit.setSummary(editPreferenceSummary(limit.getText(), getText(R.string.pref_limit_sum)));
        }

        if (!own.isEnabled()) {
            own.setSummary(Html.fromHtml(getString(R.string.pref_own_sum) + " <b>" + getString(R.string.pref_own_fill) + "</b>"));
        }

        if (PreferenceManager.getDefaultSharedPreferences(this).getInt("count", 0) < 3) {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putInt("count", PreferenceManager.getDefaultSharedPreferences(this).getInt("count", 0) + 1);
            editor.commit();
        }
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("db")) {
            String path = sharedPreferences.getString(key, "");
            dbPick.setSummary(editPreferenceSummary(path, getText(R.string.pref_db_sum)));
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
                Toast.makeText(this, getString(R.string.pref_logs_error), Toast.LENGTH_LONG).show();
                value = "1";
                radius.setText(value);
            }
            radius.setSummary(editPreferenceSummary(value + " km", getText(R.string.pref_radius_sum)));
        }

        if (key.equals("limit")) {
            String value = sharedPreferences.getString(key, "0");
            if (value.equals("") || !value.matches("[0-9]+")) {
                Toast.makeText(this, getString(R.string.pref_limit_error), Toast.LENGTH_LONG).show();
                limit.setText("0");
                limit.setSummary(editPreferenceSummary(getString(R.string.pref_limit_nolimit), getText(R.string.pref_logs_sum)));
            } else if (value.equals("0")) {
                limit.setSummary(editPreferenceSummary(getString(R.string.pref_limit_nolimit), getText(R.string.pref_limit_sum)));
            } else {
                limit.setSummary(editPreferenceSummary(value, getText(R.string.pref_limit_sum)));
            }
        }
    }

    private Spanned editPreferenceSummary(String value, CharSequence summary) {
        if (!value.equals("")) {
            return Html.fromHtml("<font color=\"#FF8000\"><b>(" + value + ")</b></font> " + summary);
        } else {
            return Html.fromHtml(summary.toString());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        if (requestCode == 0) {
            if (resultCode == RESULT_OK && data != null) {
                String filename = data.getData().toString().replace("file://", "");
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString("db", filename);
                editor.commit();
                dbPick.setSummary(editPreferenceSummary(filename, getText(R.string.pref_db_sum)));
            }
        }
    }
}