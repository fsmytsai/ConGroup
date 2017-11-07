package com.tsai.congroup;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.Gson;

import java.io.IOException;

import MyMethod.SharedService;
import ViewModel.GroupMessageView;
import ViewModel.PrivateMessageView;
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
public class MessageFragment extends MySharedFragment {
    private MessageActivity messageActivity;

    private GroupMessageView groupMessageView;
    private PrivateMessageView privateMessageView;

    public RecyclerView rv_MessageList;
    private EditText et_Message;
    private MessageListAdapter messageListAdapter;

    private boolean isFirstLoad = true;
    private boolean isLoading = true;
    private boolean isFinishLoad = false;
    private int groupId = -1;
    private int roomId = -1;
    private int skip = 0;

    //true=G false=P
    private boolean type;
    private boolean me;

    public MessageFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_message, container, false);
        messageActivity = (MessageActivity) getActivity();
        super.client = messageActivity.client;
        super.imageClient = SharedService.GetClient(getActivity());
        SetCache((int) Runtime.getRuntime().maxMemory() / 20);

        initView(view);
        return view;
    }

    private void initView(View view) {
        rv_MessageList = (RecyclerView) view.findViewById(R.id.rv_MessageList);
        et_Message = (EditText) view.findViewById(R.id.et_Message);
        final ImageButton ib_SendMessage = (ImageButton) view.findViewById(R.id.ib_SendMessage);
        ib_SendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SendMessage();
            }
        });
        et_Message.post(new Runnable() {
            @Override
            public void run() {
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(et_Message.getHeight(), et_Message.getHeight());
                ib_SendMessage.setLayoutParams(params);
            }
        });
    }

    public void GRefresh(int GroupId) {
        clearLruCache();
        skip = 0;
        type = true;
        groupId = GroupId;
        roomId = -1;
        groupMessageView = new GroupMessageView();
        privateMessageView = new PrivateMessageView();
        //避免重新接上網路時重整導致崩潰
        if (!isFirstLoad) {
            isFinishLoad = false;
            messageListAdapter.notifyDataSetChanged();
        }
        GetGMList();
    }

    public void PRefresh(int ChatRoomId, boolean Me) {
        clearLruCache();
        skip = 0;
        me = Me;
        type = false;
        groupId = -1;
        roomId = ChatRoomId;
        groupMessageView = new GroupMessageView();
        privateMessageView = new PrivateMessageView();
        //避免重新接上網路時重整導致崩潰
        if (!isFirstLoad) {
            isFinishLoad = false;
            messageListAdapter.notifyDataSetChanged();
        }
        GetPMList();
    }

    public void GetGMList() {

        Request request = new Request.Builder()
                .url(getString(R.string.BackEndPath) + "Api/GroupMessageApi/GetGMList?GroupId=" + groupId + "&Skip=" + skip)
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
                        //請求完畢
                        isLoading = false;

                        if (StatusCode == 200) {
                            Gson gson = new Gson();
                            GroupMessageView tempView = gson.fromJson(ResMsg, GroupMessageView.class);
                            if (isFirstLoad && tempView.GroupMessageList.size() == 0) {
                                //無訊息
                            }
                            groupMessageView.GroupMessageList.addAll(tempView.GroupMessageList);
                            groupMessageView.MemberNameList.addAll(tempView.MemberNameList);
                            groupMessageView.MImgNameList.addAll(tempView.MImgNameList);
                            groupMessageView.CTimeList.addAll(tempView.CTimeList);

                            skip += tempView.GroupMessageList.size();
                            if (tempView.GroupMessageList.size() < tempView.ARequestCount) {
                                //最後一次載入
                                isFinishLoad = true;
                                SharedService.ShowTextToast("已加載完畢", getActivity());
                            }
                            if (isFirstLoad) {
                                isFirstLoad = false;

                                rv_MessageList.setLayoutManager(new WrapContentLinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, true));
                                messageListAdapter = new MessageListAdapter();
                                rv_MessageList.setAdapter(messageListAdapter);
                            } else {
                                messageListAdapter.notifyDataSetChanged();
                            }
                        } else {
                            SharedService.ShowErrorDialog(StatusCode + "", getActivity());
                        }
                    }
                });

            }
        });
    }

    public void GetGMFromSignalr(final GroupMessageView tempView) {
        if (tempView.GroupMessageList.get(0).GroupId == groupId) {

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    skip += tempView.GroupMessageList.size();
                    groupMessageView.GroupMessageList.add(0, tempView.GroupMessageList.get(0));
                    groupMessageView.MemberNameList.add(0, tempView.MemberNameList.get(0));
                    groupMessageView.MImgNameList.add(0, tempView.MImgNameList.get(0));
                    groupMessageView.CTimeList.add(0, tempView.CTimeList.get(0));
                    messageListAdapter.notifyItemRangeInserted(0, 1);
//                    messageListAdapter.notifyItemRangeChanged(0, groupMessageView.GroupMessageList.size());
                    rv_MessageList.scrollToPosition(0);
                }
            });
        } else {
            //目前不在該群組畫面
            //應該要更新GroupChatRoom並把新的聊天室設成橘色
        }
    }

    public void GetPMList() {

        Request request = new Request.Builder()
                .url(getString(R.string.BackEndPath) + "Api/PrivateMessageApi/GetPMList?RoomId=" + roomId + "&Skip=" + skip)
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
                        //請求完畢
                        isLoading = false;

                        if (StatusCode == 200) {
                            Gson gson = new Gson();
                            PrivateMessageView tempView = gson.fromJson(ResMsg, PrivateMessageView.class);
                            if (isFirstLoad && tempView.PrivateMessageList.size() == 0) {
                                //無訊息
                            }
                            privateMessageView.PrivateMessageList.addAll(tempView.PrivateMessageList);
                            privateMessageView.Account = tempView.Account;
                            privateMessageView.MemberName = tempView.MemberName;
                            privateMessageView.MImgName = tempView.MImgName;
                            privateMessageView.CTimeList.addAll(tempView.CTimeList);

                            skip += tempView.PrivateMessageList.size();
                            if (tempView.PrivateMessageList.size() < tempView.ARequestCount) {
                                //最後一次載入
                                isFinishLoad = true;
                                SharedService.ShowTextToast("已加載完畢", getActivity());
                            }
                            if (isFirstLoad) {
                                isFirstLoad = false;

                                rv_MessageList.setLayoutManager(new WrapContentLinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, true));
                                messageListAdapter = new MessageListAdapter();
                                rv_MessageList.setAdapter(messageListAdapter);
                            } else {
                                messageListAdapter.notifyDataSetChanged();
                            }
                        } else {
                            SharedService.ShowErrorDialog(StatusCode + "", getActivity());
                        }
                    }
                });

            }
        });
    }

    public void GetPMFromSignalr(final PrivateMessageView tempView) {
        if (tempView.PrivateMessageList.get(0).RoomId == roomId) {
            skip += tempView.PrivateMessageList.size();
            privateMessageView.PrivateMessageList.addAll(0, tempView.PrivateMessageList);
            groupMessageView.CTimeList.addAll(0, tempView.CTimeList);

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    messageListAdapter.notifyItemRangeInserted(0, tempView.PrivateMessageList.size());
//                    messageListAdapter.notifyItemRangeChanged(0, groupMessageView.GroupMessageList.size());
                    rv_MessageList.scrollToPosition(0);
                }
            });
        } else {
            //目前不在該私訊畫面
            //應該要更新PrivateChatRoom並把新的聊天室設成橘色
        }
    }

    public class MessageListAdapter extends RecyclerView.Adapter<MessageListAdapter.ViewHolder> {

        private final int left = 87;
        private final int right = 78;

        @Override
        public int getItemViewType(int position) {
            if (type) {
                if (groupMessageView.GroupMessageList.get(position).SendAccount.equals(SharedService.identityView.Account))
                    return right;
                else
                    return left;
            } else {
                if (privateMessageView.PrivateMessageList.get(position).Sender == me)
                    return right;
                else
                    return left;
            }
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            Context context = parent.getContext();
            View view;
            if (viewType == right) {
                view = LayoutInflater.from(context).inflate(R.layout.rmessage_block, parent, false);
                view.setTag("R");
            } else {
                view = LayoutInflater.from(context).inflate(R.layout.lmessage_block, parent, false);
                view.setTag("L");
            }
            ViewHolder viewHolder = new ViewHolder(view);
            return viewHolder;
        }


        @Override
        public void onBindViewHolder(ViewHolder holder, final int position) {
            if (type) {
                holder.tv_Message.setText(groupMessageView.GroupMessageList.get(position).Content);
                if (getItemViewType(position) == left) {
                    holder.tv_MemberName.setVisibility(View.VISIBLE);
                    holder.tv_MemberName.setText(groupMessageView.MemberNameList.get(position));
                    if (groupMessageView.MImgNameList.get(position) != null) {
                        holder.iv_MImg.setImageDrawable(null);
                        holder.iv_MImg.setTag(groupMessageView.MImgNameList.get(position));
                        showImage(holder.iv_MImg, groupMessageView.MImgNameList.get(position), "M");
                    } else {
                        holder.iv_MImg.setTag("");
                        holder.iv_MImg.setImageResource(R.drawable.defaultmimg);
                    }

                    holder.iv_MImg.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(getActivity(), ProFileActivity.class);
                            intent.putExtra("Account", groupMessageView.GroupMessageList.get(position).SendAccount);
                            startActivity(intent);
                        }
                    });
                }

                //避免重複請求
                if (position > groupMessageView.GroupMessageList.size() * 0.6 && !isFinishLoad && !isLoading) {
                    isLoading = true;
                    GetGMList();
                }
            } else {
                holder.tv_Message.setText(privateMessageView.PrivateMessageList.get(position).Content);
                if (getItemViewType(position) == left) {
                    holder.tv_MemberName.setVisibility(View.GONE);
                    if (privateMessageView.MImgName != null) {
                        holder.iv_MImg.setImageDrawable(null);
                        holder.iv_MImg.setTag(privateMessageView.MImgName);
                        showImage(holder.iv_MImg, privateMessageView.MImgName, "M");
                    } else {
                        holder.iv_MImg.setTag("");
                        holder.iv_MImg.setImageResource(R.drawable.defaultmimg);
                    }
                    holder.iv_MImg.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(getActivity(), ProFileActivity.class);
                            intent.putExtra("Account", privateMessageView.Account);
                            startActivity(intent);
                        }
                    });
                }

                //避免重複請求
                if (position > privateMessageView.PrivateMessageList.size() * 0.6 && !isFinishLoad && !isLoading) {
                    isLoading = true;
                    GetPMList();
                }
            }
        }

        @Override
        public int getItemCount() {
            if (type)
                return groupMessageView.GroupMessageList.size();
            else
                return privateMessageView.PrivateMessageList.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            private TextView tv_MemberName;
            private TextView tv_Message;
            private ImageView iv_MImg;

            public ViewHolder(View itemView) {
                super(itemView);
                tv_Message = (TextView) itemView.findViewById(R.id.tv_Message);
                if (itemView.getTag().equals("L")) {
                    tv_MemberName = (TextView) itemView.findViewById(R.id.tv_MemberName);
                    iv_MImg = (ImageView) itemView.findViewById(R.id.iv_MImg);
                }
            }
        }
    }

    private void SendMessage() {
        SharedService.HideKeyboard(getActivity());
        messageActivity.activity_Outer.requestFocus();

        String Message = et_Message.getText().toString();
        if (!Message.trim().equals("")) {
            Request request;
            if (type) {
                RequestBody formBody = new FormBody.Builder()
                        .add("groupMessage.GroupId", groupId + "")
                        .add("groupMessage.Content", Message)
                        .build();

                request = new Request.Builder()
                        .url(getString(R.string.BackEndPath) + "Api/GroupMessageApi/Send")
                        .post(formBody)
                        .build();
            } else {
                if (privateMessageView == null) {
                    SharedService.ShowTextToast("沒有聊天對象", getActivity());
                    return;
                }
                RequestBody formBody = new FormBody.Builder()
                        .add("ReceiveAccount", privateMessageView.Account)
                        .add("Content", Message)
                        .build();

                request = new Request.Builder()
                        .url(getString(R.string.BackEndPath) + "Api/PrivateMessageApi/CreatePM")
                        .post(formBody)
                        .build();
            }

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
                    getActivity().runOnUiThread(new Runnable() {//这是Activity的方法，会在主线程执行任务
                        @Override
                        public void run() {
                            if (StatusCode == 200) {
                                et_Message.setText("");
                            } else if (StatusCode == 400) {
                                SharedService.ShowErrorDialog(ResMsg, getActivity());
                            } else {
                                SharedService.ShowErrorDialog(StatusCode + "", getActivity());
                            }
                        }
                    });
                }
            });
        } else {
            SharedService.ShowTextToast("請輸入內容", getActivity());
        }
    }

    public class WrapContentLinearLayoutManager extends LinearLayoutManager {
        public WrapContentLinearLayoutManager(Context context, int orientation, boolean reverseLayout) {
            super(context, orientation, reverseLayout);
        }

        @Override
        public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
            try {
                super.onLayoutChildren(recycler, state);
            } catch (IndexOutOfBoundsException e) {
                Log.e("probe", "meet a IOOBE in RecyclerView");
            }
        }
    }
}
