package com.feximin.downloader;

import android.text.TextUtils;
import android.util.Pair;

import java.io.File;
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

    private QueueFullHandler mHandler;                  //任务队列如果已经满了，则抛出异常

    private DownloadListener mDefaultListener = new SimpleDownloadListener();

    public Engine(DownloaderConfig config){
        this.mConfig = config;
        this.mHandler = new QueueFullHandler();
        this.mWorkerRunnableMap = new HashMap<>();
        this.mQueue = new ArrayBlockingQueue<>(mConfig.maxQueueCount);
        this.mExecutor = new ThreadPoolExecutor(mConfig.maxThread, mConfig.maxThread, 30, TimeUnit.SECONDS, mQueue, mHandler);
    }

    //如果不存在就添加，如果已经存在就只添加listener
    public void start(String url, DownloadListener listener){
        if (listener == null) listener = mDefaultListener;
        if (TextUtils.isEmpty(url)) {
            listener.onError(url, "url is empty");
        }else {
            WorkerRunnable workerRunnable = mWorkerRunnableMap.get(url);
            if (workerRunnable != null){
                WorkerRunnable.Status status = workerRunnable.getCurStatus();
                //如果是Error， Pause，Finish 的话需要重新进入队列
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
                try {
                    mExecutor.execute(workerRunnable.run());
                }catch (QueueFullException e){
                    listener.onError(url, "the queue is full");
                }
            } else {                             //表示队列已满
                listener.onError(url, "the queue is full");
            }
        }
    }


    //把已经
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

    public void pause(String url){
        if (TextUtils.isEmpty(url)) return;
        WorkerRunnable workerRunnable = mWorkerRunnableMap.get(url);
        if (workerRunnable == null) return;
        workerRunnable.pause();
        mExecutor.remove(workerRunnable.run());
    }


    public void delete(String url, boolean deleteFile){
        pause(url);
        mWorkerRunnableMap.remove(url);
        if (deleteFile){
            WorkerRunnable runnable = mWorkerRunnableMap.get(url);
            if (runnable != null){
                Peanut peanut = runnable.getPeanut();
                if (peanut != null){
                    File file = new File(peanut.getDestFile());
                    file.delete();
                }
            }
        }
    }

    public Pair<WorkerRunnable.Status, Integer> getStatus(String url){
        WorkerRunnable workerRunnable = mWorkerRunnableMap.get(url);
        if (workerRunnable != null){
            WorkerRunnable.Status status = workerRunnable.getCurStatus();
            int progress = workerRunnable.getProgress();
            return new Pair<>(status, progress);
        }
        return  null;
    }

}
