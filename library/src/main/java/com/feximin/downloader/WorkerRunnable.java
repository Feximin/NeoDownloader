package com.feximin.downloader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Neo on 16/3/22.
 */
public class WorkerRunnable {

    public static enum Status{
        None, Pending, Running, Pause, Finish, Error
    }

    private Status mCurStatus = Status.None;
    private Peanut mPeanut;

    private String mHttpUrl;
    private BufferedInfo mBufferedInfo;

    private DownloaderConfig mConfig;
    private List<DownloadListener> mDownloadListenerList = new ArrayList<>(1);

    private int mCurProgress;                //当前下载进度的百分比

    public WorkerRunnable(String url, DownloaderConfig config){
        this.mHttpUrl = url;
        this.mConfig = config;
        this.mPeanut = config.producer.produce(url);
        this.mBufferedInfo = config.checker.check(url);
    }

    public void addDownloadListener(DownloadListener listener){
        if (listener == null) return;
        removeDownloadListener(listener);
        mDownloadListenerList.add(listener);
    }

    public void removeDownloadListener(DownloadListener listener){
        mDownloadListenerList.remove(listener);
    }
    public void pause(){
        if (mCurStatus == Status.Pause) return;
        mCurStatus = Status.Pause;
        for (DownloadListener listener : mDownloadListenerList) {
            listener.onPause(mHttpUrl);
        }
    }

    public Status getCurStatus(){
        return mCurStatus;
    }

    public Peanut getPeanut(){
        return mPeanut;
    }

    private Runnable mRunnable;
    public Runnable run() {
        if (mRunnable == null){
            this.mCurStatus = Status.Pending;
            mRunnable = new BusinessRunnable();
            for (DownloadListener listener : mDownloadListenerList) {
                listener.onPending(mHttpUrl);
            }
        }
        return mRunnable;
    }

    private void onProgress(int cur, int total){
        int percent = (int) (cur / (float)total * 100);
        onProgress(percent);
    }

    private void  onProgress(int percent){
        if (mCurProgress != percent) {
            mCurProgress = percent;
            if (mCurProgress == 100) mCurStatus = Status.Finish;
            for (DownloadListener listener : mDownloadListenerList) {
                listener.onProgress(mHttpUrl, mCurProgress);
            }
            if (mCurProgress == 100){
                mDownloadListenerList.clear();
            }
        }
    }

    public int getProgress(){
        return mCurProgress;
    }

    private void onError(String errorInfo){
        mCurStatus = Status.Error;
        for (DownloadListener listener: mDownloadListenerList){
            listener.onError(mHttpUrl, errorInfo);
        }
        mDownloadListenerList.clear();
    }

    private void onStart(){
        mCurStatus = Status.Running;
        for (DownloadListener listener : mDownloadListenerList){
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
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(5000);
                int latestFileSize = connection.getContentLength();         //获取到最新的文件的长度
                if (latestFileSize <= 0){
                    onError("invalid content length !!");
                    return;
                }
                if (mCurStatus != Status.Running) return;
                long startPosition = 0;
                if (mConfig.breakPointEnabled){           //如果允许断点续传
                    if (mBufferedInfo != null){     //并且之前已经下载过了（包含下载完成、未完成）
                        int previousTotalSize = mBufferedInfo.getTotalSize();
                        //这里只是简单的通过比较两个文件的大小是否相等，来判断是否是同一文件
                        if (latestFileSize == previousTotalSize){
                            String path = mBufferedInfo.getLocalFilePath();
                            File file = new File(path);
                            if (file.exists()) {
                                mPeanut.setDestFile(path);
                                startPosition = file.length();
                                if (startPosition == latestFileSize){           //如果已经下载完了
                                    onProgress(100);
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
                if (mBufferedInfo == null) mBufferedInfo = new BufferedInfo();
                mBufferedInfo.setTotalSize(latestFileSize);
                mBufferedInfo.setLocalFilePath(mPeanut.getDestFile());
                mBufferedInfo.setHttpUrl(mHttpUrl);
                mConfig.checker.buffer(mBufferedInfo);

                is = connection.getInputStream();
                if (is == null){
                    onError("can not open input stream !!");
                    return;
                }
                byte[] buffer = new byte[4096];

                int length;
                long completeSize = startPosition;
                long lastTime = System.currentTimeMillis();
                onProgress((int) completeSize, latestFileSize);
                while ((length = is.read(buffer)) != -1) {
                    if (mCurStatus == Status.Pause) return;
                    randomAccessFile.write(buffer, 0, length);
                    completeSize += length;
                    if (System.currentTimeMillis() - lastTime > 1000){              //超过1秒后才去通知更新
                        onProgress((int) completeSize, latestFileSize);
                        lastTime = System.currentTimeMillis();
                    }
                }
                if (completeSize == latestFileSize){
                    onProgress(100);
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
