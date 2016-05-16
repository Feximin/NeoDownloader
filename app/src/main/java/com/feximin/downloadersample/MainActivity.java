package com.feximin.downloadersample;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.widget.ListView;

import com.feximin.downloader.CacheInfo;
import com.feximin.downloader.Downloader;
import com.feximin.downloader.DownloaderConfig;
import com.feximin.downloader.ICache;
import com.feximin.downloader.IProducer;
import com.feximin.downloader.Peanut;
import com.feximin.neodownloader.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private ListView mListView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        DownloaderConfig config = new DownloaderConfig.Builder()
                .breakPoint(true)
                .cache(new ICache() {
                    @Override
                    public CacheInfo checkOut(String url) {
                        String md5 = url;
                        String json = sharedPreferences.getString(md5, null);
                        if (!TextUtils.isEmpty(json)){
                            JSONObject object = null;
                            try {
                                object = new JSONObject(json);
                                CacheInfo info = new CacheInfo();
                                info.setHttpUrl(url);
                                info.setLocalFilePath(object.optString("local_file_path"));
                                info.setTotalSize(object.optInt("total_size"));
                                return info;
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        return null;
                    }

                    @Override
                    public void cache(CacheInfo info) {
                        Map<String, Object> map = new HashMap<>();
                        map.put("local_file_path", info.getLocalFilePath());
                        map.put("total_size", info.getTotalSize());
                        map.put("http_url", info.getHttpUrl());
                        JSONObject object = new JSONObject(map);
                        String md5 = info.getHttpUrl();
                        sharedPreferences.edit().putString(md5, object.toString()).commit();
                    }
                })
                .maxQueueCount(16)
                .maxThread(4)
                .producer(new IProducer() {
                    @Override
                    public Peanut produce(String url) {
                        Peanut peanut = new Peanut(url);
                        String path = getCacheDir() + File.separator + url.substring(url.lastIndexOf("/")+1);
                        peanut.setDestFile(path);
                        return peanut;
                    }
                })
                .build();
        Downloader.init(config);
        this.mListView = (ListView) findViewById(R.id.list_view);
        this.mListView.setAdapter(new AdapterTest(this));
    }



}
