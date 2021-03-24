package com.TMDDataApp.crc_test;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.app.NotificationManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat.Builder;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

public class BackGroundCollecting extends Service {

    final static String foldername = Environment.getExternalStorageDirectory().getAbsolutePath()+"/TMDData/Configuration";
    final static String sensor_setting_file = "SensorSetting.txt";
    final static String time_setting_file = "TimeSetting.txt";
    final static String sensor_setting2_file = "SensorSetting2.txt";

    public String time_temp = "";
    public String [] input_time_temp = null;
    public String sensor_temp = "";
    public String [] input_sensor_temp = null;
    public String location_temp = "";
    public String [] input_location_temp = null;

    public int start_num = 0;
    public int GPS_freq = 0;
    public int sensor_freq = 0;
    public int col_time = 0;
    public float temp_Sensor_freq = 0f;


    private static final String TAG = "tempbackcollecting";

    //Using the Gyroscope & Accelometer
    private SensorManager mSensormanager = null;

    //Using the Accelometer
    private SensorEventListener mGraLis;
    private Sensor mGravitySensor = null;
    private SensorEventListener mLinearAccLis;
    private Sensor mLinearAccSensor = null;
    //Using the Gyro
    private SensorEventListener mGyroLis;
    private Sensor mGyroSensor = null;
    //Using the baro
    private SensorEventListener mBaroLis;
    private Sensor mBaroSensor = null;
    //Using the Proxim
    private SensorEventListener mProxi;
    private Sensor mProxiSensor = null;
    //Using Magnetic
    private SensorEventListener mMagnetic;
    private Sensor mMagneticSensor = null;

    private SensorEventListener mLight;
    private Sensor mLightSensor = null;

    private SensorEventListener mStep;
    private Sensor mStepSensor = null;

    private SensorEventListener mRotationVector;
    private Sensor mRotationVectorSensor;

    private SensorEventListener mOrientation;
    private Sensor mAcc_for_OrientationSensor, mMag_for_OrientationSensor;

    private SensorEventListener mAccelerometer;
    private Sensor mAccelerometerSensor;

    private SensorEventListener mGame_RotationVector;
    private Sensor mGame_RotationVectorSensor;

    private SensorEventListener mTemperature;
    private Sensor mTemperatureSensor;



    //Using GPS
    GPSTracker gps = null;

    public static int RENEW_GPS = 1;
    public static int SEND_PRINT = 2;

    public float graX, linearAccX, gyroX, roll;
    public float graY, linearAccY, gyroY, pitch;
    public float graZ, linearAccZ, gyroZ, yaw;
    public float presure, height;
    public float proximity;
    public float magneticX, magneticY, magneticZ;
    public float lightLux;
    public float step;
    public float rotVec0, rotVec1, rotVec2, rotVec3, rotVec4;
    public float accX, accY, accZ;
    public float game_rotVec0, game_rotVec1, game_rotVec2, game_rotVec3; //, game_rotVec4=0;
    public float temper;

    private float[] accelerometerReading = new float[3];  // orientation을 위함
    private float[] magnetometerReading = new float[3]; // orientation을 위함
    private float[] rotationMatrix = new float[9];  // orientation을 위함
    private float[] orientationAngles = new float[3];   // orientation을 위함


    public double latitude, longitude;

    //timestampe and dt
    public float timestamp;
    public float dt;
    boolean stop = false;
    int remainedTime = 0;
    boolean first = true;

    // for radian -> dgree
    private double RAD2DGR = 180 / Math.PI;
    private static final float NS2S = 1.0f/1000000000.0f;

    Thread collectingThread;
    //GPSThread gpsThread;
    //Thread t;

    int mainFirst = 0;
    int GPSFirst = 0;
    boolean running;

    Calendar calendar;
    CSVWrite cw;
    String mode;


    List<String[]> mainData = new ArrayList<String[]>();
    List<String[]> GPSData = new ArrayList<String[]>();
    List<String[]> GPSData2 = new ArrayList<String[]>();
    List<String[]> GPSData3 = new ArrayList<String[]>();


    public double startTime;
    Notification noti;
    Intent bService;
    PendingIntent sender;
    AlarmManager.AlarmClockInfo ac;
    AlarmManager am;
    String location;
    SimpleDateFormat change_format;
    Intent power;
    String q1;
    IMyAidlInterface.Stub binder = new IMyAidlInterface.Stub() {
        @Override
        public int getRemained() throws RemoteException {
            return remainedTime;
        }
        @Override
        public void setStop(boolean i) throws RemoteException {
            stop = i;
        }
    };


    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    @Override public boolean onUnbind(Intent intent) {
        running = false;
        stop = true;
        return super.onUnbind(intent);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        if(first==true) {
            first = false;
            power = intent;
            mode = intent.getStringExtra("mode");
            location = intent.getStringExtra("location");
            q1 = intent.getStringExtra("Q3");
        }

        if(stop==false && mode!=null) {
            Calendar restart = Calendar.getInstance();
            restart.setTimeInMillis(System.currentTimeMillis());
            restart.add(Calendar.MILLISECOND, 16);

            bService.setAction(RestartService.ACTION_RESTART_SERVICE);
            //intent2.putExtra("mode", mode);
            sender = PendingIntent.getBroadcast(BackGroundCollecting.this, 0, bService, 0);
            //am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000, sender);
            ac = new AlarmManager.AlarmClockInfo(restart.getTimeInMillis(), sender);
            am.setAlarmClock(ac, sender);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                StartForground();
            }
            if(stop==true)
            {
            }
        }
        return START_NOT_STICKY;
    }



    @TargetApi(Build.VERSION_CODES.N)
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onCreate() {
        super.onCreate();

        time_temp = ReadTextFile(foldername+"/"+time_setting_file);
        sensor_temp = ReadTextFile(foldername+"/"+sensor_setting_file);
        location_temp = ReadTextFile(foldername+"/"+sensor_setting2_file);

        Log.d(TAG, sensor_temp);
        Log.d(TAG, "Location Sensor "+ location_temp);

        time_temp = time_temp.replaceAll("\n", "");
        sensor_temp = sensor_temp.replaceAll("\n", "");
        location_temp = location_temp.replaceAll("\n", "");
        input_sensor_temp = sensor_temp.split(",");
        input_time_temp = time_temp.split(",");
        input_location_temp = location_temp.split(",");

        col_time = Integer.parseInt(input_time_temp[0]);
        temp_Sensor_freq = Integer.parseInt(input_time_temp[1]);
        GPS_freq = Integer.parseInt(input_time_temp[2]);
        sensor_freq = (int)((1.0f/temp_Sensor_freq) * 1000f);


        remainedTime = col_time;

        //Using the Gyroscope & Accelometer
        mSensormanager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        /////////Gravity
        if(input_sensor_temp[0].equals("1")){
            mGravitySensor = mSensormanager.getDefaultSensor((Sensor.TYPE_GRAVITY));
            mGraLis = new GravityListener();
            mSensormanager.registerListener(mGraLis, mGravitySensor, SensorManager.SENSOR_DELAY_FASTEST);
        }

        //Gyro
        if(input_sensor_temp[1].equals("1")){
            mGyroSensor = mSensormanager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            mGyroLis = new GyroscopeListener();
            mSensormanager.registerListener(mGyroLis, mGyroSensor, SensorManager.SENSOR_DELAY_FASTEST);
        }

        //Lacc
        if(input_sensor_temp[2].equals("1")){
            mLinearAccSensor = mSensormanager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
            mLinearAccLis = new LinearAccelometerListener();
            mSensormanager.registerListener(mLinearAccLis, mLinearAccSensor, SensorManager.SENSOR_DELAY_FASTEST);
        }

        //Acc
        if(input_sensor_temp[3].equals("1")){
            mAccelerometerSensor = mSensormanager.getDefaultSensor((Sensor.TYPE_ACCELEROMETER));
            mAccelerometer = new AccelerometerListener();
            mSensormanager.registerListener(mAccelerometer, mAccelerometerSensor, SensorManager.SENSOR_DELAY_FASTEST);
        }

        //Light
        if(input_sensor_temp[4].equals("1")){
            mLightSensor = mSensormanager.getDefaultSensor(Sensor.TYPE_LIGHT);
            mLight = new LightListener();
            mSensormanager.registerListener(mLight, mLightSensor, SensorManager.SENSOR_DELAY_FASTEST);
        }

        //Mag
        if(input_sensor_temp[5].equals("1")){
            mMagneticSensor = mSensormanager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            mMagnetic = new MagneticListener();
            mSensormanager.registerListener(mMagnetic, mMagneticSensor, SensorManager.SENSOR_DELAY_FASTEST);
        }

        //Baro
        if(input_sensor_temp[6].equals("1")){
            mBaroSensor = mSensormanager.getDefaultSensor(Sensor.TYPE_PRESSURE);
            mBaroLis = new BarometerListener();
            mSensormanager.registerListener(mBaroLis, mBaroSensor, SensorManager.SENSOR_DELAY_FASTEST);
        }

        //Rot
        if(input_sensor_temp[7].equals("1")){
            mRotationVectorSensor = mSensormanager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            mRotationVector = new RotationVectorListener();
            mSensormanager.registerListener(mRotationVector, mRotationVectorSensor, SensorManager.SENSOR_DELAY_FASTEST);
        }

        //Ori
        if(input_sensor_temp[8].equals("1")){
            mAcc_for_OrientationSensor = mSensormanager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            mMag_for_OrientationSensor = mSensormanager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            mOrientation = new OrientationListener();
            mSensormanager.registerListener(mOrientation, mAcc_for_OrientationSensor, SensorManager.SENSOR_DELAY_FASTEST);
            mSensormanager.registerListener(mOrientation, mMag_for_OrientationSensor, SensorManager.SENSOR_DELAY_FASTEST);
        }

        //Proxi
        if(input_sensor_temp[9].equals("1")){
            mProxiSensor = mSensormanager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
            mProxi = new ProximityListener();
            mSensormanager.registerListener(mProxi, mProxiSensor, SensorManager.SENSOR_DELAY_FASTEST);
        }

        //Step
        if(input_sensor_temp[10].equals("1")){
            mStepSensor = mSensormanager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
            mStep = new StepListener();
            mSensormanager.registerListener(mStep, mStepSensor, SensorManager.SENSOR_DELAY_FASTEST);
        }

        //Temperature

        if(input_sensor_temp[11].equals("1")){
            mTemperatureSensor = mSensormanager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
            mTemperature = new TemperatureListener();
            Log.d(TAG, "Temperature Sensor On");
            mSensormanager.registerListener(mTemperature, mTemperatureSensor, SensorManager.SENSOR_DELAY_FASTEST);
        }

        //Press
        if(input_sensor_temp[12].equals("1")){
            mGame_RotationVectorSensor = mSensormanager.getDefaultSensor((Sensor.TYPE_GAME_ROTATION_VECTOR));
            mGame_RotationVector = new GameRotationVectorListener();
            mSensormanager.registerListener(mGame_RotationVector, mGame_RotationVectorSensor, SensorManager.SENSOR_DELAY_FASTEST);
        }

        calendar = new GregorianCalendar(Locale.KOREA);


        startTime = System.currentTimeMillis();

        gps = new GPSTracker(BackGroundCollecting.this);
        cw = new CSVWrite();

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
        bService = new Intent(this, RestartService.class);
        am = (AlarmManager) getSystemService(ALARM_SERVICE);

        change_format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onDestroy() {
        super.onDestroy();
        mSensormanager.unregisterListener(mGraLis);
        mSensormanager.unregisterListener(mLinearAccLis);
        mSensormanager.unregisterListener(mGyroLis);
        mSensormanager.unregisterListener(mBaroLis);
        mSensormanager.unregisterListener(mProxi);
        mSensormanager.unregisterListener(mMagnetic);
        mSensormanager.unregisterListener(mLight);
        mSensormanager.unregisterListener(mStep);
        mSensormanager.unregisterListener(mRotationVector);
        mSensormanager.unregisterListener(mOrientation);
        mSensormanager.unregisterListener(mAccelerometer);
        mSensormanager.unregisterListener(mGame_RotationVector);
        mSensormanager.unregisterListener(mTemperature);
        //collectingThread.isInterrupted();
        gps.stopUsingGPS();
        stop = true;
        //bService = null;
        calendar = new GregorianCalendar(Locale.KOREA);
        cw.writeCsv(GPSData, "" + calendar.get(Calendar.YEAR) + "_" + (calendar.get(Calendar.MONTH) + 1) + "_" + calendar.get(Calendar.DATE) + "_" +
                calendar.get(Calendar.HOUR_OF_DAY) + "_" + calendar.get(Calendar.MINUTE) + "_" + calendar.get(Calendar.SECOND) + "_" + mode + "_" +"GPSData.csv");
        cw.writeCsv(mainData, "" + calendar.get(Calendar.YEAR) + "_" + (calendar.get(Calendar.MONTH) + 1) + "_" + calendar.get(Calendar.DATE) + "_" +
                calendar.get(Calendar.HOUR_OF_DAY) + "_" + calendar.get(Calendar.MINUTE) + "_" + calendar.get(Calendar.SECOND) + "_" + mode + "_" + "SensorData.csv");

        Intent intent = new Intent(BackGroundCollecting.this, RestartService.class);
        intent.setAction(RestartService.ACTION_RESTART_SERVICE);
        PendingIntent sender = PendingIntent.getBroadcast(BackGroundCollecting.this, 0, intent, 0);
        AlarmManager am = (AlarmManager)getSystemService(ALARM_SERVICE);
        am.cancel(sender);
        stopForeground(true);
        // delayedEnd();
    }

    private void delayedEnd()
    {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                android.os.Process.killProcess(android.os.Process.myPid());
            }
        }, 10000);
    }

    public void StartForground() {

        if (Build.VERSION.SDK_INT >= 26) {
            String CHANNEL_ID = "default";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_LOW);
            channel.enableLights(false);
            channel.enableVibration(false);
            channel.setVibrationPattern(new long[]{0, 0, 0, 0, 0, 0, 0, 0, 0});

            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);

            noti = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("데이터 수집중")
                    .setContentText("수집중" + " " + remainedTime)
                    .setTicker("Running")
                    .setWhen(System.currentTimeMillis())
                    .setVibrate(new long[]{0, 0, 0, 0, 0, 0, 0, 0, 0})
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setAutoCancel(false)
                    .setOngoing(true)
                    .build();
            startForeground(1, noti);
        }

        if(mainFirst == 0 && GPSFirst == 0) {
            getMainData();
            getGPSData();
            collectingThread = new Thread(new CollectingThread());
            collectingThread.start();
        }
    }

    public void setServiceWatchdogTimer(boolean set, int timeout)
    {
        Intent intent;
        PendingIntent alarmIntent;
        intent = new Intent(); // forms and creates appropriate Intent and pass it to AlarmManager
        intent.setAction(RestartService.ACTION_RESTART_SERVICE);
        intent.setClass(this, RestartService.class);
        alarmIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager am=(AlarmManager)getSystemService(Context.ALARM_SERVICE);
        if(set)
            am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + timeout, alarmIntent);
        else
            am.cancel(alarmIntent);
    }

    void startForegroundService() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        //RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout);

        NotificationCompat.Builder builder;
        if (Build.VERSION.SDK_INT >= 26) {
            String CHANNEL_ID = "snwodeer_service_channel";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "SnowDeer Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT);

            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
                    .createNotificationChannel(channel);

            builder = new Builder(this, CHANNEL_ID);
        } else {
            builder = new Builder(this);
        }
        builder.setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("데이터 수집중")
                .setContentText("수집중" + " " + remainedTime)
                .setTicker("Running")
                .setContentIntent(pendingIntent);

        startForeground(1, builder.build());
    }

    protected void setAlarmTimer(){
        final Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis());
        c.add(Calendar.SECOND, 1);
        Intent intent = new Intent(this, RestartService.class);
        PendingIntent sender = PendingIntent.getBroadcast(this, 0,intent,0);

        AlarmManager mAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        mAlarmManager.set(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), sender);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            StartForground();
        }
    }
    private void getMainData(){
        if(mainFirst == 0){
            mainFirst = 1;
            mainData.add(new String[] {"Time", "Year", "Month", "Day", "Hour", "Min", "Sec", "GraX", "GraY", "GraZ", "LinearAccX", "LinearAccY", "LinearAccZ",
                    "GyroX", "GyroY", "GyroZ", "Height", "Proxi", "MagX", "MagY", "MagZ", "Light", "Step" , "Pressure", "RotVec_0",  "RotVec_1", "RotVec_2","RotVec_3","RotVec_4",
                    "Orientation_0_Azimuth", "Orientation_1_Pitch", "Orientation_2_Roll", "AccX", "AccY", "AccZ", "Game_RotVec_0",  "Game_RotVec_1", "Game_RotVec_2","Game_RotVec_3",
                    "Temper","Mode", "Survay1"});
        }
        else if(mainFirst == 1)
        {
            mainFirst = 2;
            calendar = new GregorianCalendar(Locale.KOREA);
            //firstSensor = new String[]{"" + ((System.currentTimeMillis() - startTime) / 1000), "" + calendar.get(Calendar.YEAR), "" + (calendar.get(Calendar.MONTH) + 1), "" + calendar.get(Calendar.DAY_OF_MONTH), "" + calendar.get(Calendar.HOUR_OF_DAY), "" + calendar.get(Calendar.MINUTE), "" + calendar.get(Calendar.SECOND), "" + graX, "" + graY, "" + graZ, "" + linearAccX, "" + linearAccY, "" + linearAccZ, "" + gyroX, "" + gyroY, "" + gyroZ, "" + height, "" + proximity, "" + magneticX, "" + magneticY, "" + magneticZ, "" + lightLux, "" + step, mode, location, other};
            mainData.add( new String[] {"" + ((System.currentTimeMillis() - startTime)/1000), "" + calendar.get(Calendar.YEAR), "" + (calendar.get(Calendar.MONTH) + 1), "" +
                    calendar.get(Calendar.DAY_OF_MONTH), "" + calendar.get(Calendar.HOUR_OF_DAY), "" + calendar.get(Calendar.MINUTE), "" + calendar.get(Calendar.SECOND), "" +
                    graX, "" + graY, "" + graZ, "" + linearAccX, "" + linearAccY, "" + linearAccZ, "" + gyroX, "" + gyroY, "" + gyroZ, "" + height, "" + proximity, "" +
                    magneticX, "" + magneticY, "" + magneticZ, "" + lightLux, "" + step ,"" + presure , ""+ rotVec0, ""+ rotVec1, ""+ rotVec2, ""+ rotVec3,""+ rotVec4, "" +
                    orientationAngles[0], ""+orientationAngles[1], ""+orientationAngles[2], ""+ accX, ""+accY, ""+accZ, ""+ game_rotVec0, ""+ game_rotVec1, ""+ game_rotVec2, ""+
                    game_rotVec3, "" + temper, mode, location});

        }
        else
        {
            calendar = new GregorianCalendar(Locale.KOREA);
            mainData.add( new String[] {"" + ((System.currentTimeMillis() - startTime)/1000), "" + calendar.get(Calendar.YEAR), "" + (calendar.get(Calendar.MONTH) + 1),
                    "" + calendar.get(Calendar.DAY_OF_MONTH), "" + calendar.get(Calendar.HOUR_OF_DAY), "" + calendar.get(Calendar.MINUTE), "" + calendar.get(Calendar.SECOND),
                    "" + graX, "" + graY, "" + graZ, "" + linearAccX, "" + linearAccY, "" + linearAccZ, "" + gyroX, "" + gyroY, "" + gyroZ,  "" + height, "" + proximity,
                    "" + magneticX, "" + magneticY, "" + magneticZ, "" + lightLux, "" + step,"" + presure ,""+ rotVec0, ""+ rotVec1, ""+ rotVec2, ""+ rotVec3,""+ rotVec4,
                    ""+orientationAngles[0], ""+orientationAngles[1], ""+orientationAngles[2],""+accX, ""+accY, ""+accZ, ""+ game_rotVec0, ""+ game_rotVec1, ""+ game_rotVec2,
                    ""+ game_rotVec3, "" + temper, "",""});
            //Log.i(TAG, "getMainData: " + "  "+ orientationAngles[0]+ "  "+  ""+ orientationAngles[1]+ "  "+  ""+ orientationAngles[2]+ "  "+  ""+  "  "+ "");
        }
    }

    private void getGPSData(){
        double temp_lat = 0;
        double temp_long= 0;
        double temp_alt= 0;
        float temp_accuracy= 0;
        String temp_provider= null;
        float temp_bear= 0;
        float temp_satel= 0;


        if(input_location_temp[0].equals("1")){
            temp_lat = gps.getLocation().getLatitude();
            Log.d(TAG, "LAT On");
        }

        if(input_location_temp[1].equals("1")) {
            temp_long = gps.getLocation().getLongitude();
            Log.d(TAG, "Long On");
        }

        if(input_location_temp[2].equals("1")) {
            temp_alt = gps.getLocation().getAltitude();
            Log.d(TAG, "Alt On");
        }

        if(input_location_temp[3].equals("1")) {
            temp_accuracy = gps.getLocation().getAccuracy();
            Log.d(TAG, "Acc On");
        }

        if(input_location_temp[4].equals("1")) {
            temp_provider = gps.getLocation().getProvider();
            Log.d(TAG, "Provider On");
        }

        if(input_location_temp[5].equals("1")) {
            temp_bear = gps.getLocation().getBearing();
            Log.d(TAG, "Bearing On");
        }

        if(input_location_temp[6].equals("1")) {
            temp_satel = gps.getLocation().getExtras().getInt("satellites");
            Log.d(TAG, "Satel On");
        }



        if(GPSFirst == 0){
            GPSFirst = 1;
            GPSData.add(new String[]{"Time", "Year", "Month", "Day", "Hour", "Min", "Sec", "Latitude", "Longitude", "Provider", "Accuracy", "Altitude", "Bearing",  "Extra", "Speed",
                    "WorldTime", "Mode", "Survay1"});
        }
        else if(GPSFirst == 1)
        {
            if(gps.isGetLocation())
            {
                GPSFirst = 2;
                calendar = new GregorianCalendar(Locale.KOREA);
                latitude = gps.getLatitude();
                longitude = gps.getLongitude();

                if(gps.getLocation()!=null) {
                    //firstGps = new String[]{"" + ((System.currentTimeMillis() - startTime) / 1000), "" + calendar.get(Calendar.YEAR), "" + (calendar.get(Calendar.MONTH) + 1), "" + calendar.get(Calendar.DAY_OF_MONTH), "" + calendar.get(Calendar.HOUR_OF_DAY), "" + calendar.get(Calendar.MINUTE), "" + calendar.get(Calendar.SECOND), "" + gps.getLocation().getLatitude(), "" + gps.getLocation().getLongitude(), "" + gps.getLocation().getProvider(), "" + gps.getLocation().getAccuracy(), "" + gps.getLocation().getAltitude(), "" + gps.getLocation().getBearing(), "" + gps.getLocation().getExtras().getInt("satellites"), "" + gps.getLocation().getSpeed(), change_format.format(gps.getLocation().getTime()), mode, location, other};
                    GPSData.add(new String[]{"" + ((System.currentTimeMillis() - startTime) / 1000), "" + calendar.get(Calendar.YEAR), "" + (calendar.get(Calendar.MONTH) + 1),
                            "" + calendar.get(Calendar.DAY_OF_MONTH), "" + calendar.get(Calendar.HOUR_OF_DAY), "" + calendar.get(Calendar.MINUTE), "" + calendar.get(Calendar.SECOND),
                            "" + temp_lat, "" + temp_long, "" + temp_provider, "" + temp_accuracy, "" + temp_alt, "" + temp_bear, "" + temp_satel,
                            "" + gps.getLocation().getSpeed(), change_format.format(gps.getLocation().getTime()), mode, location});
                }
                else
                {
                    GPSData.add(new String[]{"" + ((System.currentTimeMillis() - startTime) / 1000), "" + calendar.get(Calendar.YEAR), "" + (calendar.get(Calendar.MONTH) + 1),
                            "" + calendar.get(Calendar.DAY_OF_MONTH), "" + calendar.get(Calendar.HOUR_OF_DAY), "" + calendar.get(Calendar.MINUTE), "" + calendar.get(Calendar.SECOND),
                            "", "", "", "", "" , "" , "" , "", "", mode, location});
                }
            }
        }
        else
        {
            if(gps.isGetLocation())
            {
                calendar = new GregorianCalendar(Locale.KOREA);
                latitude = gps.getLatitude();
                longitude = gps.getLongitude();
                if(gps.getLocation()!=null) {
                    GPSData.add(new String[]{"" + ((System.currentTimeMillis() - startTime) / 1000), "" + calendar.get(Calendar.YEAR), "" + (calendar.get(Calendar.MONTH) + 1),
                            "" + calendar.get(Calendar.DAY_OF_MONTH), "" + calendar.get(Calendar.HOUR_OF_DAY), "" + calendar.get(Calendar.MINUTE), "" + calendar.get(Calendar.SECOND),
                            "" + temp_lat, "" + temp_long, "" + temp_provider, "" + temp_accuracy, "" + temp_alt, "" + temp_bear, "" + temp_satel,
                            "" + gps.getLocation().getSpeed(), "" + change_format.format(gps.getLocation().getTime()), "", ""});
                }
            }
        }
    }

    public class GravityListener extends Service  implements SensorEventListener {

        @Override
        public void onSensorChanged(SensorEvent event) {
            graX = event.values[0];
            graY = event.values[1];
            graZ = event.values[2];
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }
    }

    public class LinearAccelometerListener extends Service implements  SensorEventListener{

        @Override
        public void onSensorChanged(SensorEvent event) {
            linearAccX = event.values[0];
            linearAccY = event.values[1];
            linearAccZ = event.values[2];
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }
    }

    public class GyroscopeListener extends Service implements  SensorEventListener{
        @Override
        public void onSensorChanged(SensorEvent event) {
            gyroX = event.values[0];
            gyroY = event.values[1];
            gyroZ = event.values[2];

            dt = (event.timestamp - timestamp) * NS2S;
            timestamp = event.timestamp;

            if(dt - timestamp*NS2S != 0){
                pitch = pitch + gyroY*dt;
                roll = roll + gyroX*dt;
                yaw = yaw + gyroZ*dt;
            }
        }
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }
    }

    public class BarometerListener extends Service implements  SensorEventListener{
        @Override
        public void onSensorChanged(SensorEvent event) {
            presure = event.values[0];
            height = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, presure);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }
    }

    public class StepListener extends Service implements  SensorEventListener {
        @Override
        public void onSensorChanged(SensorEvent event) {
            step = event.values[0];
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }
    }

    public class ProximityListener extends Service implements  SensorEventListener{
        @Override
        public void onSensorChanged(SensorEvent event) {
            proximity = event.values[0];
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }
    }

    public class MagneticListener extends Service implements  SensorEventListener{
        @Override
        public void onSensorChanged(SensorEvent event) {
            magneticX = event.values[0];
            magneticY = event.values[1];
            magneticZ = event.values[2];
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }
    }

    public class LightListener extends Service implements  SensorEventListener{
        @Override
        public void onSensorChanged(SensorEvent event) {
            lightLux = event.values[0];
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }
    }

    public class RotationVectorListener extends Service implements SensorEventListener{

        @Nullable
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            rotVec0 = event.values[0];
            rotVec1 = event.values[1];
            rotVec2 = event.values[2];
            rotVec3 = event.values[3];
            rotVec4 = event.values[4];
        }
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }

    public class OrientationListener extends Service implements SensorEventListener{

        @Nullable
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.length);
            } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.length);
            }

            SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerReading, magnetometerReading);
            //Log.i(TAG, "onSensorChanged: " + accelerometerReading[0] + "  " + accelerometerReading[1] + "   " + accelerometerReading[2]);
            //Log.i(TAG, "onSensorChanged: " + rotationMatrix[0] + "  " + rotationMatrix[1]);
            SensorManager.getOrientation(rotationMatrix, orientationAngles);
            //Log.i(TAG, "onSensorChanged: " + orientationAngles[0] * 180.0 / Math.PI + "   " + orientationAngles[1]* 180.0 / Math.PI + "  " + orientationAngles[2] * 180.0 / Math.PI+ "  " );
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }

    public class AccelerometerListener extends Service implements SensorEventListener{

        @Nullable
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            accX = event.values[0];
            accY = event.values[1];
            accZ = event.values[2];
        }
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }

    public class GameRotationVectorListener extends Service implements SensorEventListener{

        @Nullable
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            game_rotVec0 = event.values[0];
            game_rotVec1 = event.values[1];
            game_rotVec2 = event.values[2];
            game_rotVec3 = event.values[3];
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }

    public class TemperatureListener extends Service implements SensorEventListener{

        @Nullable
        @Override
        public void onSensorChanged(SensorEvent event) {
            temper = (int)event.values[0];
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

    }

    private class CollectingThread implements Runnable{

        @TargetApi(Build.VERSION_CODES.N)
        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void run()
        {
            int second = 0;
            int GPSsecond = 0;

            while(!stop)
            {
                second++;
                //Log.d("SecondCount", Integer.toString(second));
                try{
                    getMainData();
                    if(second==(int)temp_Sensor_freq)
                    {
                        //Log.e("Finsh", ""+remainedTime);
                        //Log.d("every1second", Integer.toString(second));
                        second = 0;
                        GPSsecond++;
                        //Log.d("every1secondGPS", Integer.toString(GPSsecond));
                        //remainedTime++;
                        int timer = col_time;
                        remainedTime = timer - (int)((System.currentTimeMillis()-startTime)/1000);
                        StartForground();
                        //Log.d("GPS", Integer.toString(GPS_freq));
                        if(GPSsecond==GPS_freq) {
                            if(stop==true)
                            {
                                am.cancel(sender);
                            }
                            getGPSData();
                            GPSsecond=0;
                        }
                        if((System.currentTimeMillis()-startTime)/1000 >= col_time)
                        {
                            stop = true;
                        }
                        //collecting.handler.sendEmptyMessage(0);
                    }
                    Thread.sleep( sensor_freq);
                    //Log.d("Sensor Output", Integer.toString(sensor_freq));
                }catch (InterruptedException e){
                    //getMainData();
                }
            }
        }
        public void Stop(){
            stop = true;
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
}
