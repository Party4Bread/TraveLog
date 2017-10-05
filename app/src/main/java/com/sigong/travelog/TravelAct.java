package com.sigong.travelog;

import android.location.Location;

import java.util.Date;

/**
 * Created by solsa on 2017-10-05.
 */

public class TravelAct {
    public ActType actType=ActType.Comment;
    public String data="";
    public Date date = new Date();
    public Location location;
    public TravelAct(ActType actT,String data,Date date,Location location){
        this.actType=actT;
        this.data=data;
        this.date=date;
        this.location=location;
    }
}
