package com.feximin.downloader;

import android.util.Pair;

/**
 * Created by Neo on 16/3/22.
 */
public class Downloader {

    private Engine mEngine;
    private Downloader(){
    }

    public static void init(DownloaderConfig config){
        if (config == null){
            throw  new DownloaderException("config can not be null !!");
        }else {
            synchronized (Downloader.class){
                if (INSTANCE == null){
                    INSTANCE = new Downloader();
                    INSTANCE.mEngine = new Engine(config);
                }
            }
        }
    }

    private static Downloader INSTANCE;
    public static Downloader getInstance(){
        if (INSTANCE == null){
            throw new DownloaderException("please call init before getInstance !!");
        }
        return INSTANCE;
    }

    public void start(String url, DownloadListener listener){
        mEngine.start(url, listener);
    }

    public void pause(String url){
        mEngine.pause(url);
    }


    public void delete(String url, boolean deleteFile){
        mEngine.delete(url, deleteFile);
    }

    public Pair<WorkerRunnable.Status, Integer> getStatus(String url){
        return mEngine.getStatus(url);
    }


}
