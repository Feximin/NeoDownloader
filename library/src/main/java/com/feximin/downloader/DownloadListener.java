package com.feximin.downloader;

/**
 * Created by Neo on 16/3/23.
 */
public interface DownloadListener {
    void onStart(String peanut);
    void onProgress(String peanut, int percent);
    void onPause(String peanut);
    void onError(String url, String error);
    void onPending(String peanut);
}
