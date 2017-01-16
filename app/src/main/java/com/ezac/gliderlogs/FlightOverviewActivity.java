package com.ezac.gliderlogs;

/*
 *  Copyright (c) <2015> <Pro-Serv, P van der Wielen, EZAC>
 *  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated 
 *	documentation files (the "Software"), to deal in the Software without restriction, including without limitation 
 *	the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, 
 *	and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *	
 *	Commercial usage of  (the "Software") is not prohibited
 *  The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE 
 *	WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR 
 *	COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, 
 *	ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * GliderLog is intended to assist in the mandatory logging of glider start activity
 * 2.0				new release
 * 2.1    fixed		missing passenger list, it needed a year as minimal parameter
 * 2.2    fixed		fixed crash at startup if no network is available (NTP started to soon)
 * 					fixed landing strip orientation being forgotten, changed ogn to be default off	
 * 2.5				ogn stuff is been removed, logging been adjusted to yyyy folder with mm_dd subfolder
 * 		  			at startup, it now will automatically remove all processed flghts of previous date
 * 					but will preserve and try to report still remaining flights to be processed
 * 					adjusted application display/mode flag 'appFLG' has 10 fields separated by ';'
 * 2.6 fixed		removed oAuth, emulated and adopted session based log-on to 'Drupal' CMS
 * 					and rewritten the wifi event handling / startup timing chain
 * 2.7				release for production -> added additional logic for instruction checkbox
 * 2.8				released on 7-4-2016, major changes in network related timing / code cleanup
 * 2.9rc1			release on 25-4-2016, most setting items are now service code protected.
 * 2.9rc3			added set today's date in on_resume as to assume it doesn't get lost
 * 					small change in OnDuty, add date filtering
 * 3.0rc1           code review & cleanup
 * *****
 * note:	DON'T USE the toast function within a async task, it will crash your application
 * *****
 **/

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.Color;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StrictMode;
import android.os.SystemClock;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
//import android.os.PowerManager;
import com.ezac.gliderlogs.contentprovider.FlightsContentProvider;
import com.ezac.gliderlogs.database.GliderLogTables;
import com.ezac.gliderlogs.misc.SntpClient;
import com.ezac.gliderlogs.misc.NetworkUtility;
import com.ezac.gliderlogs.misc.Common;
import com.ezac.gliderlogs.misc.Logs;

public class FlightOverviewActivity extends ListActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final int DELETE_ID = Menu.FIRST + 1;
    protected static String TAG = "FlightsOverview";
    private SimpleCursorAdapter adapter;
    //
    private int[] intSum = new int[4];
    private int scan_cnt = 0;
    private int pass_cnt = 0;
    private int prev_cnt = 0;
    // initialize flag -> prevent startup issue's
    private boolean app_ini = true;
    private boolean app_hld = false;
    // today's date
    public static String ToDay = "";
    // some var's for preferences
    private String appURL;
    private String appPRE;
    private String appSCN;
    public String appKEY;
    public String appSCT;
    public static String appMST;
    public static String appMTR;
    private String appFLG;
    private String appLND;
    private long timedrift = 0;
    private String host_url;
    private String sync_sts = "";
    private String selection = "";
    private String btn_select = "";
    private String rec_id;
    private String[] month_list = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul",
            "Aug", "Sep", "Oct", "Nov", "Dec"};
    private String session_name;
    private String session_id;
    private String session_token;
    private Menu menu;
    private View settingsView;
    private View servicesView;
    private View membersView;
    final Context context = FlightOverviewActivity.this;
    private MenuItem MenuItem_net;

    private Spinner mMemberSpin;
    private Spinner mGliderSpin;

    private List<String> GliderList = new ArrayList<String>();
    private List<String> MemberList = new ArrayList<String>();
    private List<String> MemberIndexList = new ArrayList<String>();
    //private PowerManager.WakeLock wl;

    Handler scan = new Handler();
    Handler srvr = new Handler();
    SntpClient client = new SntpClient();
    // for Network & WiFi status
    private boolean isConnected;
    private boolean isAuthenticated = false;
    private boolean isInit = false;
    final Calendar c = Calendar.getInstance();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        app_ini = true;
        // during start up of application we define our current date
        final Calendar c = Calendar.getInstance();
        ToDay = Common.TwoDigits(c.get(Calendar.DAY_OF_MONTH)) +
                "-" + Common.TwoDigits(c.get(Calendar.MONTH) + 1) +
                "-" + Common.FourDigits(c.get(Calendar.YEAR));
        Log.d(TAG, "Started, date = " + ToDay);
        if (!getSettings(context)) {
            SharedPreferences prefs = context.getSharedPreferences("Share", Context.MODE_PRIVATE);
            prefs.edit().clear();
            SharedPreferences.Editor es = prefs.edit();
            es.putString("com.ezac.gliderlogs.url", "host").apply();
            es.putString("com.ezac.gliderlogs.pre", "45").apply();
            es.putString("com.ezac.gliderlogs.scn", "300").apply();
            es.putString("com.ezac.gliderlogs.key", "").apply();
            es.putString("com.ezac.gliderlogs.sct", "").apply();
            es.putString("com.ezac.gliderlogs.mst", "6319").apply();
            es.putString("com.ezac.gliderlogs.mtr", "EHFS;EFWO").apply();
            es.putString("com.ezac.gliderlogs.ntp", "").apply();
            es.putString("com.ezac.gliderlogs.flg", "false;false;true;true;true;true;false;false;false;false").apply();
            es.putString("com.ezac.gliderlogs.lnd", "O   - W").apply();
        }
        String v[] = appFLG.split(";");
        // check for loaded (glider / member) tables
        checktable(Boolean.parseBoolean(v[1]));
        // remove previous day(s) and/or already processed flights from table
        getTableFlightsCnt(ToDay, Boolean.parseBoolean(v[0]) ? 2 : 1);
        // check for unprocessed flights which are older than today
        if ((prev_cnt = getTableFlightsCnt(ToDay, 0)) != 0) {
            makeToast("Er zijn nog " + prev_cnt + " vluchten van een vorige vliegdag gevonden," +
                    "\ndeze zullen, zodra er een WiFi verbinding is, alsnog" +
                    "\nworden verzonden naar de server maar NIET zichtbaar zijn" +
                    "\nin het overzicht voor deze vliegdag.", 1);
        }
        // normal execution may proceed
        setContentView(R.layout.start_list);
        this.getListView().setDividerHeight(2);
        //
        fillData();
        registerForContextMenu(getListView());
        // starts our handler activity after 1 minute
        scan.postDelayed(runnable, 60000);
        // load data for spinner into array list
        addItemSpinner_1();
        addItemSpinner_2();
        // left in place as to potential add next section in future disable screen lock
/*
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK
                | PowerManager.ACQUIRE_CAUSES_WAKEUP
                | PowerManager.ON_AFTER_RELEASE, "INFO");
        wl.acquire();
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
        	    | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        	    | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
        	    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        	    | WindowManager.LayoutParams.FLAG_FULLSCREEN
        	    | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        	    | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
        	    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
 */
        // disable screen hide
        getListView().setKeepScreenOn(true);
        // setup event receiver for wifi
        IntentFilter filter = new IntentFilter(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        this.registerReceiver(wifiStatusReceiver, filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(wifiStatusReceiver);
    }

    @Override
    protected void onStop() {
        super.onStop();
        //wl.release();
        getLoaderManager().destroyLoader(0);
    }

    @Override
    public void onResume() {
        super.onResume();
        // during start up of application we define our current date
        final Calendar c = Calendar.getInstance();
        ToDay = Common.TwoDigits(c.get(Calendar.DAY_OF_MONTH)) +
                "-" + Common.TwoDigits(c.get(Calendar.MONTH) + 1) +
                "-" + Common.FourDigits(c.get(Calendar.YEAR));
        Log.d(TAG, "Resumed, date = " + ToDay);
    }

    public void addItemSpinner_1() {
        Uri uri = FlightsContentProvider.CONTENT_URI_MEMBER;
        String[] projection = {GliderLogTables.M_ID, GliderLogTables.M_2_NAME,
                GliderLogTables.M_3_NAME, GliderLogTables.M_1_NAME,
                GliderLogTables.M_INSTRUCTION};
        Cursor cur_sp = getContentResolver().query(uri, projection, null, null,
                GliderLogTables.M_1_NAME + " ASC");
        if (cur_sp != null) {
            try {
                cur_sp.moveToFirst();
                // insert dummy item with no data as to avoid pre-selection
                MemberList.add("");
                MemberIndexList.add("");
                for (int i = 0; i < cur_sp.getCount(); i++) {
                    String tmp = cur_sp.getString(cur_sp
                            .getColumnIndexOrThrow(GliderLogTables.M_1_NAME))
                            + " "
                            + cur_sp.getString(cur_sp
                            .getColumnIndexOrThrow(GliderLogTables.M_2_NAME))
                            + " "
                            + cur_sp.getString(cur_sp
                            .getColumnIndexOrThrow(GliderLogTables.M_3_NAME));
                    MemberList.add(tmp.replaceAll("\\s+", " "));
                    MemberIndexList.add(cur_sp.getString(cur_sp
                            .getColumnIndexOrThrow(GliderLogTables.M_ID)));
                    cur_sp.moveToNext();
                }
            } finally {
                if (!cur_sp.isClosed()) {
                    cur_sp.close();
                }
            }
        }
    }

    public void addItemSpinner_2() {
        Uri uri = FlightsContentProvider.CONTENT_URI_GLIDER;
        String[] projection = {GliderLogTables.G_REGISTRATION};
        Cursor cur_sp = getContentResolver().query(uri, projection, null, null,
                GliderLogTables.G_REGISTRATION + " ASC");
        if (cur_sp != null) {
            try {
                cur_sp.moveToFirst();
                // insert dummy item with no data as to avoid pre-selection
                GliderList.add("");
                for (int i = 0; i < cur_sp.getCount(); i++) {
                    GliderList
                            .add(cur_sp.getString(cur_sp
                                    .getColumnIndexOrThrow(GliderLogTables.G_REGISTRATION)));
                    cur_sp.moveToNext();
                }
            } finally {
                if (!cur_sp.isClosed()) {
                    cur_sp.close();
                }
            }
        }
    }

    public String getDetailInfo(Uri uri, String param, int mde) {
        Cursor cur_di = null;
        String tmp = "";
        switch (mde) {
            case 0:
                String[] projection_1 = {GliderLogTables.G_REGISTRATION, GliderLogTables.G_CALLSIGN,
                        GliderLogTables.G_TYPE, GliderLogTables.G_BUILD, GliderLogTables.G_SEATS,
                        GliderLogTables.G_OWNER, GliderLogTables.G_PRIVATE};
                selection = GliderLogTables.G_REGISTRATION + " like '" + param + "'";
                cur_di = getContentResolver().query(uri, projection_1, selection, null, null);
                if (cur_di != null) {
                    try {
                        cur_di.moveToFirst();
                        tmp = cur_di.getString(cur_di
                                .getColumnIndexOrThrow(GliderLogTables.G_REGISTRATION));
                        tmp = tmp + ";" + (cur_di.getString(cur_di
                                .getColumnIndexOrThrow(GliderLogTables.G_CALLSIGN)).equals("null") ? "-" : cur_di.getString(cur_di
                                .getColumnIndexOrThrow(GliderLogTables.G_CALLSIGN)));
                        tmp = tmp + ";" + (cur_di.getString(cur_di
                                .getColumnIndexOrThrow(GliderLogTables.G_TYPE)).equals("null") ? "-" : cur_di.getString(cur_di
                                .getColumnIndexOrThrow(GliderLogTables.G_TYPE)));
                        tmp = tmp + ";" + (cur_di.getString(cur_di
                                .getColumnIndexOrThrow(GliderLogTables.G_BUILD)).equals("null") ? "-" : cur_di.getString(cur_di
                                .getColumnIndexOrThrow(GliderLogTables.G_BUILD)));
                        tmp = tmp + ";\nTweezitter: " + (cur_di.getString(cur_di
                                .getColumnIndexOrThrow(GliderLogTables.G_SEATS)).equals("null") ? "?" : (cur_di.getString(cur_di
                                .getColumnIndexOrThrow(GliderLogTables.G_SEATS))).equals("1") ? "Nee" : "Ja");
                        tmp = tmp + ";\nEigenaar: " + (cur_di.getString(cur_di
                                .getColumnIndexOrThrow(GliderLogTables.G_OWNER)).equals("null") ? "" : cur_di.getString(cur_di
                                .getColumnIndexOrThrow(GliderLogTables.G_OWNER)));
                        tmp = tmp + ";Prive: " + (cur_di.getString(cur_di
                                .getColumnIndexOrThrow(GliderLogTables.G_PRIVATE)).equals("null") ? "?" : (cur_di.getString(cur_di
                                .getColumnIndexOrThrow(GliderLogTables.G_PRIVATE))).equals("0") ? "Nee" : "Ja");
                    } finally {
                        if (!cur_di.isClosed()) {
                            cur_di.close();
                        }
                    }
                }
                break;
            case 1:
                String[] projection_2 = {GliderLogTables.M_1_NAME, GliderLogTables.M_2_NAME,
                        GliderLogTables.M_3_NAME, GliderLogTables.M_PHONE, GliderLogTables.M_MOBILE,
                        GliderLogTables.M_INSTRUCTION, GliderLogTables.M_ID};
                selection = GliderLogTables.M_ID + " like '" + param + "'";
                cur_di = getContentResolver().query(uri, projection_2, selection, null, null);
                if (cur_di != null) {
                    try {
                        cur_di.moveToFirst();
                        tmp = cur_di.getString(cur_di
                                .getColumnIndexOrThrow(GliderLogTables.M_1_NAME))
                                + " "
                                + cur_di.getString(cur_di
                                .getColumnIndexOrThrow(GliderLogTables.M_2_NAME))
                                + " "
                                + cur_di.getString(cur_di
                                .getColumnIndexOrThrow(GliderLogTables.M_3_NAME));
                        tmp = tmp.replaceAll("\\s+", " ");
                        tmp = tmp + ";\n" + (cur_di.getString(cur_di
                                .getColumnIndexOrThrow(GliderLogTables.M_PHONE)).equals("null") ? "-" : cur_di.getString(cur_di
                                .getColumnIndexOrThrow(GliderLogTables.M_PHONE)));
                        tmp = tmp + ";" + (cur_di.getString(cur_di
                                .getColumnIndexOrThrow(GliderLogTables.M_MOBILE)).equals("null") ? "-" : cur_di.getString(cur_di
                                .getColumnIndexOrThrow(GliderLogTables.M_MOBILE)));
                        tmp = tmp + ";\nInstructeur: " + (cur_di.getString(cur_di
                                .getColumnIndexOrThrow(GliderLogTables.M_INSTRUCTION)).equals("0") ? "Nee" : "Ja");
                    } finally {
                        if (!cur_di.isClosed()) {
                            cur_di.close();
                        }
                    }
                }
                break;
        }
        return tmp;
    }

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            // place holder for various jobs like: any starts to create, updated or deleted
            if (isConnected) {
                if (isAuthenticated) {
                    switch (scan_cnt) {
                        case 4:
                            new NETTask().execute(host_url + "api/starts", "2");
                            // remove any user filter selections
                            OptionSelect(R.id.action_all, R.id.action_open, R.id.action_ready, R.id.action_45min, R.id.action_my);
                            DoFlightFilter(4, "");
                            scan_cnt = 0;
                            pass_cnt = pass_cnt + 1;
                            if (pass_cnt > 5) {
                                //final Calendar c = Calendar.getInstance();
                                new LDRTask().execute(host_url + "api/passagiers/*.json?datum=" + Common.FourDigits(c.get(Calendar.YEAR)), "4");
                                //+ "-" + Common.TwoDigits(c.get(Calendar.MONTH)) //  + Common.FourDigits(c.get(Calendar.YEAR))
                                pass_cnt = 0;
                            }
                            break;
                        default:
                            // check if our host still reachable
                            new NETTask().execute("www.yoursite.nl", "3");
                            MenuItem MenuItem_date = menu.findItem(R.id.action_date);
                            String s[] = ToDay.split("-");
                            MenuItem_date.setTitle(s[0] + " " + month_list[Integer.parseInt(s[1]) - 1] + " (" + getTableFlightProgress() + ")");
                            scan_cnt = scan_cnt + 1;
                    }
                } else {
                    // get current date & time from NTP server
                    new NETTask().execute("pool.ntp.org", "1");
                    // logon to Ezac site and obtain session details
                    new LoginTask().execute(host_url);
                    isAuthenticated = true;
                }
            }
            /* and here comes the "trick" to keep it running ! */
            scan.postDelayed(this, (Integer.parseInt(appSCN) * 1000) / 5);
        }
    };

    // create the menu based on the XML defintion
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.listmenu, menu);
        this.menu = menu;
        // adjust value in menu button to value in use
        MenuItem MenuItem_dur = menu.findItem(R.id.action_45min);
        MenuItem_dur.setTitle("" + appPRE + " Min");
        // adjust visibility of some menu tabs
        String v[] = appFLG.split(";");
        //Log.d(TAG,"p1 " + isInit + " p2 "+ Boolean.parseBoolean(v[1]));
        menu.findItem(R.id.action_db_start).setVisible(((isInit == true) && Boolean.parseBoolean(v[1])) ? false : true);
        menu.findItem(R.id.action_ezac).setVisible(Boolean.parseBoolean(v[2]));
        menu.findItem(R.id.action_meteo_group).setVisible(Boolean.parseBoolean(v[3]));
        menu.findItem(R.id.action_ntm_nld).setVisible(Boolean.parseBoolean(v[4]));
        menu.findItem(R.id.action_ntm_blx).setVisible(Boolean.parseBoolean(v[4]));
        menu.findItem(R.id.action_ogn_flarm).setVisible(Boolean.parseBoolean(v[5]));
        menu.findItem(R.id.action_adsb).setVisible(Boolean.parseBoolean(v[6]));
        menu.findItem(R.id.action_adsb_lcl).setVisible(Boolean.parseBoolean(v[7]));
        // adjust value in menu button to current date
        MenuItem MenuItem_date = menu.findItem(R.id.action_date);
        String s[] = ToDay.split("-");
        MenuItem_date.setTitle(s[0] + " " + month_list[Integer.parseInt(s[1]) - 1]);

        return true;
    }

    // Reaction to the menu selection
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_insert:
                if (!app_hld) {
                    createNewFlight();
                } else {
                    makeToast("Er is een te groot verschil in tijd tussen de tablet en de " +
                            "\nwerkelijke tijd gevonden, verschil is thans " + timedrift + " sec.," +
                            "\nu MOET eerst middels :" +
                            "\nInstellingen -> Datum en Tijd -> de datum en tijd GOED instellen." +
                            "\nVoer daarna de optie \"Dag opstarten\" opnieuw uit." +
                            "\nDit MOET worden gedaan binnen een active WiFi netwerk" +
                            "\nvoordat de applicatie MAG worden gebruikt", 1);
                }
                return true;
            case R.id.action_service:
                DoServices();
                return true;
            case R.id.action_db_start:
                DoAction(1, "Weet u zeker dat u een nieuwe vliegdag wilt starten ??");
                return true;
            case R.id.action_db_close:
                DoAction(2, "Wilt u een overzicht van de huidige dag ??");
                return true;
            case R.id.action_db_pass:
                DoAction(3, "Wilt u de passagiers lijst verversen ??");
                return true;
            case R.id.action_settings:
                DoSettings();
                return true;
            case R.id.action_ready:
                OptionSelect(R.id.action_ready, R.id.action_open, R.id.action_45min, R.id.action_my, R.id.action_all);
                DoFlightFilter(1, "");
                return true;
            case R.id.action_open:
                OptionSelect(R.id.action_open, R.id.action_ready, R.id.action_45min, R.id.action_my, R.id.action_all);
                DoFlightFilter(2, "");
                return true;
            case R.id.action_pass:
                createPassengerOverview();
                return true;
            case R.id.action_leden:
                createMemberOverview();
                return true;
            case R.id.action_resv:
                createReservationOverview();
                return true;
            case R.id.action_duty:
                createOnDutyOverview();
                return true;
            case R.id.action_meteo_1:
                createWeather1_Overview();
                return true;
            case R.id.action_meteo_2:
                createWeather2_Overview();
                return true;
            case R.id.action_45min:
                OptionSelect(R.id.action_45min, R.id.action_open, R.id.action_ready, R.id.action_my, R.id.action_all);
                DoFlightFilter(3, "");
                return true;
            case R.id.action_my:
                DoFlightMember();
                return true;
            case R.id.action_all:
                OptionSelect(R.id.action_all, R.id.action_open, R.id.action_ready, R.id.action_45min, R.id.action_my);
                DoFlightFilter(4, "");
                return true;
            case R.id.action_ogn_flarm:
                createOGNOverview();
                return true;
            case R.id.action_adsb:
                createGliderOverview();
                return true;
            case R.id.action_adsb_lcl:
                createADSBOverview();
                return true;
            case R.id.action_ntm_nld:
                createNotamNLDOverview();
                return true;
            case R.id.action_ntm_blx:
                createNotamBLXOverview();
                return true;
            case R.id.action_ezac:
                createEzacOverview();
                return true;
            case R.id.action_net:
                makeToast("Blauwe wereld bol = host ping test ok,\n"
                        + "Grijze wereld bol = geen internet/server verbinding\n"
                        + "Groen icon = synchronisatie data naar host server = ok\n"
                        + "Rood icon  = fout bij applicatie synchronisatie naar host\n"
                        + "Laatste synchronisatie status = " + (sync_sts.equals("") ? "ok" : sync_sts), 0);
                return true;
            case R.id.action_db_export_CSV:
                GliderLogToCSV("gliderlogs.db", "ezac");
                GliderLogToDB("com.ezac.gliderlogs", "gliderlogs.db", "ezac");
                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                        Uri.parse("file://"
                                + Environment.getExternalStorageDirectory())));
                makeToast("Export naar een CSV bestand is uitgevoerd !", 2);
                return true;
            case R.id.action_about:
                DoInfo();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void OptionUnSelect(int sel_1) {
        MenuItem mi = menu.findItem(sel_1);
        String tmp = (String) mi.getTitle();
        mi.setTitle(tmp.replace(">", "").replace("<", ""));
    }

    public void OptionSelect(int sel_1, int sel_2, int sel_3, int sel_4, int sel_5) {
        MenuItem mi = menu.findItem(sel_1);
        String tmp = (String) mi.getTitle();
        if (tmp.indexOf('>') < 0) {
            mi.setTitle(">" + mi.getTitle() + "<");
        }
        OptionUnSelect(sel_2);
        OptionUnSelect(sel_3);
        OptionUnSelect(sel_4);
        OptionUnSelect(sel_5);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case DELETE_ID:
                AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
                        .getMenuInfo();
                Uri uri = Uri.parse(FlightsContentProvider.CONTENT_URI_FLIGHT + "/"
                        + info.id);

                ContentValues values = new ContentValues();
                values.put(GliderLogTables.F_SENT, "0");
                values.put(GliderLogTables.F_ACK, "0");
                values.put(GliderLogTables.F_DEL, "1");
                String select = GliderLogTables.F_ID + "=?";
                String selArgs[] = {"" + info.id};
                getContentResolver().update(uri, values, select, selArgs);
                //Log.d(TAG, "TEST URI " + uri + " resp " + x + "Args " + selArgs[0]);
                // /getContentResolver().delete(uri, null, null);
                getLoaderManager().restartLoader(0, null, this);
                fillData();
                return true;
        }
        return super.onContextItemSelected(item);
    }

    private void createNewFlight() {
        Intent i = new Intent(this, FlightDetailActivity.class);
        startActivity(i);
    }

    private void createDayReport() {
        Intent i = new Intent(this, FlightReportActivity.class);
        startActivity(i);
    }

    private void createWeather1_Overview() {
        Intent i = new Intent(this, FlightWeatherActivity.class);
        i.putExtra("Ref", appLND);
        startActivity(i);
    }

    private void createWeather2_Overview() {
        Intent i = new Intent(this, FlightRaspActivity.class);
        startActivity(i);
    }


    private void createADSBOverview() {
        Intent i = new Intent(this, FlightADSBActivity.class);
        startActivity(i);
    }

    private void createGliderOverview() {
        Intent i = new Intent(this, FlightGliderActivity.class);
        startActivity(i);
    }

    private void createNotamNLDOverview() {
        Intent i = new Intent(this, FlightNotamNLDActivity.class);
        startActivity(i);
    }

    private void createNotamBLXOverview() {
        Intent i = new Intent(this, FlightNotamBLXActivity.class);
        startActivity(i);
    }

    private void createOGNOverview() {
        Intent i = new Intent(this, FlightOGNActivity.class);
        startActivity(i);
    }

    private void createEzacOverview() {
        Intent i = new Intent(this, FlightEzacActivity.class);
        startActivity(i);
    }

    private void createPassengerOverview() {
        Intent i = new Intent(this, FlightPassengerActivity.class);
        startActivity(i);
    }

    private void createMemberOverview() {
        Intent i = new Intent(this, FlightMemberActivity.class);
        startActivity(i);
    }

    private void createReservationOverview() {
        Intent i = new Intent(this, FlightReservationActivity.class);
        startActivity(i);
    }

    private void createOnDutyOverview() {
        Intent i = new Intent(this, FlightOnDutyActivity.class);
        startActivity(i);
    }

    // Opens the second activity if an entry is clicked
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Intent i = new Intent(this, FlightDetailActivity.class);
        Uri FlightUri = Uri.parse(FlightsContentProvider.CONTENT_URI_FLIGHT
                + "/" + id);
        i.putExtra(FlightsContentProvider.CONTENT_ITEM_TYPE, FlightUri);
        startActivity(i);
    }

    private void fillData() {
        // Must include the _id column for the adapter to work properly
        String[] from = new String[]{
                GliderLogTables.F_REGISTRATION, GliderLogTables.F_CALLSIGN,
                GliderLogTables.F_TYPE, GliderLogTables.F_INSTRUCTION,
                GliderLogTables.F_PILOT, GliderLogTables.F_COPILOT,
                GliderLogTables.F_STARTED, GliderLogTables.F_LANDED,
                GliderLogTables.F_DURATION, GliderLogTables.F_LAUNCH,
                GliderLogTables.F_NOTES};
        // Fields on the UI to which we want to map
        int[] to = new int[]{R.id.label2, R.id.label2a, R.id.label3,
                R.id.label4, R.id.label5, R.id.label6, R.id.label7,
                R.id.label8, R.id.label9, R.id.label10, R.id.label11};

        getLoaderManager().initLoader(0, null, this);
        adapter = new SimpleCursorAdapter(this, R.layout.start_row, null, from,
                to, 0);
        // added to avoid error in case no data in adapter
        Log.d(TAG, "adapter count: " + adapter.getCount());
        //	if (adapter.getCount() > 0) {
        setListAdapter(adapter);
        //	}
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, DELETE_ID, 0, R.string.menu_delete);
    }

    // creates a new loader after the initLoader () call
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] projection = {
                GliderLogTables.F_ID, GliderLogTables.F_DATE,
                GliderLogTables.F_REGISTRATION, GliderLogTables.F_CALLSIGN,
                GliderLogTables.F_TYPE, GliderLogTables.F_INSTRUCTION,
                GliderLogTables.F_PILOT, GliderLogTables.F_COPILOT,
                GliderLogTables.F_STARTED, GliderLogTables.F_LANDED,
                GliderLogTables.F_DURATION, GliderLogTables.F_LAUNCH,
                GliderLogTables.F_NOTES, GliderLogTables.F_SENT,
                GliderLogTables.F_ACK};
        if (btn_select.equals("")) {
            selection = GliderLogTables.F_DEL + "=0";
        } else {
            selection = btn_select;
        }
        // added sort criteria
        String sort = GliderLogTables.F_SENT + " ASC, " + GliderLogTables.F_STARTED + " DESC";
        try {
            CursorLoader cursorLoader = new CursorLoader(this,
                    FlightsContentProvider.CONTENT_URI_FLIGHT, projection,
                    selection, null, sort);
            return cursorLoader;
        } catch (Exception e) {
            Log.d(TAG, "unable to get records, error: " + e.getMessage() + ", select:" + selection);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        //if (!data.isClosed() ) {
        //if ((data. == null) || (data.getCount() == 0)){
        //	makeToast("Mogelijk ten gevolge van een netwerk probleem\n"
        //			+ "nog geen data, kontroleer de netwerk status en voer:\n"
        //			+ "Opties -> Voer de actie 'Dag opstarten' opnieuw uit !.", 1);
        //} else {
        if ((data != null) && (data.getCount() > 0)) {
            adapter.swapCursor(data);
        }
        //}
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // data is not available anymore, delete reference
        adapter.swapCursor(null);
    }

    private void makeToast(String msg, int mode) {

        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.toast_layout,
                (ViewGroup) findViewById(R.id.toast_layout_root));

        ImageView image = (ImageView) layout.findViewById(R.id.image);
        image.setImageResource(R.drawable.ic_launcher);
        TextView text = (TextView) layout.findViewById(R.id.text);
        text.setText(msg);

        Toast toast = new Toast(getApplicationContext());
        toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setView(layout);
        if (mode == 1) {
            text.setTextColor(Color.WHITE);
            toast.getView().setBackgroundColor(Color.RED);
            toast.setDuration(Toast.LENGTH_LONG);
        }
        if (mode == 2) {
            text.setTextColor(Color.WHITE);
            toast.getView().setBackgroundColor(Color.GREEN);
        }
        toast.show();
    }

    public void DoFlightMember() {
        // get member_list.xml view
        LayoutInflater li = LayoutInflater.from(context);
        membersView = li.inflate(R.layout.member_list, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                context);
        // set member_list.xml to alertdialog builder
        alertDialogBuilder.setView(membersView);
        //
        Button mMember_Btn;
        final EditText mDetailInfo;
        Button mGlider_Btn;
        // set dialog message
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        String fRegi = (String) mGliderSpin.getSelectedItem();
                        String fPilo = (String) mMemberSpin.getSelectedItem();
                        if (!fPilo.equals("") && !fRegi.equals("")) {
                            String fRegiPilo = "";
                            for (int i = 0; i < mGliderSpin.getCount(); i++) {
                                String s = (String) mGliderSpin.getItemAtPosition(i);
                                if (s.equalsIgnoreCase(fRegi)) {
                                    fRegiPilo = GliderList.get(i);
                                }
                            }
                            for (int i = 0; i < mMemberSpin.getCount(); i++) {
                                String s = (String) mMemberSpin.getItemAtPosition(i);
                                if (s.equalsIgnoreCase(fPilo)) {
                                    fRegiPilo = fRegiPilo + ":" + MemberIndexList.get(i);
                                }
                            }
                            DoFlightFilter(7, fRegiPilo);
                            OptionSelect(R.id.action_my, R.id.action_all, R.id.action_open, R.id.action_ready, R.id.action_45min);
                        } else if (!fRegi.equals("")) {
                            for (int i = 0; i < mGliderSpin.getCount(); i++) {
                                String s = (String) mGliderSpin.getItemAtPosition(i);
                                if (s.equalsIgnoreCase(fRegi)) {
                                    // set filter criteria for selected member
                                    DoFlightFilter(6, GliderList.get(i));
                                    OptionSelect(R.id.action_my, R.id.action_all, R.id.action_open, R.id.action_ready, R.id.action_45min);
                                }
                            }
                        } else if (!fPilo.equals("")) {
                            for (int i = 0; i < mMemberSpin.getCount(); i++) {
                                String s = (String) mMemberSpin.getItemAtPosition(i);
                                if (s.equalsIgnoreCase(fPilo)) {
                                    // set filter criteria for selected member
                                    DoFlightFilter(5, MemberIndexList.get(i));
                                    OptionSelect(R.id.action_my, R.id.action_all, R.id.action_open, R.id.action_ready, R.id.action_45min);
                                }
                            }
                        }
                    }
                })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                DoFlightFilter(4, "");
                                OptionSelect(R.id.action_all, R.id.action_my, R.id.action_open, R.id.action_ready, R.id.action_45min);
                                dialog.cancel();
                            }
                        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        // get reference to and fill spinner with members
        mMemberSpin = (Spinner) membersView.findViewById(R.id.flight_member);
        ArrayAdapter<String> dataAdapter_1 = new ArrayAdapter<String>(
                FlightOverviewActivity.this,
                android.R.layout.simple_spinner_item, MemberList);
        dataAdapter_1
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mMemberSpin.setAdapter(dataAdapter_1);
        // get reference to and fill spinner with gliders
        mGliderSpin = (Spinner) membersView.findViewById(R.id.flight_glider);
        ArrayAdapter<String> dataAdapter_2 = new ArrayAdapter<String>(
                FlightOverviewActivity.this,
                android.R.layout.simple_spinner_item, GliderList);
        dataAdapter_2
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mGliderSpin.setAdapter(dataAdapter_2);
        mDetailInfo = (EditText) membersView.findViewById(R.id.editText1);
        // make field read only
        mDetailInfo.setClickable(false);
        mDetailInfo.setFocusable(false);
        mGlider_Btn = (Button) membersView.findViewById(R.id.flight_glider_detail);
        mGlider_Btn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                String fRegi = (String) mGliderSpin.getSelectedItem();
                if (fRegi.equals(null) || fRegi.equals("")) {
                    mDetailInfo.setText("Geen Registratie selectie gevonden,\nmaak een keuze !");
                } else {
                    String fRegiPilo = "";
                    for (int i = 0; i < mGliderSpin.getCount(); i++) {
                        String s = (String) mGliderSpin.getItemAtPosition(i);
                        if (s.equalsIgnoreCase(fRegi)) {
                            fRegiPilo = GliderList.get(i);
                        }
                    }
                    mDetailInfo.setText(getDetailInfo(FlightsContentProvider.CONTENT_URI_GLIDER, fRegiPilo, 0));
                }
            }
        });
        mMember_Btn = (Button) membersView.findViewById(R.id.flight_member_detail);
        mMember_Btn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                String fPilo = (String) mMemberSpin.getSelectedItem();
                if (fPilo.equals(null) || fPilo.equals("")) {
                    mDetailInfo.setText("Geen Naam selectie gevonden,\nmaak een keuze !");
                } else {
                    String fRegiPilo = "";
                    for (int i = 0; i < mMemberSpin.getCount(); i++) {
                        String s = (String) mMemberSpin.getItemAtPosition(i);
                        if (s.equalsIgnoreCase(fPilo)) {
                            fRegiPilo = MemberIndexList.get(i);
                        }
                    }
                    mDetailInfo.setText(getDetailInfo(FlightsContentProvider.CONTENT_URI_MEMBER, fRegiPilo, 1));
                }
            }
        });
        alertDialog.show();
    }

    public void DoFlightFilter(Integer filter, String arg1) {
        switch (filter) {
            case 1:
                // filter for ready flights
                btn_select = GliderLogTables.F_STARTED + " IS '' AND "
                        + GliderLogTables.F_LANDED + " IS '' AND "
                        + GliderLogTables.F_DEL + "=0";
                break;
            case 2:
                // filter for open flights
                btn_select = GliderLogTables.F_STARTED + " IS NOT '' AND "
                        + GliderLogTables.F_LANDED + " IS '' AND "
                        + GliderLogTables.F_DEL + "=0";
                break;
            case 3:
                // get current time - 45 minutes & format to HH:MM
                Locale locale = Locale.getDefault();//GERMANY;
                //TimeZone tz = TimeZone.getTimeZone("CET");
                TimeZone tz = TimeZone.getDefault();
                Calendar c = Calendar.getInstance(tz, locale);
                int HOUR = c.get(Calendar.HOUR_OF_DAY);
                int MINUTE = c.get(Calendar.MINUTE);
                int minutes = HOUR * 60 + MINUTE - Integer.parseInt(appPRE);
                HOUR = minutes / 60;
                MINUTE = modulo(minutes, 60);
                String chk = Common.TwoDigits(HOUR) + ":" + Common.TwoDigits(MINUTE);
                // filter for flight shortly overdue
                btn_select = GliderLogTables.F_STARTED + " IS NOT '' AND "
                        + GliderLogTables.F_STARTED + " < " + "'" + chk + "' AND "
                        + GliderLogTables.F_LANDED + " IS '' AND "
                        + GliderLogTables.F_DEL + "=0";
                break;
            case 4:
                // remove filter
                btn_select = "";
                break;
            case 5:
                btn_select = GliderLogTables.F_PILOT_ID + "=" + Integer.parseInt(arg1) + " OR "
                        + GliderLogTables.F_COPILOT_ID + "=" + Integer.parseInt(arg1);
                break;
            case 6:
                btn_select = GliderLogTables.F_REGISTRATION + " LIKE '" + arg1 + "'";
                break;
            case 7:
                String[] args = arg1.split(":");
                btn_select = "(" + GliderLogTables.F_PILOT_ID + "=" + Integer.parseInt(args[1]) + " OR "
                        + GliderLogTables.F_COPILOT_ID + "=" + Integer.parseInt(args[1]) + ") AND "
                        + GliderLogTables.F_REGISTRATION + " LIKE '" + args[0] + "'";
                break;
        }
        Log.d(TAG, "flt=" + filter + " selection - " + btn_select);
        getLoaderManager().restartLoader(0, null, this);
        fillData();
    }

    public int modulo(int m, int n) {
        int mod = m % n;
        return (mod < 0) ? mod + n : mod;
    }

    public void DoImport() {
        app_ini = false;
        if (isAuthenticated) {
            // logon to Ezac site and obtain session name & id
            new LoginTask().execute(host_url);
            new LDRTask().execute(host_url + "api/kisten/*.json", "1");
            new LDRTask().execute(host_url + "api/leden/*.json", "2");
            new LDRTask().execute(host_url + "api/starts/*.json", "3");
            //final Calendar c = Calendar.getInstance();
            new LDRTask().execute(host_url + "api/passagiers/*.json?datum=" + Common.FourDigits(c.get(Calendar.YEAR)), "4");
            //+ "-" + Common.TwoDigits(c.get(Calendar.MONTH)) // + Common.FourDigits(c.get(Calendar.YEAR))
            new LDRTask().execute(host_url + "api/reserveringen/*.json?datum=", "5");
            new LDRTask().execute(host_url + "api/rooster/*.json?datum=", "6");
        }
    }

    public void DoInfo() {
        try {
			/* First, get the Display from the WindowManager */
            Display display = ((WindowManager) getSystemService(WINDOW_SERVICE))
                    .getDefaultDisplay();
			/* Now we can retrieve all display-related infos */
            Point size = new Point();
            display.getSize(size);
            PackageManager manager = this.getPackageManager();
            PackageInfo info = manager.getPackageInfo(this.getPackageName(), 0);
            makeToast("PackageName = " + info.packageName
                    + "\nVersionName = " + info.versionName
                    + "\nAndroid version " + Build.VERSION.RELEASE
                    + "\nScreen size = " + size.x + "x" + size.y
                    + "\nDevelopment: (2013-2016)"
                    + "\n  Android  - P. van der Wielen (Ezac)"
                    + "\n  Web Site - E. Fekkes (Ezac)", 0);
            Log.d(TAG, "Name " + info.versionName + ", Code "
                    + info.versionCode);
        } catch (NameNotFoundException pkg) {
            Log.d(TAG, "unable to get app version info !" + pkg.getMessage());
        } finally {
            ;
        }
    }

    public boolean getSettings(Context con) {
        SharedPreferences prefs = con.getSharedPreferences("Share", Context.MODE_PRIVATE);
        // get url from settings
        appURL = prefs.getString("com.ezac.gliderlogs.url",
                "http://www.yoursite.nl/drupal");
        if (appURL.length() > 0) {
            // appURL = null; // handy way to force a crash report for 'acra' testing
            String s = appURL.substring(appURL.length() - 1);
            host_url = appURL;
            // add slash to it if not last char in string
            if (!s.equals("/")) {
                host_url = host_url + "/";
            }
        } else {
            host_url = "";
        }
        appPRE = prefs.getString("com.ezac.gliderlogs.pre", "45");
        appSCN = prefs.getString("com.ezac.gliderlogs.scn", "300");
        appKEY = prefs.getString("com.ezac.gliderlogs.key", "");
        appSCT = prefs.getString("com.ezac.gliderlogs.sct", "");
        appMST = prefs.getString("com.ezac.gliderlogs.mst", "6319");
        appMTR = prefs.getString("com.ezac.gliderlogs.mtr", "EHFS;EFWO");
        appFLG = prefs.getString("com.ezac.gliderlogs.flg", "false;false;true;true;true;true;false;false;false;false");
        appLND = prefs.getString("com.ezac.gliderlogs.lnd", "O   - W");
        // added as to deal with update issue's as appFLG variable grows larger
        String v[] = appFLG.split(";");
        // currently we need 10 flags
        if (v.length != 10) {
            prefs.edit().clear();
            SharedPreferences.Editor es = prefs.edit();
            es.putString("com.ezac.gliderlogs.flg", "false;false;true;true;true;true;false;false;false;false").apply();
            appFLG = prefs.getString("com.ezac.gliderlogs.flg", "false;false;true;true;true;true;false;false;false;false");
        }
        return true;
    }

    public int getTableGliderCnt(boolean Notify) {
        int res = 0;
        Uri uri = FlightsContentProvider.CONTENT_URI_GLIDER;
        String[] projection = {GliderLogTables.G_REGISTRATION};
        Cursor cur_gc = getContentResolver().query(uri, projection, null, null,
                null);
        if (cur_gc != null) {
            try {
                res = cur_gc.getCount();
            } finally {
                if (!cur_gc.isClosed()) {
                    cur_gc.close();
                }
            }
        }

        if ((Notify) && (res == 0)) {
            makeToast("Kisten tabel is nog niet geladen", 1);
        }
        return res;
    }

    public int getTableMemberCnt(boolean Notify) {
        int res = 0;
        Uri uri = FlightsContentProvider.CONTENT_URI_MEMBER;
        String[] projection = {GliderLogTables.M_ID};
        Cursor cur_mc = getContentResolver().query(uri, projection, null, null,
                null);
        if (cur_mc != null) {
            try {
                res = cur_mc.getCount();
            } finally {
                if (!cur_mc.isClosed()) {
                    cur_mc.close();
                }
            }
        }
        if ((Notify) && (res == 0)) {
            makeToast("Leden tabel is nog niet geladen", 1);
        }
        return res;
    }

    public int getTableFlightsCnt(String act_date, int mode) {
        // Mode = 0 => return # flights in table, Mode = 1 => remove old reported flights
        int res = 0;
        Cursor cur_fc = null;
        Uri uri = FlightsContentProvider.CONTENT_URI_FLIGHT;
        String[] projection = {GliderLogTables.F_ID, GliderLogTables.F_DATE};
        String selection = GliderLogTables.F_DATE + " < '" + act_date + "'";
        // add selection for delete operation
        if (mode > 0) {
            if (mode == 1) {
                selection += " AND ((" + GliderLogTables.F_SENT + " = 1 AND " + GliderLogTables.F_ACK + " = 1"
                        + " AND " + GliderLogTables.F_HID + " > 0) OR ("
                        + GliderLogTables.F_DEL + " = 1 AND " + GliderLogTables.F_HID + " = 0))";
            }
            res = getContentResolver().delete(uri, selection,
                    null);
        } else {
            cur_fc = getContentResolver().query(uri, projection, selection,
                    null, null);
            if (cur_fc != null) {
                try {
                    res = cur_fc.getCount();
                } finally {
                    if (!cur_fc.isClosed()) {
                        cur_fc.close();
                    }
                }
            }
        }
        return res;
    }

    public String getTableFlightProgress() {
        String tmp_a, tmp_b;
        String selection;
        Cursor cur_fp;
        String sort;
        Uri uri = FlightsContentProvider.CONTENT_URI_FLIGHT;
        String[] projection = {GliderLogTables.F_ID};
        selection = "("
                + GliderLogTables.F_STARTED
                + " IS NOT '' )";
        sort = GliderLogTables.F_SENT + " ASC, " + GliderLogTables.F_STARTED + " DESC";
        cur_fp = getContentResolver().query(uri, projection, selection, null,
                sort);
        if (cur_fp != null) {
            try {
                tmp_a = String.valueOf(cur_fp.getCount());
            } finally {
                if (!cur_fp.isClosed()) {
                    cur_fp.close();
                }
            }
        } else {
            tmp_a = "0";
        }

        selection = "("
                + GliderLogTables.F_STARTED
                + " IS NOT '' AND " + GliderLogTables.F_HID + " > 0)";
        sort = GliderLogTables.F_SENT + " ASC, " + GliderLogTables.F_STARTED + " DESC";
        cur_fp = getContentResolver().query(uri, projection, selection, null,
                sort);
        if (cur_fp != null) {
            try {
                tmp_b = String.valueOf(cur_fp.getCount());
            } finally {
                if (!cur_fp.isClosed()) {
                    cur_fp.close();
                }
            }
        } else {
            tmp_b = "0";
        }
        return tmp_a + "/" + tmp_b;
    }

    public void checktable(boolean AutoInit) {
        if (AutoInit) {
            DoImport();
        }
        if ((getTableGliderCnt(true) == 0) | (getTableMemberCnt(true) == 0)) {
            makeToast("Opties -> Voer over 2 minuten de Actie 'Dag opstarten' opnieuw uit, er was mogelijk nu een netwerk probleem !.", 1);
        } else {
            isInit = true;
        }
    }

    @SuppressLint("SimpleDateFormat")
    public void GliderLogToDB(String DBPath, String DB, String device) {
        // format date
        SimpleDateFormat TSD = new SimpleDateFormat("yyyyMMdd_kkss");
        SimpleDateFormat DIR = new SimpleDateFormat("yyyy/MM_dd");
        Date myDate = new Date();
        String backupDBPath = device + "_" + TSD.format(myDate);
        String TS_DIR = DIR.format(myDate);
        // to internal sdcard
        File dir = new File(Environment.getExternalStorageDirectory() + "/Download/" + TS_DIR);
        if (!dir.exists() || !dir.isDirectory()) {
            dir.mkdir();
        }
        File data = Environment.getDataDirectory();
        // create a file channel object
        FileChannel src = null;
        FileChannel des = null;
        File currentDB = new File(data + "/data/" + DBPath + "/databases/", DB);
        File backupDB = new File(dir, backupDBPath);
        try {
            backupDB.delete();
            src = new FileInputStream(currentDB).getChannel();
            des = new FileOutputStream(backupDB).getChannel();
            des.transferFrom(src, 0, src.size());
            src.close();
            des.close();
        } catch (IOException e) {
            Log.d(TAG, e.toString());
            e.printStackTrace();
        }
    }

    @SuppressLint("SimpleDateFormat")
    public void GliderLogToCSV(String DB, String device_id) {
        // format date's
        SimpleDateFormat CSV = new SimpleDateFormat("yyyyMMdd_kkss");
        SimpleDateFormat DIR = new SimpleDateFormat("yyyy/MM_dd");
        Date myDate = new Date();
        String TS_DB = CSV.format(myDate);
        String TS_DIR = DIR.format(myDate);
        // to internal sdcard
        File dir = new File(Environment.getExternalStorageDirectory() + "/Download/" + TS_DIR);
        if (!dir.exists() || !dir.isDirectory()) {
            dir.mkdir();
        }
        File myFile = new File(Environment.getExternalStorageDirectory()
                + "/Download/" + TS_DIR + "/" + device_id.toUpperCase(Locale.US) + "_" + TS_DB + ".csv");

        try {
            myFile.createNewFile();
            FileOutputStream fOut = new FileOutputStream(myFile);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
            myOutWriter
                    .append("datum;start;landing;duur;soort;registratie;piloot;piloot_id;tweede;tweede_id;instructie;opmerking;methode");
            myOutWriter.append("\n");
            Uri uri = FlightsContentProvider.CONTENT_URI_FLIGHT;
            String[] projection = {
                    GliderLogTables.F_DATE, GliderLogTables.F_STARTED,
                    GliderLogTables.F_LANDED, GliderLogTables.F_DURATION,
                    GliderLogTables.F_TYPE, GliderLogTables.F_REGISTRATION,
                    GliderLogTables.F_PILOT, GliderLogTables.F_PILOT_ID,
                    GliderLogTables.F_COPILOT, GliderLogTables.F_COPILOT_ID,
                    GliderLogTables.F_INSTRUCTION, GliderLogTables.F_NOTES,
                    GliderLogTables.F_LAUNCH};
            Cursor cur_go = getContentResolver().query(uri, projection, null,
                    null, null);
            if (cur_go != null) {
                //Log.d(TAG,"cnt " + cursor.getCount());
                try {
                    if ((cur_go.getCount()) > 0) {
                        cur_go.moveToFirst();
                        do {
                            myOutWriter
                                    .append(cur_go.getString(cur_go
                                            .getColumnIndexOrThrow(GliderLogTables.F_DATE))
                                            + ";"
                                            + cur_go.getString(cur_go
                                            .getColumnIndexOrThrow(GliderLogTables.F_STARTED))
                                            + ";"
                                            + cur_go.getString(cur_go
                                            .getColumnIndexOrThrow(GliderLogTables.F_LANDED))
                                            + ";"
                                            + cur_go.getString(cur_go
                                            .getColumnIndexOrThrow(GliderLogTables.F_DURATION))
                                            + ";"
                                            + cur_go.getString(cur_go
                                            .getColumnIndexOrThrow(GliderLogTables.F_TYPE))
                                            + ";"
                                            + cur_go.getString(cur_go
                                            .getColumnIndexOrThrow(GliderLogTables.F_REGISTRATION))
                                            + ";"
                                            + cur_go.getString(cur_go
                                            .getColumnIndexOrThrow(GliderLogTables.F_PILOT))
                                            + ";"
                                            + cur_go.getString(cur_go
                                            .getColumnIndexOrThrow(GliderLogTables.F_PILOT_ID))
                                            + ";"
                                            + cur_go.getString(cur_go
                                            .getColumnIndexOrThrow(GliderLogTables.F_COPILOT))
                                            + ";"
                                            + cur_go.getString(cur_go
                                            .getColumnIndexOrThrow(GliderLogTables.F_COPILOT_ID))
                                            + ";"
                                            + cur_go.getString(cur_go
                                            .getColumnIndexOrThrow(GliderLogTables.F_INSTRUCTION))
                                            + ";"
                                            + cur_go.getString(cur_go
                                            .getColumnIndexOrThrow(GliderLogTables.F_NOTES))
                                            + ";"
                                            + cur_go.getString(cur_go
                                            .getColumnIndexOrThrow(GliderLogTables.F_LAUNCH)));
                            myOutWriter.append("\n");
                            //	Log.d(TAG,"gld " + cursor
                            //			.getColumnIndexOrThrow(GliderLogTables.F_REGISTRATION));
                        } while (cur_go.moveToNext());
                    }
                } finally {
                    if (!cur_go.isClosed()) {
                        cur_go.close();
                    }
                }
            }
            myOutWriter.close();
            fOut.close();
        } catch (IOException ioe) {
            Log.e(TAG,
                    "Could not open/write the csv file, error: "
                            + ioe.getMessage());
        } catch (SQLiteException e) {
            Log.e(TAG, "SQLiteException:" + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Could not open/read the DB, error: " + e.getMessage());
        }

    }

    public boolean DoStatusCheck() {
        String sts_sel;
        Cursor cur_sc;
        String sort;
        Uri uri = FlightsContentProvider.CONTENT_URI_FLIGHT;
        String[] projection = {
                GliderLogTables.F_STARTED, GliderLogTables.F_LANDED,
                GliderLogTables.F_DEL, GliderLogTables.F_HID};
        sts_sel = "(" + GliderLogTables.F_STARTED + " IS '') AND ("
                + GliderLogTables.F_SENT + "=0) AND (" + GliderLogTables.F_HID + ">0)";
        sort = GliderLogTables.F_SENT + " ASC, " + GliderLogTables.F_STARTED + " DESC";
        cur_sc = getContentResolver().query(uri, projection, sts_sel, null,
                sort);
        if (cur_sc != null) {
            try {
                intSum[0] = cur_sc.getCount();
            } finally {
                if (!cur_sc.isClosed()) {
                    cur_sc.close();
                }
            }
        }
        sts_sel = "(" + GliderLogTables.F_LANDED + " IS '') AND ("
                + GliderLogTables.F_SENT + "=0) AND (" + GliderLogTables.F_HID + ">0)";
        sort = GliderLogTables.F_SENT + " ASC, " + GliderLogTables.F_STARTED + " DESC";
        cur_sc = getContentResolver().query(uri, projection, sts_sel, null,
                sort);
        if (cur_sc != null) {
            try {
                intSum[1] = cur_sc.getCount();
            } finally {
                if (!cur_sc.isClosed()) {
                    cur_sc.close();
                }
            }
        }
        sts_sel = "(" + GliderLogTables.F_DEL + "=1) AND ("
                + GliderLogTables.F_SENT + "=0) AND (" + GliderLogTables.F_HID + ">0)";
        sort = GliderLogTables.F_SENT + " ASC, " + GliderLogTables.F_STARTED + " DESC";
        cur_sc = getContentResolver().query(uri, projection, sts_sel, null,
                sort);
        if (cur_sc != null) {
            try {
                intSum[2] = cur_sc.getCount();
            } finally {
                if (!cur_sc.isClosed()) {
                    cur_sc.close();
                }
            }
        }
        // if any of these check result in value not zero we have issue's
        return ((intSum[0] + intSum[1] + intSum[2]) == 0) ? true
                : false;
    }

    public int ToMinute(String Duration) {
        String s[] = Duration.toString().split(":");
        return (Integer.parseInt(s[0]) * 60) + Integer.parseInt(s[1]);
    }

    public boolean DoAction(int MyAct, String MyMsg) {
        final int Action = MyAct;
        final String Message = MyMsg;
        new AlertDialog.Builder(FlightOverviewActivity.this)
                .setTitle("Bevestig deze opdracht")
                .setMessage(Message)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        switch (Action) {
                            case 1:
                                if (DoStatusCheck()) {
                                    // first make backup of existing data
                                    GliderLogToCSV("gliderlogs.db", "ezac");
                                    sendBroadcast(new Intent(
                                            Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                                            Uri.parse("file://"
                                                    + Environment
                                                    .getExternalStorageDirectory())));
                                    // first remove records from flights table
                                    DoDrop();
                                    // then do import of support table (glider &
                                    // members) and any starts of this day
                                    DoImport();
                                } else {
                                    if ((intSum[0] + intSum[1]) != 0) {
                                        makeToast("Er zijn nog "
                                                + intSum[0]
                                                + " vluchten niet gestart en \n"
                                                + intSum[1]
                                                + " vluchten niet geland, deze moeten eerst opgelost worden.", 1);
                                    }
                                    if ((intSum[2] + intSum[3]) != 0) {
                                        makeToast("Er zijn nog "
                                                + intSum[2]
                                                + " vluchten niet verwijderd en \n"
                                                + intSum[3]
                                                + " vluchten moeten nog verwerkt worden\n probeer het later opnieuw, na 5 min.", 1);
                                    }
                                }
                                break;
                            case 2:
                                if (DoStatusCheck()) {
                                    createDayReport();
                                    GliderLogToCSV("gliderlogs.db", "ezac");
                                    sendBroadcast(new Intent(
                                            Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                                            Uri.parse("file://"
                                                    + Environment
                                                    .getExternalStorageDirectory())));
                                } else {
                                    if ((intSum[0] + intSum[1]) != 0) {
                                        makeToast("Er zijn nog "
                                                + intSum[0]
                                                + " vluchten niet gestart en \n"
                                                + intSum[1]
                                                + " vluchten niet geland, deze moeten eerst opgelost worden.", 1);
                                    }
                                    if ((intSum[2] + intSum[3]) != 0) {
                                        makeToast("Er zijn nog "
                                                + intSum[2]
                                                + " vluchten niet verwijderd en \n"
                                                + intSum[3]
                                                + " vluchten moeten nog verwerkt worden\n probeer het later opnieuw, na 5 min.", 1);
                                    }
                                }
                                break;
                            case 3:
                                if (isAuthenticated) {
                                    //final Calendar c = Calendar.getInstance();
                                    new LDRTask().execute(host_url + "api/passagiers/*.json?datum=" + Common.FourDigits(c.get(Calendar.YEAR)), "4");
                                    //+ "-" + Common.TwoDigits(c.get(Calendar.MONTH))
                                }
                                break;
                            case 4:
                                break;
                            default:
                                break;
                        }
                    }
                })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int whichButton) {
                                // Do nothing.
                            }
                        }).show();
        return true;
    }

    public void DoDrop() {
        String[] se = null;
        getContentResolver().delete(FlightsContentProvider.CONTENT_URI_FLIGHT,
                "", se);
        se = null;
        fillData();
    }

    public void DoSettings() {
        // get settings.xml view
        LayoutInflater li = LayoutInflater.from(context);
        settingsView = li.inflate(R.layout.settings, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        // set settings.xml to alertdialog builder
        alertDialogBuilder.setView(settingsView);
        // get user input for service code
        final EditText userInput = (EditText) settingsView
                .findViewById(R.id.editTextDialogUserInput);
        // set dialog message
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        SharedPreferences prefs = context.getSharedPreferences(
                                "Share", Context.MODE_PRIVATE);
                        prefs.edit().clear();
                        SharedPreferences.Editor es = prefs.edit();
                        String[] mTestArray;
                        if (userInput.getText().toString().equals("NoAccess")) {
                            // prefs URL
                            EditText et1 = (EditText) settingsView.findViewById(R.id.setting_url);
                            appURL = et1.getText().toString();
                            es.putString("com.ezac.gliderlogs.url", appURL).apply();
                        }
                        // prefs PRE
                        EditText et2 = (EditText) settingsView.findViewById(R.id.setting_pre);
                        appPRE = et2.getText().toString().replace(" ", "");
                        es.putString("com.ezac.gliderlogs.pre", appPRE).apply();
                        if (userInput.getText().toString().equals("NoAccess")) {
                            // prefs SCN
                            EditText et3 = (EditText) settingsView.findViewById(R.id.setting_scn);
                            appSCN = et3.getText().toString();
                            es.putString("com.ezac.gliderlogs.scn", appSCN).apply();
                            // prefs KEY
                            EditText et4 = (EditText) settingsView.findViewById(R.id.setting_key);
                            appKEY = et4.getText().toString();
                            es.putString("com.ezac.gliderlogs.key", appKEY).apply();
                            // prefs KEY
                            EditText et5 = (EditText) settingsView.findViewById(R.id.setting_secret);
                            appSCT = et5.getText().toString();
                            es.putString("com.ezac.gliderlogs.sct", appSCT).apply();
                            // prefs Meteo

                            mTestArray = getResources().getStringArray(R.array.meteo_id_arrays);
                            Spinner et6 = (Spinner) settingsView.findViewById(R.id.setting_station);
                            String sel6 = (String) et6.getSelectedItem();
                            //String sel6_id = "";
                            for (int i = 0; i < et6.getCount(); i++) {
                                String s1 = (String) et6.getItemAtPosition(i);
                                if (s1.equalsIgnoreCase(sel6)) {
                                    appMST = mTestArray[i];
                                }
                            }
                            es.putString("com.ezac.gliderlogs.mst", appMST).apply();
                            // prefs Metar
                            EditText et7 = (EditText) settingsView.findViewById(R.id.setting_metar);
                            appMTR = et7.getText().toString();
                            es.putString("com.ezac.gliderlogs.mst", appMST).apply();
                        }
                        // prefs Flags
                        CheckBox et9a = (CheckBox) settingsView.findViewById(R.id.setting_menu01);
                        CheckBox et9b = (CheckBox) settingsView.findViewById(R.id.setting_menu02);

                        CheckBox et9c = (CheckBox) settingsView.findViewById(R.id.setting_menu11);
                        CheckBox et9d = (CheckBox) settingsView.findViewById(R.id.setting_menu12);
                        CheckBox et9e = (CheckBox) settingsView.findViewById(R.id.setting_menu13);
                        CheckBox et9f = (CheckBox) settingsView.findViewById(R.id.setting_menu14);
                        CheckBox et9g = (CheckBox) settingsView.findViewById(R.id.setting_menu21);
                        CheckBox et9h = (CheckBox) settingsView.findViewById(R.id.setting_menu22);
                        String et9aa, et9ab;
                        String v[];
                        if (userInput.getText().toString().equals("To3Myd4T")) {
                            et9aa = et9a.isChecked() ? "true" : "false";
                            et9ab = et9b.isChecked() ? "true" : "false";
                        } else {
                            v = appFLG.split(";");
                            et9aa = v[0];
                            et9ab = v[1];
                        }
                        String et9ac = et9c.isChecked() ? "true" : "false";
                        String et9ad = et9d.isChecked() ? "true" : "false";
                        String et9ae = et9e.isChecked() ? "true" : "false";
                        String et9af = et9f.isChecked() ? "true" : "false";
                        String et9ag = et9g.isChecked() ? "true" : "false";
                        String et9ah = et9h.isChecked() ? "true" : "false";
                        appFLG = et9aa + ";" + et9ab + ";" + et9ac + ";" + et9ad + ";" + et9ae + ";" + et9af
                                + ";" + et9ag + ";" + et9ah + ";false;false";
                        v = appFLG.split(";");

                        menu.findItem(R.id.action_ezac).setVisible(Boolean.parseBoolean(v[2]));
                        menu.findItem(R.id.action_meteo_group).setVisible(Boolean.parseBoolean(v[3]));
                        menu.findItem(R.id.action_ntm_nld).setVisible(Boolean.parseBoolean(v[4]));
                        menu.findItem(R.id.action_ntm_blx).setVisible(Boolean.parseBoolean(v[4]));
                        menu.findItem(R.id.action_ogn_flarm).setVisible(Boolean.parseBoolean(v[5]));
                        menu.findItem(R.id.action_adsb).setVisible(Boolean.parseBoolean(v[6]));
                        menu.findItem(R.id.action_adsb_lcl).setVisible(Boolean.parseBoolean(v[7]));
                        es.putString("com.ezac.gliderlogs.flg", appFLG).apply();
                        // adjust value in menu button to value in use
                        MenuItem MenuItem_dur = menu
                                .findItem(R.id.action_45min);
                        MenuItem_dur.setTitle(" " + appPRE + " Min ");
                        if (userInput.getText().toString().equals("To3Myd4T")) {
                            // prefs airfield heading
                            mTestArray = getResources().getStringArray(R.array.heading_arrays);
                            Spinner et10 = (Spinner) settingsView.findViewById(R.id.setting_heading);
                            String sel10 = (String) et10.getSelectedItem();
                            //String sel10_id = "";
                            for (int i = 0; i < et10.getCount(); i++) {
                                String s2 = (String) et10.getItemAtPosition(i);
                                if (s2.equalsIgnoreCase(sel10)) {
                                    appLND = mTestArray[i];
                                    es.putString("com.ezac.gliderlogs.lnd", appLND).apply();
                                }
                            }
                        }
                    }
                })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
        // create alert dialog & and load it's data
        AlertDialog alertDialog = alertDialogBuilder.create();
        EditText et1 = (EditText) settingsView.findViewById(R.id.setting_url);
        EditText et2 = (EditText) settingsView.findViewById(R.id.setting_pre);
        EditText et3 = (EditText) settingsView.findViewById(R.id.setting_scn);
        EditText et4 = (EditText) settingsView.findViewById(R.id.setting_key);
        EditText et5 = (EditText) settingsView.findViewById(R.id.setting_secret);
        Spinner et6 = (Spinner) settingsView.findViewById(R.id.setting_station);
        EditText et7 = (EditText) settingsView.findViewById(R.id.setting_metar);
        //EditText et8 = (EditText) settingsView.findViewById(R.id.setting_ntp);
        CheckBox et9a = (CheckBox) settingsView.findViewById(R.id.setting_menu01);
        CheckBox et9b = (CheckBox) settingsView.findViewById(R.id.setting_menu02);
        CheckBox et9c = (CheckBox) settingsView.findViewById(R.id.setting_menu11);
        CheckBox et9d = (CheckBox) settingsView.findViewById(R.id.setting_menu12);
        CheckBox et9e = (CheckBox) settingsView.findViewById(R.id.setting_menu13);
        CheckBox et9f = (CheckBox) settingsView.findViewById(R.id.setting_menu14);
        CheckBox et9g = (CheckBox) settingsView.findViewById(R.id.setting_menu21);
        CheckBox et9h = (CheckBox) settingsView.findViewById(R.id.setting_menu22);
        Spinner et10 = (Spinner) settingsView.findViewById(R.id.setting_heading);
        et1.setText(appURL);
        et2.setText(appPRE);
        et3.setText(appSCN);
        et4.setText(appKEY);
        et5.setText(appSCT);
        // get settings value for meteo station and set spinner
        String[] mTestArray;
        mTestArray = getResources().getStringArray(R.array.meteo_id_arrays);
        for (int i = 0; i < mTestArray.length; i++) {
            String s = mTestArray[i];
            if (s.equals(appMST)) {
                et6.setSelection(i);
            }
        }
        et7.setText(appMTR);
        // get settings value for menu tabs and set checkboxes
        String v[] = appFLG.split(";");
        et9a.setChecked(Boolean.parseBoolean(v[0]));
        et9b.setChecked(Boolean.parseBoolean(v[1]));
        et9c.setChecked(Boolean.parseBoolean(v[2]));
        et9d.setChecked(Boolean.parseBoolean(v[3]));
        et9e.setChecked(Boolean.parseBoolean(v[4]));
        et9f.setChecked(Boolean.parseBoolean(v[5]));
        et9g.setChecked(Boolean.parseBoolean(v[6]));
        et9h.setChecked(Boolean.parseBoolean(v[7]));
        // re-use mTestArray
        mTestArray = getResources().getStringArray(R.array.heading_arrays);
        for (int i = 0; i < mTestArray.length; i++) {
            String s = mTestArray[i];
            if (s.equals(appLND)) {
                et10.setSelection(i);
            }
        }
        // show it
        alertDialog.show();
    }

    public void DoServices() {
        // get services.xml view
        LayoutInflater li = LayoutInflater.from(context);
        servicesView = li.inflate(R.layout.services, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                context);

        // set prompts.xml to alertdialog builder
        alertDialogBuilder.setView(servicesView);

        final EditText userInput = (EditText) servicesView
                .findViewById(R.id.editTextDialogUserInput);

        // set dialog message
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                // get user input and set it to result
                                if (userInput.getText().toString().equals("To3Myd4T")) {
                                    Log.d(TAG, "ok, user request to delete all, do it");
                                    CheckBox csv_ok = (CheckBox) servicesView.findViewById(R.id.service_csv);
                                    CheckBox db_ok = (CheckBox) servicesView.findViewById(R.id.service_db);
                                    // make a copy to csv file
                                    if (csv_ok.isChecked()) {
                                        GliderLogToCSV("gliderlogs.db", "ezac");
                                        makeToast("Export naar een CSV bestand is uitgevoerd !", 2);
                                    }
                                    // make a copy to sqlite database file
                                    if (db_ok.isChecked()) {
                                        GliderLogToDB("com.ezac.gliderlogs", "gliderlogs.db", "ezac");
                                        makeToast("Export naar een DB bestand is uitgevoerd !", 2);
                                    }
                                    if (csv_ok.isChecked() || db_ok.isChecked()) {
                                        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                                                Uri.parse("file://" + Environment.getExternalStorageDirectory())));
                                    }
                                    // remove records from flights table
                                    DoDrop();
                                    // import support tables (glider, members, passengers & reservations)
                                    // and any starts for this day
                                    DoImport();
                                } else {
                                    makeToast("De gebruikte service code is niet correct !", 0);
                                    Log.d(TAG, "Fail, user service code error >" + userInput.getText().toString() + "<");
                                }
                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();
        // show it
        alertDialog.show();
    }

    class NETTask extends AsyncTask<String, String, String> {

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected String doInBackground(String... params) {

            String Ret_Sts = "0::NOP";
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);

            if (params[1].equals("1")) {
                // NTP works ok but also needs separate thread
                if (client.requestTime(params[0], 0)) {
                    long now = client.getNtpTime()
                            + SystemClock.elapsedRealtime()
                            - client.getNtpTimeReference();
                    Date current = new Date(now);

                    //Log.i("NTP tag", "frm_1 " + current);
                    Ret_Sts = "0" + "::" + current;
                } else {
                    Ret_Sts = "1" + "::" + "failed";
                }
            }

            if (params[1].equals("2")) {
                Uri uri = FlightsContentProvider.CONTENT_URI_FLIGHT;
                String[] projection = {GliderLogTables.F_ID,
                        GliderLogTables.F_DATE, GliderLogTables.F_STARTED,
                        GliderLogTables.F_LANDED, GliderLogTables.F_DURATION,
                        GliderLogTables.F_TYPE, GliderLogTables.F_REGISTRATION,
                        GliderLogTables.F_PILOT, GliderLogTables.F_PILOT_ID,
                        GliderLogTables.F_COPILOT,
                        GliderLogTables.F_COPILOT_ID,
                        GliderLogTables.F_INSTRUCTION, GliderLogTables.F_NOTES,
                        GliderLogTables.F_LAUNCH, GliderLogTables.F_DEL,
                        GliderLogTables.F_HID};
                String selection = "("
                        + GliderLogTables.F_STARTED
                        + " IS NOT '' "
                        + "AND " // + GliderLogTables.F_LANDED +
                        // " IS NOT '' AND "
                        + GliderLogTables.F_SENT + " = 0 AND "
                        + GliderLogTables.F_ACK + " = 0) AND ("
                        + GliderLogTables.F_HID + " = 0 OR "
                        + GliderLogTables.F_HID + " > 0)";
                String sort = GliderLogTables.F_SENT + " ASC, " + GliderLogTables.F_STARTED + " DESC";
                Cursor cur_ug = getContentResolver().query(uri, projection,
                        selection, null, sort);
                int req_type = 0;
                if (cur_ug != null) {
                    try {
                        if ((cur_ug.getCount()) > 0) {
                            cur_ug.moveToFirst();
                            do {
                                String URL_Str = "";
                                try {
                                    rec_id = ""
                                            + cur_ug.getInt(cur_ug
                                            .getColumnIndexOrThrow(GliderLogTables.F_ID));
                                    String[] tmp_date = cur_ug
                                            .getString(
                                                    cur_ug.getColumnIndexOrThrow(GliderLogTables.F_DATE))
                                            .split("-");
                                    String chk_instruc = cur_ug
                                            .getString(cur_ug
                                                    .getColumnIndexOrThrow(GliderLogTables.F_INSTRUCTION));
                                    String chk_pilot_id = cur_ug
                                            .getString(cur_ug
                                                    .getColumnIndexOrThrow(GliderLogTables.F_PILOT_ID));
                                    String chk_copilot_id = cur_ug
                                            .getString(cur_ug
                                                    .getColumnIndexOrThrow(GliderLogTables.F_COPILOT_ID));
                                    // string based
                                    URL_Str = "datum=" + tmp_date[2] + "-"
                                            + tmp_date[1] + "-" + tmp_date[0];
                                    URL_Str = URL_Str
                                            + "&start="
                                            + cur_ug.getString(cur_ug
                                            .getColumnIndexOrThrow(GliderLogTables.F_STARTED));
                                    if (!cur_ug
                                            .getString(
                                                    cur_ug.getColumnIndexOrThrow(GliderLogTables.F_LANDED))
                                            .equals("")) {
                                        URL_Str = URL_Str
                                                + "&landing="
                                                + cur_ug.getString(cur_ug
                                                .getColumnIndexOrThrow(GliderLogTables.F_LANDED));
                                    }
                                    URL_Str = URL_Str
                                            + "&duur="
                                            + cur_ug.getString(cur_ug
                                            .getColumnIndexOrThrow(GliderLogTables.F_DURATION));
                                    URL_Str = URL_Str
                                            + "&soort="
                                            + cur_ug.getString(cur_ug
                                            .getColumnIndexOrThrow(GliderLogTables.F_TYPE));
                                    URL_Str = URL_Str
                                            + "&registratie="
                                            + cur_ug.getString(cur_ug
                                            .getColumnIndexOrThrow(GliderLogTables.F_REGISTRATION));
                                    if (Integer.parseInt(chk_pilot_id) >= 20000) {
                                        URL_Str = URL_Str
                                                + "&gezagvoerder="
                                                + cur_ug.getString(cur_ug
                                                .getColumnIndexOrThrow(GliderLogTables.F_PILOT));
                                    } else {
                                        URL_Str = URL_Str
                                                + "&gezagvoerder="
                                                + cur_ug.getString(cur_ug
                                                .getColumnIndexOrThrow(GliderLogTables.F_PILOT_ID));
                                    }
                                    if ((chk_copilot_id != null)
                                            && !(chk_copilot_id.equals(""))) {
                                        if (Integer.parseInt(chk_copilot_id) >= 20000) {
                                            URL_Str = URL_Str
                                                    + "&tweede="
                                                    + cur_ug.getString(cur_ug
                                                    .getColumnIndexOrThrow(GliderLogTables.F_COPILOT));
                                        } else {
                                            URL_Str = URL_Str
                                                    + "&tweede="
                                                    + cur_ug.getString(cur_ug
                                                    .getColumnIndexOrThrow(GliderLogTables.F_COPILOT_ID));
                                        }
                                    } else {
                                        URL_Str = URL_Str + "&tweede=";
                                    }
                                    if (chk_instruc.equals("J")) {
                                        URL_Str = URL_Str + "&instructie=1";
                                    } else {
                                        URL_Str = URL_Str + "&instructie=0";
                                    }
                                    URL_Str = URL_Str
                                            + "&opmerking="
                                            + cur_ug.getString(
                                            cur_ug.getColumnIndexOrThrow(GliderLogTables.F_NOTES))
                                            .replace(" ", "%20");
                                    URL_Str = URL_Str
                                            + "&startmethode="
                                            + cur_ug.getString(cur_ug
                                            .getColumnIndexOrThrow(GliderLogTables.F_LAUNCH));
                                    if (Integer
                                            .parseInt(cur_ug.getString(cur_ug
                                                    .getColumnIndexOrThrow(GliderLogTables.F_HID))) == 0) {
                                        req_type = 1;
                                        URL_Str = "?" + URL_Str;
                                    }
                                    if (Integer
                                            .parseInt(cur_ug.getString(cur_ug
                                                    .getColumnIndexOrThrow(GliderLogTables.F_HID))) > 0) {
                                        req_type = 2;
                                        URL_Str = "/"
                                                + cur_ug.getString(cur_ug
                                                .getColumnIndexOrThrow(GliderLogTables.F_HID))
                                                + "?" + URL_Str;
                                    }
                                    if (Integer
                                            .parseInt(cur_ug.getString(cur_ug
                                                    .getColumnIndexOrThrow(GliderLogTables.F_DEL))) == 1) {
                                        req_type = 3;
                                        URL_Str = "/"
                                                + cur_ug.getString(cur_ug
                                                .getColumnIndexOrThrow(GliderLogTables.F_HID))
                                                + "?" + "datum=" + tmp_date[2]
                                                + "-" + tmp_date[1] + "-"
                                                + tmp_date[0];
                                    }
                                } catch (NumberFormatException e) {
                                    e.printStackTrace();
                                    Ret_Sts = e.toString();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    Ret_Sts = e.toString();
                                }

                                if (req_type == 0) {
                                    Log.d(TAG, "type " + req_type + " req data "
                                            + URL_Str);
                                } else {
                                    URL_Str = URL_Str.replace(" ", "%20");
                                    String spar = session_name + ";" + session_id + ";" + session_token;
                                    String resp = NetworkUtility.SentToServer(
                                            params[0], srvr, req_type, URL_Str, spar);
                                    String[] http_msg = resp.split("::");
                                /*
								 * Ret_Sts = params[1] + "::" + resp;  
								 * the above caused a wrong response string to be returned!!
								 */
                                    Log.d(TAG, "task result " + resp + "_" + appKEY + "_" + appSCT);
                                    switch (Integer.parseInt(http_msg[0])) {
                                        case 0:
                                            String select = GliderLogTables.F_ID + "=?";
                                            String selArgs[] = {rec_id};
                                            // made change here as to differentiate between case 1 & 2
                                            // which caused the remote record id to be lost.
                                            String r_mde = "";
                                            switch (req_type) {
                                                case 1:
                                                    ContentValues value1 = new ContentValues();
                                                    value1.put(GliderLogTables.F_SENT, "1");
                                                    value1.put(GliderLogTables.F_ACK, "1");
                                                    value1.put(GliderLogTables.F_HID,
                                                            http_msg[1].toString());
                                                    getContentResolver().update(uri,
                                                            value1, select, selArgs);
                                                    r_mde = "\nnew_id= " + http_msg[1].toString();
                                                    break;
                                                case 2:
                                                    ContentValues value2 = new ContentValues();
                                                    value2.put(GliderLogTables.F_SENT, "1");
                                                    value2.put(GliderLogTables.F_ACK, "1");
                                                    getContentResolver().update(uri,
                                                            value2, select, selArgs);
                                                    r_mde = "\nupd_id= " + http_msg[1].toString();
                                                    break;
                                                case 3:
                                                    getContentResolver().delete(uri,
                                                            select, selArgs);
                                                    r_mde = "\ndel_id= " + http_msg[1].toString();
                                                    break;
                                            }
                                            Logs.appendLog("gliderlog.txt", r_mde + URL_Str + "\r\n");
                                        case 1:
                                            if (req_type == 1) {
                                                Log.d(TAG, "Response " + http_msg[1]);
                                                //Ret_Sts = params[1] + "::" + resp;//http_msg[1];
                                            }
                                            Logs.appendLog("gliderlog.txt", "\nresp=" + params[1] + "::" + resp + "\r\n");
                                            break;
                                        case 2:
                                            Log.d(TAG, "Exception msg " + http_msg[1]);
                                            Logs.appendLog("gliderlog.txt", "\nexcep=" + params[1] + "::" + resp + "\r\n");
                                            //Ret_Sts = params[1] + "::" + resp;//http_msg[1];
                                            break;
                                    }
                                }
                                URL_Str = null;
                            } while (cur_ug.moveToNext());
                        } else {
                            // Log.d(TAG, "Scan, No action needed");
                            // leave here for potential removal of test records
						/*
						 * String resp = NetworkUtility.SentToServer(params[0],
						 * srvr, 3, "/85981?datum=2014-01-04");
						 */
                        }
                    } finally {
                        if (!cur_ug.isClosed()) {
                            cur_ug.close();
                        }
                    }
                }
            }
            if (params[1].equals("3")) {
                Ret_Sts = NetworkUtility.URLReachable(params[0], context);
            }
            return params[1] + "::" + Ret_Sts;
        }

        @SuppressLint("SimpleDateFormat")
        @Override
        protected void onPostExecute(String result) {
            Log.d(TAG, "NETTask - " + result);

            if (result.indexOf(":") > 0) {
                String[] http_msg = result.split("::");
                //Log.d(TAG, "NETTask -" + Integer.parseInt(http_msg[0]) + "-" + Integer.parseInt(http_msg[1]) + "-" + http_msg[2]);
                switch (Integer.parseInt(http_msg[0])) {
                    case 1:
                        switch (Integer.parseInt(http_msg[1])) {
                            case 0:
                                //Log.d(TAG, http_msg[2]);
                                String s[] = http_msg[2].toString().split(" ");
                                String t[] = s[3].toString().split(":");
                                // format NTP time
                                String NTPDay = Common.TwoDigits(Integer.parseInt(s[2])) + "-" +
                                        Common.TwoDigits(Arrays.asList(month_list).indexOf(s[1]) + 1) + "-" +
                                        Common.FourDigits(Integer.parseInt(s[5])) + " " +
                                        Common.TwoDigits(Integer.parseInt(t[0])) + ":" +
                                        Common.TwoDigits(Integer.parseInt(t[1])) + ":" +
                                        Common.TwoDigits(Integer.parseInt(t[2]));
                                // get actual local time
                                final Calendar c = Calendar.getInstance();
                                int hour = c.get(Calendar.HOUR_OF_DAY);
                                int minute = c.get(Calendar.MINUTE);
                                int second = c.get(Calendar.SECOND);
                                String TBLDay = ToDay + " " + Common.TwoDigits(hour) + ":" + Common.TwoDigits(minute) + ":" + Common.TwoDigits(second);
                                try {
                                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                    timedrift = (dateFormat.parse(TBLDay).getTime() - dateFormat.parse(NTPDay).getTime()) / 1000;
                                } catch (Exception e) {
                                    Log.d(TAG, "" + e);
                                }
                                if (timedrift > 300) {
                                    app_hld = true;
                                    makeToast("Er is een te groot verschil in tijd tussen de tablet en de " +
                                            "\nwerkelijke tijd gevonden, verschil is thans " + timedrift + " sec.," +
                                            "\nu MOET eerst middels :" +
                                            "\nInstellingen -> Datum en Tijd -> de datum en tijd GOED instellen." +
                                            "\nVoer daarna de optie \"Dag opstarten\" opnieuw uit." +
                                            "\nDit MOET worden gedaan binnen een active WiFi netwerk" +
                                            "\nvoordat de applicatie KAN worden gebruikt", 1);
                                    Log.d(TAG, ToDay + " " + Common.TwoDigits(hour) + ":" + Common.TwoDigits(minute) + " - " + NTPDay + " - " + timedrift);
                                }
                                break;
                        }
                        break;
                    case 2:
                        MenuItem_net = menu.findItem(R.id.action_net);
                        switch (Integer.parseInt(http_msg[1])) {
                            case 0:
                                MenuItem_net.setIcon(getResources().getDrawable(
                                        R.drawable.ok));
                                break;
                            default:
                                MenuItem_net.setIcon(getResources().getDrawable(
                                        R.drawable.error));
                                break;
                        }
                        break;
                    case 3:
                        MenuItem_net = menu.findItem(R.id.action_net);
                        switch (Integer.parseInt(http_msg[1])) {
                            case 0:
                                MenuItem_net.setIcon(getResources().getDrawable(
                                        R.drawable.globe_connected));
                                break;
                            default:
                                MenuItem_net.setIcon(getResources().getDrawable(
                                        R.drawable.globe_disconnect));
                                break;
                        }
                        break;
                    default:
                        Log.d(TAG, "?");
                }
            }
        }
    }

    class LoginTask extends AsyncTask<String, String, String> {

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected String doInBackground(String... params) {

            // avoid execution with no params available
            if ((params[0].equals("") || params[0] == null)) {
                return params[0] + "No params";
            }

            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);

            HttpClient httpclient = new DefaultHttpClient();

            //set the remote endpoint URL
            HttpPost httppost = new HttpPost(params[0] + "api/user/login");
            //Log.d(TAG,"usr "+appKEY+"  pwd " + appSCT + " host " + params[0] + "api/user/login");
            try {

                JSONObject json = new JSONObject();
                //extract the username and password from UI elements and create a JSON object
                json.put("username", appKEY);
                json.put("password", appSCT);

                //add serialised JSON object into POST request
                StringEntity se = new StringEntity(json.toString());
                //set request content type
                se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
                httppost.setEntity(se);
                //send the POST request
                HttpResponse response = httpclient.execute(httppost);
                //read the response from Services endpoint
                String jsonResponse = EntityUtils.toString(response.getEntity());
                //Log.d(TAG,"Json - " + jsonResponse);
                JSONObject jsonObject = new JSONObject(jsonResponse);
                //read the session information
                session_name = jsonObject.getString("session_name");
                session_id = jsonObject.getString("sessid");
                session_token = jsonObject.getString("token");
                return "";

            } catch (Exception e) {
                Log.e("Error session id/name", e.getMessage());
            }

            return "";
        }

        protected void onPostExecute(Integer result) {

        }
    }

    BroadcastReceiver wifiStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Log.d(TAG,"checking wifi state...");
            SupplicantState supState;
            WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            supState = wifiInfo.getSupplicantState();
            //Log.d(TAG,"supplicant state: " + supState);
            if (supState.equals(SupplicantState.COMPLETED)) {
                Log.d(TAG, "WiFi enabled and connected");
                if (!isAuthenticated) {
                    // enable networking, scan driven, to resolve dns
                    isConnected = true;
                }
            } else {
                isAuthenticated = false;
                isConnected = false;
                if (supState.equals(SupplicantState.SCANNING)) {
                    Log.d(TAG, "WiFi scanning");
                } else if (supState.equals(SupplicantState.DISCONNECTED)) {
                    Log.d(TAG, "WiFi disonnected");
                } else {
                    Log.d(TAG, "WiFi connecting");
                }
            }
        }
    };

    class LDRTask extends AsyncTask<String, String, String> {

        InputStream inputStream = null;
        String result = "";
        int cnt = 0;

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected String doInBackground(String... params) {
            if (app_ini) {
                return "0::0::NOP";
            }
            // avoid execution with no params available
            if ((params[0].equals("") || params[0] == null)
                    || (params[1].equals("") || params[1] == null)) {
                return params[1] + "::0::NOP";
            }

            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);
            String spar = session_name + ";" + session_id + ";" + session_token;
            String resp = NetworkUtility.GetFromServer(params[0], srvr, spar);

            String[] http_msg = resp.split("::");
            switch (Integer.parseInt(http_msg[0])) {
                case 0:
                    try {
                        //
                        JSONArray jArray = new JSONArray(http_msg[1]);
                        String[] se = null;
                        switch (Integer.parseInt(params[1])) {
                            case 1:
                                cnt = getContentResolver().delete(
                                        FlightsContentProvider.CONTENT_URI_GLIDER, "",
                                        se);
                                break;
                            case 2:
                                cnt = getContentResolver().delete(
                                        FlightsContentProvider.CONTENT_URI_MEMBER, "",
                                        se);
                                break;
                            case 3:
                                cnt = getContentResolver().delete(
                                        FlightsContentProvider.CONTENT_URI_FLIGHT, "",
                                        se);
                                break;
                            case 4:
                                cnt = getContentResolver().delete(
                                        FlightsContentProvider.CONTENT_URI_EXTERN, "",
                                        se);
                                break;
                            case 5:
                                cnt = getContentResolver().delete(
                                        FlightsContentProvider.CONTENT_URI_RESERV, "",
                                        se);
                                break;
                            case 6:
                                cnt = getContentResolver().delete(
                                        FlightsContentProvider.CONTENT_URI_DUTIES, "",
                                        se);
                                break;
                            default:
                        }
                        se = null;
                        ContentValues values = new ContentValues();
                        for (int i = 0; i < jArray.length(); i++) {
                            JSONObject jObject = jArray.getJSONObject(i);
                            //
                            switch (Integer.parseInt(params[1])) {
                                case 1:
                                    values.put(GliderLogTables.G_REGISTRATION,
                                            jObject.getString("Registratie"));
                                    values.put(GliderLogTables.G_CALLSIGN,
                                            jObject.getString("Callsign"));
                                    values.put(GliderLogTables.G_TYPE,
                                            jObject.getString("Type"));
                                    values.put(GliderLogTables.G_BUILD,
                                            jObject.getString("Bouwjaar"));
                                    values.put(GliderLogTables.G_SEATS,
                                            jObject.getString("Inzittenden"));
                                    values.put(GliderLogTables.G_OWNER,
                                            jObject.getString("Eigenaar"));
                                    values.put(GliderLogTables.G_PRIVATE,
                                            jObject.getString("Prive"));
                                    // insert in table
                                    getContentResolver().insert(
                                            FlightsContentProvider.CONTENT_URI_GLIDER,
                                            values);
                                    break;
                                case 2:
                                    values.put(GliderLogTables.M_ID,
                                            jObject.getString("Id"));
                                    if (jObject.getString("Voorvoeg") == "null") {
                                        values.put(GliderLogTables.M_2_NAME, "");
                                    } else {
                                        values.put(GliderLogTables.M_2_NAME,
                                                jObject.getString("Voorvoeg"));
                                    }
                                    values.put(GliderLogTables.M_3_NAME,
                                            jObject.getString("Achternaam"));
                                    values.put(GliderLogTables.M_1_NAME,
                                            jObject.getString("Voornaam"));
                                    values.put(GliderLogTables.M_ABBREV,
                                            jObject.getString("Afkorting"));
                                    values.put(GliderLogTables.M_ADRS,
                                            jObject.getString("Adres"));
                                    values.put(GliderLogTables.M_POST,
                                            jObject.getString("Postcode"));
                                    values.put(GliderLogTables.M_CITY,
                                            jObject.getString("Plaats"));
                                    values.put(GliderLogTables.M_CNTRY,
                                            jObject.getString("Land"));
                                    values.put(GliderLogTables.M_PHONE,
                                            jObject.getString("Telefoon"));
                                    values.put(GliderLogTables.M_MOBILE,
                                            jObject.getString("Mobiel"));
                                    values.put(GliderLogTables.M_CODE,
                                            jObject.getString("Code"));
                                    values.put(GliderLogTables.M_ACTIVE,
                                            jObject.getString("Actief"));
                                    values.put(GliderLogTables.M_TRAIN,
                                            jObject.getString("Leerling"));
                                    values.put(GliderLogTables.M_INSTRUCTION,
                                            jObject.getString("Instructie"));
                                    values.put(GliderLogTables.M_MAIL,
                                            jObject.getString("E_mail"));
                                    // insert in table
                                    getContentResolver().insert(
                                            FlightsContentProvider.CONTENT_URI_MEMBER,
                                            values);
                                    break;
                                case 3:
                                    String sel_1;
                                    String sel_2;
                                    Cursor cur_lt;
                                    Uri uri;
                                    String[] tmp = jObject.getString("datum")
                                            .split("-");
                                    values.put(GliderLogTables.F_DATE, tmp[2] + "-"
                                            + tmp[1] + "-" + tmp[0]);
                                    values.put(
                                            GliderLogTables.F_STARTED,
                                            jObject.getString("start")
                                                    .substring(
                                                            0,
                                                            jObject.getString("start")
                                                                    .length() - 3));
                                    values.put(
                                            GliderLogTables.F_LANDED,
                                            jObject.getString("landing").substring(
                                                    0,
                                                    jObject.getString("landing")
                                                            .length() - 3));
                                    values.put(
                                            GliderLogTables.F_DURATION,
                                            jObject.getString("duur")
                                                    .substring(
                                                            0,
                                                            jObject.getString("duur")
                                                                    .length() - 3));
                                    values.put(GliderLogTables.F_TYPE,
                                            jObject.getString("soort"));
                                    values.put(GliderLogTables.F_LAUNCH,
                                            jObject.getString("startmethode"));
                                    values.put(GliderLogTables.F_REGISTRATION,
                                            jObject.getString("registratie"));
                                    // todo -> lookup for Call sign, type

                                    uri = FlightsContentProvider.CONTENT_URI_MEMBER;
                                    String[] projection = {GliderLogTables.M_ID,
                                            GliderLogTables.M_1_NAME,
                                            GliderLogTables.M_2_NAME,
                                            GliderLogTables.M_3_NAME,
                                            GliderLogTables.M_ABBREV};
                                    sel_1 = GliderLogTables.M_ABBREV
                                            + " LIKE '"
                                            + jObject.getString("gezagvoerder") + "'";
                                    cur_lt = getContentResolver().query(uri,
                                            projection, sel_1, null, null);
                                    try {
                                        if ((cur_lt != null) && (cur_lt.getCount() > 0)) {
                                            cur_lt.moveToFirst();
                                            values.put(
                                                    GliderLogTables.F_PILOT,
                                                    cur_lt.getString(cur_lt
                                                            .getColumnIndexOrThrow(GliderLogTables.M_1_NAME))
                                                            + " "
                                                            + cur_lt.getString(cur_lt
                                                            .getColumnIndexOrThrow(GliderLogTables.M_2_NAME))
                                                            + " "
                                                            + cur_lt.getString(cur_lt
                                                            .getColumnIndexOrThrow(GliderLogTables.M_3_NAME)));
                                            values.put(
                                                    GliderLogTables.F_PILOT_ID,
                                                    cur_lt.getString(cur_lt
                                                            .getColumnIndexOrThrow(GliderLogTables.M_ID)));

                                        }
                                    } finally {
                                        if (!cur_lt.isClosed()) {
                                            cur_lt.close();
                                        }
                                    }
                                    if (!jObject.getString("tweede").equals("")) {
                                        sel_2 = GliderLogTables.M_ABBREV
                                                + " LIKE '" + jObject.getString("tweede")
                                                + "'";
                                        cur_lt = getContentResolver().query(uri,
                                                projection, sel_2, null, null);
                                        try {
                                            if ((cur_lt != null) && (cur_lt.getCount() > 0)) {
                                                cur_lt.moveToFirst();
                                                values.put(
                                                        GliderLogTables.F_COPILOT,
                                                        cur_lt.getString(cur_lt
                                                                .getColumnIndexOrThrow(GliderLogTables.M_1_NAME))
                                                                + " "
                                                                + cur_lt.getString(cur_lt
                                                                .getColumnIndexOrThrow(GliderLogTables.M_2_NAME))
                                                                + " "
                                                                + cur_lt.getString(cur_lt
                                                                .getColumnIndexOrThrow(GliderLogTables.M_3_NAME)));
                                                values.put(
                                                        GliderLogTables.F_COPILOT_ID,
                                                        cur_lt.getString(cur_lt
                                                                .getColumnIndexOrThrow(GliderLogTables.M_ID)));
                                            }
                                        } finally {
                                            if (!cur_lt.isClosed()) {
                                                cur_lt.close();
                                            }
                                        }
                                    } else {
                                        values.put(GliderLogTables.F_COPILOT, "");
                                        values.put(GliderLogTables.F_COPILOT_ID, "");
                                    }
                                    values.put(GliderLogTables.F_INSTRUCTION, (jObject
                                            .getString("instructie") == "1") ? "J"
                                            : "N");
                                    values.put(GliderLogTables.F_NOTES, jObject
                                            .getString("opmerking").replace("%20", " "));
                                    values.put(GliderLogTables.F_HID,
                                            jObject.getString("id"));
                                    values.put(GliderLogTables.F_SENT, 1);
                                    values.put(GliderLogTables.F_ACK, 1);
                                    values.put(GliderLogTables.F_DEL, 0);
                                    // insert in table
                                    getContentResolver().insert(
                                            FlightsContentProvider.CONTENT_URI_FLIGHT,
                                            values);
                                    projection = null;
                                    sel_1 = null;
                                    sel_2 = null;
                                    break;
                                case 4:
                                    values.put(GliderLogTables.E_ID,
                                            jObject.getString("id"));
                                    values.put(GliderLogTables.E_DATE,
                                            jObject.getString("datum"));
                                    values.put(GliderLogTables.E_TIME,
                                            jObject.getString("tijd"));
                                    values.put(GliderLogTables.E_NAME,
                                            jObject.getString("naam"));
                                    values.put(GliderLogTables.E_PHONE,
                                            jObject.getString("telefoon"));
                                    // insert in table
                                    getContentResolver().insert(
                                            FlightsContentProvider.CONTENT_URI_EXTERN,
                                            values);
                                    break;
                                case 5:
                                    values.put(GliderLogTables.R_ID,
                                            jObject.getString("id"));
                                    values.put(GliderLogTables.R_DATE,
                                            jObject.getString("datum"));
                                    values.put(GliderLogTables.R_PERIOD,
                                            jObject.getString("periode"));
                                    values.put(GliderLogTables.R_TYPE,
                                            jObject.getString("soort"));
                                    values.put(GliderLogTables.R_NAME_ID,
                                            jObject.getString("leden_id"));
                                    values.put(GliderLogTables.R_PURPOSE,
                                            jObject.getString("doel"));
                                    values.put(GliderLogTables.R_RESERVE,
                                            jObject.getString("reserve"));
                                    values.put(GliderLogTables.R_STAMPED,
                                            jObject.getString("aangemaakt"));
                                    values.put(GliderLogTables.R_1_NAME,
                                            jObject.getString("voornaam"));
                                    values.put(GliderLogTables.R_2_NAME,
                                            jObject.getString("voorvoeg"));
                                    values.put(GliderLogTables.R_3_NAME,
                                            jObject.getString("achternaam"));
                                    // insert in table
                                    getContentResolver().insert(
                                            FlightsContentProvider.CONTENT_URI_RESERV,
                                            values);
                                    break;
                                case 6:
                                    values.put(GliderLogTables.D_DATE,
                                            jObject.getString("Datum"));
                                    values.put(GliderLogTables.D_PERIOD,
                                            jObject.getString("Periode"));
                                    values.put(GliderLogTables.D_P_NAME,
                                            jObject.getString("Periode_naam"));
                                    values.put(GliderLogTables.D_DUTY,
                                            jObject.getString("Dienst"));
                                    values.put(GliderLogTables.D_NAME,
                                            jObject.getString("Naam"));
                                    // insert in table
                                    getContentResolver().insert(
                                            FlightsContentProvider.CONTENT_URI_DUTIES,
                                            values);
                                    break;
                                default:
                                    break;
                            }
                            jObject = null;    // added, no result as GC still does cleanup
                        }
                        values = null;
                        jArray = null;
                        result = params[1] + "::0::OK";
                    } catch (JSONException e) {
                        Log.e("JSONException", "Error: " + e.toString());
                        throw new RuntimeException(e);
                    } catch (Exception e) {
                        Log.e("Exception", "Error: " + e.toString());
                    }
                    break;
                case 1:
                    Log.d(TAG, "Response " + http_msg[1]);
                    result = params[1] + "::1::" + http_msg[1];
                    break;
                case 2:
                    Log.d(TAG, "Exception msg " + http_msg[1]);
                    result = params[1] + "::1::" + http_msg[1];
                    break;
            }
            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            //Log.d(TAG, "LDRTask " + result);
            String[] http_msg = result.split("::");
            MenuItem MenuItem_net = menu.findItem(R.id.action_net);
            switch (Integer.parseInt(http_msg[1])) {
                case 0:
                    MenuItem_net.setIcon(getResources().getDrawable(R.drawable.ok));
                    switch (Integer.parseInt(http_msg[0])) {
                        case 1:
                            makeToast("Kisten tabel geladen !", 2);
                            break;
                        case 2:
                            makeToast("Leden tabel geladen !", 2);
                            break;
                        case 3:
                            makeToast("Start tabel her-geladen !", 2);
                            break;
                        case 4:
                            makeToast("Passagiers lijst ververst !", 2);
                            break;
                        case 5:
                            makeToast("Reserverings lijst geladen !", 2);
                            break;
                        case 6:
                            makeToast("Rooster lijst geladen !", 2);
                            break;
                    }
                    break;
                default:
                    MenuItem_net.setIcon(getResources().getDrawable(
                            R.drawable.error));
            }
            sync_sts = http_msg[2];
        }
    }
}