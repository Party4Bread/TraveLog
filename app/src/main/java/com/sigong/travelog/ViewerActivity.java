package com.sigong.travelog;

import android.app.Fragment;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.Environment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class ViewerActivity extends FragmentActivity implements OnMapReadyCallback {
    ArrayList<Location> loctracked = new ArrayList<Location>();
    private ArrayList<TravelAct> acttracked = new ArrayList<TravelAct>();
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
        while(c.isLast()==false){
            String data = c.getString(c.getColumnIndexOrThrow("DATA"));
            Log.d("viewer",data);
            if(data.length()==0){
                Location loc = new Location("dummy");
                loc.setTime(c.getLong(c.getColumnIndexOrThrow("RECTIME")));
                loc.setLatitude(c.getDouble(c.getColumnIndexOrThrow("LAT")));
                loc.setLongitude(c.getDouble(c.getColumnIndexOrThrow("LNG")));
                loctracked.add(loc);
            }
            else {
                Location loc = new Location("dummy");
                loc.setTime(c.getLong(c.getColumnIndexOrThrow("RECTIME")));
                loc.setLatitude(c.getDouble(c.getColumnIndexOrThrow("LAT")));
                loc.setLongitude(c.getDouble(c.getColumnIndexOrThrow("LNG")));
                acttracked.add(new TravelAct(
                   data.indexOf("TEXT:")==0?ActType.Comment:ActType.Photo,data.substring(data.indexOf(':')),new Date(c.getLong(3)),loc
                ));
            }
            c.moveToNext();
        }
        MapFragment mapFragment = (MapFragment) getFragmentManager()
            .findFragmentById(R.id.map1);
        mapFragment.getMapAsync(this);
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

    @Override
    public void onMapReady(GoogleMap mMap) {
        PolylineOptions options = new PolylineOptions();
        for(Location i : loctracked){
            options.add(new LatLng(i.getLatitude(),i.getLongitude()));
            mMap.addPolyline(options);
        }
    }
}
