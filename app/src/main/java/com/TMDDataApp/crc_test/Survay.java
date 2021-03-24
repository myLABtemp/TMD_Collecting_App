package com.TMDDataApp.crc_test;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.usage.NetworkStatsManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioGroup;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

public class Survay extends Activity {

    final static String foldername = Environment.getExternalStorageDirectory().getAbsolutePath()+"/TMDData/Configuration";
    final static String mode_setting_file = "ModeSetting.txt";
    final static String position_setting_file = "PositionSetting.txt";
    final static String time_setting_file = "TimeSetting.txt";
    public String time_temp = "";
    public String [] input_time_temp = null;

    public RadioGroup group1;
    public RadioGroup.LayoutParams radio_parm;
    private Button doneButton;
    private LinearLayout layout2;
    String mode;
    public String pos_temp = "";
    public String [] input_pos_temp = null;
    List<String> output_mode = new ArrayList<String>();

    int q1 = -1;
    int q2 = 0;
    int done = 1;
    CSVWrite cw;
    Collecting collecting;
    Calendar calendar;

    List<String[]> mainData = new ArrayList<String[]>();


    String location, other;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_survay);


        mode = getIntent().getExtras().getString("mode");
        doneButton = (Button)findViewById(R.id.PosSelect);
        cw = new CSVWrite();
        collecting = new Collecting();
        calendar = new GregorianCalendar(Locale.KOREA);

        pos_temp = ReadTextFile(foldername+"/"+position_setting_file);
        pos_temp = pos_temp.replaceAll("\n", "");
        input_pos_temp = pos_temp.split(",");


        final ArrayList<String> items = new ArrayList<String>() ;
        final ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_single_choice, items);
        final ListView listview = (ListView) findViewById(R.id.listview1) ;
        listview.setAdapter(adapter);

        for (int i=0; i<input_pos_temp.length; i++){
            items.add(input_pos_temp[i]);
        }

        adapter.notifyDataSetChanged();




        /*group1.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                if(i==R.id.)
            }
        });

*/

        Button recordButton = (Button)findViewById(R.id.PosSelect) ;
        recordButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                SparseBooleanArray checkedItems = listview.getCheckedItemPositions();

                int count = adapter.getCount() ;

                for (int i = 0; i < count; i++) {
                    if (checkedItems.get(i)) {
                        String vo = (String)items.get(i);
                        output_mode.add(vo);
                    }
                }

                String[] tempstr = output_mode.toArray(new String[output_mode.size()]);
                String temp_output = "";
                for (int i=0; i<tempstr.length; i++){
                    if(i==(tempstr.length-1)){
                        temp_output = temp_output + tempstr[i];
                    }
                    else{
                        temp_output = temp_output + tempstr[i] + ",";
                    }
                }
                String contents = temp_output;
                Log.d("선택출력값", contents);
                location = contents;;
                Intent intent = new Intent(Survay.this, Collecting.class);
                intent.putExtra("mode", mode);
                intent.putExtra("location", location);
                startActivity(intent);
                finish();

            }
        });


        //RadioButtonSetting();
        //doneButtonSetting();

    }




    private static final String TAG = "메시지Survey";

    //* 권한 요청 관련 변수들 *********************************************************************************//
    private static final int ACCESS_FINE_LOCATION_REQUEST_CODE = 101;
    private static final int ACCESS_COARSE_LOCATION_REQUEST_CODE = 102;
    private static final int WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 103;
    private static final int READ_EXTERNAL_STORAGE_REQUEST_CODE = 104;
    private static final int LOCATION_REQUEST_CODE = 111;
    private static final int STORAGE_REQUEST_CODE = 112;
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int PERMISSION_IGNORE_OPTIMIZATION_REQUEST_CODE = 200;

    private boolean IsFirstMessageOn = false;
    private boolean IsFirstMessageOnForSetting = false;
    private boolean IsSecondMessageOnForSetting = false;

    LocationManager mLocationManager;
    NetworkStatsManager mNetworkStatsManager;
    ConnectivityManager mConnectivityManager;

    private boolean IsFinishing = false;
    //******************************************************************************* 권한 요청 관련 변수들 *//

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "onStart: ");


        // Permission Request ******************************************************************************************//
        // Battery Optimization
        PowerManager pm = (PowerManager) getApplicationContext().getSystemService(POWER_SERVICE);
        boolean isWhiteListing = false;

        // 권한은 api 23 (6.0)이상 인 경우
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "onStart: Permission request");
                ActivityCompat.requestPermissions(Survay.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
            }
        }
        // ***********************************************************************************************************************Permission Request//
    }
    @Override
    protected void onResume() {
        Log.i(TAG, "onResume: ");;
        super.onResume();

        // battery optimization, network, gps check ********************************************************************************************//
        IsFirstMessageOnForSetting = false;
        IsSecondMessageOnForSetting = false;

        PowerManager pm = (PowerManager) getApplicationContext().getSystemService(POWER_SERVICE);
        boolean isWhiteListing = false;



        // Android version, GPS, Network check
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                ||ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){ }
        else {
            // GPS turn off
            Log.i(TAG, "onResume: Permission");
            mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                IsFirstMessageOnForSetting = true;
                Log.i(TAG, "onResume: turn off gps");
                AlertDialog.Builder builder = new AlertDialog.Builder(Survay.this);
                builder.setMessage("GPS information is required for data collection. Turn on the position.");
                builder.setTitle("Turn on the position");
                builder.setCancelable(false);
                builder.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            // finishAndRemoveTask();
                            Intent gpsOptionsIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivity(gpsOptionsIntent);
                        } else {
                            // finish();
                            Intent gpsOptionsIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivity(gpsOptionsIntent);
                        }
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
            // wifi and mobile network check.
            mConnectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
            if (mNetworkInfo == null || !mNetworkInfo.isConnected()) {
                Log.i(TAG, "onResume: network off");
                IsSecondMessageOnForSetting = true;
                AlertDialog.Builder builder = new AlertDialog.Builder(Survay.this);
                builder.setMessage("Network connection is required for data collection. Please turn on mobile data.");
                builder.setTitle("Turn on mobile data");
                builder.setCancelable(false);
                builder.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            //finishAndRemoveTask();
                            Intent gpsOptionsIntent = new Intent(android.provider.Settings.ACTION_NETWORK_OPERATOR_SETTINGS);
                            startActivity(gpsOptionsIntent);
                        } else {
                            //finish();
                            Intent gpsOptionsIntent = new Intent(android.provider.Settings.ACTION_NETWORK_OPERATOR_SETTINGS);
                            startActivity(gpsOptionsIntent);
                        }
                    }
                });
                AlertDialog dialog = builder.create();
                if (!IsFirstMessageOnForSetting) {
                    dialog.show();
                }
            }

            // battery optimization.
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                isWhiteListing = pm.isIgnoringBatteryOptimizations(getApplicationContext().getPackageName());
            } else {
                isWhiteListing = false;
            }
            if (!isWhiteListing) {
                Log.i(TAG, "onResume: Battery optimization");
                AlertDialog.Builder builder = new AlertDialog.Builder(Survay.this);
                builder.setMessage("For data collecting, turn off battery optimization.");
                builder.setTitle("Turn off battery optimization");
                builder.setCancelable(false);
                builder.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            // finishAndRemoveTask();
                            Intent intent = new Intent();
                            intent.setAction(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                            intent.setData(Uri.parse("package:" + getApplicationContext().getPackageName()));
                            startActivity(intent);
                        } else {
                            // finish();
                            Intent intent = new Intent();
                            intent.setAction(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                            intent.setData(Uri.parse("package:" + getApplicationContext().getPackageName()));
                            startActivity(intent);
                        }
                    }
                });
                AlertDialog dialog = builder.create();
                if (!IsFirstMessageOnForSetting && !IsSecondMessageOnForSetting) { dialog.show(); }
            }
            // ********************************************************************************************베터리 최적화, 네트워크, gps 확인 //
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event){
        if ( event.getAction() == KeyEvent.ACTION_DOWN ){
            if ( keyCode == KeyEvent.KEYCODE_BACK ){
                AlertDialog.Builder alert_confirm = new AlertDialog.Builder(this);
                alert_confirm.setMessage("Do you want exit the program?")
                        .setCancelable(false)
                        .setPositiveButton("Exit",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        // 'YES'
                                        finish();
                                        //System.runFinalization();
                                    }
                                }).setNeutralButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //finish();
                            }
                        }).setNegativeButton("background",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // 'No'
                                System.out.println("Run on background");
                                Intent intent = new Intent();
                                intent.setAction(Intent.ACTION_MAIN);
                                intent.addCategory(Intent.CATEGORY_HOME);
                                startActivity(intent);
                                return;
                            }
                        });
                AlertDialog alert = alert_confirm.create();
                alert.show();
            }
            if ( keyCode == KeyEvent.KEYCODE_HOME ){

            }
        }
        return super.onKeyDown(keyCode, event);
    }

    public String ReadTextFile(String path){
        StringBuffer strBuffer = new StringBuffer();
        try{
            InputStream is = new FileInputStream(path);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line="";
            while((line=reader.readLine())!=null){
                strBuffer.append(line+"\n");
            }
            reader.close();
            is.close();
        }catch (IOException e){
            e.printStackTrace();
            return "";
        }
        return strBuffer.toString();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void checkPermission(){
        if(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }
}
