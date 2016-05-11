package com.feximin.downloader;

/**
 * Created by Neo on 16/3/23.
 */
public interface DownloadListener {
    void onStart(Peanut peanut);
    void onProgress(Peanut peanut, int percent);
    void onCancel(String peanut);
    void onError(String url, String error);
    void onFinish(Peanut peanut);
}
