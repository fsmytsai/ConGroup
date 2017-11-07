package MyMethod;

import android.content.Context;

import com.tsai.congroup.R;

import java.util.List;

import microsoft.aspnet.signalr.client.Credentials;
import microsoft.aspnet.signalr.client.http.Request;
import okhttp3.Cookie;
import okhttp3.HttpUrl;

/**
 * Created by user on 2017/4/13.
 */

public class MyCookieCredentials implements Credentials {

    @Override
    public void prepareRequest(Request request) {
        String Cookie = SharedService.Cookie;
        request.addHeader("Cookie", Cookie);
    }
}
