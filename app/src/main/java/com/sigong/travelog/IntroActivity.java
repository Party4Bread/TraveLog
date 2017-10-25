package com.sigong.travelog;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

public class IntroActivity extends IntroBaseActivity {
    private TextView txtAndroid;
    private Animation anim;
    private Runnable mEndActivityRunnable;
    private Handler mEndActivityHandler = new Handler();
    private Animation.AnimationListener endAnimset;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);
        mEndActivityRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    startActivity(new Intent(IntroActivity.this,MainActivity.class));
                    finish();
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                }
            }
        };
        endAnimset=new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                animEnd();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        };
        initView1();
        initView2();
        initView3();
    }

    private void animEnd(){
        mEndActivityHandler.removeCallbacks(mEndActivityRunnable);
        mEndActivityHandler.postDelayed(mEndActivityRunnable,1000);
    }
    @Override
    protected void onDestroy(){
        mEndActivityHandler.removeCallbacks(mEndActivityRunnable);
        super.onDestroy();
    }
    private void initView1(){
        txtAndroid = (TextView)findViewById(R.id.text1);
        anim = AnimationUtils.loadAnimation(this,R.anim.loading);
        anim.setAnimationListener(endAnimset);
        txtAndroid.setAnimation(anim);
    }

    private void initView2(){
        txtAndroid = (TextView)findViewById(R.id.Trave);
        anim = AnimationUtils.loadAnimation(this,R.anim.traveloading);
        anim.setAnimationListener(endAnimset);
        txtAndroid.setAnimation(anim);
    }

    private void initView3(){
        txtAndroid = (TextView)findViewById(R.id.Log);
        anim = AnimationUtils.loadAnimation(this,R.anim.logloading);
        anim.setAnimationListener(endAnimset);
        txtAndroid.setAnimation(anim);
    }
}
