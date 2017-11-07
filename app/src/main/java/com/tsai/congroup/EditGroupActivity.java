package com.tsai.congroup;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;

import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

import MyMethod.FileChooser;
import MyMethod.SharedService;
import ViewModel.GroupView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;

public class EditGroupActivity extends MySharedActivity {
    private GroupView groupView;
    String[] JoinTypes = {"公開加入", "申請加入"};
    String[] InviteTypes = {"公開邀請", "僅限管理員"};
    private EditText et_GName;
    private EditText et_GIntroduction;
    private Spinner sp_JoinType;
    private Spinner sp_InviteType;
    private ImageView iv_GImg;

    private final int REQUEST_EXTERNAL_STORAGE = 18;
    private FileChooser fileChooser;

    private String gImgName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_group);
        InitView(true, "編輯群組");

        String JsonGroupView = getIntent().getStringExtra("JsonGroupView");
        groupView = new Gson().fromJson(JsonGroupView, GroupView.class);
        initView();
    }

    private void initView() {
        et_GName = (EditText) findViewById(R.id.et_GName);
        et_GName.setText(groupView.Group.GName);
        et_GIntroduction = (EditText) findViewById(R.id.et_GIntroduction);
        et_GIntroduction.setText(groupView.Group.GIntroduction);
        sp_JoinType = (Spinner) findViewById(R.id.sp_JoinType);
        sp_InviteType = (Spinner) findViewById(R.id.sp_InviteType);

        ArrayAdapter<CharSequence> JoinTypesArrayAdapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, JoinTypes);
        JoinTypesArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp_JoinType.setAdapter(JoinTypesArrayAdapter);
        sp_JoinType.setSelection(groupView.Group.JoinType - 1);

        ArrayAdapter<CharSequence> InviteTypesArrayAdapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, InviteTypes);
        InviteTypesArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp_InviteType.setAdapter(InviteTypesArrayAdapter);
        sp_InviteType.setSelection(groupView.Group.InviteType - 1);

        iv_GImg = (ImageView) findViewById(R.id.iv_GImg);
        if (groupView.Group.GImgName != null)
            showImage(iv_GImg, groupView.Group.GImgName, "G");
        else
            iv_GImg.setImageResource(R.drawable.defaultgimg);
    }

    public void EditGroup(View v) {
        if (!gImgName.equals("")) {
            RequestBody formBody = new FormBody.Builder()
                    .add("GroupId", groupView.Group.GroupId + "")
                    .add("GImgName", gImgName)
                    .build();

            Request request = new Request.Builder()
                    .url(getString(R.string.BackEndPath) + "Api/GroupApi/EditGImgName")
                    .put(formBody)
                    .build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            SharedService.ShowTextToast("請檢察網路連線", EditGroupActivity.this);
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
                                EditGroupData();
                            } else if (StatusCode == 400) {
                                SharedService.ShowErrorDialog(ResMsg, EditGroupActivity.this);
                            } else {
                                SharedService.ShowTextToast("ERROR:" + StatusCode, EditGroupActivity.this);
                            }
                        }
                    });
                }
            });
        } else {
            EditGroupData();
        }
    }

    private void EditGroupData() {
        RequestBody formBody = new FormBody.Builder()
                .add("GroupId", groupView.Group.GroupId + "")
                .add("GName", et_GName.getText().toString())
                .add("GIntroduction", et_GIntroduction.getText().toString())
                .add("JoinType", sp_JoinType.getSelectedItemPosition() + 1 + "")
                .add("InviteType", sp_InviteType.getSelectedItemPosition() + 1 + "")
                .build();

        Request request = new Request.Builder()
                .url(getString(R.string.BackEndPath) + "Api/GroupApi/Edit")
                .put(formBody)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        SharedService.ShowTextToast("請檢察網路連線", EditGroupActivity.this);
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
                            SharedService.ShowTextToast("修改成功", EditGroupActivity.this);
                            setResult(RESULT_OK, getIntent());
                            finish();
                        } else if (StatusCode == 400) {
                            SharedService.ShowErrorDialog(ResMsg, EditGroupActivity.this);
                        } else {
                            SharedService.ShowTextToast("ERROR:" + StatusCode, EditGroupActivity.this);
                        }
                    }
                });
            }
        });
    }

    public void EditGImg(View view) {
        int permission = ActivityCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE);
        if (permission == PackageManager.PERMISSION_GRANTED) {
            fileChooser = new FileChooser(this);
            if (!fileChooser.showFileChooser("image/*", null, false, true)) {
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
                    if (!fileChooser.showFileChooser("image/*", null, false, true)) {
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
                    File[] files = fileChooser.getChosenFiles();
                    UploadGImg(files);
                }
                return;
        }
    }

    private void UploadGImg(File[] files) {
        SharedService.ShowTextToast("圖片上傳中...", this);

        MultipartBody.Builder builder = new MultipartBody.Builder();
        builder.setType(MultipartBody.FORM);
        String FileName = files[0].getName();
        String[] Type = FileName.split(Pattern.quote("."));
        if (Type[Type.length - 1].equals("jpg"))
            Type[Type.length - 1] = "jpeg";
        builder.addFormDataPart("file[0]", files[0].getName(), RequestBody.create(MediaType.parse("image/" + Type[Type.length - 1]), files[0]));


        RequestBody body = builder.build();
        Request request = new Request.Builder()
                .url(getString(R.string.BackEndPath) + "Api/GroupApi/UploadGImg")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        SharedService.ShowTextToast("請檢察網路連線", EditGroupActivity.this);
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
                            SharedService.ShowTextToast("上傳成功", EditGroupActivity.this);
                            gImgName = new Gson().fromJson(ResMsg, String.class);
                            showImage(iv_GImg, gImgName, "G");
                        } else if (StatusCode == 400) {
                            SharedService.ShowErrorDialog(ResMsg, EditGroupActivity.this);
                        } else {
                            SharedService.ShowTextToast(StatusCode + "", EditGroupActivity.this);
                        }
                    }
                });

            }
        });
    }
}
