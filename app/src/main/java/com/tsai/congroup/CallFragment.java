package com.tsai.congroup;


import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.google.gson.Gson;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.VideoCapturerAndroid;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import MyMethod.MyCookieCredentials;
import MyMethod.SharedService;
import microsoft.aspnet.signalr.client.SignalRFuture;
import microsoft.aspnet.signalr.client.hubs.HubConnection;
import microsoft.aspnet.signalr.client.hubs.HubProxy;
import microsoft.aspnet.signalr.client.hubs.SubscriptionHandler1;
import microsoft.aspnet.signalr.client.transport.ServerSentEventsTransport;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


/**
 * A simple {@link Fragment} subclass.
 */
public class CallFragment extends Fragment {

    private HubConnection connection = null;
    private HubProxy mHub = null;

    private Gson gson;
    private GLSurfaceView sfv_VideoCall;
    private PeerConnection peerConnection;

    private VideoRenderer.Callbacks localRender;
    private VideoRenderer.Callbacks remoteRender;
    private VideoSource videoSource;
    private AudioSource audioSource;
    private PeerConnectionFactory peerConnectionFactory;

    private CallActivity callActivity;
    private OkHttpClient client;
    private ImageButton ib_OpenSpeaker;


    public CallFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_call, container, false);
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        callActivity = (CallActivity) getActivity();
        client = callActivity.client;
        gson = new Gson();
        UserSignalr();
        GetCamara(view);
        initView(view);
        return view;
    }

    private void initView(View view) {
        ImageButton ib_HangUp = (ImageButton) view.findViewById(R.id.ib_HangUp);
        ib_HangUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HangUp();
            }
        });

        ib_OpenSpeaker = (ImageButton) view.findViewById(R.id.ib_OpenSpeaker);
        ib_OpenSpeaker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OpenSpeaker();
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        if (null != peerConnection) {
            peerConnection.dispose();
            peerConnection = null;
        }

        if (null != videoSource) {
            videoSource.dispose();
            videoSource = null;
        }

        if (null != audioSource) {
            audioSource.dispose();
            audioSource = null;
        }

        if (null != peerConnectionFactory) {
            peerConnectionFactory.dispose();
            peerConnectionFactory = null;
        }

        if (sfv_VideoCall != null) {
            sfv_VideoCall.setVisibility(View.GONE);
            sfv_VideoCall = null;
        }

        if (connection != null) {
            connection.stop();
            connection = null;
            getActivity().finish();
        }
    }

    private boolean isFirstGet = true;

    private void UserSignalr() {
        final String HUB_URL = getString(R.string.BackEndPath) + "signalr";
        final String HUB_NAME = "conGroupHub";
        connection = new HubConnection(HUB_URL);
        mHub = connection.createHubProxy(HUB_NAME);
        connection.setCredentials(new MyCookieCredentials());
        SignalRFuture<Void> mSignalRFuture = connection.start(new ServerSentEventsTransport(connection.getLogger()));
        mHub.on("GotContext", new SubscriptionHandler1<String>() {
            @Override
            public void run(String Context) {
                if (Context.contains("offer"))
                    Context = Context.replace("offer", "OFFER");
                if (Context.contains("answer"))
                    Context = Context.replace("answer", "ANSWER");
                if (Context.contains("sdp"))
                    Context = Context.replace("sdp", "description");
                SessionDescription answer = gson.fromJson(Context, SessionDescription.class);
                final SessionDescription sdp = new SessionDescription(answer.type, answer.description);

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (peerConnection != null) {
                            peerConnection.setRemoteDescription(new SdpObserver() {
                                @Override
                                public void onCreateSuccess(SessionDescription sessionDescription) {
                                    Log.d("Remote", "setRemoteDescription onCreateSuccess");
                                }

                                @Override
                                public void onSetSuccess() {
                                    Log.d("Remote", "setRemoteDescription onSetSuccess");
                                    if (sdp.type.name().equals("OFFER") && isFirstGet) {
                                        isFirstGet = false;
                                        CreateAnswer();
                                    }
                                }

                                @Override
                                public void onCreateFailure(String s) {
                                    Log.e("Remote", "setRemoteDescription onCreateFailure " + s);
                                }

                                @Override
                                public void onSetFailure(String s) {
                                    Log.e("Remote", "setRemoteDescription onSetFailure " + s);
                                }
                            }, sdp);
                        }
                    }
                });
            }
        }, String.class);


        mHub.on("GotMessage", new SubscriptionHandler1<String>() {
            @Override
            public void run(final String Message) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        SharedService.ShowTextToast(Message, getActivity());
                        getActivity().finish();
                    }
                });
            }
        }, String.class);

        mHub.on("myGotIceCandidate", new SubscriptionHandler1<String>() {
            @Override
            public void run(String ice) {
                Log.d("IceCandidate", "GotIceCandidate");
                if (ice.contains("candidate"))
                    ice = ice.replace("candidate", "sdp");
                if (ice.contains("sdp:"))
                    ice = ice.replace("sdp:", "candidate:");
                IceCandidate iceCandidate = gson.fromJson(ice, IceCandidate.class);
                peerConnection.addIceCandidate(iceCandidate);
            }
        }, String.class);
    }

    private void GetCamara(View view) {
        sfv_VideoCall = new GLSurfaceView(getActivity());
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        sfv_VideoCall.setLayoutParams(params);
        LinearLayout ll_GLSurface = (LinearLayout) view.findViewById(R.id.ll_GLSurface);
        ll_GLSurface.addView(sfv_VideoCall);
        PeerConnectionFactory.initializeAndroidGlobals(
                getActivity(),  // Context
                true,  // Audio Enabled
                true,  // Video Enabled
                true,
                null); // Hardware Acceleration Enabled

        peerConnectionFactory = new PeerConnectionFactory();

        // Creates a VideoCapturerAndroid instance for the device name
        VideoCapturerAndroid videoCapturerAndroid = VideoCapturerAndroid.create(VideoCapturerAndroid.getNameOfFrontFacingDevice());

        MediaConstraints videoConstraints = new MediaConstraints();
        videoConstraints.mandatory.add(
                new MediaConstraints.KeyValuePair("maxWidth", "1280"));

        videoSource = peerConnectionFactory.createVideoSource(videoCapturerAndroid, videoConstraints);

        VideoTrack localVideoTrack = peerConnectionFactory.createVideoTrack("123", videoSource);

        audioSource = peerConnectionFactory.createAudioSource(new MediaConstraints());

        AudioTrack localAudioTrack = peerConnectionFactory.createAudioTrack("456", audioSource);

        VideoRendererGui.setView(sfv_VideoCall, null);
        remoteRender = VideoRendererGui.create(0, 0, 100, 100, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, false);
        localRender = VideoRendererGui.create(70, 5, 25, 25, VideoRendererGui.ScalingType.SCALE_ASPECT_FIT, false);
        localVideoTrack.addRenderer(new VideoRenderer(localRender));
        MediaStream LocalMediaStream = peerConnectionFactory.createLocalMediaStream("ConGroup");

        // Now we can add our tracks.
        LocalMediaStream.addTrack(localVideoTrack);
        LocalMediaStream.addTrack(localAudioTrack);

        List<PeerConnection.IceServer> iceServers = new ArrayList();
        iceServers.add(new PeerConnection.IceServer("stun:stun3.l.google.com:19302"));

        MediaConstraints pcConstraints = new MediaConstraints();

        peerConnection = peerConnectionFactory.createPeerConnection(
                iceServers,
                pcConstraints,
                observer);

        peerConnection.addStream(LocalMediaStream);
        if (callActivity.isCaller)
            CreateCall();
        else
            CreateOffer();
    }

    PeerConnection.Observer observer = new PeerConnection.Observer() {
        @Override
        public void onSignalingChange(PeerConnection.SignalingState signalingState) {

        }

        @Override
        public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {

        }

        @Override
        public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {

        }

        @Override
        public void onIceCandidate(IceCandidate iceCandidate) {
            String ice = gson.toJson(iceCandidate);
            mHub.invoke("myGetIceCandidate", ice, callActivity.otherAccount);
        }

        @Override
        public void onAddStream(final MediaStream mediaStream) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if ((mediaStream.videoTracks.size() == 1)) {

                        setSpeakerphoneOn();
                        mHub.invoke("changeConnectType");
                        mediaStream.videoTracks.get(0).addRenderer(
                                new VideoRenderer(remoteRender));
//                        if (!myOffer.equals("")) {
//                            final Handler handler = new Handler();
//                            handler.postDelayed(new Runnable() {
//                                @Override
//                                public void run() {
//                                    myOffer = myOffer.replace("OFFER", "offer");
//                                    SendAnswer(myOffer);
//                                }
//                            }, 3500);
//                        }
                    }
                }
            });
        }

        @Override
        public void onRemoveStream(MediaStream mediaStream) {
            mediaStream.videoTracks.get(0).dispose();
        }

        @Override
        public void onDataChannel(DataChannel dataChannel) {

        }

        @Override
        public void onRenegotiationNeeded() {

        }
    };

    private boolean isSpeakerphoneOn = false;

    public void OpenSpeaker() {
        if (isSpeakerphoneOn) {
            isSpeakerphoneOn = false;
            ib_OpenSpeaker.setImageResource(R.drawable.louderspeaker);
        } else {
            isSpeakerphoneOn = true;
            ib_OpenSpeaker.setImageResource(R.drawable.louderspeakerl);
        }
        setSpeakerphoneOn();
    }

    private void setSpeakerphoneOn() {
        AudioManager audioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
        if (isSpeakerphoneOn) {
            // 为true打开喇叭扩音器；为false关闭喇叭扩音器.
            audioManager.setSpeakerphoneOn(true);
            // 2016年06月18日 添加的代码，恢复系统声音设置
            audioManager.setMode(AudioManager.STREAM_SYSTEM);
            getActivity().setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);
        } else {
            audioManager.setSpeakerphoneOn(false);//关闭扬声器
            audioManager.setMode(AudioManager.STREAM_SYSTEM);
            getActivity().setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);
            //把声音设定成Earpiece（听筒）出来，设定为正在通话中
//            audioManager.setMode(AudioManager.MODE_IN_CALL);
//            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        }
    }

    public void CreateOffer() {
        MediaConstraints constraints = new MediaConstraints();
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        peerConnection.createOffer(Offer, constraints);
    }

//    private String myOffer = "";

    SdpObserver Offer = new SdpObserver() {
        @Override
        public void onCreateSuccess(SessionDescription sessionDescription) {
            Log.d("Offer", "createOffer onCreateSuccess");
            final SessionDescription sdp = new SessionDescription(sessionDescription.type, sessionDescription.description);

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (peerConnection != null) {
                        peerConnection.setLocalDescription(new SdpObserver() {
                            @Override
                            public void onCreateSuccess(SessionDescription sessionDescription) {
                                Log.d("Local", "setLocalDescription onCreateSuccess");
                            }

                            @Override
                            public void onSetSuccess() {
                                Log.d("Local", "setLocalDescription onSetSuccess");
                                String Context = gson.toJson(sdp);
//                                myOffer = Context;
                                ProcessCall(Context);
//                                mHub.invoke("myGetContext", Context);
                            }

                            @Override
                            public void onCreateFailure(String s) {
                                Log.e("Local", "setLocalDescription onCreateFailure " + s);
                            }

                            @Override
                            public void onSetFailure(String s) {
                                Log.e("Local", "setLocalDescription onSetFailure " + s);
                            }
                        }, sdp);
                    }
                }
            });
        }

        @Override
        public void onSetSuccess() {
            Log.d("Offer", "createOffer onSetSuccess");
        }

        @Override
        public void onCreateFailure(String s) {
            Log.d("Offer", "createOffer onCreateFailure " + s);
        }

        @Override
        public void onSetFailure(String s) {
            Log.d("Offer", "createOffer onSetFailure " + s);
        }
    };

    public void CreateAnswer() {
        MediaConstraints constraints = new MediaConstraints();
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));

        peerConnection.createAnswer(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                Log.d("Answer", "createAnswer onCreateSuccess");

                final SessionDescription sdp = new SessionDescription(sessionDescription.type, sessionDescription.description);

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (peerConnection != null) {
                            // must be done to before sending the invitation message
                            peerConnection.setLocalDescription(new SdpObserver() {
                                @Override
                                public void onCreateSuccess(SessionDescription sessionDescription) {
                                    Log.d("Local", "setLocalDescription onCreateSuccess");
                                }

                                @Override
                                public void onSetSuccess() {
                                    Log.d("Local", "setLocalDescription onSetSuccess");
                                    String Context = gson.toJson(sdp);
                                    SendAnswer(Context);
//                                    mHub.invoke("myGetContext", Context);
                                }

                                @Override
                                public void onCreateFailure(String s) {
                                    Log.e("Local", "setLocalDescription onCreateFailure " + s);
                                }

                                @Override
                                public void onSetFailure(String s) {
                                    Log.e("Local", "setLocalDescription onSetFailure " + s);
                                }
                            }, sdp);
                        }
                    }
                });
            }

            @Override
            public void onSetSuccess() {
                Log.d("Answer", "createAnswer onSetSuccess");
            }

            @Override
            public void onCreateFailure(String s) {
                Log.e("Answer", "createAnswer onCreateFailure " + s);
            }

            @Override
            public void onSetFailure(String s) {
                Log.e("Answer", "createAnswer onSetFailure " + s);
            }
        }, constraints);

    }

    private void CreateCall() {
        RequestBody formBody = new FormBody.Builder()
                .add("ReceiveAccount", callActivity.otherAccount)
                .build();

        Request request = new Request.Builder()
                .url(getString(R.string.BackEndPath) + "Api/CallApi/CreateCall")
                .post(formBody)
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
                            callActivity.callId = gson.fromJson(ResMsg, Integer.class);
                        } else if (StatusCode == 400) {
                            String ErrorMessage = gson.fromJson(ResMsg, String.class);
                            SharedService.ShowTextToast(ErrorMessage, getActivity());
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

    private void ProcessCall(String Context) {
        RequestBody formBody = new FormBody.Builder()
                .add("Context", Context)
                .add("CallId", callActivity.callId + "")
                .add("IsAccept", "true")
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
                            new AlertDialog.Builder(getActivity())
                                    .setTitle("提醒")
                                    .setMessage("本功能處於實驗性階段，請在網路訊號良好的情況下使用，否則可能會沒有畫面")
                                    .setPositiveButton("知道了", null)
                                    .show();
                        } else if (StatusCode == 400) {
                            String ErrorMessage = gson.fromJson(ResMsg, String.class);
                            SharedService.ShowTextToast(ErrorMessage, getActivity());
                            getActivity().finish();
                        } else {
                            SharedService.ShowTextToast("ERROR:" + StatusCode, getActivity());
                        }
                    }
                });
            }
        });
    }

    private void SendAnswer(String Context) {
        RequestBody formBody = new FormBody.Builder()
                .add("Context", Context)
                .add("CallId", callActivity.callId + "")
                .add("IsAccept", "true")
                .build();

        Request request = new Request.Builder()
                .url(getString(R.string.BackEndPath) + "Api/CallApi/SendAnswer")
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
                        } else if (StatusCode == 400) {
                            String ErrorMessage = gson.fromJson(ResMsg, String.class);
                            SharedService.ShowTextToast(ErrorMessage, getActivity());
                            getActivity().finish();
                        } else {
                            SharedService.ShowTextToast("ERROR:" + StatusCode, getActivity());
                        }
                    }
                });
            }
        });
    }

    public void HangUp() {
        Request request = new Request.Builder()
                .url(getString(R.string.BackEndPath) + "Api/CallApi/FinishCall?CallId=" + callActivity.callId)
                .delete()
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
                        } else if (StatusCode == 400) {
                            String ErrorMessage = gson.fromJson(ResMsg, String.class);
                            SharedService.ShowTextToast(ErrorMessage, getActivity());
                            getActivity().finish();
                        } else {
                            SharedService.ShowTextToast("ERROR:" + StatusCode, getActivity());
                        }
                    }
                });
            }
        });
    }
}
