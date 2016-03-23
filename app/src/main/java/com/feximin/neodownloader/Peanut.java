package com.feximin.neodownloader;

/**
 * Created by Neo on 16/3/22.
 */
public class Peanut {

    private String url;
    private int totalSize;
    private int curPercent;
    private String destFile;        //

    public Peanut(String url) {
        this.url = url;
    }

    public String getUrl(){
        return url;
    }
    public int getTotalSize(){
        return totalSize;
    }
    public int getCurPencent(){
        return curPercent;
    }

    public int getCurPercent() {
        return curPercent;
    }

    public void setCurPercent(int curPercent) {
        this.curPercent = curPercent;
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
