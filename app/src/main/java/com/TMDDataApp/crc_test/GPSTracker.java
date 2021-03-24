package com.TMDDataApp.crc_test;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;

import java.util.Calendar;
import java.util.Iterator;


public class GPSTracker extends Service implements LocationListener {

    private final Context mContext;

    // 현재 GPS 사용유무
    boolean isGPSEnabled = false;

    // 네트워크 사용유무
    boolean isNetworkEnabled = false;

    // GPS 상태값
    boolean isGetLocation = false;

    Location location, location2, location3, location4;
    double lat; // 위도
    double lon; // 경도

    // 최소 GPS 정보 업데이트 거리 10미터
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 1;

    // 최소 GPS 정보 업데이트 시간 밀리세컨이므로 1분
    private static final long MIN_TIME_BW_UPDATES = 500;

    protected LocationManager locationManager;
    protected TelephonyManager tm;
    GsmCellLocation TeleLocation;
    GpsStatus.Listener GPSListner;
    int updatedTime;

    //기지국 id
    int cellID = 0;
    //location local area code
    int lac = 0;

    int findSat = -1;

    public GPSTracker(Context context) {
        this.mContext = context;
        getLocation(); //gps 우선
        //getLocation2(); //Network 우선
        //getLocation3(); // criteria
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public Location getLocation() {
        int GPSorNetwork  = -1;
        boolean gpsUse = false, networkUse = false;

        if (Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission(
                        mContext, android.Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                        mContext, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {

            return null;
        }

        try {
                locationManager = (LocationManager) mContext
                        .getSystemService(LOCATION_SERVICE);

                // GPS 정보 가져오기
                isGPSEnabled = locationManager
                        .isProviderEnabled(LocationManager.GPS_PROVIDER);
                // 현재 네트워크 상태 값 알아오기
                isNetworkEnabled = locationManager
                        .isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!isGPSEnabled && !isNetworkEnabled) {
                lat = 0;
                lon = 0;
            } else {
                this.isGetLocation = true;
                // GPS
                if (isGPSEnabled) {
                    locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            0,
                            0, this);
                    if (locationManager != null) {
                        location = locationManager
                                .getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        //gpsTime = location.getTime();
                        if(location != null && location.getTime() > Calendar.getInstance().getTimeInMillis() - 5000) {
                            gpsUse = true;
                        }
                        if (location != null) {
                            lat = location.getLatitude();
                            lon = location.getLongitude();
                        }
                    }
                }
                if (isNetworkEnabled) {
                        locationManager.requestLocationUpdates(
                                LocationManager.NETWORK_PROVIDER,
                                0,
                                0, this);
                        if (locationManager != null) {
                            location4 = locationManager
                                    .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                            //networkTime = location4.getTime();
                            if(location4 != null && location4.getTime() > Calendar.getInstance().getTimeInMillis() - 5000) {
                                networkUse = true;
                            }
                            if (location4 != null) {
                                // 위도 경도 저장
                                lat = location4.getLatitude();
                                lon = location4.getLongitude();
                                //Log.e("getCid", "" + TeleLocation.getCid());
                                //cellID = TeleLocation.getCid();
                                //lac = TeleLocation.getLac();
                            }
                        }
                    }
                }
        } catch (Exception e) {
            e.printStackTrace();
        }


        if( gpsUse == true && networkUse == true)
        {
            if(location4.getAccuracy() < location.getAccuracy())
            {
                GPSorNetwork = 1;
            }
            else if(location.getAccuracy() < location4.getAccuracy())
            {
                GPSorNetwork = 0;
            }
            else if(location.getAccuracy()==location4.getAccuracy())
            {
                GPSorNetwork = 0;
            }
        }
        else if(gpsUse == false && networkUse == true)
        {
            GPSorNetwork = 1;
        }
        else if(gpsUse == true && networkUse == false)
        {
            GPSorNetwork = 0;
        }
        else if(gpsUse == false && networkUse ==false)
        {
            if(location != null && location4 != null) {
                if (location.getTime() >= location4.getTime()) {
                    GPSorNetwork = 0;
                } else if (location.getTime() < location4.getTime()) {
                    GPSorNetwork = 1;
                }
            }
            else if(location == null && location4 != null)
            {
                GPSorNetwork = 1;
            }
            else if(location != null && location4 ==null)
            {
                GPSorNetwork = 0;
            }
            else if(location == null && location4 == null)
            {
                GPSorNetwork = -1;
            }
        }

        switch (GPSorNetwork) {
            case 0:
                return location;
            case 1:
                return location4;
            default:
                return null;
        }
    }


    /**
     * GPS 종료
     * */
    public void stopUsingGPS(){
        if(locationManager != null){
            locationManager.removeUpdates(GPSTracker.this);
        }
    }


    /**
     * 위도값을 가져옵니다.
     * */
    public double getLatitude(){
        if(location != null){
            lat = location.getLatitude();
        }
        return lat;
    }

    public int getFindSat(){
        return findSat;
    }

    /**
     * 경도값을 가져옵니다.
     * */
    public double getLongitude(){
        if(location != null){
            lon = location.getLongitude();
        }
        return lon;
    }

    /**
     * GPS 나 wife 정보가 켜져있는지 확인합니다.
     * */
    public boolean isGetLocation() {
        return this.isGetLocation;
    }

    public boolean isGetGPSEnabled()
    {
        return this.isGPSEnabled;
    }
    public boolean isGetNetworkEnabled()
    {
        return this.isNetworkEnabled;
    }

    public int getCellID()
    {
        return cellID;
    }

    public int getLac()
    {
        return lac;
    }

    public Location getLocation4(){
        return location4;
    }

    /**
     * GPS 정보를 가져오지 못했을때
     * 설정값으로 갈지 물어보는 alert 창
     * */
    public void showSettingsAlert(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);

        alertDialog.setTitle("GPS 사용유무셋팅");
        alertDialog.setMessage("GPS 셋팅이 되지 않았을수도 있습니다. \n 설정창으로 가시겠습니까?");

        // OK 를 누르게 되면 설정창으로 이동합니다.
        alertDialog.setPositiveButton("Settings",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int which) {
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        mContext.startActivity(intent);
                    }
                });
        // Cancle 하면 종료 합니다.
        alertDialog.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

        alertDialog.show();
    }

    private int getGpsSatelliteCount(LocationManager loc)
    {
        @SuppressLint("MissingPermission") final GpsStatus gs = loc.getGpsStatus(null);

        int i = 0;
        int j = 0;
        final Iterator< GpsSatellite > it = gs.getSatellites().iterator();

        while(it.hasNext()) {
            GpsSatellite satellite = it.next();

            // [수정 : 2013/10/25]
            // 단순 위성 갯수가 아니라 사용할 수 있게 잡히는 위성의 갯수가 중요하다.
            if (satellite.usedInFix()) {
                j++; // i 값 보다는 이 값이 GPS 위성 사용 여부를 확인하는데 더 중요하다.
            }
            i++;
        }

        return j;
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }



    @Override
    public void onLocationChanged(Location location) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO Auto-generated method stub

    }



    @Override
    public void onProviderEnabled(String provider) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onProviderDisabled(String provider) {
        // TODO Auto-generated method stub

    }


}