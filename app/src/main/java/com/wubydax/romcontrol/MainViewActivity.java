package com.wubydax.romcontrol;

/*      Created by Roberto Mariani and Anna Berkovitch, 2015
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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.software.shell.fab.ActionButton;
import com.stericson.RootShell.exceptions.RootDeniedException;
import com.stericson.RootShell.execution.Command;
import com.stericson.RootTools.RootTools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;


public class MainViewActivity extends AppCompatActivity
        implements NavigationDrawerCallbacks, View.OnClickListener {

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;
    private Toolbar mToolbar;
    int[] ids;
    ActionButton[] rebootFabs;
    ActionButton reboot, hotboot, recovery, bl, ui;
    View overlay;
    AssetManager am;
    HandleScripts hs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        ThemeSelectorUtility theme = new ThemeSelectorUtility(this);
        theme.onActivityCreateSetTheme(this);
        CheckSu suPrompt = new CheckSu();
        suPrompt.execute();



        // populate the navigation drawer

    }

    public List<NavItem> getMenu() {
        List<com.wubydax.romcontrol.NavItem> items = new ArrayList<>();
        String[] mTitles = getResources().getStringArray(R.array.nav_drawer_items);
        int[] mIcons = {R.drawable.ic_ui_mods,
                R.drawable.ic_phone_mods,
                R.drawable.ic_general_framework,
                R.drawable.ic_apps,
                R.drawable.ic_settings};
        for (int i = 0; i < mTitles.length && i < mIcons.length; i++) {
            com.wubydax.romcontrol.NavItem current = new com.wubydax.romcontrol.NavItem();
            current.setText(mTitles[i]);
            current.setDrawable(mIcons[i]);
            items.add(current);
        }

        return items;
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments

        setTitle(getMenu().get(position).getText());
        switch (position) {
            case 0:
                getFragmentManager().beginTransaction().addToBackStack(null).replace(R.id.container, new UIPrefsFragment()).commitAllowingStateLoss();
                break;
            case 1:
                getFragmentManager().beginTransaction().addToBackStack(null).replace(R.id.container, new PhonePrefsFragment()).commitAllowingStateLoss();
                break;
            case 2:
                getFragmentManager().beginTransaction().addToBackStack(null).replace(R.id.container, new FrameworksGeneralFragment()).commitAllowingStateLoss();
                break;
            case 3:
                getFragmentManager().beginTransaction().addToBackStack(null).replace(R.id.container, new AppLinksFragment()).commitAllowingStateLoss();
                break;
            case 4:
                showThemeChooserDialog();
                break;

        }

    }


    @Override
    public void onBackPressed() {
        if (mNavigationDrawerFragment.isDrawerOpen())
            mNavigationDrawerFragment.closeDrawer();
        else if (overlay.getVisibility() == View.VISIBLE) {
            showHideRebootMenu();
        } else
            super.onBackPressed();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main_view, menu);
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.reboot_menu) {
            showHideRebootMenu();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.overlay:
                showHideRebootMenu();
                break;
            case R.id.action_reboot:
                getRebootAction("reboot");
                break;
            case R.id.action_reboot_hotboot:
                getRebootAction("busybox killall system_server");
                break;
            case R.id.action_reboot_recovery:
                getRebootAction("reboot recovery");
                break;
            case R.id.action_reboot_bl:
                getRebootAction("reboot download");
                break;
            case R.id.action_reboot_systemUI:
                getRebootAction("pkill com.android.systemui");
                break;
        }


    }

    private void getRebootAction(String command){
        Command c = new Command(0, command);
        try {
            RootTools.getShell(true).add(c);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (RootDeniedException e) {
            e.printStackTrace();
        }
    }

    private void initRebootMenu() {
        ids = new int[]{R.id.action_reboot, R.id.action_reboot_hotboot, R.id.action_reboot_recovery, R.id.action_reboot_bl, R.id.action_reboot_systemUI};
        rebootFabs = new ActionButton[]{reboot, hotboot, recovery, bl, ui};
        overlay = findViewById(R.id.overlay);
        int l = ids.length;
        for (int i = 0; i < l; i++) {
            rebootFabs[i] = (ActionButton) findViewById(ids[i]);
            rebootFabs[i].hide();
            rebootFabs[i].setHideAnimation(ActionButton.Animations.ROLL_TO_RIGHT);
            rebootFabs[i].setShowAnimation(ActionButton.Animations.ROLL_FROM_RIGHT);
        }
    }

    public void showHideRebootMenu() {

        for (int i = 0; i < rebootFabs.length; i++) {
            if (rebootFabs[i].isShown()) {
                overlay.setVisibility(View.GONE);
                rebootFabs[i].hide();
            } else {
                overlay.setVisibility(View.VISIBLE);
                rebootFabs[i].show();

            }
        }
    }
    private void showThemeChooserDialog(){
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        Adapter adapter = new ArrayAdapter<>(this, R.layout.simple_list_item_single_choice, getResources().getStringArray(R.array.theme_items));
        b.setTitle(getString(R.string.theme_chooser_dialog_title))
                .setSingleChoiceItems((ListAdapter) adapter, PreferenceManager.getDefaultSharedPreferences(this).getInt("theme_prefs", 0), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                       initTheme(which);
                    }
                })
                       ;
        AlertDialog d = b.create();
        d.show();
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = this.getTheme();
        theme.resolveAttribute(R.attr.colorAccent, typedValue, true);

        Button cancel = d.getButton(AlertDialog.BUTTON_NEGATIVE);
        cancel.setTextColor(typedValue.data);
        Button ok = d.getButton(AlertDialog.BUTTON_POSITIVE);
        ok.setTextColor(typedValue.data);
        d.getWindow().setBackgroundDrawableResource(R.drawable.dialog_bg);
        ListView lv = d.getListView();
        int paddingTop = Math.round(this.getResources().getDimension(R.dimen.dialog_listView_top_padding));
        int paddingBottom = Math.round(this.getResources().getDimension(R.dimen.dialog_listView_bottom_padding));
        lv.setPadding(0, paddingTop, 0, paddingBottom);
    }

    private void initTheme(int i){
        PreferenceManager.getDefaultSharedPreferences(this).edit().putInt("theme_prefs", i).commit();
        finish();
        this.overridePendingTransition(0,R.animator.fadeout);
        startActivity(new Intent(this, MainViewActivity.class));
        this.overridePendingTransition(R.animator.fadein, 0);

    }

    public class CheckSu extends AsyncTask<String, Integer, Boolean> {
        ProgressDialog mProgressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog = new ProgressDialog(MainViewActivity.this);
            mProgressDialog.setMessage(getString(R.string.gaining_root));
            mProgressDialog.show();
        }

        @Override
        protected Boolean doInBackground(String... params) {
            if (RootTools.isAccessGiven()) {
                return null;

            } else {
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            mProgressDialog.dismiss();

            if (!RootTools.isAccessGiven()) {
                //If no su access detected, throw and alert dialog with single button that will finish the activity
                AlertDialog.Builder mNoSuBuilder = new AlertDialog.Builder(MainViewActivity.this);
                mNoSuBuilder.setTitle(R.string.missing_su_title);
                mNoSuBuilder.setMessage(R.string.missing_su);
                mNoSuBuilder.setNegativeButton(R.string.exit, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
                mNoSuBuilder.create();
                Dialog mNoSu = mNoSuBuilder.create();
                mNoSu.show();


            }else{
                setContentView(R.layout.activity_main_view);
                mToolbar = (Toolbar) findViewById(R.id.toolbar_actionbar);
                setSupportActionBar(mToolbar);

                mNavigationDrawerFragment = (NavigationDrawerFragment)
                        getFragmentManager().findFragmentById(R.id.fragment_drawer);

                // Set up the drawer.
                mNavigationDrawerFragment.setup(R.id.fragment_drawer, (DrawerLayout) findViewById(R.id.drawer), mToolbar, MainViewActivity.this);
                initRebootMenu();
                am = getAssets();
                hs = new HandleScripts(MainViewActivity.this);
                hs.copyAssetFolder();
            }


        }
    }
}
