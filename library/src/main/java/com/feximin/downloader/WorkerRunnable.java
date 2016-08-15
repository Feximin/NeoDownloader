package com.feximin.downloader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Neo on 16/3/22.
 */
public class WorkerRunnable {

    public static enum Status{
        Pending, Running, Pause, Finish, Error
    }

    private Status mCurStatus;
    private Peanut mPeanut;

    private String mHttpUrl;
    private CacheInfo mCacheInfo;

    private DownloaderConfig mConfig;

    private int mCurProgress;                //当前下载进度的百分比

    public WorkerRunnable(String url, DownloaderConfig config){
        this.mHttpUrl = url;
        this.mConfig = config;
        this.mCacheInfo = config.checker.checkOut(url);
        if (mCacheInfo != null){
            this.mPeanut = new Peanut(url);
            this.mPeanut.setTotalSize(mCacheInfo.getTotalSize());
            File file = new File(mCacheInfo.getLocalFilePath());
            this.mPeanut.setCurSize((int) file.length());
            if (mPeanut.getCurPercent() == 100) {
                mPeanut.setCurStatus(WorkerRunnable.Status.Finish);
            }else{
                mPeanut.setCurStatus(WorkerRunnable.Status.Pause);
            }
            this.mPeanut.setDestFile(mCacheInfo.getLocalFilePath());
        }
        if (this.mPeanut == null) {
            this.mPeanut = config.producer.produce(url);
        }
    }

    public void pause(){
//        if (mCurStatus == Status.Pause) return;
        mCurStatus = Status.Pause;
        mPeanut.setCurStatus(mCurStatus);
        for (DownloadListener listener : Downloader.getInstance().mDownloadListenerSet) {
            listener.onPause(mHttpUrl);
        }
    }

    public Peanut getPeanut(){
        return mPeanut;
    }

    private Runnable mRunnable;
    public Runnable generateIfNull() {
        if (mRunnable == null) mRunnable = new BusinessRunnable();
        this.mCurStatus = Status.Pending;
        mPeanut.setCurStatus(mCurStatus);
        for (DownloadListener listener : Downloader.getInstance().mDownloadListenerSet) {
            listener.onPending(mHttpUrl);
        }

        return mRunnable;
    }

    public Runnable getRunnable(){
        return mRunnable;
    }


    private void  onProgress(int cur, int total){
        int percent = (int) (cur / (float)total * 100);
        mPeanut.setCurSize(cur);
        mPeanut.setTotalSize(total);
        if (mCurProgress != percent) {
            mCurProgress = percent;
            if (mCurProgress == 100){
                mCurStatus = Status.Finish;
                mPeanut.setCurStatus(mCurStatus);
            }
            for (DownloadListener listener : Downloader.getInstance().mDownloadListenerSet) {
                listener.onProgress(mHttpUrl, mCurProgress, cur, total);
            }
        }
    }

    private void onError(String errorInfo){
        mCurStatus = Status.Error;
        mPeanut.setCurStatus(mCurStatus);
        for (DownloadListener listener: Downloader.getInstance().mDownloadListenerSet){
            listener.onError(mHttpUrl, errorInfo);
        }
    }

    private void onStart(){
        mCurStatus = Status.Running;
        mPeanut.setCurStatus(mCurStatus);
        for (DownloadListener listener : Downloader.getInstance().mDownloadListenerSet){
            listener.onStart(mHttpUrl);
        }
    }

    private class BusinessRunnable implements Runnable{

        @Override
        public void run() {
            if (mCurStatus == Status.Pause) return;
            if (mCurStatus == Status.Running) return;

            onStart();
            HttpURLConnection connection = null;
            RandomAccessFile randomAccessFile = null;
            InputStream is = null;
            try {
                URL url = new URL(mHttpUrl);
                connection = (HttpURLConnection) url.openConnection();      //getContentLength是一个比较费时的操作n();
                connection.setConnectTimeout(5000);
//                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.11; rv:47.0) Gecko/20100101 Firefox/47.0");
//                connection.setRequestProperty("Connection", "keep-alive");
//                connection.setRequestProperty("Accept-Encoding", "identity");
//                connection.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.8,en-US;q=0.5,en;q=0.3");
                connection.connect();
                int latestFileSize = connection.getContentLength();         //获取到最新的文件的长度
                if (latestFileSize <= 0){
                    onError("invalid content length !!");
                    return;
                }
                if (mCurStatus != Status.Running) return;
                long startPosition = 0;
                if (mConfig.breakPointEnabled){           //如果允许断点续传
                    if (mCacheInfo != null){     //并且之前已经下载过了（包含下载完成、未完成）
                        int previousTotalSize = mCacheInfo.getTotalSize();
                        //这里只是简单的通过比较两个文件的大小是否相等，来判断是否是同一文件
                        if (latestFileSize == previousTotalSize){
                            String path = mCacheInfo.getLocalFilePath();
                            File file = new File(path);
                            if (file.exists()) {
                                mPeanut.setDestFile(path);
                                startPosition = file.length();
                                if (startPosition == latestFileSize){           //如果已经下载完了
                                    onProgress((int) startPosition, latestFileSize);
                                    return;
                                }else if (startPosition > 0 && startPosition < latestFileSize){      //
                                    randomAccessFile = new RandomAccessFile(file, "rwd");
                                    randomAccessFile.seek(startPosition);
                                }
                            }
                        }
                    }
                }
                if (randomAccessFile == null){          //表示之前没有下载过
//                  connection.setRequestMethod("GET");
                    // 设置范围，格式为Range：bytes x-y;
                    randomAccessFile = new RandomAccessFile(mPeanut.getDestFile(), "rwd");
                }else{          //如果不为空表示需要断点，则需要重新连接一下
                    connection.disconnect();
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setConnectTimeout(5000);
                    connection.setRequestProperty("Range", "bytes="+startPosition + "-");
                }

                is = connection.getInputStream();
                if (is == null){
                    onError("can not open input stream !!");
                    return;
                }
                if (mCacheInfo == null) mCacheInfo = new CacheInfo();
                mCacheInfo.setTotalSize(latestFileSize);
                mCacheInfo.setLocalFilePath(mPeanut.getDestFile());
                mCacheInfo.setHttpUrl(mHttpUrl);
                mConfig.checker.cache(mCacheInfo);
                byte[] buffer = new byte[4096];

                int length;
                long completeSize = startPosition;
                long lastTime = System.currentTimeMillis();
                onProgress((int) completeSize, latestFileSize);
                for (;;) {
                    if (mCurStatus != Status.Running) return;               //如果没有getContentLength的话 read是一个比较费时的操作ze);
                    length = is.read(buffer);
                    if (length < 0) break;
                    randomAccessFile.write(buffer, 0, length);
                    completeSize += length;
                    if (System.currentTimeMillis() - lastTime > 1000){              //超过1秒后才去通知更新
                        onProgress((int) completeSize, latestFileSize);
                        lastTime = System.currentTimeMillis();
                    }
                }
                if (completeSize == latestFileSize){
                    onProgress((int) completeSize, latestFileSize);
                }else{
                    onError("unknown error");
                }
            } catch (IOException e) {
                e.printStackTrace();
                onError("unknown error");
            } finally {
                try {
                    if (is != null) is.close();
                    if (connection != null) connection.disconnect();
                    if (randomAccessFile != null)randomAccessFile.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }


        //我了个去，还必须这么去获取文件长度，因为如果设置断点续传的话，首先要知道文件总长度，然后才能根据本地已下载的部分去设置断点续传的位置，很焦灼啊
        //有新的办法了，先把断点续传的位置设置上，connect之后，看是不
        //还是不行， 如果设置的Range大于的真正的contentLength 则返回的contentLength 为0，还是需要两次
//        public int getHttpContentLength(HttpURLConnection connection){
//            try{
//                connection.setConnectTimeout(5000);
//                connection.connect();
//                int latestFileSize = connection.getContentLength();         //获取到最新的文件的长度
//                return latestFileSize;
//            }catch (Exception e){
//                return 0;
//            }finally {
//                if (connection != null) connection.disconnect();
//            }
//        }

    }

}
