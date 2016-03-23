package com.feximin.neodownloader;

import com.mianmian.guild.util.Tool;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by Neo on 16/3/23.
 */
public class Engine {
    private DownloaderConfig mConfig;
    private ThreadPoolExecutor mExecutor;
    private ArrayBlockingQueue<Runnable> mQueue;
    private Map<String, WorkerRunnable> mWorkerRunnableMap;
    private DownloadListener mDefaultListener = new SimpleDownloadListener();
    public Engine(DownloaderConfig config){
        this.mConfig = config;
        this.mWorkerRunnableMap = new HashMap<>();
        this.mQueue = new ArrayBlockingQueue<>(mConfig.maxQueueCount);
        this.mExecutor = new ThreadPoolExecutor(1, mConfig.maxThread, 30, TimeUnit.SECONDS, mQueue);
    }

    public void start(String url, DownloadListener listener){
        if (listener == null) listener = mDefaultListener;
        if (Tool.isEmpty(url)) {
            listener.onError(url, "url 为空");
        }else {
            if (mQueue.size() < mConfig.maxQueueCount) {
                WorkerRunnable workerRunnable = new WorkerRunnable(url, mConfig);
                workerRunnable.setDownloadListener(listener);
                mWorkerRunnableMap.put(url, workerRunnable);
                mExecutor.execute(workerRunnable.run());
            } else {                             //表示队列已满
                listener.onError(url, "队列已满");
            }
        }
    }

    public void cancel(String url){
        if (Tool.isEmpty(url)) return;
        WorkerRunnable workerRunnable = mWorkerRunnableMap.get(url);
        if (workerRunnable == null || workerRunnable.getCurStatus() == WorkerRunnable.Status.Cancel) return;
        workerRunnable.cancel();
        mExecutor.remove(workerRunnable.run());
    }
}
