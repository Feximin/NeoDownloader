//package com.feximin.downloader;
//
//import android.text.TextUtils;
//
//import java.io.File;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.concurrent.ArrayBlockingQueue;
//import java.util.concurrent.ThreadPoolExecutor;
//import java.util.concurrent.TimeUnit;
//
///**
// * Created by Neo on 16/3/23.
// */
//public class Engine_bal {
//    private DownloaderConfig mConfig;
//    private ThreadPoolExecutor mExecutor;
//    private ArrayBlockingQueue<Runnable> mQueue;
//    private Map<String, WorkerRunnable> mWorkerRunnableMap;
//
//    private DownloadListener DEFAULT_LISTENER = new SimpleDownloadListener();
//
//    Engine_bal(DownloaderConfig config){
//        this.mConfig = config;
//        this.mWorkerRunnableMap = new HashMap<>();
//        this.mQueue = new ArrayBlockingQueue<>(mConfig.maxQueueCount);
//        QueueFullHandler mHandler = new QueueFullHandler();   //任务队列如果已经满了，则抛出异常
//        this.mExecutor = new ThreadPoolExecutor(mConfig.maxThread, mConfig.maxThread, 30, TimeUnit.SECONDS, mQueue, mHandler);
//    }
//
//    //如果不存在就添加，如果已经存在就只添加listener
//    public void start(String url, DownloadListener  ){
//        if (listener == null) listener = DEFAULT_LISTENER;
//        if (TextUtils.isEmpty(url)) {
//            listener.onError(url, "url is empty");
//        }else {
//            WorkerRunnable workerRunnable = mWorkerRunnableMap.get(url);
//            if (workerRunnable != null){
//                WorkerRunnable.Status status = workerRunnable.getPeanut().getCurStatus();
//                //如果是Error， Pause，Finish 的话需要重新进入队列
//                if (status == WorkerRunnable.Status.Pending || status == WorkerRunnable.Status.Running){
//                    //第二次添加的时候，如果还是默认的listener就不add了
////                    workerRunnable.addDownloadListener(listener);
//                    return;
//                }
//            }
//            mWorkerRunnableMap.remove(url);
//            if (mQueue.size() < mConfig.maxQueueCount) {
//                if (workerRunnable == null) {
//                    workerRunnable = new WorkerRunnable(url, mConfig);
//                }else{
////                    workerRunnable = workerRunnable.transform();
//                }
////                workerRunnable.addDownloadListener(listener);
//                mWorkerRunnableMap.put(url, workerRunnable);
//                try {
//                    mExecutor.execute(workerRunnable.generateIfNull());
//                }catch (QueueFullException e){
//                    listener.onError(url, "the queue is full");
//                }
//            } else {                             //表示队列已满
//                listener.onError(url, "the queue is full");
//            }
//        }
//    }
//
//    private void error(String url, String info){
//        for (DownloadListener listener : Downloader.getInstance().mDownloadListenerSet){
//            listener.onError(url, info);
//        }
//    }
//
//    public void pause(String url){
//        if (TextUtils.isEmpty(url)) return;
//        WorkerRunnable workerRunnable = mWorkerRunnableMap.get(url);
//        if (workerRunnable == null) return;
//        workerRunnable.pause();
//        mExecutor.remove(workerRunnable.generateIfNull());
//    }
//
//
//    public void delete(String url, boolean deleteFile){
//        pause(url);
//        mWorkerRunnableMap.remove(url);
//        if (deleteFile){
//            WorkerRunnable runnable = mWorkerRunnableMap.get(url);
//            if (runnable != null){
//                Peanut peanut = runnable.getPeanut();
//                if (peanut != null){
//                    File file = new File(peanut.getDestFile());
//                    file.delete();
//                }
//            }
//        }
//    }
//
//
//    public WorkerRunnable getWorkerRunnable(String url){
//        return mWorkerRunnableMap.get(url);
//    }
//
//}
