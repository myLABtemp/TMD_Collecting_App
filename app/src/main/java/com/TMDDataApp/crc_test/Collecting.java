package com.TMDDataApp.crc_test;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.usage.NetworkStatsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.Vibrator;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;


public class Collecting extends AppCompatActivity{

    final static String foldername = Environment.getExternalStorageDirectory().getAbsolutePath()+"/TMDData/Configuration";
    final static String time_setting_file = "TimeSetting.txt";
    final static String val_setting_file = "ValidationSetting.txt";
    public String val_temp = "";
    public String [] input_val_temp = null;

    public String mode;
    CSVWrite cw;
    Calendar calendar;
    public static PowerManager.WakeLock sCpuWakeLock;
    Button doneButton;

    private final int PERMISSIONS_ACCESS_FINE_LOCATION = 1000;
    private final int PERMISSIONS_ACCESS_COARSE_LOCATION = 1001;
    private boolean isAccessFineLocation = false;
    private boolean isAccessCoarseLocation = false;
    private boolean isPermission = false;
    private IMyAidlInterface binder;
    List<String[]> mainData = new ArrayList<String[]>();
    ThreadTest test;
    public int remain = 0;
    private boolean stopFlag = false;

    Handler handler;
    boolean done=false;
    RestartService restartService;
    String location, other;
    public double startTime;
    //BackGroundCollecting backGroundCollecting;

    private Intent serviceIntent;

    private TextView timeRemain;

    public int start_num = 0;
    public String time_temp = "";
    public String [] input_time_temp = null;
    int exp_time = 0;


    private static final String TAG = "메시지Collecting";


    /* GPS, Network  *********************************************************/
    LocationManager mLocationManager1;
    ConnectivityManager mConnectivityManager1;
    /* ******************************************************** GPS, Network */

    //**********************************************************************************//
    // Permission request
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

    //*********************************************************************************//
    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "onStart: ");
        // Permission Request ******************************************************************************************//
        // Battery Optimization
        PowerManager pm = (PowerManager) getApplicationContext().getSystemService(POWER_SERVICE);
        // 권한은 api 23 (6.0)이상 인 경우
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "onStart: Permission Request");
                ActivityCompat.requestPermissions(Collecting.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
            }
        }
        // ***********************************************************************************************************************Permission Request//
        startTime = System.currentTimeMillis();
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
                AlertDialog.Builder builder = new AlertDialog.Builder(Collecting.this);
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
                AlertDialog.Builder builder = new AlertDialog.Builder(Collecting.this);
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
                Log.i(TAG, "onResume: Battery optimization ");
                AlertDialog.Builder builder = new AlertDialog.Builder(Collecting.this);
                builder.setMessage("For the data.");
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

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            binder = IMyAidlInterface.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    @TargetApi(Build.VERSION_CODES.O)
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activty_collecting);
        val_temp = ReadTextFile(foldername+"/"+val_setting_file);
        val_temp = val_temp.replaceAll("\n", "");
        input_val_temp = val_temp.split(",");

        time_temp = ReadTextFile(foldername+"/"+time_setting_file);
        time_temp = time_temp.replaceAll("\n", "");
        input_time_temp = time_temp.split(",");

        remain = Integer.parseInt(input_time_temp[0]);
        exp_time = Integer.parseInt(input_time_temp[0]);


        Intent intent2 = getIntent();
        mode = intent2.getExtras().getString("mode");
        location =  intent2.getExtras().getString("location");


        PowerManager pm = (PowerManager) getApplicationContext().getSystemService(POWER_SERVICE);
        boolean isWhiteListing = false;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            isWhiteListing = pm.isIgnoringBatteryOptimizations(getApplicationContext().getPackageName());
        }
        if (!isWhiteListing) {
            Intent intent = new Intent();
            intent.setAction(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + getApplicationContext().getPackageName()));
            startActivity(intent);
        }
        callPermission();
        //remain = 900;

        timeRemain = (TextView)findViewById(R.id.TimeRemaining);
        //doneButton = (Button)findViewById(R.id.doneButton);
        //setButton();

        handler = new Handler()
        {
            @Override
            public void handleMessage(Message msg) {
                    setText();
            }
        };
        restartService = new RestartService();
        IntentFilter filter = new IntentFilter();
        filter.addAction("mode");
        registerReceiver(restartService, filter);

        Intent intent = new Intent("mode");
        intent.putExtra("mode", mode);
        sendBroadcast(intent);

        serviceIntent =  new Intent(getApplicationContext(), BackGroundCollecting.class);
        serviceIntent.setPackage("com.TMDDataApp.crc_test");
        serviceIntent.putExtra("mode", mode);
        serviceIntent.putExtra("location", location);
        serviceIntent.putExtra("other", other);
        startForegroundService(serviceIntent);
        startService(serviceIntent);
        bindService(serviceIntent, connection, BIND_AUTO_CREATE);


        new Thread(new ThreadTest()).start();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event){
        if ( event.getAction() == KeyEvent.ACTION_DOWN ){
            if ( keyCode == KeyEvent.KEYCODE_BACK ){
                AlertDialog.Builder alert_confirm = new AlertDialog.Builder(this);
                alert_confirm.setMessage("Do you want to exit this application?")
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
                        }).setNegativeButton("Background",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // 'No'
                                System.out.println("Do you want to run on background");
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




    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected  void onStop(){
        super.onStop();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected  void onDestroy(){
        super.onDestroy();
        if(done==false) {
            Finish();
        }
    }

    private void setButton()
    {
        doneButton.setOnLongClickListener(new View.OnLongClickListener(){
            @Override
            public boolean onLongClick(View v) {

                done = true;
                return true;
            }
        });
    }

    public void Finish()
    {
        List<String[]> GPSData = new ArrayList<String[]>();
        String[] firstSensor, firstGps;

        done = true;
        try {
            binder.setStop(true);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        PowerManager manager = (PowerManager)getSystemService(Context.POWER_SERVICE);

        boolean bScreenOn = manager.isScreenOn();

        stopService(serviceIntent);
        unbindService(connection);
        unregisterReceiver(restartService);
        restartService = null;


        if(bScreenOn){
            Intent Survay = new Intent(getApplicationContext(), Survay2.class);
            Survay.putExtra("mode", mode);
            Survay.putExtra("location", location);
            startActivity(Survay);
            finish();
        }
        else
        {
            Intent popup = new Intent(getApplicationContext(), PopUp.class);
            popup.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            popup.putExtra("mode", mode);
            popup.putExtra("location", location);
            startActivity(popup);
            finish();
        }
    }

    public void setText(){
        try {
            remain = binder.getRemained();
        }catch (RemoteException e) {
            e.printStackTrace();
        }

        timeRemain.setText(remain + " Second\nRemaining");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        if (requestCode == PERMISSIONS_ACCESS_FINE_LOCATION
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            isAccessFineLocation = true;

        } else if (requestCode == PERMISSIONS_ACCESS_COARSE_LOCATION
                && grantResults[0] == PackageManager.PERMISSION_GRANTED){

            isAccessCoarseLocation = true;
        }

        if (isAccessFineLocation && isAccessCoarseLocation) {
            isPermission = true;
        }
    }

    // 전화번호 권한 요청
    private void callPermission() {
        // Check the SDK version and whether the permission is already granted or not.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_ACCESS_FINE_LOCATION);

        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED){
            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSIONS_ACCESS_COARSE_LOCATION);
        } else {
            isPermission = true;
        }
    }

    private class ThreadTest extends Thread{

        @Override
        public void run()
        {
            int second = 0;

            while(!stopFlag)
            {
                //second++;
                if(binder == null) { continue; }
                handler.sendEmptyMessage(0);
                try{
                    //if(done == true)
                    if((System.currentTimeMillis()-startTime)/1000 >= exp_time)
                    {
                        stopFlag = true;
                        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                        Ringtone ringtone = RingtoneManager.getRingtone(getApplicationContext(), notification);
                        ringtone.play();
                        Vibrator vib = (Vibrator)getSystemService(VIBRATOR_SERVICE);
                        vib.vibrate(1500);
                        Finish();
                        //startSurvay();
                        //handler2.sendEmptyMessage(0);
                    }
                    Thread.sleep(1000);
                }catch (InterruptedException e){

                }
            }
        }

        public void Stop(){
            stopFlag = true;
        }
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
