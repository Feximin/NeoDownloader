package com.feximin.neodownloader;

import com.mianmian.guild.util.Tool;

import java.util.HashMap;
import java.util.Iterator;
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

    //如果不存在就添加，如果已经存在就只添加listener
    public void start(String url, DownloadListener listener){
        clear();
        if (listener == null) listener = mDefaultListener;
        if (Tool.isEmpty(url)) {
            listener.onError(url, "url is empty");
        }else {
            WorkerRunnable workerRunnable = mWorkerRunnableMap.get(url);
            if (workerRunnable != null){
                WorkerRunnable.Status status = workerRunnable.getCurStatus();
                if (status == WorkerRunnable.Status.Pending || status == WorkerRunnable.Status.Running){
                    //第二次添加的时候，如果还是默认的listener就不add了
                    workerRunnable.addDownloadListener(listener);
                    return;
                }
            }
            mWorkerRunnableMap.remove(url);
            if (mQueue.size() < mConfig.maxQueueCount) {
                workerRunnable = new WorkerRunnable(url, mConfig);
                workerRunnable.addDownloadListener(listener);
                mWorkerRunnableMap.put(url, workerRunnable);
                mExecutor.execute(workerRunnable.run());
            } else {                             //表示队列已满
                listener.onError(url, "the queue is full");
            }
        }
    }


    private void clear(){
        Iterator<Map.Entry<String , WorkerRunnable>> iterator = mWorkerRunnableMap.entrySet().iterator();
        while (iterator.hasNext()){
            WorkerRunnable runnable = iterator.next().getValue();
            boolean flag = true;
            if (runnable != null){
                WorkerRunnable.Status status = runnable.getCurStatus();
                flag = status != WorkerRunnable.Status.Pending && status != WorkerRunnable.Status.Running;
            }
            if (flag) iterator.remove();
        }
    }

    public void cancel(String url){
        clear();
        if (Tool.isEmpty(url)) return;
        WorkerRunnable workerRunnable = mWorkerRunnableMap.get(url);
        if (workerRunnable == null) return;
        workerRunnable.cancel();
        mWorkerRunnableMap.remove(url);
        mExecutor.remove(workerRunnable.run());
    }
}
