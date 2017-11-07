package com.tsai.congroup;


import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import MyMethod.SharedService;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * A simple {@link Fragment} subclass.
 */
public class MySharedFragment extends Fragment {
    public OkHttpClient client;
    public OkHttpClient imageClient;
    public List<ImageView> wImageViewList;
    public List<String> loadingImgNameList;

    public MySharedFragment() {
        // Required empty public constructor
        wImageViewList = new ArrayList<>();
        loadingImgNameList = new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return null;
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

        Bitmap bitmap = getBitmapFromLrucache(ImgName);
        if (bitmap == null) {
            LoadImgByOkHttp(imageView, ImgName, getString(R.string.BackEndPath) + "Thumb" + Type + "Images/" + ImgName);
        } else if (imageView != null) {
            imageView.setImageBitmap(bitmap);
        }
    }

    public void showBigImage(ImageView imageView, String ImgName, String Type) {

        Bitmap bitmap = getBitmapFromLrucache(ImgName);
        if (bitmap == null) {
            LoadImgByOkHttp(imageView, ImgName, getString(R.string.BackEndPath) + Type + "Images/" + ImgName);
        } else if (imageView != null) {
            imageView.setImageBitmap(bitmap);
        }
    }

    public void LoadImgByOkHttp(final ImageView imageView, final String ImgName, final String url) {
        if (imageView != null) {
            for (ImageView mImgView : wImageViewList) {
                if (mImgView == imageView)
                    return;
            }
            wImageViewList.add(imageView);
        }

        for (String imgName : loadingImgNameList) {
            if (imgName.equals(ImgName))
                return;
        }
        loadingImgNameList.add(ImgName);

        Request request = new Request.Builder()
                .url(url)
                .build();

        imageClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                loadingImgNameList.remove(ImgName);
                for (int i = 0; i < wImageViewList.size(); i++) {
                    if (wImageViewList.get(i).getTag().equals(ImgName)) {
                        wImageViewList.remove(i);
                        i--;
//                        break;有可能有多個同名Img
                    }
                }
//                getActivity().runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        SharedService.ShowTextToast("請檢察網路連線", getActivity());
//                    }
//                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                InputStream inputStream = response.body().byteStream();
                final Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                try {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
//                            loadingImgNameList.remove(ImgName);
//                            if (bitmap != null) {
//                                addBitmapToLrucaches(ImgName, bitmap);
//                                if (imageView != null){
//                                    imageView.setImageBitmap(bitmap);
//
//                                }
//                                else {
//                                    for (int i = 0; i < wImageViewList.size(); i++) {
//                                        if (wImageViewList.get(i).getTag().equals(ImgName)) {
//                                            wImageViewList.get(i).setImageBitmap(bitmap);
//                                            wImageViewList.remove(i);
//                                            i--;
//                                        }
//                                    }
//                                }
//                            }
                            if (bitmap != null) {
                                addBitmapToLrucaches(ImgName, bitmap);
                                loadingImgNameList.remove(ImgName);
                                for (int i = 0; i < wImageViewList.size(); i++) {
                                    if (wImageViewList.get(i).getTag().equals(ImgName)) {
                                        wImageViewList.get(i).setImageBitmap(bitmap);
                                        wImageViewList.remove(i);
                                        i--;
                                    }
                                }
                            }
                            Log.d("loadingImgNameListSize:", loadingImgNameList.size() + "");
                            Log.d("wImageViewListSize:", wImageViewList.size() + "");
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        ClearAllImageRequest();
    }

    public void ClearAllImageRequest() {
        if (imageClient != null) {
            imageClient.dispatcher().cancelAll();
            wImageViewList = new ArrayList<>();
            loadingImgNameList = new ArrayList<>();
        }
    }
}
