package com.zebra.scannercontrol.app.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.Xml;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import com.zebra.scannercontrol.DCSSDKDefs;
import com.zebra.scannercontrol.app.helpers.ScannerAppEngine;
import com.zebra.scannercontrol.app.R;
import com.zebra.scannercontrol.app.application.Application;
import com.zebra.scannercontrol.app.helpers.Constants;
import com.zebra.scannercontrol.app.helpers.CustomProgressDialog;

import org.xmlpull.v1.XmlPullParser;

import static com.zebra.scannercontrol.RMDAttributes.*;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

public class VibrationFeedback extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener,ScannerAppEngine.IScannerAppEngineDevConnectionsDelegate {
    private NumberPicker vibrationPicker;
    SwitchCompat switchCompat;
    private ArrayList vibrationDurations;
    private NavigationView navigationView;
    private int scannerID;
    static MyAsyncTask cmdExecTask=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vibration_feedback);

        Configuration configuration = getResources().getConfiguration();
        if(configuration.orientation == Configuration.ORIENTATION_LANDSCAPE){
            if(configuration.smallestScreenWidthDp<Application.minScreenWidth){
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        }else{
            if(configuration.screenWidthDp<Application.minScreenWidth){
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        Toolbar subActionBar = (Toolbar) findViewById(R.id.sub_actionbar);
        setSupportActionBar(subActionBar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Active Scanner");

        setSupportActionBar(subActionBar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        scannerID = getIntent().getIntExtra(Constants.SCANNER_ID, -1);
        vibrationDurations = new ArrayList();
        vibrationDurations.add(RMD_ATTR_VALUE_VIBRATION_150);
        vibrationDurations.add(RMD_ATTR_VALUE_VIBRATION_200);
        vibrationDurations.add(RMD_ATTR_VALUE_VIBRATION_250);
        vibrationDurations.add(RMD_ATTR_VALUE_VIBRATION_300);
        vibrationDurations.add(RMD_ATTR_VALUE_VIBRATION_400);
        vibrationDurations.add(RMD_ATTR_VALUE_VIBRATION_500);
        vibrationDurations.add(RMD_ATTR_VALUE_VIBRATION_600);
        vibrationDurations.add(RMD_ATTR_VALUE_VIBRATION_750);

    }


    @Override
    protected void onPause() {
        super.onPause();
        removeDevConnectiosDelegate(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        addDevConnectionsDelegate(this);

        boolean isPagerMotorAvailable = true; //isPagerMotorAvailable(); // This is already checked in the parent activity
        TextView textView = (TextView)findViewById(R.id.txt_no_pager_motor);
        LinearLayout linearLayout = (LinearLayout)findViewById(R.id.layout_vibration_feedback);
        if(isPagerMotorAvailable){
            if (linearLayout != null) {
                linearLayout.setVisibility(View.VISIBLE);
            }
            if (textView != null) {
                textView.setVisibility(View.INVISIBLE);
                textView.setVisibility(View.GONE);
            }
            fetchVibrationStatus();
        }else{
            if (textView != null) {
                textView.setVisibility(View.VISIBLE);
            }
            if (linearLayout != null) {
                linearLayout.setVisibility(View.INVISIBLE);
                linearLayout.setVisibility(View.GONE);
            }
        }
    }

    private boolean isPagerMotorAvailable() {
        boolean isFound = false;
        String in_xml = "<inArgs><scannerID>" + scannerID + "</scannerID><cmdArgs><arg-xml><attrib_list>613</attrib_list></arg-xml></cmdArgs></inArgs>";
        StringBuilder outXML = new StringBuilder();
        cmdExecTask = new MyAsyncTask(scannerID,DCSSDKDefs.DCSSDK_COMMAND_OPCODE.DCSSDK_RSM_ATTR_GET,outXML);
        cmdExecTask.execute(new String[]{in_xml});
        try {
            cmdExecTask.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        if(outXML !=null) {
            if(outXML.toString().contains("<id>613</id>")){
                isFound = true;
            }
        }
        return isFound;
    }

    private void fetchVibrationStatus() {
        switchCompat = (SwitchCompat) findViewById(R.id.switch_vibration);
        vibrationPicker = (NumberPicker) findViewById(R.id.vibration_duration_picker);
        vibrationPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        vibrationPicker.setDisplayedValues(getResources().getStringArray(R.array.vibration_durations));
        vibrationPicker.setMaxValue(7);
        vibrationPicker.setMinValue(0);
        updateUI();
        switchCompat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                changeVibrationOnOffTableRow(isChecked);
                setDurationEnable(isChecked);
                setVibrationStatus(isChecked);
            }
        });
    }

    private void setVibrationStatus(boolean isChecked) {
        String in_xml = "<inArgs><scannerID>" + scannerID + "</scannerID><cmdArgs><arg-xml><attrib_list><attribute><id>" +613+"</id><datatype>F</datatype><value>" + isChecked + "</value></attribute></attrib_list></arg-xml></cmdArgs></inArgs>";
        StringBuilder outXML = new StringBuilder();
        cmdExecTask = new MyAsyncTask(scannerID,DCSSDKDefs.DCSSDK_COMMAND_OPCODE.DCSSDK_RSM_ATTR_SET,outXML);
        cmdExecTask.execute(new String[]{in_xml});

    }

    private void updateUI() {
        boolean vibrationStatus = getVibrationStatus();
        switchCompat.setChecked(vibrationStatus);
        changeVibrationOnOffTableRow(vibrationStatus);
        vibrationPicker.setValue(7);
        //vibrationPicker.setValue(getVibrationDurationIndex(getVibrationDuration()));
        setDurationEnable(vibrationStatus);
    }

    private void setDurationEnable(boolean enable){

        vibrationPicker.setEnabled(enable);
        TextView txtVibrationDuration = (TextView)findViewById(R.id.vibration_duration);
        txtVibrationDuration.setEnabled(enable);
        if(enable){
            if(txtVibrationDuration!=null)txtVibrationDuration.setTextColor(ContextCompat.getColor(this, R.color.font_color));
        }else{
            if(txtVibrationDuration!=null)txtVibrationDuration.setTextColor(ContextCompat.getColor(this, R.color.inactive_text));

        }
        Button btnTestVibration = (Button)findViewById(R.id.btn_test_vibration);
        btnTestVibration.setEnabled(enable);
    }

    private void changeVibrationOnOffTableRow(boolean vibrationStatus) {
        final TextView txtVibration = (TextView)findViewById(R.id.txt_vibration);
        if(vibrationStatus){
            if(txtVibration!=null)txtVibration.setTextColor(ContextCompat.getColor(this, R.color.font_color));
        }else{
            if(txtVibration!=null)txtVibration.setTextColor(ContextCompat.getColor(this, R.color.inactive_text));

        }
    }

    private int getVibrationDurationIndex(int vibrationDuration) {
        int ret = 0;
        for (int i=0;i<vibrationDurations.size();i++){
            if(vibrationDurations.get(i).equals(vibrationDuration)){
                ret = i;
                break;
            }
        }
        return ret;
    }

    public boolean getVibrationStatus(){
        String in_xml = "<inArgs><scannerID>" + scannerID + "</scannerID><cmdArgs><arg-xml><attrib_list>613</attrib_list></arg-xml></cmdArgs></inArgs>";
        StringBuilder outXML = new StringBuilder();
        cmdExecTask = new MyAsyncTask(scannerID,DCSSDKDefs.DCSSDK_COMMAND_OPCODE.DCSSDK_RSM_ATTR_GET,outXML);
        cmdExecTask.execute(new String[]{in_xml});
        boolean attr_val = false;
        try {
            cmdExecTask.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        if(outXML !=null) {
            try {
                XmlPullParser parser = Xml.newPullParser();
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
                parser.setInput(new StringReader(outXML.toString()));
                int event = parser.getEventType();
                String text = null;
                while (event != XmlPullParser.END_DOCUMENT) {
                    String name = parser.getName();
                    switch (event) {
                        case XmlPullParser.START_TAG:
                            break;
                        case XmlPullParser.TEXT:
                            text = parser.getText();
                            break;

                        case XmlPullParser.END_TAG:
                            if (name.equals("value")) {
                                attr_val = Boolean.parseBoolean(text.trim());
                            }
                            break;
                    }
                    event = parser.next();
                }
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }
        }
        return attr_val;
    }

    public int getVibrationDuration(){
        String in_xml = "<inArgs><scannerID>" + scannerID + "</scannerID><cmdArgs><arg-xml><attrib_list>626</attrib_list></arg-xml></cmdArgs></inArgs>";
        StringBuilder outXML = new StringBuilder();
        cmdExecTask = new MyAsyncTask(scannerID,DCSSDKDefs.DCSSDK_COMMAND_OPCODE.DCSSDK_RSM_ATTR_GET,outXML);
        cmdExecTask.execute(new String[]{in_xml});
        int attr_val = 150;
        try {
            cmdExecTask.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        if(outXML !=null) {
            try {
                XmlPullParser parser = Xml.newPullParser();
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
                parser.setInput(new StringReader(outXML.toString()));
                int event = parser.getEventType();
                String text = null;
                while (event != XmlPullParser.END_DOCUMENT) {
                    String name = parser.getName();
                    switch (event) {
                        case XmlPullParser.START_TAG:
                            break;
                        case XmlPullParser.TEXT:
                            text = parser.getText();
                            break;

                        case XmlPullParser.END_TAG:
                            if (name.equals("value")) {
                                attr_val = Integer.parseInt(text.trim());
                            }
                            break;
                    }
                    event = parser.next();
                }
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }
        }
        return attr_val;
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        Intent intent;
        if (id == R.id.nav_pair_device) {
            AlertDialog.Builder dlg = new AlertDialog.Builder(this);
            dlg.setTitle("This will disconnect your current scanner");
            //dlg.setIcon(android.R.drawable.ic_dialog_alert);
            dlg.setPositiveButton("Continue", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int arg) {

                    disconnect(scannerID);
                    Application.barcodeData.clear();
                    Application.CurScannerId = Application.SCANNER_ID_NONE;
                    finish();
                    Intent intent = new Intent(VibrationFeedback.this, HomeActivity.class);
                    startActivity(intent);
                }
            });

            dlg.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int arg) {

                }
            });
            dlg.show();

        } else if (id == R.id.nav_devices) {
            intent = new Intent(this, ScannersActivity.class);

            startActivity(intent);
        }else if (id == R.id.nav_find_cabled_scanner) {
            AlertDialog.Builder dlg = new  AlertDialog.Builder(this);
            dlg.setTitle("This will disconnect your current scanner");
            //dlg.setIcon(android.R.drawable.ic_dialog_alert);
            dlg.setPositiveButton("Continue", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int arg) {

                    disconnect(scannerID);
                    Application.barcodeData.clear();
                    Application.CurScannerId = Application.SCANNER_ID_NONE;
                    finish();
                    Intent intent = new Intent(VibrationFeedback.this, FindCabledScanner.class);
                    startActivity(intent);
                }
            });

            dlg.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int arg) {

                }
            });
            dlg.show();
        }else if (id == R.id.nav_connection_help) {
            intent = new Intent(this, ConnectionHelpActivity2.class);
            startActivity(intent);
        } else if (id == R.id.nav_settings) {
            intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_about) {
            intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        drawer.setSelected(true);
        return true;
    }

    @Override
    public boolean scannerHasAppeared(int scannerID) {
        return false;
    }

    @Override
    public boolean scannerHasDisappeared(int scannerID) {
        return false;
    }

    @Override
    public boolean scannerHasConnected(int scannerID) {
        return false;
    }

    @Override
    public boolean scannerHasDisconnected(int scannerID) {
        Application.barcodeData.clear();
        this.finish();
        return true;
    }

    public void vibrateScanner(View view) {
        setDuration();
        String in_xml = "<inArgs><scannerID>" + scannerID + "</scannerID></inArgs>";
        cmdExecTask = new MyAsyncTask(scannerID,DCSSDKDefs.DCSSDK_COMMAND_OPCODE.DCSSDK_DEVICE_VIBRATION_FEEDBACK,null);
        cmdExecTask.execute(new String[]{in_xml});
    }

    private void setDuration() {
        String in_xml = "<inArgs><scannerID>" + scannerID + "</scannerID><cmdArgs><arg-xml><attrib_list><attribute><id>" +626+"</id><datatype>B</datatype><value>" + getSelectedDurationValue() + "</value></attribute></attrib_list></arg-xml></cmdArgs></inArgs>";
        StringBuilder outXML = new StringBuilder();
        cmdExecTask = new MyAsyncTask(scannerID,DCSSDKDefs.DCSSDK_COMMAND_OPCODE.DCSSDK_RSM_ATTR_SET,outXML);
        cmdExecTask.execute(new String[]{in_xml});
        try {
            cmdExecTask.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    int getSelectedDurationValue(){
        int ret = 15;
        ret = (int) vibrationDurations.get(vibrationPicker.getValue());
        return ret;
    }

    private class MyAsyncTask extends AsyncTask<String, Integer, Boolean> {
        int scannerId;
        StringBuilder outXML;
        DCSSDKDefs.DCSSDK_COMMAND_OPCODE opcode;
        private CustomProgressDialog progressDialog;

        public MyAsyncTask(int scannerId, DCSSDKDefs.DCSSDK_COMMAND_OPCODE opcode, StringBuilder outXML) {
            this.scannerId = scannerId;
            this.opcode = opcode;
            this.outXML = outXML;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new CustomProgressDialog(VibrationFeedback.this, "Execute Command...");
            progressDialog.show();
        }


        @Override
        protected Boolean doInBackground(String... strings) {
            return executeCommand(opcode, strings[0], outXML, scannerId);
        }

        @Override
        protected void onPostExecute(Boolean b) {
            super.onPostExecute(b);
            if (progressDialog != null && progressDialog.isShowing())
                progressDialog.dismiss();
            if (!b) {
                Toast.makeText(VibrationFeedback.this, "Cannot perform the action", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
