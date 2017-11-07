package com.tsai.congroup;


import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import MyMethod.SharedService;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


/**
 * A simple {@link Fragment} subclass.
 */
public class RegisterFragment extends Fragment {
    private EditText et_Account;
    private EditText et_Password;
    private EditText et_PasswordCheck;
    private EditText et_Name;
    private EditText et_Email;
    private String account;
    private String password;
    private String passwordCheck;
    private String email;
    private String name;

    private MainActivity mainActivity;

    public RegisterFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_register, container, false);
        mainActivity = (MainActivity) getActivity();
        initView(view);
        return view;
    }

    private void initView(View view) {
        et_Account = (EditText) view.findViewById(R.id.et_Account);
        et_Password = (EditText) view.findViewById(R.id.et_Password);
        et_PasswordCheck = (EditText) view.findViewById(R.id.et_PasswordCheck);
        et_Name = (EditText) view.findViewById(R.id.et_Name);
        et_Email = (EditText) view.findViewById(R.id.et_Email);
        view.findViewById(R.id.ib_Register).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Register(v);
            }
        });
    }


    public void Register(View v) {
        account = et_Account.getText().toString();
        password = et_Password.getText().toString();
        passwordCheck = et_PasswordCheck.getText().toString();
        email = et_Email.getText().toString();
        name = et_Name.getText().toString();

        if (account.equals(""))
            account = "a";

        RequestBody formBody = new FormBody.Builder()
                .add("newMember.Account", account)
                .add("newMember.Name", name)
                .add("newMember.Email", email)
                .add("Password", password)
                .add("PasswordCheck", passwordCheck)
                .build();

        Request request = new Request.Builder()
                .url(getString(R.string.BackEndPath) + "Api/MemberApi/Register")
                .post(formBody)
                .build();
        mainActivity.client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        SharedService.ShowTextToast("請檢察網路連線", getActivity());
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final int StatusCode = response.code();
                final String ResMsg = response.body().string();
                getActivity().runOnUiThread(new Runnable() {//这是Activity的方法，会在主线程执行任务
                    @Override
                    public void run() {

                        if (StatusCode == 200) {
                            new AlertDialog.Builder(getActivity())
                                    .setTitle("註冊成功")
                                    .setMessage("請前往信箱驗證您的Email")
                                    .setPositiveButton("知道了", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            mainActivity.CheckLogon();
                                        }
                                    }).show();
                        } else if (StatusCode == 400) {
                            List<String> ErrorMsgs = new ArrayList();
                            if (ResMsg.contains(",")) {
                                ErrorMsgs = Arrays.asList(ResMsg.split(","));
                            } else {
                                ErrorMsgs.add(ResMsg);
                            }
//                            Gson gson = new Gson();
//                            List<String> ErrorMsgs = gson.fromJson(ResMsg, new TypeToken<List<String>>() {
//                            }.getType());
                            SharedService.ShowErrorDialog(ErrorMsgs, getActivity());
                            et_Password.setText("");
                            et_PasswordCheck.setText("");
                        } else {
                            SharedService.ShowTextToast("ERROR:" + StatusCode, getActivity());
                        }
                    }
                });
            }
        });

    }
}
