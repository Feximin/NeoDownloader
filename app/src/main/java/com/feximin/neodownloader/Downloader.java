package com.feximin.neodownloader;

import android.util.Log;

/**
 * Created by Neo on 16/3/22.
 */
public class Downloader {

    private final String TAG = "Downloader";
    private DownloaderConfig mConfig;
    private Engine mEngine;
    private Downloader(){
    }

    public void init(DownloaderConfig config){
        if (mConfig == null){
            synchronized (Downloader.class){
                if (mConfig == null){
                    this.mConfig = config;
                    this.mEngine = new Engine(mConfig);
                }
            }
        }else {
            Log.d(TAG, "config already init !!");
        }
    }

    private static final Downloader INSTANCE = new Downloader();
    public static Downloader getInstance(){
        return INSTANCE;
    }

    public void start(String url, DownloadListener listener){
        mEngine.start(url, listener);
    }

    public void cancel(String url){
        mEngine.cancel(url);
    }

}
