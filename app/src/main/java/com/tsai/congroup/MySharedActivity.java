package com.tsai.congroup;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.support.v4.util.LruCache;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;

import MyMethod.SharedService;
import me.grantland.widget.AutofitTextView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MySharedActivity extends AppCompatActivity {
    public OkHttpClient client;
    public RelativeLayout rl_toolBar;
    public int myWidth;
    public Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        client = SharedService.GetClient(this);
        SetMyWidth();
    }

    private void SetMyWidth() {
        DisplayMetrics dm = new DisplayMetrics();
        this.getWindowManager().getDefaultDisplay().getMetrics(dm);
        myWidth = dm.widthPixels;
    }

    public void InitView(boolean HasBack, String Title) {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        //toolbar.setLogo(R.drawable.icon);  //左上方logo圖
        rl_toolBar = (RelativeLayout) findViewById(R.id.rl_ToolBar);
        rl_toolBar.removeAllViews();
        if (!Title.equals("")) {
            AutofitTextView tv_Title = new AutofitTextView(this);
            tv_Title.setText(Title);
            tv_Title.setTextColor(Color.parseColor("#FFFFFF"));
            tv_Title.setMaxLines(1);
            tv_Title.setTextSize(24.0f);
            tv_Title.setMaxTextSize(24.0f);
            tv_Title.setMinTextSize(16);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);

            if (HasBack) {
                params.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
                SetMyWidth();
                int LeftMargin = (int) (myWidth * (0.4 - Title.length() * 0.04));
                if (LeftMargin < 0)
                    LeftMargin = 0;
                params.leftMargin = LeftMargin;
            } else {
                params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
            }
            tv_Title.setLayoutParams(params);
            rl_toolBar.addView(tv_Title);
        }

        setSupportActionBar(toolbar);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);  //取消Toolbar的內建靠左title(像Actionbar的特性)
        actionBar.setDisplayHomeAsUpEnabled(HasBack);
    }

    private boolean isFirstSetupUi = true;
    public View activity_Outer;

    public void SetupUI(final View view) {
        if (isFirstSetupUi) {
            activity_Outer = view;
            isFirstSetupUi = false;
        }

        // Set up touch listener for non-text box views to hide keyboard.
        if (!(view instanceof EditText) && !(view instanceof Button) && !(view instanceof ImageButton)) {
            view.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    SharedService.HideKeyboard(MySharedActivity.this);
                    view.requestFocus();
                    return false;
                }
            });
        }

        //If a layout container, iterate over children and seed recursion.
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                View innerView = ((ViewGroup) view).getChildAt(i);
                SetupUI(innerView);
            }
        }
    }


    //cache
    public void SetCache(int Size) {
        cacheSizes = Size;
        mMemoryCaches = new LruCache<String, Bitmap>(cacheSizes) {
            @SuppressLint("NewApi")
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount();
            }
        };
    }

    private int cacheSizes = 0;

    private LruCache<String, Bitmap> mMemoryCaches;

    public Bitmap getBitmapFromLrucache(String ImgName) {
        return mMemoryCaches.get(ImgName);
    }

    public void addBitmapToLrucaches(String url, Bitmap bitmap) {
        if (getBitmapFromLrucache(url) == null) {
            mMemoryCaches.put(url, bitmap);
        }
    }

    public void clearLruCache() {
        if (mMemoryCaches != null) {
            if (mMemoryCaches.size() > 0) {
                mMemoryCaches.evictAll();
            }
        }
    }

    public void showImage(ImageView imageView, String ImgName, String Type) {

        Bitmap bitmap = null;
        if (mMemoryCaches != null)
            bitmap = getBitmapFromLrucache(ImgName);
        if (bitmap == null) {
            LoadImgByOkHttp(imageView, ImgName, getString(R.string.BackEndPath) + "Thumb" + Type + "Images/" + ImgName);
        } else if (imageView != null) {
            imageView.setImageBitmap(bitmap);
        }
    }

    public void showBigImage(ImageView imageView, String ImgName, String Type) {

        Bitmap bitmap = null;
        if (mMemoryCaches != null)
            bitmap = getBitmapFromLrucache(ImgName);
        if (bitmap == null) {
            LoadImgByOkHttp(imageView, ImgName, getString(R.string.BackEndPath) + Type + "Images/" + ImgName);
        } else if (imageView != null) {
            imageView.setImageBitmap(bitmap);
        }
    }

    public void LoadImgByOkHttp(final ImageView imageView, final String ImgName, final String url) {
        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        SharedService.ShowTextToast("網速過慢導致圖片載入失敗", MySharedActivity.this);
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                InputStream inputStream = response.body().byteStream();
                final Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (bitmap != null) {
                            if (mMemoryCaches != null)
                                addBitmapToLrucaches(ImgName, bitmap);
                            if (imageView != null)
                                imageView.setImageBitmap(bitmap);
                        }
                    }
                });
            }
        });
    }
}
