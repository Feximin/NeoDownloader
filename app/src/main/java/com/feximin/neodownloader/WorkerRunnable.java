package com.feximin.neodownloader;

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
        None, Pending, Running, Cancel, Finish, Error
    }

    private Status mCurStatus = Status.None;
    private Peanut mPeanut;

    private BufferedInfo mBufferedInfo;

    private DownloaderConfig mConfig;
    private List<DownloadListener> mDownloadListenerList = new ArrayList<>(1);

    public void addDownloadListener(DownloadListener listener){
        if (listener == null || mDownloadListenerList.contains(listener)) return;
        mDownloadListenerList.add(listener);
    }
    public WorkerRunnable(String url, DownloaderConfig config){
        this.mConfig = config;
        this.mPeanut = config.producer.produce(url);
        this.mBufferedInfo = config.checker.check(url);
    }

    public void cancel(){
        if (mCurStatus == Status.Cancel) return;
        mCurStatus = Status.Cancel;
        for (DownloadListener listener : mDownloadListenerList) {
            listener.onCancel(mPeanut.getUrl());
        }
    }

    public Status getCurStatus(){
        return mCurStatus;
    }

    private Runnable mRunnable;
    public Runnable run() {
        if (mRunnable == null){
            mCurStatus = Status.Pending;
            mRunnable = new BusinessRunnable(mPeanut.getUrl());
        }
        return mRunnable;
    }


    private int getPercent(int cur, int total){
        int percent = (int) (cur / (float)total * 100);
        return percent;
    }

    private void onProgress(int percent){
        for (DownloadListener listener : mDownloadListenerList){
            listener.onProgress(mPeanut, percent);
        }
    }

    private void onError(String errorInfo){
        mCurStatus = Status.Error;
        for (DownloadListener listener: mDownloadListenerList){
            listener.onError(mPeanut.getUrl(), errorInfo);
        }
    }

    private void onFinish(){
        mCurStatus = Status.Finish;
        for (DownloadListener listener : mDownloadListenerList){
            listener.onFinish(mPeanut);
        }
    }

    private class BusinessRunnable implements Runnable{
        private String httpUrl;
        public BusinessRunnable(String url){
            this.httpUrl = url;
        }

        @Override
        public void run() {
            if (mCurStatus == Status.Cancel) return;

            mCurStatus = Status.Running;

            HttpURLConnection connection = null;
            RandomAccessFile randomAccessFile = null;
            InputStream is = null;
            try {
                URL url = new URL(httpUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(5000);
                connection.connect();
                int latestFileSize = connection.getContentLength();         //获取到最新的文件的长度
                if (latestFileSize <= 0){
                    onError("invalid content length !!");
                    return;
                }
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
                                    onFinish();
                                    return;
                                }else{
                                    randomAccessFile = new RandomAccessFile(file, "rwd");
                                    randomAccessFile.seek(startPosition);
                                }
                            }
                        }
                    }
                }
                if (randomAccessFile == null){          //表示之前没有下载过
                    randomAccessFile = new RandomAccessFile(mPeanut.getDestFile(), "rwd");
                }
                if (mBufferedInfo == null) mBufferedInfo = new BufferedInfo();
                mBufferedInfo.setTotalSize(latestFileSize);
                mBufferedInfo.setLocalFilePath(mPeanut.getDestFile());
                mBufferedInfo.setHttpUrl(httpUrl);
                mConfig.checker.buffer(mBufferedInfo);

//                connection.setRequestMethod("GET");
                // 设置范围，格式为Range：bytes x-y;
                connection.setRequestProperty("Range", "bytes="+startPosition + "-");
                connection.connect();
                is = connection.getInputStream();
                if (is == null){
                    onError("can not open input stream !!");
                    return;
                }
                byte[] buffer = new byte[4096];

                int length;
                long completeSize = startPosition;
                long lastTime = System.currentTimeMillis();
                onProgress(getPercent((int) completeSize, latestFileSize));
                while ((length = is.read(buffer)) != -1) {
                    if (mCurStatus == Status.Cancel) return;
                    randomAccessFile.write(buffer, 0, length);
                    completeSize += length;
                    if (lastTime - System.currentTimeMillis() > 1000){              //超过1秒后才去通知更新
                        onProgress(getPercent((int) completeSize, latestFileSize));
                        lastTime = System.currentTimeMillis();
                    }
                }
                if (completeSize == latestFileSize){
                    onFinish();
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            BusinessRunnable that = (BusinessRunnable) o;

            return httpUrl != null ? httpUrl.equals(that.httpUrl) : that.httpUrl == null;

        }

        @Override
        public int hashCode() {
            return httpUrl != null ? httpUrl.hashCode() : 0;
        }
    }

}
