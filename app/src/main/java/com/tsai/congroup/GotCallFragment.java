package com.tsai.congroup;


import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

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
public class GotCallFragment extends MySharedFragment {
    private CallActivity callActivity;
    private Vibrator vibrator;
    private Ringtone ringtone;

    public GotCallFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_got_call, container, false);
        callActivity = (CallActivity) getActivity();
        super.client = callActivity.client;
        SetCache((int) Runtime.getRuntime().maxMemory() / 20);
        initView(view);
        if (ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    getActivity(),
                    new String[]{android.Manifest.permission.CAMERA, android.Manifest.permission.RECORD_AUDIO},
                    97);
        }

        ringtone = RingtoneManager.getRingtone(getActivity(), RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE));
        vibrator = (Vibrator) getActivity().getSystemService(getActivity().VIBRATOR_SERVICE);

        long[] pattern = {1000, 2000};
        int repeat = 0;
        vibrator.vibrate(pattern, repeat);
        ringtone.play();
        return view;
    }

    private void initView(View view) {
        ImageView iv_MImg = (ImageView) view.findViewById(R.id.iv_MImg);
        if (callActivity.memberView.MImgName != null) {
            iv_MImg.setImageDrawable(null);
            LoadImgAsyncTask imgAsyncTask = new LoadImgAsyncTask(iv_MImg, getString(R.string.BackEndPath) + "MImages/" + callActivity.memberView.MImgName);
            imgAsyncTask.execute();
        } else {
            iv_MImg.setImageResource(R.drawable.defaultmimg);
        }
        TextView tv_Message = (TextView) view.findViewById(R.id.tv_Message);
        tv_Message.setText(callActivity.memberView.Name + " 想與您視訊");
        ImageButton ib_AcceptCall = (ImageButton) view.findViewById(R.id.ib_AcceptCall);
        ImageButton ib_RejectionCall = (ImageButton) view.findViewById(R.id.ib_RejectionCall);
        ib_AcceptCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ringtone.stop();
                vibrator.cancel();
                callActivity.AcceptCall();
            }
        });
        ib_RejectionCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RejectionCall();
            }
        });
    }

    public class LoadImgAsyncTask extends AsyncTask<Void, Void, Bitmap> {
        private String mUrl;
        private ImageView dataImageView;

        public LoadImgAsyncTask(ImageView imageView, String url) {
            mUrl = url;
            dataImageView = imageView;
        }

        @Override
        protected Bitmap doInBackground(Void... params) {

            Bitmap bitmap;

            bitmap = getBitmapFromUrl(mUrl);
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            dataImageView.setImageBitmap(bitmap);
        }

        public Bitmap getBitmapFromUrl(String urlString) {
            Bitmap bitmap;
            InputStream is = null;
            try {
                URL mUrl = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) mUrl.openConnection();
                is = new BufferedInputStream(connection.getInputStream());
                bitmap = BitmapFactory.decodeStream(is);
                connection.disconnect();
                return bitmap;
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (is != null) {
                        is.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

    }

    public void RejectionCall() {
        ringtone.stop();
        vibrator.cancel();
        RequestBody formBody = new FormBody.Builder()
                .add("Context", "123")
                .add("CallId", callActivity.callId + "")
                .add("IsAccept", "false")
                .build();

        Request request = new Request.Builder()
                .url(getString(R.string.BackEndPath) + "Api/CallApi/ProcessCall")
                .put(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
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
                            getActivity().finish();
                        } else if (StatusCode == 400) {
                            SharedService.ShowErrorDialog(ResMsg, getActivity());
                            getActivity().finish();
                        } else {
                            SharedService.ShowTextToast("ERROR:" + StatusCode, getActivity());
                            getActivity().finish();
                        }
                    }
                });
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
    }
}
