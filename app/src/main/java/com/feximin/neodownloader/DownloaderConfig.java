package com.feximin.neodownloader;

/**
 * Created by Neo on 16/3/23.
 */
public class DownloaderConfig {

    private DownloaderConfig(){}
    IChecker checker;
    IProducer producer;
    boolean breakPointEnabled;      //是否支持断点续传
    int maxThread;                  //最多同时执行的数量线程数
    int maxQueueCount;              //队列中最大正在等待的数量



    public static class Builder{
        IChecker checker;
        boolean breakPointEnabled = true;
        int maxThread = Runtime.getRuntime().availableProcessors();
        int maxQueueCount = 10;
        IProducer producer;
        public Builder bufferChecker(IChecker checker){
            this.checker = checker;
            return this;
        }

        public Builder breakPoint(boolean b){
            this.breakPointEnabled = b;
            return this;
        }

        public Builder maxThread(int max){
            this.maxThread = max;
            return this;
        }

        public Builder maxQueueCount(int max){
            this.maxQueueCount = max;
            return this;
        }

        public Builder producer(IProducer producer){
            this.producer = producer;
            return this;
        }

        public DownloaderConfig build(){
            DownloaderConfig config = new DownloaderConfig();
            config.checker = this.checker;
            config.breakPointEnabled = this.breakPointEnabled;
            config.maxThread = this.maxThread;
            config.maxQueueCount = this.maxQueueCount;
            config.producer = this.producer;
            return config;
        }
    }
}
