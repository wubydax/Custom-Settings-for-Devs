package com.wubydax.romcontrol;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.widget.ListAdapter;
import android.widget.Toast;

import com.stericson.RootShell.exceptions.RootDeniedException;
import com.stericson.RootShell.execution.Command;
import com.stericson.RootTools.RootTools;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.TimeoutException;


/*      Created by Roberto Mariani and Anna Berkovitch, 21/06/15
        This program is free software: you can redistribute it and/or modify
        it under the terms of the GNU General Public License as published by
        the Free Software Foundation, either version 3 of the License, or
        (at your option) any later version.

        This program is distributed in the hope that it will be useful,
        but WITHOUT ANY WARRANTY; without even the implied warranty of
        MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
        GNU General Public License for more details.

        You should have received a copy of the GNU General Public License
        along with this program.  If not, see <http://www.gnu.org/licenses/>.*/
public class HandlePreferenceFragments implements SharedPreferences.OnSharedPreferenceChangeListener, Preference.OnPreferenceClickListener {
    PreferenceFragment pf;
    Context c;
    PreferenceManager pm;
    String spName;
    SharedPreferences prefs;
    SharedPreferences.Editor ed;
    ContentResolver cr;
    ListAdapter adapter;

    public HandlePreferenceFragments(Context context, PreferenceFragment pf, String spName) {
        this.pf = pf;
        this.c = context;
        this.spName = spName;
        pm = pf.getPreferenceManager();
        pm.setSharedPreferencesName(spName);
        prefs = pm.getSharedPreferences();
        ed = prefs.edit();
        cr = c.getContentResolver();
        int id = c.getResources().getIdentifier(spName, "xml", c.getPackageName());
        pf.addPreferencesFromResource(id);
    }

    public void onResumeFragment() {
        prefs.registerOnSharedPreferenceChangeListener(this);
        initAllKeys();
        getAllPrefs();
    }

    private void getAllPrefs() {

        adapter = pf.getPreferenceScreen().getRootAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            Preference p = (Preference) adapter.getItem(i);
            if (p instanceof PreferenceScreen) {
                allGroups(p);

            }
        }
    }

    public void allGroups(Preference p) {
        PreferenceScreen ps = (PreferenceScreen) p;
        if (ps.getKey() != null) {
            if (ps.getKey().contains("script#") || ps.getKey().contains(".")) {
                ps.setOnPreferenceClickListener(this);
            }
            if (ps.getKey().contains(".")) {
                int lastDot = ps.getKey().lastIndexOf(".");
                String pkgName = ps.getKey().substring(0, lastDot);
                try {
                    Drawable icon = c.getPackageManager().getApplicationIcon(pkgName);
                    ps.setIcon(icon);
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                    Map<Preference, PreferenceScreen> preferenceParentTree = buildPreferenceParentTree();
                    PreferenceScreen preferenceParent = preferenceParentTree.get(ps);
                    preferenceParent.removePreference(ps);

                }
            }
        }
        for (int i = 0; i < ps.getPreferenceCount(); i++) {
            Preference p1 = ps.getPreference(i);
            if (p1 instanceof PreferenceScreen) {
                allGroups(p1);

            }
        }
    }

    public Map<Preference, PreferenceScreen> buildPreferenceParentTree() {
        final Map<Preference, PreferenceScreen> result = new HashMap<>();
        final Stack<PreferenceScreen> curParents = new Stack<>();
        curParents.add(pf.getPreferenceScreen());
        while (!curParents.isEmpty()) {
            final PreferenceScreen parent = curParents.pop();
            final int childCount = parent.getPreferenceCount();
            for (int i = 0; i < childCount; ++i) {
                final Preference child = parent.getPreference(i);
                result.put(child, parent);
                if (child instanceof PreferenceScreen)
                    curParents.push((PreferenceScreen) child);
            }
        }
        return result;
    }

    private void initAllKeys() {
        Map<String, ?> keys = pm.getSharedPreferences().getAll();

        for (Map.Entry<String, ?> entry : keys.entrySet()) {
            String key = entry.getKey();
            Preference p = pf.findPreference(key);

            if (entry.getValue() instanceof Boolean) {
                int prefInt;
                int actualInt = 0;
                boolean actualBoolean;
                boolean boolValue = prefs.getBoolean(key, true);

                prefInt = (boolValue) ? 1 : 0;

                try {
                    actualInt = Settings.System.getInt(cr, key);
                } catch (Settings.SettingNotFoundException e) {
                    Settings.System.putInt(cr, key, prefInt);
                }

                actualBoolean = (actualInt == 0) ? false : true;
                if (!String.valueOf(boolValue).equals(String.valueOf(actualBoolean))) {
                    ed.putBoolean(key, actualBoolean).commit();
                }
//
            } else if (entry.getValue() instanceof Integer) {
                int prefInt = prefs.getInt(key, 0);
                int actualInt = 0;
                try {
                    actualInt = Settings.System.getInt(cr, key);
                } catch (Settings.SettingNotFoundException e) {
                    Settings.System.putInt(cr, key, prefInt);
                }
                if (prefInt != actualInt) {
                    ed.putInt(key, actualInt).commit();
                }

            } else if (entry.getValue() instanceof String) {
                String prefString = prefs.getString(key, "");
                String actualString;
                actualString = Settings.System.getString(cr, key);
                if (actualString == null) {
                    Settings.System.putString(cr, key, prefString);
                } else {
                    if (!prefString.equals(actualString)) {
                        ed.putString(key, actualString).commit();
                    }
                }
                if (p instanceof MyListPreference) {
                    MyListPreference l = (MyListPreference) pf.findPreference(key);
                    CharSequence[] mEntries = l.getEntries();
                    int mValueIndex = l.findIndexOfValue(actualString);
                    l.setSummary(mEntries[mValueIndex]);
                } else if (p instanceof MyEditTextPreference) {
                    MyEditTextPreference et = (MyEditTextPreference) pf.findPreference(key);
                    et.setSummary(actualString);
                }
            }
        }
    }


    public void onPauseFragment() {
        prefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference p = pf.findPreference(key);
        switch (p.getClass().getSimpleName()) {
            case "SwitchPreference":
                SwitchPreference s = (SwitchPreference) pf.findPreference(key);
                s.setChecked(sharedPreferences.getBoolean(key, true));
                break;
            case "CheckBoxPreference":
                CheckBoxPreference cbp = (CheckBoxPreference) pf.findPreference(key);
                cbp.setChecked(sharedPreferences.getBoolean(key, true));
                break;
            case "MyListPreference":
                MyListPreference l = (MyListPreference) pf.findPreference(key);
                CharSequence[] mEntries = l.getEntries();
                l.setSummary(mEntries[l.findIndexOfValue(sharedPreferences.getString(key, ""))]);
                break;
            case "MyEditTextPreference":
                MyEditTextPreference et = (MyEditTextPreference) pf.findPreference(key);
                et.setSummary(sharedPreferences.getString(key, ""));
                break;
            case "ColorPickerPreference":
                ColorPickerPreference cpp = (ColorPickerPreference) pf.findPreference(key);
                cpp.setColor(sharedPreferences.getInt(key, Color.WHITE));
        }
        updateDatabase(key, p, sharedPreferences);
    }

    private void updateDatabase(String key, Object o, SharedPreferences sp) {
        boolean isEnabled;
        int dbInt;
        String value = "";

        if (o instanceof SwitchPreference || o instanceof CheckBoxPreference) {
            isEnabled = sp.getBoolean(key, true);
            dbInt = (isEnabled) ? 1 : 0;
            Settings.System.putInt(cr, key, dbInt);
        } else if (o instanceof MyEditTextPreference || o instanceof MyListPreference) {
            value = sp.getString(key, "");
            Settings.System.putString(cr, key, value);
        } else if (o instanceof ColorPickerPreference) {
            dbInt = sp.getInt(key, Color.WHITE);
            Settings.System.putInt(cr, key, dbInt);
        } else if (o instanceof SeekBarPreference) {
            dbInt = sp.getInt(key, 0);
            Settings.System.putInt(cr, key, dbInt);
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference.getKey() != null && preference.getKey().contains("script#")) {
            String scriptName = preference.getKey().substring(preference.getKey().lastIndexOf("#") + 1) + ".sh";
            String pathToScript = c.getFilesDir() + File.separator + "scripts" + File.separator + scriptName;
            File script = new File(pathToScript);
            if (script.exists()) {
                boolean isChmoded = script.canExecute() ? true : false;
                if (!isChmoded) {
                    script.setExecutable(true);
                }
                Command command = new Command(0, pathToScript) {
                    @Override
                    public void commandCompleted(int id, int exitcode) {
                        super.commandCompleted(id, exitcode);
                        if (exitcode != 0) {
                            Toast.makeText(c, String.valueOf(exitcode), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(c, "Executed Successfully", Toast.LENGTH_SHORT).show();

                        }
                    }
                };
                try {
                    RootTools.getShell(true).add(command);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (TimeoutException e) {
                    e.printStackTrace();
                } catch (RootDeniedException e) {
                    e.printStackTrace();
                }
            }
        } else if (preference.getKey().contains(".")) {
            String cls = preference.getKey();
            String pkg = cls.substring(0, cls.lastIndexOf("."));
            Intent intent = new Intent(Intent.ACTION_MAIN).setClassName(pkg,
                    cls).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .setComponent(new ComponentName(pkg,
                            cls));
            try {
                c.startActivity(intent);
            } catch (ActivityNotFoundException anf) {
                Toast.makeText(c, "App not installed or intent not valid", Toast.LENGTH_SHORT).show();
            }

        }
        return true;
    }

}
