package com.feximin.downloadersample;

import android.app.Activity;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.feximin.downloader.DownloadListener;
import com.feximin.downloader.Downloader;
import com.feximin.downloader.Peanut;
import com.feximin.downloader.WorkerRunnable;
import com.feximin.neodownloader.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Neo on 16/3/24.
 */
public class AdapterTest extends BaseAdapter {

    private List<Peanut> mData;
    private LayoutInflater mInflater;
    private Activity mActivity;
    public AdapterTest(Activity activity){
        this.mActivity = activity;
        this.mInflater = LayoutInflater.from(activity);
        this.mData = new ArrayList<>();
        this.mData.add(new Peanut( "http://shouji.360tpcdn.com/151030/20ef65b3ad426cbd4479b8a46769d8a7/com.nd.shihua_5.apk"));
        this.mData.add(new Peanut("http://shouji.360tpcdn.com/360sj/tpi/20120815/oms.Huahuikepu_1.apk"));
        this.mData.add(new Peanut("http://shouji.360tpcdn.com/360sjop/auto/20130513/com.zaide.happyflowers_6_123709.apk"));
        this.mData.add(new Peanut("http://shouji.360tpcdn.com/360sj/dev/20130509/com.tixa.zhongguohuahuimenhu_2_142102.apk"));
        this.mData.add(new Peanut("http://shouji.360tpcdn.com/141124/c4e3e62a14b69be456041ec75e56ee53/com.cutt.zhiyue.android.app332603_540.apk"));
        this.mData.add(new Peanut("http://shouji.360tpcdn.com/141124/c4e3e62a14b69be456041ec75e56ee53/com.cutt.zhiyue.android.app332603_540.apk"));
        this.mData.add(new Peanut("http://shouji.360tpcdn.com/140704/3ae8a53026a912c1c8b2f4405f8974fe/com.jh.APP112093.news_16.apk"));
        this.mData.add(new Peanut("http://shouji.360tpcdn.com/140410/75e6ff61198b121ee30730f9f069c6b1/com.flowerbaike_3.apk"));
        this.mData.add(new Peanut("http://shouji.360tpcdn.com/140410/75e6ff61198b121ee30730f9f069c6b1/com.flowerbaike_3.apk"));
        this.mData.add(new Peanut("http://shouji.360tpcdn.com/131208/0e216531a9579ecceb34748c2f11a6ae/com.books.flower_book_1.apk"));
        this.mData.add(new Peanut("http://shouji.360tpcdn.com/151015/be461320ea41430d34d116dca47047d2/com.nd.iflowerpot_38.apk"));
        this.mData.add(new Peanut("http://shouji.360tpcdn.com/151015/be461320ea41430d34d116dca47047d2/com.nd.iflowerpot_38.apk"));
        this.mData.add(new Peanut("http://shouji.360tpcdn.com/160119/a1bdec6d4adfaf3c8ea20fea177686d1/com.zskj.aynhh_1.apk"));
        this.mData.add(new Peanut("http://shouji.360tpcdn.com/360sj/tpi/20120814/bora.wallmar6b_4.apk"));
        this.mData.add(new Peanut("http://shouji.360tpcdn.com/360sj/tpi/20120814/bora.wallmar6b_4.apk"));
        this.mData.add(new Peanut("http://shouji.360tpcdn.com/160305/8ff7a82fb6d2225a80158585b00f6050/com.fengqiaoju.hua_95.apk"));
        this.mData.add(new Peanut("http://shouji.360tpcdn.com/360sj/tpi/20130515/com.example.client_ylhh_1.apk"));
        this.mData.add(new Peanut("http://shouji.360tpcdn.com/360sj/tpi/20130515/com.example.client_ylhh_1.apk"));
    }
    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public Peanut getItem(int position) {
        return mData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null){
            convertView = mInflater.inflate(R.layout.item, null);
        }
        final Peanut entity = getItem(position);
        final TextView txtTitle = (TextView) convertView.findViewById(R.id.txt_title);
        final Button butDown = (Button) convertView.findViewById(R.id.but_download);
        final ProgressBar progressBar = (ProgressBar) convertView.findViewById(R.id.progress_bar);
        txtTitle.setText(entity.getUrl());
        Pair<WorkerRunnable.Status, Integer> pair = Downloader.getInstance().getStatus(entity.getUrl());
        WorkerRunnable.Status status = null;
        if (pair != null) status = pair.first;
        if (status == null || status == WorkerRunnable.Status.None){
            butDown.setText("下载");
        }else if (status == WorkerRunnable.Status.Pending){
            butDown.setText("等待中");
        }else if (status == WorkerRunnable.Status.Running){
            butDown.setText("下载中");
            int progress = pair.second;
            progressBar.setProgress(progress);
        }else if (status == WorkerRunnable.Status.Finish){
            butDown.setText("安装");
        }else if (status == WorkerRunnable.Status.Error){
            butDown.setText("重试");
        }
        final WorkerRunnable.Status finalStatus = status;
        butDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (finalStatus == null || finalStatus == WorkerRunnable.Status.None || finalStatus == WorkerRunnable.Status.Error){
                    Downloader.getInstance().start(entity, new DownloadListener(){
                        @Override
                        public void onStart(Peanut peanut) {
                            notifyOnUiThread();
                        }

                        @Override
                        public void onProgress(Peanut peanut, final int percent) {
                            notifyOnUiThread();
                        }

                        @Override
                        public void onPause(String peanut) {
                            notifyOnUiThread();
                        }

                        @Override
                        public void onError(String url, String error) {
                            notifyOnUiThread();
                        }

                        @Override
                        public void onFinish(Peanut peanut) {
                            notifyOnUiThread();
                        }
                    });
                }else if (finalStatus == WorkerRunnable.Status.Pending || finalStatus == WorkerRunnable.Status.Running){
                    Downloader.getInstance().pause(entity);
                }else if (finalStatus == WorkerRunnable.Status.Finish){


                }
            }
        });
        return convertView;
    }

    private void notifyOnUiThread(){
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                notifyDataSetChanged();
            }
        });
    }

    private static class DownloadEntity{
        String url;
        boolean isDownloading;

        public DownloadEntity(String url) {
            this.url = url;
        }
    }
}
