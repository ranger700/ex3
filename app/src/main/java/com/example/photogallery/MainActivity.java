package com.example.photogallery;

import android.content.Context;

import androidx.fragment.app.Fragment;


public class MainActivity extends SingleFragmentActivity{

    @Override
    protected Fragment createFragment(){
        return PhotoGalleryFragment.newInstance();
    }
    
}