package com.feximin.downloader;

/**
 * Created by Neo on 16/3/22.
 */
public class Peanut {

    public String url;
    private int totalSize;
    private String destFile;        //
    private int curSize;
    private WorkerRunnable.Status curStatus;

    public Peanut(String url) {
        this.url = url;
    }

    public String getUrl(){
        return url;
    }
    public int getTotalSize(){
        return totalSize;
    }
    public int getCurPercent(){
        int percent = (int) ((float)curSize/(float)totalSize * 100) ;
        return percent;
    }


    public String getDestFile() {
        return destFile;
    }

    public void setDestFile(String destFile) {
        this.destFile = destFile;
    }

    public void setTotalSize(int totalSize) {
        this.totalSize = totalSize;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getCurSize() {
        return curSize;
    }

    public void setCurSize(int curSize) {
        this.curSize = curSize;
    }

    public WorkerRunnable.Status getCurStatus() {
        return curStatus;
    }

    public void setCurStatus(WorkerRunnable.Status curStatus) {
        this.curStatus = curStatus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Peanut peanut = (Peanut) o;

        return url != null ? url.equals(peanut.url) : peanut.url == null;

    }

    @Override
    public int hashCode() {
        return url != null ? url.hashCode() : 0;
    }
}
