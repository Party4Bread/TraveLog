package com.sigong.travelog;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ViewerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viewer);
        TrackerDBHelper trackerDBHelper = new TrackerDBHelper(this, Environment.getExternalStorageDirectory().getPath()
                + File.separatorChar+"TraveLog"+File.separatorChar+"test.db",null,1);
        SQLiteDatabase tdb = trackerDBHelper.getReadableDatabase();
        ContentValues values;/*
        tdb.query("LocationTable");
        for(Location i : locTracker){
            values = new ContentValues();
            values.put("LAT", i.getLatitude());
            values.put("LNG", i.getLongitude());
            values.put("DATA", "");
            values.put("TIME", i.getElapsedRealtimeNanos());
            tdb.insert("LocationTable",null,values);
            Log.i("FFU",new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(i.getElapsedRealtimeNanos())));
        }
        for(TravelAct i : actTracker){
            values = new ContentValues();
            values.put("LAT", i.location.getLatitude());
            values.put("LNG", i.location.getLongitude());
            values.put("DATA", (i.actType==ActType.Comment?"Text:":"Image:")+i.data);
            values.put("TIME", i.location.getElapsedRealtimeNanos());
            tdb.insert("LocationTable",null,values);
        }
*/
        tdb.close();
    }
}
