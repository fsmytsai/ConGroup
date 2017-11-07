package com.tsai.congroup;

import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.google.gson.Gson;

import java.io.IOException;

import MyMethod.SharedService;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AddGroupActivity extends MySharedActivity {
    String[] GTypes = {"同好會", "朋友圈", "公司"};
    String[] JoinTypes = {"公開加入", "申請加入"};
    String[] InviteTypes = {"公開邀請", "僅限管理員"};
    private EditText et_GName;
    private EditText et_GIntroduction;
    private Spinner sp_GType;
    private Spinner sp_JoinType;
    private Spinner sp_InviteType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_group);
        InitView(true, "新增群組");
        initView();
    }

    private void initView() {
        et_GName = (EditText) findViewById(R.id.et_GName);
        et_GIntroduction = (EditText) findViewById(R.id.et_GIntroduction);
        sp_GType = (Spinner) findViewById(R.id.sp_GType);
        sp_JoinType = (Spinner) findViewById(R.id.sp_JoinType);
        sp_InviteType = (Spinner) findViewById(R.id.sp_InviteType);

        ArrayAdapter<CharSequence> GTypesArrayAdapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, GTypes);
        GTypesArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp_GType.setAdapter(GTypesArrayAdapter);

        ArrayAdapter<CharSequence> JoinTypesArrayAdapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, JoinTypes);
        JoinTypesArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp_JoinType.setAdapter(JoinTypesArrayAdapter);

        ArrayAdapter<CharSequence> InviteTypesArrayAdapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, InviteTypes);
        InviteTypesArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp_InviteType.setAdapter(InviteTypesArrayAdapter);
    }

    public void AddGroup(View v) {
        SharedService.HideKeyboard(this);
        RequestBody formBody = new FormBody.Builder()
                .add("GName", et_GName.getText().toString())
                .add("GIntroduction", et_GIntroduction.getText().toString())
                .add("GType", sp_GType.getSelectedItemPosition() + 1 + "")
                .add("JoinType", sp_JoinType.getSelectedItemPosition() + 1 + "")
                .add("InviteType", sp_InviteType.getSelectedItemPosition() + 1 + "")
                .build();

        Request request = new Request.Builder()
                .url(getString(R.string.BackEndPath) + "Api/GroupApi/Add")
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        SharedService.ShowTextToast("請檢察網路連線", AddGroupActivity.this);
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
                            getIntent().putExtra("GroupId", new Gson().fromJson(ResMsg, Integer.class));
                            setResult(RESULT_OK, getIntent());
                            finish();
                        } else if (StatusCode == 400) {
                            SharedService.ShowErrorDialog(ResMsg, AddGroupActivity.this);

                        } else {
                            SharedService.ShowTextToast(StatusCode + "", AddGroupActivity.this);
                        }
                    }
                });
            }
        });

    }
}
