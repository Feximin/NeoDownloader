package com.feximin.downloader;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Neo on 16/3/22.
 */
public class Downloader {

    private Engine mEngine;
    private DownloaderConfig mConfig;
    Set<DownloadListener> mDownloadListenerSet;
    private Downloader(){
    }

    public static void init(DownloaderConfig config){
        if (config == null){
            throw  new DownloaderException("config can not be null !!");
        }else {
            synchronized (Downloader.class){
                if (INSTANCE == null){
                    INSTANCE = new Downloader();
                    INSTANCE.mConfig = config;
                    INSTANCE.mEngine = new Engine(config);
                    INSTANCE.mDownloadListenerSet = new HashSet<>(1);
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

    public void start(String url ){
        mEngine.start(url);
    }

    public void pause(String url){
        mEngine.pause(url);
    }

    public void pauseAll(){
        mEngine.pauseAll();
    }

    public void clear(){
        mEngine.clear();
    }

    public void delete(String url, boolean deleteFile){
        mEngine.delete(url, deleteFile);
    }


    public Peanut getPeanut(String url){
        if (url == null) return null;
        WorkerRunnable runnable = mEngine.getWorkerRunnable(url);
        if (runnable != null){
            return runnable.getPeanut();
        }
        return null;
    }

    public Peanut getCachePeanut(String url){
        CacheInfo info = mConfig.checker.checkOut(url);
        Peanut peanut = null;
        if (info != null){
            peanut = new Peanut(url);
            peanut.setTotalSize(info.getTotalSize());
            File file = new File(info.getLocalFilePath());
            peanut.setCurSize((int) file.length());
            if (peanut.getCurPercent() == 100) {
                peanut.setCurStatus(WorkerRunnable.Status.Finish);
            }else{
                peanut.setCurStatus(WorkerRunnable.Status.Pause);
            }
            peanut.setDestFile(info.getLocalFilePath());
        }
        return peanut;
    }

//    public void removeDownloadListener(String url, DownloadListener listener){
//        if (url == null || listener == null) return;
//        WorkerRunnable runnable = mEngine.getWorkerRunnable(url);
//        if (runnable != null){
//            runnable.removeDownloadListener(listener);
//        }
//    }

    public void addDownloadListener(DownloadListener listener){
        mDownloadListenerSet.add(listener);
    }

    public void removeDownloadListener(DownloadListener listener){
        mDownloadListenerSet.remove(listener);
    }


}
