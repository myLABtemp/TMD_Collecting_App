package com.TMDDataApp.crc_test;

import android.Manifest;
import android.app.AlertDialog;
import android.app.usage.NetworkStatsManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    ImageButton stillButton, walkingButton, motorizedButton, metroButton, carButton, busButton, nonMotorizedButton;

    final static String foldername = Environment.getExternalStorageDirectory().getAbsolutePath()+"/TMDData/Configuration";
    final static String mode_setting_file = "ModeSetting.txt";
    final static String position_setting_file = "PositionSetting.txt";
    final static String sensor_setting_file = "SensorSetting.txt";
    final static String sensor_setting2_file = "SensorSetting2.txt";
    final static String time_setting_file = "TimeSetting.txt";
    final static String val_setting_file = "ValidationSetting.txt";

    public String mode_temp = "";
    public String time_temp = "";
    public String sensor_temp = "";
    public String sensor_temp2 = "";
    public String pos_temp = "";
    public String val_temp = "";

    public int start_num = 0;
    public int start_num2 = 0;

    public String [] input_mode_temp = null;
    public String [] input_val_temp = null;


    String temp_input = ""; // 한줄씩 읽기
    List<String> output_mode = new ArrayList<String>();
    String [] txt_input_mode = null;

    Collecting collecting;
    String mode;
    private File file;

    private static final String TAG = "메시지MainActivity";

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

    private boolean IsFinishing = false;
    //*********************************************************************************//
    @Override
    protected void onStart() {
        super.onStart();
        //* Permission ******************************************************************************************//
        // Battery optimizatio
        PowerManager pm = (PowerManager) getApplicationContext().getSystemService(POWER_SERVICE);
        boolean isWhiteListing = false;

        // 권한은 api 23 (6.0)이상 인 경우
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "onStart: RQ permission");
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
            }
        }
        //***********************************************************************************************************************Permission*/
    }
    @Override
    protected void onResume() {
        Log.i(TAG, "onResume: ");;
        super.onResume();

        // Battery optimization, network, gps check ********************************************************************************************//
        IsFirstMessageOnForSetting = false;
        IsSecondMessageOnForSetting = false;

        PowerManager pm = (PowerManager) getApplicationContext().getSystemService(POWER_SERVICE);
        boolean isWhiteListing = false;

        // Android version, network, gps check.
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                ||ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){ }
        else {
            // GPS turn off
            Log.i(TAG, "onResume: check permission");
            mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                IsFirstMessageOnForSetting = true;
                Log.i(TAG, "onResume: gps ff");
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setMessage("For data collecting, turn on GPS and Location.");
                builder.setTitle("turn on GPS and Location");
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
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setMessage("For data collecting, turn on mobile network.");
                builder.setTitle("Turn on mobile network");
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
                Log.i(TAG, "onResume: Battery Optimization On");
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setMessage("For data collecting, turn off battery optimization.");
                builder.setTitle("Trun off battery optimization");
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
    // 권한 요청에 대한 결과 처리 (거부했을 때 처리). 20200701수정됨.
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case PERMISSION_REQUEST_CODE:{
                Log.i(TAG, "onRequestPermissionsResult: permission denied");
                IsFinishing = true;
                // 위치 권한 거부시
                if (grantResults.length!=4 || grantResults[0] != PackageManager.PERMISSION_GRANTED || grantResults[1] != PackageManager.PERMISSION_GRANTED){
                    IsFirstMessageOn = true;
                    Log.i(TAG, "onRequestPermissionsResult: location permission");
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setMessage("For data collecting, [Setting > Application > CRC_Test > Authorization > Location] change to permission.");
                    builder.setTitle("Location Permission");
                    builder.setCancelable(false);
                    builder.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){ finishAndRemoveTask(); Log.i(TAG, "onClick: quite the screen");}
                            else { finish();}
                        }
                    });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
                //저장공간 거부시
                if (grantResults.length!=4 || grantResults[2] != PackageManager.PERMISSION_GRANTED || grantResults[3] != PackageManager.PERMISSION_GRANTED){
                    Log.i(TAG, "onRequestPermissionsResult: storage permission");
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setMessage("For data collecting, [Setting > Application > CRC_Test > Authorization > Storage] change to permission.");
                    builder.setTitle("Stroage Permission");
                    builder.setCancelable(false);
                    builder.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){ finishAndRemoveTask(); Log.i(TAG, "onClick: 화면종료!!!");}
                            else { finish();}
                        }
                    });
                    AlertDialog dialog = builder.create();

                    if(!IsFirstMessageOn){
                        Log.i(TAG, "onRequestPermissionsResult: " + IsFirstMessageOn);
                        dialog.show();
                    }
                }
                return;
            }
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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

        if(start_num == 0) {
            mode_temp = ReadTextFile(foldername+"/"+mode_setting_file);
            time_temp = ReadTextFile(foldername+"/"+time_setting_file);
            sensor_temp = ReadTextFile(foldername+"/"+sensor_setting_file);
            sensor_temp2 = ReadTextFile(foldername+"/"+sensor_setting2_file);
            pos_temp = ReadTextFile(foldername+"/"+position_setting_file);
            val_temp = ReadTextFile(foldername+"/"+val_setting_file);

            if(mode_temp.equals("") || time_temp.equals("") || sensor_temp.equals("")
                    || sensor_temp2.equals("") || pos_temp.equals("") || val_temp.equals("")){
                AlertDialog.Builder settingCheck = new AlertDialog.Builder(MainActivity.this);
                String notInputed = "";

                if(TextUtils.isEmpty(mode_temp)){
                    notInputed = notInputed + "Mode Configuration \n";
                }

                if(TextUtils.isEmpty(time_temp)){
                    notInputed = notInputed + "Time Configuration \n";
                }
                if(TextUtils.isEmpty(sensor_temp)){
                    notInputed = notInputed + "Sensor(inertial) Configuration \n";
                }
                if(TextUtils.isEmpty(sensor_temp2)){
                    notInputed = notInputed + "Sensor(location) Configuration \n";
                }
                if(TextUtils.isEmpty(pos_temp)){
                    notInputed = notInputed + "Mode Configuration \n";
                }
                if(TextUtils.isEmpty(val_temp)){
                    notInputed = notInputed + "Configuration \n";
                }
                settingCheck.setTitle("Need Configuration");
                settingCheck.setMessage(notInputed);

                settingCheck.setPositiveButton("Exit",new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog,int which){
                        exitProgram();
                    }
                });
                settingCheck.show();
            }
            start_num = 1;
        }

        final ArrayList<String> items = new ArrayList<String>() ;
        final ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_single_choice, items);
        final ListView listview = (ListView) findViewById(R.id.listview1) ;
        listview.setAdapter(adapter);

        /*
        mode_temp = "a,b,c,d,e,f,g";
        val_temp = "1,1";

        mode_temp = mode_temp.replaceAll("\n", "");
        val_temp = val_temp.replaceAll("\n", "");
        input_mode_temp = mode_temp.split(",");
        input_val_temp = val_temp.split(",");

        for (int i=0; i<input_mode_temp.length; i++){
            items.add(input_mode_temp[i]);
        }*/

        //Mode 데이터 불러오기

        mode_temp = ReadTextFile(foldername+"/"+mode_setting_file);
        val_temp = ReadTextFile(foldername+"/"+val_setting_file);
        mode_temp = mode_temp.replaceAll("\n", "");
        val_temp = val_temp.replaceAll("\n", "");
        input_mode_temp = mode_temp.split(",");
        input_val_temp = val_temp.split(",");

        for (int i=0; i<input_mode_temp.length; i++){
            items.add(input_mode_temp[i]);
        }

        adapter.notifyDataSetChanged();


        Button recordButton = (Button)findViewById(R.id.ModeSelect) ;
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
                mode = contents;
                if(input_val_temp[0].equals("1")){
                    CreatePhoto(mode);
                }
                else{
                    StartSurvay(mode);
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 8;
        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        StartSurvay(mode);
        finish();
    }

    private void StartSurvay(String getMode){
        Intent intent = new Intent(MainActivity.this, Survay.class);
        intent.putExtra("mode", getMode);
        startActivity(intent);
        finish();
    }

    private void CreatePhoto(String getMode){
        //파일 저장 경로 설정.
        String dirPath = Environment.getExternalStorageDirectory().getAbsolutePath()+"/TMDData";
        //디렉토리 없으면 생성.
        File dir = new File(dirPath);
        if(!dir.exists()){dir.mkdir();}

        Calendar calendar = new GregorianCalendar(Locale.KOREA);
        String filename = "" + calendar.get(Calendar.YEAR) + "_" + (calendar.get(Calendar.MONTH) + 1) + "_" + calendar.get(Calendar.DATE) + "_" + calendar.get(Calendar.HOUR_OF_DAY) + "_" + calendar.get(Calendar.MINUTE) + "_" + calendar.get(Calendar.SECOND) + "_" + getMode + ".png";
        file = new File(dir, filename);
        Intent intent =  new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        Uri uri = FileProvider.getUriForFile(getApplicationContext(), "com.tmddataapp.crc_test.fileprovider", file);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        startActivityForResult(intent, 1);
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

    private void exitProgram() {
        if (Build.VERSION.SDK_INT >= 21) {
            finishAndRemoveTask();
        } else {
            finish();
        }
        System.exit(0);
    }
}
