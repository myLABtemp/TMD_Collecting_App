package com.TMDDataApp.crc_test;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.IdRes;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

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

public class Survay2 extends Activity {

    final static String foldername = Environment.getExternalStorageDirectory().getAbsolutePath()+"/TMDData/Configuration";
    final static String val_setting_file = "ValidationSetting.txt";
    public String val_temp = "";
    public String [] input_val_temp = null;

    private TextView question;

    private LinearLayout layout;
    private Button doneButton;
    private RadioGroup group;
    String location, mode;
    String wo;
    Calendar calendar;
    CSVWrite cw;

    int q1 = -1;

    List<String[]> mainData = new ArrayList<String[]>();

    private static final String TAG = "survay2debugging";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        val_temp = ReadTextFile(foldername+"/"+val_setting_file);
        val_temp = val_temp.replaceAll("\n", "");
        input_val_temp = val_temp.split(",");

        if(input_val_temp[1].equals("0")){
            finishAndRemoveTask();
            exitProgram();
            android.os.Process.killProcess(android.os.Process.myPid());
        }
        else{
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_survay2);

            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

            Intent intent2 = getIntent();
            mode = intent2.getExtras().getString("mode");
            location =  intent2.getExtras().getString("location");

            layout = (LinearLayout)findViewById(R.id.Layout3);
            doneButton = (Button)findViewById(R.id.Donebutton2);
            group = (RadioGroup)findViewById(R.id.R3);
            question = (TextView)findViewById(R.id.Q3);
            cw = new CSVWrite();

            startSurvay();

        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected  void onDestroy(){
        super.onDestroy();
    }


    private void startSurvay()
    {
        String first = "Do you collect ";
        String second = " mode? and is the recording position is ";
        String third = "?";
        question.setText(first+mode+second+location+third);


        RadioButtonSetting();
        doneButtonSetting();
    }

    private void RadioButtonSetting()
    {
        RadioGroup.OnCheckedChangeListener g2GroupButtonChangeListener = new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int i) {
                if(i == R.id.YesButton)
                    q1 = 0;
                else if(i == R.id.NoButton)
                    q1 = 1;
            }
        };

        group.setOnCheckedChangeListener(g2GroupButtonChangeListener);
    }
    private void doneButtonSetting()
    {
        doneButton.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onClick(View v) {
                if(q1 == 0 || q1 == 1)
                {
                    if(q1 ==0) {
                        wo = "Yes";
                    }
                    else if(q1 ==1)
                    {
                        wo = "No";
                    }
                    mainData.add(new String[]{"Survay3"});
                    mainData.add(new String[]{wo});
                    calendar = new GregorianCalendar(Locale.KOREA);
                    cw.writeCsv(mainData, "" + calendar.get(Calendar.YEAR) + "_" + (calendar.get(Calendar.MONTH) + 1) + "_" + calendar.get(Calendar.DATE) + "_" + calendar.get(Calendar.HOUR_OF_DAY) + "_" + calendar.get(Calendar.MINUTE) + "_" + calendar.get(Calendar.SECOND) + "_" + mode + "_" + "Confirm.csv");
                    finishAndRemoveTask();
                    android.os.Process.killProcess(android.os.Process.myPid());
                }
            }
        });
    }

    private void exitProgram() {
        if (Build.VERSION.SDK_INT >= 21) {
            finishAndRemoveTask();
        } else {
            finish();
        }
        System.exit(0);
    }

    @Override
    public void onBackPressed() {
        // 이 함수를 오버라이드해서 뒤로 가기 버튼 사용 못하게 막음
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
