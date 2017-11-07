package com.tsai.congroup;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

import MyMethod.FileChooser;
import MyMethod.SharedService;
import ViewModel.MemberView;
import ViewModel.PChatRoomView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;

public class ProFileActivity extends MySharedActivity {
    private ImageView iv_MImg;
    private TextView tv_MemberName;
    private TextView tv_Email;
    private TextView tv_IsOnLine;
    private TextView tv_LastOnTime;
    private TextView tv_Birthday;
    private String account;
    private MemberView memberView;
    private Button bt_EditMImg;

    private final int REQUEST_EXTERNAL_STORAGE = 18;
    private FileChooser fileChooser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pro_file);

        account = getIntent().getStringExtra("Account");
        InitView(true, "個人頁面");
        initView();
        GetMemberView();
    }

    @Override
    public Intent getSupportParentActivityIntent() {
        finish();
        return null;
    }

    private void initView() {
        iv_MImg = (ImageView) findViewById(R.id.iv_MImg);
        tv_MemberName = (TextView) findViewById(R.id.tv_MemberName);
        tv_Email = (TextView) findViewById(R.id.tv_Email);
        tv_Birthday = (TextView) findViewById(R.id.tv_Birthday);
        tv_IsOnLine = (TextView) findViewById(R.id.tv_IsOnLine);
        tv_LastOnTime = (TextView) findViewById(R.id.tv_LastOnTime);
        bt_EditMImg = (Button) findViewById(R.id.bt_EditMImg);

        if (account.equals(SharedService.identityView.Account)) {
            findViewById(R.id.ll_Connection).setVisibility(View.GONE);
            tv_IsOnLine.setVisibility(View.GONE);
            bt_EditMImg.setVisibility(View.VISIBLE);
        } else {
            tv_Email.setVisibility(View.GONE);
        }
    }

    private void GetMemberView() {
        Request request = new Request.Builder()
                .url(getString(R.string.BackEndPath) + "Api/MemberApi/GetMemberView?Account=" + account)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        SharedService.ShowTextToast("請檢察網路連線", ProFileActivity.this);
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
                            Gson gson = new Gson();
                            memberView = gson.fromJson(ResMsg, MemberView.class);
                            SetMemberView();
                        } else if (StatusCode == 400) {
                            SharedService.ShowErrorDialog(ResMsg, ProFileActivity.this);
                        } else {
                            SharedService.ShowTextToast("ERROR:" + StatusCode, ProFileActivity.this);
                        }
                    }
                });
            }
        });
    }

    private void SetMemberView() {
        if (memberView.MImgName != null) {
            showBigImage(iv_MImg, memberView.MImgName, "M");
        } else {
            iv_MImg.setImageResource(R.drawable.defaultmimg);
        }
        tv_MemberName.setText(memberView.Name);
        tv_Email.setText(memberView.Email);
        if (memberView.Birthday != null)
            tv_Birthday.setText(memberView.Birthday);
        else
            tv_Birthday.setText("生日尚未填寫");

        if (account.equals(SharedService.identityView.Account))
            SharedService.identityView.MImgName = memberView.MImgName;

        if (memberView.LastOnTime.equals("")) {
            tv_IsOnLine.setBackgroundColor(ContextCompat.getColor(this, R.color.colorOnLineGreen));
        } else {
            tv_IsOnLine.setBackgroundColor(ContextCompat.getColor(this, R.color.colorOffLineRed));
            tv_LastOnTime.setVisibility(View.VISIBLE);
            tv_LastOnTime.setText(memberView.LastOnTime);
        }
    }

//    public void LoadImgByOkHttp(final ImageView imageView, final String ImgName) {
//        Request request = new Request.Builder()
//                .url(getString(R.string.BackEndPath) + "MImages/" + ImgName)
//                .build();
//
//        client.newCall(request).enqueue(new Callback() {
//            @Override
//            public void onFailure(Call call, IOException e) {
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        SharedService.ShowTextToast("請檢察網路連線", ProFileActivity.this);
//                    }
//                });
//            }
//
//            @Override
//            public void onResponse(Call call, Response response) throws IOException {
//                InputStream inputStream = response.body().byteStream();
//                final Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        if (bitmap != null) {
//                            imageView.setImageBitmap(bitmap);
//                        }
//                    }
//                });
//            }
//        });
//    }

    public void Chat(View v) {
        Request request = new Request.Builder()
                .url(getString(R.string.BackEndPath) + "Api/PrivateMessageApi/GetChatRoom?Account=" + account)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        SharedService.ShowTextToast("請檢察網路連線", ProFileActivity.this);
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
                            Gson gson = new Gson();
                            PChatRoomView.ChatRooms chatRooms = gson.fromJson(ResMsg, PChatRoomView.ChatRooms.class);
                            Intent intent = new Intent(ProFileActivity.this, MessageActivity.class);
                            intent.putExtra("RoomId", chatRooms.RoomId);
                            intent.putExtra("Account", SharedService.identityView.Account);
                            intent.putExtra("ReceiveAccount", memberView.Account);
                            intent.putExtra("MemberName", memberView.Name);
                            intent.putExtra("Me", chatRooms.ReceiveAccount.equals(SharedService.identityView.Account));
                            startActivity(intent);
                        } else if (StatusCode == 400) {
                            SharedService.ShowErrorDialog(ResMsg, ProFileActivity.this);
                        } else {
                            SharedService.ShowTextToast("ERROR:" + StatusCode, ProFileActivity.this);
                        }
                    }
                });
            }
        });
    }

    public void VideoCall(View v) {
        new AlertDialog.Builder(this)
                .setTitle("提醒")
                .setMessage("本功能處於實驗性階段，請在網路訊號良好的情況下使用，否則可能會沒有畫面")
                .setPositiveButton("知道了", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(ProFileActivity.this, CallActivity.class);
                        intent.putExtra("IsCaller", true);
                        intent.putExtra("OtherAccount", memberView.Account);
                        startActivity(intent);
                    }
                })
                .show();
    }

    public void EditMImg(View v) {
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
                    UploadMImg(files);
                }
                return;
        }
    }

    private void UploadMImg(File[] files) {
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
                .url(getString(R.string.BackEndPath) + "Api/MemberApi/EditMImgName")
                .put(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        SharedService.ShowTextToast("請檢察網路連線", ProFileActivity.this);
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
                            SharedService.ShowTextToast("修改成功", ProFileActivity.this);
                            GetMemberView();
                        } else if (StatusCode == 400) {
                            SharedService.ShowErrorDialog(ResMsg, ProFileActivity.this);
                        } else {
                            SharedService.ShowTextToast(StatusCode + "", ProFileActivity.this);
                        }
                    }
                });

            }
        });
    }
}
