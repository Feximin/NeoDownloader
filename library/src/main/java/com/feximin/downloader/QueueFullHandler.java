package com.feximin.downloader;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Created by Neo on 16/5/12.
 */

public  class QueueFullHandler implements RejectedExecutionHandler {
    /**
     * Creates an {@code AbortPolicy}.
     */
    public QueueFullHandler() {
    }

    /**
     * Always throws RejectedExecutionException.
     *
     * @param r the runnable task requested to be executed
     * @param e the executor attempting to execute this task
     * @throws RejectedExecutionException always
     */
    public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
        throw new QueueFullException("Task " + r.toString() +
                " rejected from " +
                e.toString());
    }
}
