package com.feximin.downloader;

/**
 * Created by Neo on 16/5/12.
 */
public class QueueFullException extends RuntimeException {
    public QueueFullException(String message){
        super(message);
    }
}
