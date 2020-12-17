package com.example.photogallery;

public class GalleryItem {
    private String mId;
    private String mUrl;

    public void setUrl(String url) {
        mUrl = url;
    }

    public void setId(String id) {
        mId = id;
    }

    public String getUrl() {
        return mUrl;
    }

    public String getId() {
        return mId;
    }

    @Override
    public String toString(){
        return mId;
    }
}
