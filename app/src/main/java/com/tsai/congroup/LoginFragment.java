package com.tsai.congroup;


import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import MyMethod.CheckInput;
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
public class LoginFragment extends Fragment implements GoogleApiClient.OnConnectionFailedListener {

    private static final int GOOGLESIGNIN_CODE = 87;
    private EditText et_Account;
    private EditText et_Password;
    private String account;
    private String password;
    private MainActivity mainActivity;
    private GoogleApiClient mGoogleApiClient;
    private CallbackManager callbackManager;

    public LoginFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_login, container, false);
        findViews(view);
        mainActivity = (MainActivity) getActivity();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.AndroidGoogleClientId))
                .requestEmail()
                .build();
        mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                .enableAutoManage(getActivity() /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();
        return view;
    }

    private void findViews(View view) {
        et_Account = (EditText) view.findViewById(R.id.et_Account);
        et_Account.setText(SharedService.sp_httpData.getString("Account", ""));
        et_Password = (EditText) view.findViewById(R.id.et_Password);

        view.findViewById(R.id.ib_Login).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Login();
            }
        });

        view.findViewById(R.id.ib_GoogleLogin).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GoogleSignIn();
            }
        });

        ImageButton ib_FBLogin = (ImageButton) view.findViewById(R.id.ib_FBLogin);
        ib_FBLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FBLogin();
            }
        });
        SetFBCallBack();
    }

    public void Login() {
        account = et_Account.getText().toString();
        password = et_Password.getText().toString();
        SharedService.HideKeyboard(getActivity());
        RequestBody formBody = new FormBody.Builder()
                .add("Account", account)
                .add("Password", password)
                .add("CaptchaCode", "c8763")
                .build();

        Request request = new Request.Builder()
                .url(getString(R.string.BackEndPath) + "Api/MemberApi/Login")
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

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (StatusCode == 200) {
                            Gson gson = new Gson();
                            String Cookie = gson.fromJson(ResMsg, String.class);
                            SharedService.sp_httpData.edit()
                                    .putString("Cookie", "ConGroupMember=" + Cookie)
                                    .putString("Account", account)
                                    .apply();
                            mainActivity.CheckLogon();
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
                            if (ErrorMsgs.contains("未通過Email驗證，請至信箱收取驗證信。")) {
                                final View EditCommentView = LayoutInflater.from(getActivity()).inflate(R.layout.editcomment_view, null);
                                final EditText et_EditContent = (EditText) EditCommentView.findViewById(R.id.et_EditContent);
                                new AlertDialog.Builder(getActivity())
                                        .setView(EditCommentView)
                                        .setTitle("重寄驗證信或輸入驗證碼")
                                        .setNegativeButton("取消", null)
                                        .setNeutralButton("重寄", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                SendEmail(account);
                                                et_Password.setText("");
                                            }
                                        })
                                        .setPositiveButton("驗證", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                EmailValidate(account, et_EditContent.getText().toString());
                                            }
                                        }).show();
                            } else {
                                SharedService.ShowErrorDialog(ErrorMsgs, getActivity());
                                et_Password.setText("");
                            }
                        } else {
                            SharedService.ShowTextToast("ERROR:" + StatusCode, getActivity());
                        }
                    }
                });
            }
        });

    }

    public void SendEmail(String Account) {
        Request request = new Request.Builder()
                .url(getString(R.string.BackEndPath) + "Api/MemberApi/SendEmail?Account=" + Account)
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

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (StatusCode == 200) {
                            SharedService.ShowTextToast("驗證信已寄出", getActivity());
                        } else {
                            SharedService.ShowTextToast("ERROR:" + StatusCode, getActivity());
                        }
                    }
                });
            }
        });

    }

    private void EmailValidate(String Account, String AuthCode) {
        Request request = new Request.Builder()
                .url(getString(R.string.BackEndPath) + "Api/MemberApi/EmailValidate?Account=" + Account + "&AuthCode=" + AuthCode)
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

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (StatusCode == 200) {
                            Gson gson = new Gson();
                            String Cookie = gson.fromJson(ResMsg, String.class);
                            SharedService.sp_httpData.edit().putString("Cookie", "ConGroupMember=" + Cookie).apply();
                            mainActivity.CheckLogon();
                        } else if (StatusCode == 400) {

                            Gson gson = new Gson();
                            String ErrorMsg = gson.fromJson(ResMsg, String.class);
                            SharedService.ShowErrorDialog(ErrorMsg, getActivity());

                        } else {
                            SharedService.ShowTextToast("ERROR:" + StatusCode, getActivity());
                        }
                    }
                });
            }
        });
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        SharedService.ShowTextToast("Google登入連接失敗", getActivity());
    }

    public void GoogleSignIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, GOOGLESIGNIN_CODE);
    }

    public void FBLogin() {
        LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("email"));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == GOOGLESIGNIN_CODE) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }
    }

    private void handleSignInResult(GoogleSignInResult result) {
        Log.d("GoogleSignInResult", "handleSignInResult:" + result.isSuccess());
        if (result.isSuccess()) {
            // Signed in successfully, show authenticated UI.
            GoogleSignInAccount acct = result.getSignInAccount();

            RequestBody formBody = new FormBody.Builder()
                    .add("accessToken", acct.getIdToken())
                    .build();

            Request request = new Request.Builder()
                    .url(getString(R.string.BackEndPath) + "Api/MemberApi/AndroidLoginByGoogle")
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

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (StatusCode == 200) {
                                Gson gson = new Gson();
                                String Cookie = gson.fromJson(ResMsg, String.class);
                                SharedService.sp_httpData.edit().putString("Cookie", "ConGroupMember=" + Cookie).apply();
                                mainActivity.CheckLogon();
                            } else if (StatusCode == 400) {
                                SharedService.ShowErrorDialog(ResMsg, getActivity());
                            } else {
                                SharedService.ShowTextToast("ERROR:" + StatusCode, getActivity());
                            }
                        }
                    });
                }
            });
        } else {
            // Signed out, show unauthenticated UI.
            SharedService.ShowTextToast("Google登入失敗", getActivity());
        }
    }

    private void SetFBCallBack() {
        callbackManager = CallbackManager.Factory.create();

        LoginManager.getInstance().registerCallback(callbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        // App code
                        if (loginResult.getAccessToken().getDeclinedPermissions().size() > 0) {
                            SharedService.ShowTextToast("您拒絕了某些權限", getActivity());
                            return;
                        }

                        RequestBody formBody = new FormBody.Builder()
                                .add("accessToken", loginResult.getAccessToken().getToken())
                                .build();

                        Request request = new Request.Builder()
                                .url(getString(R.string.BackEndPath) + "Api/MemberApi/LoginByFB")
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

                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (StatusCode == 200) {
                                            Gson gson = new Gson();
                                            String Cookie = gson.fromJson(ResMsg, String.class);
                                            SharedService.sp_httpData.edit().putString("Cookie", "ConGroupMember=" + Cookie).apply();
                                            mainActivity.CheckLogon();
                                        } else if (StatusCode == 400) {
                                            SharedService.ShowErrorDialog(ResMsg, getActivity());
                                        } else {
                                            SharedService.ShowTextToast("ERROR:" + StatusCode, getActivity());
                                        }
                                    }
                                });
                            }
                        });
                    }

                    @Override
                    public void onCancel() {
                        // App code
                    }

                    @Override
                    public void onError(FacebookException exception) {
                        // App code
                        SharedService.ShowTextToast("FB登入失敗", getActivity());
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mGoogleApiClient.stopAutoManage(getActivity());
        mGoogleApiClient.disconnect();
    }
}
