package com.TMDDataApp.crc_test;

import android.os.Build;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import au.com.bytecode.opencsv.CSVWriter;

public class CSVWrite {

    public CSVWrite() {}

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void writeCsv(List<String[]> data, String filename) {
        //파일 저장 경로 설정
        String dirPath = Environment.getExternalStorageDirectory().getAbsolutePath()+"/TMDData";
        //디렉토리 없으면 생성
        File dir = new File(dirPath);
        if(!dir.exists()){dir.mkdir();}



        try {
            CSVWriter cw = new CSVWriter(new FileWriter(dir + "/" + filename), ',',  '"');
            Iterator<String[]> it = data.iterator();
            try {
                while (it.hasNext()) {
                    String[] s = (String[]) it.next();
                    cw.writeNext(s);
                }
            } finally {
                cw.close();
            }
        } catch (IOException e) {
            Log.e("LOG", "Can't Save");
            e.printStackTrace();
        }
    }
}
