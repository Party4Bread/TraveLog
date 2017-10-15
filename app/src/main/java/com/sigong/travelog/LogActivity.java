package com.sigong.travelog;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Process;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
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
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

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

        buildGoogleApiClinet();
        mGoogleApiClient.connect();
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mMap==null)return;
                actTracker.add(new TravelAct(ActType.Comment,"KIIIIKII",new Date(),mCurrentLocation));
                mMap.addMarker(new MarkerOptions()
                        .title("KIIIIKII")
                        .position(new LatLng(locTracker.get(locTracker.size()-1).getLatitude(),
                                locTracker.get(locTracker.size()-1).getLongitude())));
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
    protected void onDestroy() {
        mZoomOutHandler.removeCallbacks(mZoomOutRunnable);
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
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
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
                mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(jj.location.getLatitude(),jj.location.getLongitude()))
                        .title(jj.data));
            }
        }
        else{
            curPoly.remove();
            curPoly = mMap.addPolyline(cur);
        }
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(curLoc,17));
        mLatLngBoundBuilder.include(curLoc);
        mZoomOutHandler.removeCallbacks(mZoomOutRunnable);
        mZoomOutHandler.postDelayed(mZoomOutRunnable,8000);

        /*
        for(int i = 0;i<actTracker.size();i++){
            TravelAct jj = actTracker.get(i);
            mMap.addMarker(new MarkerOptions()
            .position(new LatLng(jj.location.getLatitude(),jj.location.getLongitude()))
            .title(jj.data));
        }*/
    }
}
