package com.sigong.travelog;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class ViewerActivity extends AppCompatActivity {
    ArrayList<LatLng> loctracked = new ArrayList<LatLng>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viewer);
        TrackerDBHelper trackerDBHelper = new TrackerDBHelper(this, Environment.getExternalStorageDirectory().getPath()
                + File.separatorChar+"TraveLog"+File.separatorChar+"test.db",null,1);
        SQLiteDatabase tdb = trackerDBHelper.getReadableDatabase();
        ContentValues values;
        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                "_ID",
                "LAT",
                "LNG",
                "DATA",
                "RECTIME"
        };

        Cursor c = tdb.query(
            "LocationTable",// The table to query
            projection,// The columns to return
            null,// The columns for the WHERE clause
            null,// The values for the WHERE clause
            null,// don't group the rows
            null,// don't filter by row groups
            null// The sort order
        );
        c.moveToFirst();
        loctracked.add(new LatLng(c.getDouble(1),c.getDouble(2)));
        //c.;
        Log.i("d", (String.valueOf(c.getCount())));
        /*
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
