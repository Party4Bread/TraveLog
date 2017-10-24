package com.sigong.travelog;

import android.app.Fragment;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class ViewerActivity extends FragmentActivity implements OnMapReadyCallback {
    ArrayList<Location> loctracked = new ArrayList<Location>();
    private ArrayList<TravelAct> acttracked = new ArrayList<TravelAct>();
    private LatLngBounds.Builder mLatLngBoundBuilder;
    boolean ViewReady=false,MapReady=false;
    GoogleMap mMap;
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
        mLatLngBoundBuilder=new LatLngBounds.Builder();
        do{
            String data = c.getString(c.getColumnIndex("DATA"));
            Log.d("viewer",data);
            if(data.length()==0){
                Location loc = new Location("dummy");
                loc.setTime(c.getLong(c.getColumnIndex("RECTIME")));
                loc.setLatitude(c.getDouble(c.getColumnIndex("LAT")));
                loc.setLongitude(c.getDouble(c.getColumnIndex("LNG")));
                loctracked.add(loc);
            }
            else {
                Location loc = new Location("dummy");
                loc.setTime(c.getLong(c.getColumnIndex("RECTIME")));
                loc.setLatitude(c.getDouble(c.getColumnIndex("LAT")));
                loc.setLongitude(c.getDouble(c.getColumnIndex("LNG")));
                acttracked.add(new TravelAct(
                   data.indexOf("TEXT:")==0?ActType.Comment:ActType.Photo,data.substring(data.indexOf(':')),new Date(c.getLong(3)),loc
                ));


            }
        }while (c.moveToNext());
        MapFragment mapFragment = (MapFragment) getFragmentManager()
            .findFragmentById(R.id.map1);

        tdb.close();
        ViewReady=true;
        mapFragment.getMapAsync(this);
        Log.i("d", (String.valueOf(c.getCount())));
        setMap();
    }
    private  synchronized void setMap(){
        if((!MapReady||!ViewReady)) return;
        PolylineOptions options = new PolylineOptions();
        for(Location i : loctracked){
            options.add(new LatLng(i.getLatitude(),i.getLongitude()));
            mLatLngBoundBuilder.include(new LatLng(i.getLatitude(),i.getLongitude()));
        }
        mMap.addPolyline(options);
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(mLatLngBoundBuilder.build(), 10));
        for(TravelAct jj : acttracked){
            if(jj.actType==ActType.Comment) {
                mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(jj.location.getLatitude(), jj.location.getLongitude()))
                        .title(jj.data));
            }
            else if(jj.actType==ActType.Photo)
            {
                try {//// TODO: 2017-10-24 NO MARKER??? WHY?
                    mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(jj.location.getLatitude(), jj.location.getLongitude()))
                            .icon(BitmapDescriptorFactory.fromBitmap(MediaStore.Images.Media.getBitmap(getContentResolver(), Uri.fromFile(new File(jj.data))))));
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
    }
    @Override
    public void onMapReady(GoogleMap mMap) {
        this.mMap=mMap;
        this.mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                MapReady=true;
                setMap();
            }
        });
    }
}
