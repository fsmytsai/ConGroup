package com.tsai.congroup;


import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;

import com.google.gson.Gson;

import MyMethod.SharedService;
import ViewModel.MemberView;

public class CallActivity extends MySharedActivity {

    public boolean isCaller;
    public String otherAccount;
    public int callId;
    public MemberView memberView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);

        isCaller = getIntent().getBooleanExtra("IsCaller", false);
        otherAccount = getIntent().getStringExtra("OtherAccount");
        callId = getIntent().getIntExtra("CallId", -1);
        String JsonMemberView = getIntent().getStringExtra("MemberView");
        memberView = new Gson().fromJson(JsonMemberView, MemberView.class);
        CheckCaller();
    }

    private void CheckCaller() {
        if (isCaller) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{android.Manifest.permission.CAMERA, android.Manifest.permission.RECORD_AUDIO},
                        87);
            } else {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.CallFrameLayout, new CallFragment(), "CallFragment")
                        .commit();
            }

        } else {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.CallFrameLayout, new GotCallFragment(), "GotCallFragment")
                    .commit();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 87) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.CallFrameLayout, new CallFragment(), "CallFragment")
                        .commit();
            } else {
                SharedService.ShowTextToast("您拒絕了視訊權限", this);
                finish();
            }
        }
        if (requestCode == 97 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            GotCallFragment gotCallFragment = (GotCallFragment) getSupportFragmentManager().findFragmentByTag("GotCallFragment");
            gotCallFragment.RejectionCall();
            SharedService.ShowTextToast("您拒絕了通話", this);
        }
    }

    public void AcceptCall() {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.CallFrameLayout, new CallFragment(), "CallFragment")
                .commit();
    }

}
