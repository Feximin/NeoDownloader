package com.feximin.downloader;

/**
 * Created by Neo on 16/3/23.
 */
public interface DownloadListener {
    void onStart(Peanut peanut);
    void onProgress(Peanut peanut, int percent);
    void onPause(Peanut peanut);
    void onError(Peanut url, String error);
    void onPending(Peanut peanut);
}
