package com.feximin.neodownloader;

/**
 * Created by Neo on 16/3/23.
 */
public interface IChecker {

    //是否已经开始下载了，下载了部分或者全部
    //下载的百分比
    BufferedInfo check(String url);

    //将信息持久化
    void buffer(BufferedInfo info);
}
