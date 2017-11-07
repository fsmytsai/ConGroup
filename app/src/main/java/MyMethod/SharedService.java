package MyMethod;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import ViewModel.IdentityView;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by user on 2017/4/8.
 */

public class SharedService {
    public static SharedPreferences sp_httpData;
    public static String Cookie;
    public static IdentityView identityView;
    public static int toolBarHeight;

    public static OkHttpClient GetClient(Context context) {
//        return new OkHttpClient().newBuilder()
//                .addInterceptor(new AddHeaderInterceptor(context))
//                .build();
        return new OkHttpClient.Builder()
                .addInterceptor(new AddHeaderInterceptor(context))
                .connectTimeout(3, TimeUnit.SECONDS)
                .readTimeout(8, TimeUnit.SECONDS)
                .build();
    }

    private static class AddHeaderInterceptor implements Interceptor {
        private Context context;

        public AddHeaderInterceptor(Context mContext) {
            context = mContext;
        }

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            sp_httpData = context.getSharedPreferences("HttpData", context.MODE_PRIVATE);
            Cookie = sp_httpData.getString("Cookie", "");
            request = request.newBuilder().addHeader("Cookie", Cookie).build();
            return chain.proceed(request);
        }
    }

    public static boolean CheckNetWork(Context context) {
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isConnected() || !networkInfo.isAvailable()) {
            return false;
        }
        return true;
    }

    //開啟鍵盤
    public static void ShowKeyboard(Activity activity, View v) {
        v.requestFocus();
        InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.showSoftInput(v, 0);
    }

    //關閉鍵盤
    public static void HideKeyboard(Activity activity) {
        InputMethodManager inputMethodManager =
                (InputMethodManager) activity.getSystemService(
                        Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(
                activity.getCurrentFocus().getWindowToken(), 0);
    }

    //避免重複Toast
    private static Toast toast = null;

    public static void ShowTextToast(String msg, Context context) {
        if (toast == null) {
            toast = Toast.makeText(context, msg, Toast.LENGTH_SHORT);
        } else {
            toast.setText(msg);
        }
        toast.show();
    }

    public static void ShowErrorDialog(String msg, Context context) {
        new AlertDialog.Builder(context)
                .setTitle("錯誤訊息")
                .setMessage(msg)
                .setPositiveButton("知道了", null)
                .show();
    }

    public static void ShowErrorDialog(List<String> msgs, Context context) {
        String msg = "";
        for (int i = 0; i < msgs.size(); i++) {
            msg += msgs.get(i);
            if (i != msgs.size() - 1) {
                msg += "\n";
            }
        }
        new AlertDialog.Builder(context)
                .setTitle("錯誤訊息")
                .setMessage(msg)
                .setPositiveButton("知道了", null)
                .show();
    }
}
