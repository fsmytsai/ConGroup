package com.tsai.congroup;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import MyMethod.FileChooser;
import MyMethod.SharedService;
import ViewModel.PostView;
import ViewModel.UploadImagesView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;

public class EditPostActivity extends MySharedActivity {
    private final int REQUEST_EXTERNAL_STORAGE = 18;

    private int postId;
    private String content;
    private EditText et_EditContent;


    private LinearLayout ll_PostImageList;
    private LinearLayout ll_PostImageListOuter;
    private TextView tv_locationName;

    private FileChooser fileChooser;
    private List<String> ImgNameList;
    private List<Integer> deletedImgNoList;
    private String locationName = "";
    private List<PostView.PostImage> postImages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_post);

        InitView(true, "編輯貼文");
        SetupUI(findViewById(R.id.activity_Outer));
        SetCache((int) Runtime.getRuntime().maxMemory() / 10);
        postId = getIntent().getIntExtra("PostId", -1);
        if (postId == -1) {
            SharedService.ShowTextToast("ERROR", this);
            finish();
        }

        initView();
    }

    private void initView() {
        ImgNameList = new ArrayList<>();
        deletedImgNoList = new ArrayList<>();
        et_EditContent = (EditText) findViewById(R.id.et_EditContent);
//        et_EditContent.setInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE);
//        et_EditContent.setSingleLine(false);
//        et_EditContent.setHorizontallyScrolling(false);
        content = getIntent().getStringExtra("Content");
        et_EditContent.setText(content);
        et_EditContent.setSelection(et_EditContent.length());

        ll_PostImageList = (LinearLayout) findViewById(R.id.ll_PostImageList);
        ll_PostImageListOuter = (LinearLayout) findViewById(R.id.ll_PostImageListOuter);
        ll_PostImageListOuter.setVisibility(View.GONE);
        tv_locationName = (TextView) findViewById(R.id.tv_LocationName);

        locationName = getIntent().getStringExtra("LocationName");
        if (locationName != null) {
            tv_locationName.setText(locationName);
        } else {
            tv_locationName.setVisibility(View.GONE);
        }


        String JsonPostImages = getIntent().getStringExtra("JsonPostImages");
        postImages = new Gson().fromJson(JsonPostImages, new TypeToken<List<PostView.PostImage>>() {
        }.getType());

        for (int i = 0; i < postImages.size(); i++) {
            final int ImgNo = postImages.get(i).ImgNo;
            final ImageView iv_PostImage = new ImageView(EditPostActivity.this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
            iv_PostImage.setAdjustViewBounds(true);
            iv_PostImage.setLayoutParams(params);
            ll_PostImageList.addView(iv_PostImage);
            ll_PostImageListOuter.setVisibility(View.VISIBLE);
            showImage(iv_PostImage, postImages.get(i).ImgName, "P");
            iv_PostImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    deletedImgNoList.add(ImgNo);
                    ll_PostImageList.removeView(iv_PostImage);
                    if (ll_PostImageList.getChildCount() == 0)
                        ll_PostImageListOuter.setVisibility(View.GONE);
//                    DeletePostImage(iv_PostImage, ImgNo);
                }
            });
        }

        ImageButton imageButton = new ImageButton(this);
        imageButton.setImageResource(R.drawable.send);
        imageButton.setBackgroundColor(ContextCompat.getColor(this, R.color.colorTransparent));
        imageButton.setAdjustViewBounds(true);
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Edit();
            }
        });
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(SharedService.toolBarHeight, SharedService.toolBarHeight);
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
        params.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        imageButton.setLayoutParams(params);
        rl_toolBar.addView(imageButton);
    }

    public void Edit() {
        final String EditContent = et_EditContent.getText().toString();
        if (!EditContent.trim().equals("")) {
            if (deletedImgNoList.size() > 0)
                DeletePostImages();
            else if (ImgNameList.size() > 0)
                CreatePostImage();
            else
                EditPostContent();
        } else {
            SharedService.ShowTextToast("請輸入內容", EditPostActivity.this);
        }
    }

    private void EditPostContent() {
        RequestBody formBody = new FormBody.Builder()
                .add("PostId", postId + "")
                .add("Content", et_EditContent.getText().toString())
                .build();

        Request request = new Request.Builder()
                .url(getString(R.string.BackEndPath) + "Api/PostApi/Edit")
                .put(formBody)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        SharedService.ShowTextToast("請檢察網路連線", EditPostActivity.this);
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final int StatusCode = response.code();
                final String ResMsg = response.body().string();
                runOnUiThread(new Runnable() {//这是Activity的方法，会在主线程执行任务
                    @Override
                    public void run() {
                        if (StatusCode == 200) {
                            SharedService.ShowTextToast("修改成功", EditPostActivity.this);
                            setResult(RESULT_OK, getIntent());
                            finish();
                        } else if (StatusCode == 400) {
                            SharedService.ShowErrorDialog(ResMsg, EditPostActivity.this);
                        } else {
                            SharedService.ShowTextToast("ERROR:" + StatusCode, EditPostActivity.this);
                        }
                    }
                });
            }
        });
    }

    public void AddImage(View v) {
        int permission = ActivityCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE);
        if (permission == PackageManager.PERMISSION_GRANTED) {
            fileChooser = new FileChooser(this);
            if (!fileChooser.showFileChooser("image/*", null, true, true)) {
                SharedService.ShowTextToast("您沒有適合的檔案選取器", this);
            }
        } else {
            ActivityCompat.requestPermissions(this, new String[]{READ_EXTERNAL_STORAGE}, REQUEST_EXTERNAL_STORAGE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    fileChooser = new FileChooser(this);
                    if (!fileChooser.showFileChooser("image/*", null, true, true)) {
                        SharedService.ShowTextToast("您沒有適合的檔案選取器", this);
                    }
                } else {
                    SharedService.ShowTextToast("您拒絕選取檔案", this);
                }
                return;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case FileChooser.ACTIVITY_FILE_CHOOSER:
                if (fileChooser.onActivityResult(requestCode, resultCode, data)) {
                    SharedService.ShowTextToast("圖片上傳中...", this);
                    File[] files = fileChooser.getChosenFiles();
                    UploadImages(files);
                }
                return;
        }
    }

    private void UploadImages(File[] files) {
        MultipartBody.Builder builder = new MultipartBody.Builder();
        builder.setType(MultipartBody.FORM);
        for (int i = 0; i < files.length; i++) {
            String FileName = files[i].getName();
            String[] Type = FileName.split(Pattern.quote("."));
            if (Type[Type.length - 1].equals("jpg"))
                Type[Type.length - 1] = "jpeg";
            builder.addFormDataPart("file[" + i + "]", files[i].getName(), RequestBody.create(MediaType.parse("image/" + Type[Type.length - 1]), files[i]));
        }
        RequestBody body = builder.build();
        Request request = new Request.Builder()
                .url(getString(R.string.BackEndPath) + "Api/PostImageApi/UploadImages")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        SharedService.ShowTextToast("請檢察網路連線", EditPostActivity.this);
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final int StatusCode = response.code();
                final String ResMsg = response.body().string();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (StatusCode == 200) {
                            SharedService.ShowTextToast("上傳成功", EditPostActivity.this);
                            Gson gson = new Gson();
                            UploadImagesView uploadImagesView = gson.fromJson(ResMsg, UploadImagesView.class);
                            String ErrorMessage = "";
                            for (int i = 0; i < uploadImagesView.IsSuccess.size(); i++) {
                                if (uploadImagesView.IsSuccess.get(i)) {
                                    final String ImgName = uploadImagesView.ImgName.get(i);
                                    ImgNameList.add(ImgName);
                                    final ImageView iv_PostImage = new ImageView(EditPostActivity.this);
                                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
                                    iv_PostImage.setAdjustViewBounds(true);
                                    iv_PostImage.setLayoutParams(params);
                                    ll_PostImageList.addView(iv_PostImage);
                                    ll_PostImageListOuter.setVisibility(View.VISIBLE);
                                    showImage(iv_PostImage, ImgName, "P");
                                    iv_PostImage.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            ImgNameList.remove(ImgName);
                                            ll_PostImageList.removeView(iv_PostImage);
                                            if (ll_PostImageList.getChildCount() == 0)
                                                ll_PostImageListOuter.setVisibility(View.GONE);
                                        }
                                    });
                                } else {
                                    ErrorMessage += uploadImagesView.ResStr.get(i);
                                    if (i != uploadImagesView.IsSuccess.size() - 1) {
                                        ErrorMessage += "\n";
                                    }
                                }
                            }
                            if (!ErrorMessage.equals("")) {
                                SharedService.ShowErrorDialog(ErrorMessage, EditPostActivity.this);
                            }
                        } else if (StatusCode == 400) {
                            SharedService.ShowErrorDialog(ResMsg, EditPostActivity.this);
                        }
                    }
                });

            }
        });
    }

    private void CreatePostImage() {
        FormBody.Builder builder = new FormBody.Builder();
        builder.add("PostId", postId + "");
        for (int i = 0; i < ImgNameList.size(); i++) {
            builder.add("ImgNameList[" + i + "]", ImgNameList.get(i));
        }
        RequestBody formBody = builder.build();

        Request request = new Request.Builder()
                .url(getString(R.string.BackEndPath) + "Api/PostApi/CreatePostImage")
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        SharedService.ShowTextToast("請檢察網路連線", EditPostActivity.this);
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final int StatusCode = response.code();
                final String ResMsg = response.body().string();
                runOnUiThread(new Runnable() {//这是Activity的方法，会在主线程执行任务
                    @Override
                    public void run() {
                        if (StatusCode == 200) {
                            EditPostContent();
                        } else if (StatusCode == 400) {
                            SharedService.ShowErrorDialog(ResMsg, EditPostActivity.this);
                        } else {
                            SharedService.ShowTextToast("ERROR:" + StatusCode, EditPostActivity.this);
                        }
                    }
                });
            }
        });
    }

    private void DeletePostImages() {
        String url = getString(R.string.BackEndPath) + "Api/PostApi/DeletePostImage?PostId=" + postId;
        for (int i = 0; i < deletedImgNoList.size(); i++) {
            url += "&ImgNoArr[" + i + "]=" + deletedImgNoList.get(i);
        }
        Request request = new Request.Builder()
                .url(url)
                .delete()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        SharedService.ShowTextToast("請檢察網路連線", EditPostActivity.this);
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final int StatusCode = response.code();
                final String ResMsg = response.body().string();
                runOnUiThread(new Runnable() {//这是Activity的方法，会在主线程执行任务
                    @Override
                    public void run() {
                        if (StatusCode == 200) {
                            if (ImgNameList.size() > 0) {
                                CreatePostImage();
                            } else
                                EditPostContent();
                        } else if (StatusCode == 400) {
                            SharedService.ShowErrorDialog(ResMsg, EditPostActivity.this);
                        } else {
                            SharedService.ShowTextToast("ERROR:" + StatusCode, EditPostActivity.this);
                        }
                    }
                });
            }
        });
    }
}
