package com.example.photogallery;

import android.app.Application;
import android.content.Context;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

public class MyApplication extends Application {
    private static Context sContext;

    @Override
    public void onCreate(){
        super.onCreate();
        sContext=getApplicationContext();
    }

    public static Context getContext(){
        return sContext;
    }

}
