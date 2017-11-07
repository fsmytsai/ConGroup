package com.tsai.congroup;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import MyMethod.FileChooser;
import MyMethod.SharedService;
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

public class AddPostActivity extends MySharedActivity {
    private static final int ADDLOCATION_CODE = 20;
    private final int REQUEST_EXTERNAL_STORAGE = 18;

    private int groupId;

    private EditText et_AddPostContent;
    private LinearLayout ll_PostImageList;
    private LinearLayout ll_PostImageListOuter;
    private TextView tv_locationName;

    private FileChooser fileChooser;
    private List<String> ImgNameList;
    private String locationName = "";
    private String placeId = "";
    private int[] groupIdArr;
    private String[] gNameArr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_post);

        InitView(true, "");
        SetupUI(findViewById(R.id.activity_Outer));
        SetCache((int) Runtime.getRuntime().maxMemory() / 10);
        gNameArr = getIntent().getStringArrayExtra("GNameArr");
        groupIdArr = getIntent().getIntArrayExtra("GroupIdArr");
        groupId = getIntent().getIntExtra("groupId", -1);
        if (groupId != -1) {
            for (int i = 0; i < groupIdArr.length; i++) {
                if (groupIdArr[i] == groupId) {
                    String tempGName = gNameArr[i];
                    gNameArr[i] = gNameArr[0];
                    gNameArr[0] = tempGName;
                    groupIdArr[i] = groupIdArr[0];
                    groupIdArr[0] = groupId;
                    break;
                }
            }
        }
        initView();
    }

    private void initView() {
        ImgNameList = new ArrayList<>();
        et_AddPostContent = (EditText) findViewById(R.id.et_AddPostContent);
        ll_PostImageList = (LinearLayout) findViewById(R.id.ll_PostImageList);
        ll_PostImageListOuter = (LinearLayout) findViewById(R.id.ll_PostImageListOuter);
        ll_PostImageListOuter.setVisibility(View.GONE);
        tv_locationName = (TextView) findViewById(R.id.tv_LocationName);
        tv_locationName.setVisibility(View.GONE);

        final Spinner spinner = new Spinner(this);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
        params.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        spinner.setLayoutParams(params);
        rl_toolBar.addView(spinner);

        ArrayAdapter<CharSequence> arrayAdapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, gNameArr);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(arrayAdapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                groupId = groupIdArr[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        ImageButton imageButton = new ImageButton(this);
        imageButton.setImageResource(R.drawable.send);
        imageButton.setBackgroundColor(ContextCompat.getColor(this, R.color.colorTransparent));
        imageButton.setAdjustViewBounds(true);
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AddPost(v);
            }
        });
        params = new RelativeLayout.LayoutParams(SharedService.toolBarHeight, SharedService.toolBarHeight);
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
        params.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        imageButton.setLayoutParams(params);
        rl_toolBar.addView(imageButton);
    }

    public void AddPost(View v) {
        String Content = et_AddPostContent.getText().toString();
        if (!Content.trim().equals("")) {
            FormBody.Builder builder = new FormBody.Builder();
            builder.add("post.groupId", groupId + "")
                    .add("post.Content", Content);
            for (int i = 0; i < ImgNameList.size(); i++) {
                builder.add("ImgNameList[" + i + "]", ImgNameList.get(i));
            }
            if (!locationName.equals("")) {
                builder.add("LocationList[0].LocationName", locationName);
                builder.add("LocationList[0].PlaceId", placeId);
            }
            RequestBody formBody = builder.build();

            Request request = new Request.Builder()
                    .url(getString(R.string.BackEndPath) + "Api/PostApi/Add")
                    .post(formBody)
                    .build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            SharedService.ShowTextToast("請檢察網路連線", AddPostActivity.this);
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
                                SharedService.ShowTextToast("發文成功", AddPostActivity.this);
                                Gson gson = new Gson();
                                int PostId = gson.fromJson(ResMsg, Integer.class);
                                getIntent().putExtra("groupId", groupId);
                                getIntent().putExtra("PostId", PostId);
                                setResult(RESULT_OK, getIntent());
                                finish();

                            } else if (StatusCode == 400) {
                                SharedService.ShowErrorDialog(ResMsg, AddPostActivity.this);
                            } else {
                                SharedService.ShowTextToast(StatusCode + "", AddPostActivity.this);
                            }
                        }
                    });
                }
            });
        } else {
            SharedService.ShowTextToast("請輸入內容", AddPostActivity.this);
        }
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

    public void AddLocation(View v) {
//        Intent intent = new Intent(this, LocationActivity.class);
//        startActivityForResult(intent, ADDLOCATION_CODE);
        PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
        try {
            startActivityForResult(builder.build(this), ADDLOCATION_CODE);
        } catch (GooglePlayServicesRepairableException e) {
            e.printStackTrace();
        } catch (GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
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
            case ADDLOCATION_CODE:
                if (resultCode == RESULT_OK) {
                    Place place = PlacePicker.getPlace(this, data);
//                    String toastMsg = String.format("Place: %s", place.getName());
//                    Toast.makeText(this, toastMsg, Toast.LENGTH_LONG).show();
//                    locationName = data.getStringExtra("LocationName");
                    locationName = place.getName().toString();
//                    placeId = data.getStringExtra("PlaceId");
                    placeId = place.getId();
                    tv_locationName.setText(locationName);
                    tv_locationName.setVisibility(View.VISIBLE);
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
                        SharedService.ShowTextToast("請檢察網路連線", AddPostActivity.this);
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
                            SharedService.ShowTextToast("上傳成功", AddPostActivity.this);
                            Gson gson = new Gson();
                            UploadImagesView uploadImagesView = gson.fromJson(ResMsg, UploadImagesView.class);
                            String ErrorMessage = "";
                            for (int i = 0; i < uploadImagesView.IsSuccess.size(); i++) {
                                if (uploadImagesView.IsSuccess.get(i)) {
                                    final String ImgName = uploadImagesView.ImgName.get(i);
                                    ImgNameList.add(ImgName);
                                    final ImageView iv_PostImage = new ImageView(AddPostActivity.this);
                                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
                                    iv_PostImage.setAdjustViewBounds(true);
                                    iv_PostImage.setLayoutParams(params);
                                    ll_PostImageList.addView(iv_PostImage);
                                    ll_PostImageListOuter.setVisibility(View.VISIBLE);
                                    showImage(iv_PostImage, ImgName, "P");
//                                    LoadImgByOkHttp(iv_PostImage, uploadImagesView.ImgName.get(i));
                                    iv_PostImage.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            ll_PostImageList.removeView(iv_PostImage);
                                            ImgNameList.remove(ImgName);
                                            if (ImgNameList.size() == 0)
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
                                SharedService.ShowErrorDialog(ErrorMessage, AddPostActivity.this);
                            }
                        } else if (StatusCode == 400) {
                            SharedService.ShowErrorDialog(ResMsg, AddPostActivity.this);
                        }
                    }
                });

            }
        });
    }

//    public void LoadImgByOkHttp(final ImageView imageView, final String ImgName) {
//        Request request = new Request.Builder()
//                .url(getString(R.string.BackEndPath) + "/ThumbPImages/" + ImgName)
//                .build();
//
//        client.newCall(request).enqueue(new Callback() {
//            @Override
//            public void onFailure(Call call, IOException e) {
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        SharedService.ShowTextToast("請檢察網路連線", AddPostActivity.this);
//                    }
//                });
//            }
//
//
//            @Override
//            public void onResponse(Call call, Response response) throws IOException {
//                InputStream inputStream = response.body().byteStream();
//                final Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        if (bitmap != null) {
//                            addBitmapToLrucaches(ImgName, bitmap);
//                            imageView.setImageBitmap(bitmap);
//                        }
//                    }
//                });
//            }
//        });
//    }

    public void ClearLocation(View v) {
        locationName = "";
        placeId = "";
        tv_locationName.setText("");
        tv_locationName.setVisibility(View.GONE);
    }
}