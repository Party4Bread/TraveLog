package com.sigong.travelog;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.os.Handler;
import android.os.Process;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionButton;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class LogActivity extends FragmentActivity implements  OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener{


    GoogleApiClient mGoogleApiClient=null;
    GoogleMap mMap=null;
    boolean mLocationPermissionGranted =false;
    String TAG="HI";
    LocationRequest mLocationRequest;
    private Location mCurrentLocation;
    private static final String CAMERA_POSITION = "camera_position";
    private static final String LOCATION = "location";
    CameraPosition mCameraPosition;
    MapFragment mMapFragment;
    ArrayList<Location> locTracker = new ArrayList<Location>();
    ArrayList<TravelAct> actTracker = new ArrayList<TravelAct>();
    PolylineOptions cur = new PolylineOptions().geodesic(true);
    Polyline curPoly;
    LatLngBounds.Builder mLatLngBoundBuilder;
    Handler mZoomOutHandler = new Handler();
    Runnable mZoomOutRunnable;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState!=null){
            mCurrentLocation=savedInstanceState.getParcelable(LOCATION);
            mCameraPosition=savedInstanceState.getParcelable(CAMERA_POSITION);
        }
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_log);
        mLocationPermissionGranted=AskLocationPermission();
        mMapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mMapFragment.getMapAsync(this);
        mLatLngBoundBuilder=new LatLngBounds.Builder();

        File folder = new File(Environment.getExternalStorageDirectory() +
                File.separator + "TraveLog");
        boolean success = true;
        if (!folder.exists()) {
            success = folder.mkdirs();
        }
        buildGoogleApiClinet();
        mGoogleApiClient.connect();
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mMap==null)return;
                if(locTracker.size()==0)return;
                final String items[] = { "Comment", "Picture" };
                AlertDialog.Builder ab = new AlertDialog.Builder(LogActivity.this);
                ab.setTitle("Marker");
                ab.setSingleChoiceItems(items, 0,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // 각 리스트를 선택했을때
                                dialog.cancel();
                                switch (whichButton){
                                    case 0:
                                        AlertDialog.Builder builder = new AlertDialog.Builder(LogActivity.this);
                                        builder.setTitle("Comment");

                                        final EditText input = new EditText(LogActivity.this);
                                        input.setInputType(InputType.TYPE_CLASS_TEXT);
                                        builder.setView(input);

                                        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                actTracker.add(new TravelAct(ActType.Comment,input.getText().toString(),new Date(),mCurrentLocation));

                                                mMap.addMarker(new MarkerOptions()
                                                        .title(input.getText().toString())
                                                        .position(new LatLng(locTracker.get(locTracker.size()-1).getLatitude(),
                                                                locTracker.get(locTracker.size()-1).getLongitude())));
                                            }
                                        });
                                        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.cancel();
                                            }
                                        });

                                        builder.show();
                                        break;

                                    case 1:
                                        Intent pickPhoto = new Intent(Intent.ACTION_PICK,
                                                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                                        startActivityForResult(pickPhoto , 1);//one can be replaced with any action code
                                        break;
                                }
                            }
                });

                ab.show();
            }
        });

        mZoomOutRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    if(mMap==null)return;
                    if(locTracker.size()<2)return;
                    mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(mLatLngBoundBuilder.build(), 10));
                    mZoomOutHandler.postDelayed(mZoomOutRunnable,8000);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                }
            }
        };
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
        switch(requestCode) {
            case 1:
                if(resultCode == RESULT_OK){
                    Uri selectedImage = imageReturnedIntent.getData();
                    String filename=Environment.getExternalStorageDirectory().getPath()
                            +File.separatorChar+"TraveLog"+File.separatorChar+actTracker.size()+".pic";
                    savefile(selectedImage,filename);

                    actTracker.add(new TravelAct(ActType.Photo,filename,new Date(),mCurrentLocation));
                    try {
                        Bitmap bm = decodeFile(new File(filename));
                        //bm.re
                        mMap.addMarker(new MarkerOptions()
                                .icon(BitmapDescriptorFactory.fromBitmap(bm))
                                .position(new LatLng(locTracker.get(locTracker.size() - 1).getLatitude(),
                                        locTracker.get(locTracker.size() - 1).getLongitude())));
                        bm.recycle();
                    }
                    catch (Exception e){
                        e.printStackTrace();
                    }
                }
                break;
        }
    }

    Bitmap decodeFile(File f){

        try {
            //Decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            FileInputStream stream1=new FileInputStream(f);
            BitmapFactory.decodeStream(stream1,null,o);
            stream1.close();
            //Find the correct scale value. It should be the power of 2.
            // Set width/height of recreated image
            final int REQUIRED_SIZE=130;
            int width_tmp=o.outWidth, height_tmp=o.outHeight;
            int scale=1;
            while(true){
                if(width_tmp/2<REQUIRED_SIZE || height_tmp/2<REQUIRED_SIZE)
                    break;
                width_tmp/=2;
                height_tmp/=2;
                scale*=2;
            }

            //decode with current scale values
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize=scale;
            FileInputStream stream2=new FileInputStream(f);
            Bitmap bitmap=BitmapFactory.decodeStream(stream2, null, o2);
            stream2.close();
            return bitmap;

        } catch (FileNotFoundException e) {
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    void savefile(Uri sourceuri,String destinationFilename)
    {
        String sourceFilename= sourceuri.getPath();
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;

        try {
            bis = new BufferedInputStream(getContentResolver().openInputStream(sourceuri));
            bos = new BufferedOutputStream(new FileOutputStream(new File(destinationFilename), false));
            byte[] buf = new byte[1024];
            bis.read(buf);
            do {
                bos.write(buf);
            } while(bis.read(buf) != -1);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bis != null) bis.close();
                if (bos != null) bos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    @Override
    protected void onDestroy() {
        mZoomOutHandler.removeCallbacks(mZoomOutRunnable);
        TrackerDBHelper trackerDBHelper = new TrackerDBHelper(this,Environment.getExternalStorageDirectory().getPath()
                +File.separatorChar+"TraveLog"+File.separatorChar+"test.db",null,1);
        SQLiteDatabase tdb = trackerDBHelper.getWritableDatabase();
        ContentValues values;
        for(Location i : locTracker){
            values = new ContentValues();
            values.put("LAT", i.getLatitude());
            values.put("LNG", i.getLongitude());
            values.put("ACTDATA", "");
            values.put("RECTIME", i.getTime());
            tdb.insert("LocationTable",null,values);
            Log.i("debuggii",new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(i.getTime())));
        }
        for(TravelAct i : actTracker){
            values = new ContentValues();
            values.put("LAT", i.location.getLatitude());
            values.put("LNG", i.location.getLongitude());
            values.put("ACTDATA", (i.actType==ActType.Comment?"Text:":"Image:")+i.data);
            values.put("RECTIME", i.location.getTime());
            tdb.insert("LocationTable",null,values);
        }

        tdb.close();
        //tdb.insert()
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mGoogleApiClient.isConnected()){
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient,this);
        }
    }

    @Override
    protected void onResume() {
        if(mGoogleApiClient.isConnected()){
            getDevideLocation();
        }
        super.onResume();
    }

    protected synchronized void buildGoogleApiClinet(){
        if(mGoogleApiClient==null){
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .enableAutoManage(this,this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .addApi(Places.GEO_DATA_API)
                    .addApi(Places.PLACE_DETECTION_API)
                    .build();
        }
        createLocationRequest();
    }

    public static int checkSelfPermission(@NonNull Context context,@NonNull String permission){
        if(permission==null){
            throw new IllegalArgumentException("permission is null");
        }
        return context.checkPermission(permission, android.os.Process.myPid(), Process.myUid());
    }

    public static boolean checkPermissions(Activity activity,String permission){
        int permissionResult = ActivityCompat.checkSelfPermission(activity,permission);
        if (permissionResult == PackageManager.PERMISSION_GRANTED)return true;
        else return false;
    }

    public static boolean verifyPermission(int[] grantresults){
        if (grantresults.length < 1){
            return false;
        }
        for (int result : grantresults){
            if (result!=PackageManager.PERMISSION_GRANTED){
                return false;
            }
        }
        return true;
    }

    private boolean AskLocationPermission() {
        if (Build.VERSION.SDK_INT >= 23) {

            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    &&checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG,"Permission is granted");
                return true;
            }else{
                Log.v(TAG,"Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                return false;
            }
        }else{
            Toast.makeText(this, "Permission is Grant", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Permission is Grant ");
            return true;
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (Build.VERSION.SDK_INT >= 23) {
            if(verifyPermission(grantResults)){
                Log.v(TAG,"Permission: "+permissions[0]+ "was "+grantResults[0]);
                mLocationPermissionGranted=true;
            }
            else{
                showRequestAgainDialog();
                mLocationPermissionGranted=false;
            }
        }
    }

    private void showRequestAgainDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("꼭 필요한 권한입니다 설정에서 허용해주세요").create();
        builder.setPositiveButton("설정", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                try{
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            .setData(Uri.parse("package:"+getPackageName()));
                    startActivity(intent);
                }catch (ActivityNotFoundException e){
                    e.printStackTrace();
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS);
                    startActivity(intent);
                }
            }
        });
        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(1);
            }
        });
        builder.create();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        getDevideLocation();
        mMapFragment=(MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mMapFragment.getMapAsync(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState){
        if(mMap!=null){
            outState.putParcelable(CAMERA_POSITION,mMap.getCameraPosition());
            outState.putParcelable(LOCATION,mCurrentLocation);
            super.onSaveInstanceState(outState);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap=googleMap;
        updateMapUI();
        if(mCameraPosition!=null) {
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(mCameraPosition));
        }else if(mCurrentLocation != null){
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(mCurrentLocation.getLatitude(),
                            mCurrentLocation.getLongitude()),16));
        }else{
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(25.3,34.3),16));
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
        }
    }

    @SuppressWarnings("MissingPermission")
    private void updateMapUI() {
        if(mMap==null) return;;
        if(mLocationPermissionGranted){
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
            mMap.getUiSettings().setCompassEnabled(true);
        }else{
            mMap.setMyLocationEnabled(false);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
            mMap.getUiSettings().setCompassEnabled(false);
        }
    }

    private void createLocationRequest(){
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(500);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @SuppressWarnings("MissingPermission")
    private void getDevideLocation(){
        if(checkPermissions(this,Manifest.permission.ACCESS_FINE_LOCATION)){
            mLocationPermissionGranted=true;
            mCurrentLocation=LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,mLocationRequest,this);
        }else{
            AskLocationPermission();
        }
    }
    @Override
    public void onLocationChanged(Location location){
        mCurrentLocation = location;

        LatLng curLoc=new LatLng(mCurrentLocation.getLatitude(),
                mCurrentLocation.getLongitude());
        Log.i("debugging",String.valueOf(mCurrentLocation.getTime()));
        locTracker.add(mCurrentLocation);
        cur.add(curLoc);
        if(mMap==null)return;
        if(locTracker.size()<2) {

            return;
        }
        else if(locTracker.size()<3) {
            // Polylines are useful for marking paths and routes on the map.
            mMap.clear();
            curPoly = mMap.addPolyline(cur);
            for(int i = 0;i<actTracker.size();i++){
                TravelAct jj = actTracker.get(i);
                if(jj.actType==ActType.Comment) {
                    mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(jj.location.getLatitude(), jj.location.getLongitude()))
                            .title(jj.data));
                }
                else if(jj.actType==ActType.Photo)
                {
                    try {
                        mMap.addMarker(new MarkerOptions()
                                .position(new LatLng(jj.location.getLatitude(), jj.location.getLongitude()))
                                .icon(BitmapDescriptorFactory.fromBitmap(MediaStore.Images.Media.getBitmap(getContentResolver(), Uri.fromFile(new File(jj.data))))));
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                }
            }
        }
        else{
            curPoly.remove();
            curPoly = mMap.addPolyline(cur);
        }
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(curLoc,17));
        mLatLngBoundBuilder.include(curLoc);
        //if(mCurrentLocation.hasSpeed()) {
            mZoomOutHandler.removeCallbacks(mZoomOutRunnable);
            mZoomOutHandler.postDelayed(mZoomOutRunnable, 8000);
        //}

        /*
        for(int i = 0;i<actTracker.size();i++){
            TravelAct jj = actTracker.get(i);
            mMap.addMarker(new MarkerOptions()
            .position(new LatLng(jj.location.getLatitude(),jj.location.getLongitude()))
            .title(jj.data));
        }*/
    }
}
