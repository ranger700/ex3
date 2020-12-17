package com.example.photogallery;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Base64;
import android.util.Log;
import android.os.Handler;
import android.os.Message;
import android.util.LruCache;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static android.content.Context.MODE_PRIVATE;

public class ThumbnailDownloader<T> extends HandlerThread {
    private static final String TAG="ThumbnailDownloader";
    private static final int MESSAGE_DOWNLOAD=0;
    private Boolean mHasQuit=false;
    private Handler mRequestHandler;
    private ConcurrentMap<T,String> mRequestMap=new ConcurrentHashMap<T, String>();
    private Handler mResponseHandler;
    private ThumbnailDownloadListener<T> mThumbnailDownloadListener;

    private int maxSize=(int)(Runtime.getRuntime().freeMemory()/2);
    private LruCache<String,Bitmap> mBitmapCache;


    public interface ThumbnailDownloadListener<T>{
        void onThumbnailDownloaded(T target,Bitmap thumbnail);
    }

    public void setThumbnailDownloadListener(ThumbnailDownloadListener<T> listener){
        mThumbnailDownloadListener=listener;
    }

    public ThumbnailDownloader(Handler responseHandler){
        super(TAG);
        mResponseHandler=responseHandler;
//        cacheDir= MyApplication.getContext().getCacheDir();
//        Log.i(TAG,cacheDir.toString());
    }

    @Override
    protected void onLooperPrepared(){
        mRequestHandler=new Handler() {
            @Override
            public void handleMessage(Message msg){
                if(msg.what==MESSAGE_DOWNLOAD){
                    T target=(T)msg.obj;
                    Log.i(TAG,"Got a request for URL: "+mRequestMap.get(target));
                    handleRequest(target);
                }
            }
        };
        mBitmapCache=new LruCache<String, Bitmap>(24*1024);
    }

    @Override
    public boolean quit(){
        mHasQuit=true;
        return super.quit();
    }
    public void queueThumbnail(T target,String url){
        Log.i(TAG,"Got a URL: "+url);
        
        if(url==null){
            mRequestMap.remove(target);
        }else{
            mRequestMap.put(target,url);
            mRequestHandler.obtainMessage(MESSAGE_DOWNLOAD,target)
                    .sendToTarget();
        }
    }

    public void clearQueue(){
        mResponseHandler.removeMessages(MESSAGE_DOWNLOAD);
        mRequestMap.clear();
    }

    private void handleRequest(final T target){
        try{
            final String url = mRequestMap.get(target);

            if(url==null){
                return;
            }

            Bitmap bitmap=mBitmapCache.get(url);
            if(bitmap!=null){
                Log.e(TAG,"从内存获取数据");
            }else {
                bitmap=getBitmap(url);
                if(bitmap!=null) {
                    Log.e(TAG,"从文件获取数据");
                }else {
                    byte[] bitmapBytes = new FlickrFetchr().getUrlBytes(url);
                    bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
                    Log.i(TAG, "从网络获取数据");
                    Log.i(TAG, maxSize + "");
                    mBitmapCache.put(url, bitmap);
                    saveBitmap(bitmap,url);
                }
            }

            final Bitmap fbitmap=bitmap;

            mResponseHandler.post(new Runnable() {
                public void run() {
                    if(mRequestMap.get(target)!=url||mHasQuit){
                        return;
                    }
                    mRequestMap.remove(target);
                    //要求fbitmap为final变量
                    mThumbnailDownloadListener.onThumbnailDownloaded(target,fbitmap);
                }
            });
        }catch (IOException ioe){
            Log.e(TAG,"Error downloading img",ioe);
        }
    }

    public void saveBitmap(Bitmap bitmap,String url){
        SharedPreferences.Editor editor=MyApplication.getContext().getSharedPreferences("data", MODE_PRIVATE).edit();
        Log.e(TAG,"已存入本地");

        ByteArrayOutputStream bos=new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG,50,bos);
        String base64=new String(android.util.Base64.encodeToString(bos.toByteArray(), Base64.DEFAULT));
//        Log.e(TAG,base64);
        editor.putString(getMD5(url),base64);
        editor.commit();

    }

    public Bitmap getBitmap(String url){
        SharedPreferences finder=MyApplication.getContext().getSharedPreferences("data", MODE_PRIVATE);
        String bt=finder.getString(getMD5(url),"");
//        if(bt==null){
//            Log.e(TAG,"没有");
//        }
//        Log.e(TAG,"调用");
        byte[] bis= Base64.decode(bt, Base64.DEFAULT);
//        ByteArrayInputStream bis=new ByteArrayInputStream(android.util.Base64.encode(bt.getBytes(), Base64.DEFAULT));
        Bitmap result=BitmapFactory.decodeByteArray(bis,0,bis.length);
        return result;
    }

    private static String getMD5(String url) {
        String result="";
        try {
            MessageDigest md=MessageDigest.getInstance("md5");
            md.update(url.getBytes());
            byte[] bytes = md.digest();
            StringBuilder sb=new StringBuilder();
            for(byte b:bytes){
                String str=Integer.toHexString(b&0xFF);
                if(str.length()==1){
                    sb.append("0");
                }
                sb.append(str);
            }
            result=sb.toString();
        }catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return result;
    }
}
