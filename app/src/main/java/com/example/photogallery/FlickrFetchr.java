package com.example.photogallery;

import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class FlickrFetchr {

    private static final String TAG="FlickrFetchr";
    private static final String API_KEY="rWVexWf8Bn7sy5wnt7pJ4fe78meIaTW5fySVzXbK";

    public byte[] getUrlBytes(String urlSpec) throws IOException{
        URL url=new URL(urlSpec);
        HttpURLConnection connection=(HttpURLConnection)url.openConnection();
        connection.setInstanceFollowRedirects(false);
        String redirect=connection.getHeaderField("Location");
        if(redirect!=null){
            connection=(HttpURLConnection)new URL(redirect).openConnection();
        }

        try{
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            InputStream in=connection.getInputStream();
            if(connection.getResponseCode()!=HttpURLConnection.HTTP_OK){
                throw new IOException(connection.getResponseMessage()+": with"+urlSpec);
            }
            int bytesRead=0;
            byte[] buffer=new byte[1024];
            while((bytesRead=in.read(buffer))>0){
                out.write(buffer,0,bytesRead);
            }
            out.close();
            return out.toByteArray();
        }finally {
            connection.disconnect();
        }
    }

    public String getUrlString(String urlSpec) throws IOException{
        return new String(getUrlBytes(urlSpec));
    }

    public List<GalleryItem> fetchItems(){
        List<GalleryItem> items=new ArrayList<>();
        try{
            String url= Uri.parse("https://api.nasa.gov/mars-photos/api/v1/rovers/curiosity/photos")
                    .buildUpon()
                    .appendQueryParameter("sol","1000")
                    .appendQueryParameter("api_key",API_KEY)
                    .appendQueryParameter("format","json")
                    .appendQueryParameter("nojsoncallback","1")
                    .appendQueryParameter("extras","url_s")
                    .build().toString();
            String jsonString=getUrlString(url);
            Log.i(TAG,"Received JSON: "+jsonString);
            JSONObject jsonBody=new JSONObject(jsonString);
            parseItems(items,jsonBody);
        }catch (IOException ioe){
            Log.e(TAG,"Failed to fetch items",ioe);
        }catch (JSONException je){
            Log.e(TAG,"Failed to parse JSON",je);
        }
        return items;
    }

    private void parseItems(List<GalleryItem> items,JSONObject jsonBody) throws IOException, JSONException{
        JSONArray photosJsonArray = jsonBody.getJSONArray("photos");

        for(int i=0;i<photosJsonArray.length();i++){
            JSONObject photoJsonObject = photosJsonArray.getJSONObject(i);

            GalleryItem item=new GalleryItem();
            item.setId(photoJsonObject.getString("id"));
            if(!photoJsonObject.has("img_src")){
                continue;
            }
            item.setUrl(photoJsonObject.getString("img_src"));
            items.add(item);
        }
    }
}
