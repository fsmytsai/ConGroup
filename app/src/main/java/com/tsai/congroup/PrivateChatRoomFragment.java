package com.tsai.congroup;


import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.Gson;

import java.io.IOException;

import MyMethod.SharedService;
import ViewModel.PChatRoomView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;


/**
 * A simple {@link Fragment} subclass.
 */
public class PrivateChatRoomFragment extends MySharedFragment {

    private MessageActivity messageActivity;
    private SwipeRefreshLayout mSwipeLayout;

    public PChatRoomView pChatRoomView;

    public RecyclerView rv_PrivateChatRoomList;
    private PrivateCRListAdapter privateCRListAdapter;

    private boolean isFirstLoad = true;
    private boolean isLoading = true;
    private boolean isFinishLoad = false;
    private int skip = 0;


    public PrivateChatRoomFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_private_chat_room, container, false);
        messageActivity = (MessageActivity) getActivity();
        super.client = messageActivity.client;
        super.imageClient = SharedService.GetClient(getActivity());
        SetCache((int) Runtime.getRuntime().maxMemory() / 20);

        rv_PrivateChatRoomList = (RecyclerView) view.findViewById(R.id.rv_PrivateChatRoomList);
        setSwipeRefresh(view);
        Refresh();
        return view;
    }

    private void setSwipeRefresh(View view) {
        mSwipeLayout = (SwipeRefreshLayout) view.findViewById(R.id.srl_PrivateChatRoomList);
        mSwipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                //檢查網路連線
                if (!SharedService.CheckNetWork(getActivity())) {
                    SharedService.ShowTextToast("請檢察網路連線", getActivity());
                    mSwipeLayout.setRefreshing(false);
                    return;
                }
                Refresh();
            }
        });
        mSwipeLayout.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(getActivity(), R.color.colorTransparent));
        // 设置下拉圆圈上的颜色，蓝色、绿色、橙色、红色
        mSwipeLayout.setColorSchemeResources(android.R.color.holo_blue_bright, android.R.color.holo_green_light,
                android.R.color.holo_orange_light, android.R.color.holo_red_light);
        mSwipeLayout.setDistanceToTriggerSync(400);// 设置手指在屏幕下拉多少距离会触发下拉刷新
        mSwipeLayout.setSize(SwipeRefreshLayout.DEFAULT);
    }

    public void Refresh() {
        mSwipeLayout.setRefreshing(true);
        clearLruCache();
        skip = 0;
        pChatRoomView = new PChatRoomView();
        //避免重新接上網路時重整導致崩潰
        if (!isFirstLoad) {
            isFinishLoad = false;
            privateCRListAdapter.notifyDataSetChanged();
        }
        GetCRList();
    }

    private void GetCRList() {
        Request request = new Request.Builder()
                .url(getString(R.string.BackEndPath) + "Api/PrivateMessageApi/GetCRList?Skip=" + skip)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // 停止刷新
                        mSwipeLayout.setRefreshing(false);
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
                        // 停止刷新
                        mSwipeLayout.setRefreshing(false);
                        //請求完畢
                        isLoading = false;

                        if (StatusCode == 200) {
                            Gson gson = new Gson();
                            PChatRoomView tempView = gson.fromJson(ResMsg, PChatRoomView.class);
                            if (isFirstLoad && tempView.CRList.size() == 0) {
                                //無聊天室
                            }

                            pChatRoomView.CRList.addAll(tempView.CRList);
                            pChatRoomView.LatestContentList.addAll(tempView.LatestContentList);
                            pChatRoomView.LTimeList.addAll(tempView.LTimeList);
                            pChatRoomView.MemberNameList.addAll(tempView.MemberNameList);
                            pChatRoomView.MImgNameList.addAll(tempView.MImgNameList);
                            pChatRoomView.LastOnTimeList.addAll(tempView.LastOnTimeList);
                            skip += tempView.CRList.size();

                            if (isFirstLoad) {
                                isFirstLoad = false;
                                if (messageActivity.isFromProFile)
                                    messageActivity.GetPMList(messageActivity.roomId, messageActivity.memberName, messageActivity.me);
                                rv_PrivateChatRoomList.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
                                privateCRListAdapter = new PrivateCRListAdapter();
                                rv_PrivateChatRoomList.setAdapter(privateCRListAdapter);
                            } else {
                                privateCRListAdapter.notifyDataSetChanged();
                            }

                            if (tempView.CRList.size() < tempView.ARequestCount) {
                                //最後一次載入
                                isFinishLoad = true;
                                View footer = LayoutInflater.from(getActivity()).inflate(R.layout.footer, rv_PrivateChatRoomList, false);
                                privateCRListAdapter.setFooterView(footer);
                            }
                        } else {
                            SharedService.ShowErrorDialog(StatusCode + "", getActivity());
                        }
                    }
                });

            }
        });
    }

    public class PrivateCRListAdapter extends RecyclerView.Adapter<PrivateCRListAdapter.ViewHolder> {

        public final int TYPE_HEADER = 0;  //说明是带有Header的
        public final int TYPE_FOOTER = 1;  //说明是带有Footer的
        public final int TYPE_NORMAL = 2;  //说明是不带有header和footer的
        private View mHeaderView;
        private View mFooterView;

        public void setHeaderView(View headerView) {
            if (mHeaderView == null) {
                mHeaderView = headerView;
                notifyItemInserted(0);
            } else {
                mHeaderView = headerView;
                notifyDataSetChanged();
            }
        }

        public void setFooterView(View footerView) {
            mFooterView = footerView;
            notifyItemInserted(getItemCount() - 1);
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0 && mHeaderView != null) {
                //第一个item应该加载Header
                return TYPE_HEADER;
            }
            if (position == getItemCount() - 1 && mFooterView != null) {
                //最后一个,应该加载Footer
                return TYPE_FOOTER;
            }
            return TYPE_NORMAL;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
//            if (mHeaderView != null && viewType == TYPE_HEADER) {
//                return new ViewHolder(mHeaderView);
//            }
            if (mFooterView != null && viewType == TYPE_FOOTER) {
                return new ViewHolder(mFooterView);
            }
            Context context = parent.getContext();
            View view = LayoutInflater.from(context).inflate(R.layout.pchatroom_block, parent, false);
            ViewHolder viewHolder = new ViewHolder(view);
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, final int position) {
            if (getItemViewType(position) == TYPE_FOOTER) {
                if (pChatRoomView.CRList.size() == 0)
                    holder.tv_Footer.setText("快與朋友開始聊天吧!");
                else
                    holder.tv_Footer.setText("沒有更多私人聊天室囉!");
                return;
            }
            holder.tv_MemberName.setText(pChatRoomView.MemberNameList.get(position));
            holder.tv_LTime.setText(pChatRoomView.LTimeList.get(position));
            holder.tv_LatestContent.setText(pChatRoomView.LatestContentList.get(position));

            if (pChatRoomView.MImgNameList.get(position) != null) {
                holder.iv_MImg.setImageDrawable(null);
                holder.iv_MImg.setTag(pChatRoomView.MImgNameList.get(position));
                showImage(holder.iv_MImg, pChatRoomView.MImgNameList.get(position), "M");
            } else {
                holder.iv_MImg.setTag("");
                holder.iv_MImg.setImageResource(R.drawable.defaultmimg);
            }

            holder.ll_GroupChatRoomBlock.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    messageActivity.GetPMList(pChatRoomView.CRList.get(position).RoomId,
                            pChatRoomView.MemberNameList.get(position),
                            pChatRoomView.CRList.get(position).ReceiveAccount.equals(SharedService.identityView.Account));
                }
            });

            //避免重複請求
            if (position > pChatRoomView.CRList.size() * 0.6 && !isFinishLoad && !isLoading) {
                isLoading = true;
                GetCRList();
            }
        }

        @Override
        public int getItemCount() {
            int NormalCount = pChatRoomView.CRList.size();
            if (mHeaderView != null)
                NormalCount++;
            if (mFooterView != null)
                NormalCount++;
            return NormalCount;
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            private TextView tv_MemberName;
            private TextView tv_LTime;
            private TextView tv_LatestContent;
            private ImageView iv_MImg;
            private LinearLayout ll_GroupChatRoomBlock;
            private TextView tv_Footer;

            public ViewHolder(View itemView) {
                super(itemView);
                if (itemView == mFooterView) {
                    tv_Footer = (TextView) itemView.findViewById(R.id.tv_Footer);
                    return;
                }
                tv_MemberName = (TextView) itemView.findViewById(R.id.tv_MemberName);
                tv_LTime = (TextView) itemView.findViewById(R.id.tv_LTime);
                tv_LatestContent = (TextView) itemView.findViewById(R.id.tv_LatestContent);
                iv_MImg = (ImageView) itemView.findViewById(R.id.iv_MImg);
                ll_GroupChatRoomBlock = (LinearLayout) itemView.findViewById(R.id.ll_GroupChatRoomBlock);
            }
        }
    }

}
