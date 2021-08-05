package com.ecsoftlzx.somusic.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class InternetFileGetService {
    public static InputStream getInternetFile(String url, int timeout) throws IOException {
        URL rsUrl = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) rsUrl.openConnection();
        conn.setConnectTimeout(timeout);
        conn.setRequestMethod("GET");
        if (conn.getResponseCode() != 200){
            throw new RuntimeException("Error to get Internet file！");
        }
        return conn.getInputStream();
    }
}
