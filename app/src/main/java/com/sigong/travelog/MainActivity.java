package com.sigong.travelog;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }
    public void StartLogger(View v){
        Intent intent = new Intent(this, LogActivity.class);
        startActivity(intent);
    }
    public void StartViewer(View v){
        Intent intent = new Intent(this, LogActivity.class);
        startActivity(intent);
    }

}
