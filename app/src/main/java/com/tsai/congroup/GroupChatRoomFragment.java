package com.tsai.congroup;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import java.io.InputStream;

import MyMethod.SharedService;
import MyMethod.SpaceItemDecoration;
import ViewModel.GroupChatRoomView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


/**
 * A simple {@link Fragment} subclass.
 */
public class GroupChatRoomFragment extends MySharedFragment {
    private MessageActivity messageActivity;
    private SwipeRefreshLayout mSwipeLayout;

    public GroupChatRoomView groupChatRoomView;

    public RecyclerView rv_GroupChatRoomList;
    private GroupCRListAdapter groupCRListAdapter;

    private boolean isFirstLoad = true;
    private boolean isLoading = true;
    private boolean isFinishLoad = false;
    private int skip = 0;


    public GroupChatRoomFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_group_chat_room, container, false);
        messageActivity = (MessageActivity) getActivity();
        super.client = messageActivity.client;
        super.imageClient = SharedService.GetClient(getActivity());
        SetCache((int) Runtime.getRuntime().maxMemory() / 20);

        rv_GroupChatRoomList = (RecyclerView) view.findViewById(R.id.rv_GroupChatRoomList);
        setSwipeRefresh(view);
        Refresh();
        return view;
    }

    private void setSwipeRefresh(View view) {
        mSwipeLayout = (SwipeRefreshLayout) view.findViewById(R.id.srl_GroupChatRoomList);
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
        groupChatRoomView = new GroupChatRoomView();
        //避免重新接上網路時重整導致崩潰
        if (!isFirstLoad) {
            isFinishLoad = false;
            groupCRListAdapter.notifyDataSetChanged();
        }
        GetGroupCRList();
    }

    public void GetGroupCRList() {

        Request request = new Request.Builder()
                .url(getString(R.string.BackEndPath) + "Api/GroupMessageApi/GetGroupCRList?Skip=" + skip)
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
                            GroupChatRoomView tempView = gson.fromJson(ResMsg, GroupChatRoomView.class);
                            if (isFirstLoad && tempView.GroupCRList.size() == 0) {
                                //無通知
                            }

                            groupChatRoomView.GroupCRList.addAll(tempView.GroupCRList);
                            groupChatRoomView.LastCTimeList.addAll(tempView.LastCTimeList);
                            skip += tempView.GroupCRList.size();

                            if (isFirstLoad) {
                                isFirstLoad = false;
                                messageActivity.SetupUI(getActivity().findViewById(R.id.activity_Outer));
                                if (!messageActivity.isFromProFile && tempView.GroupCRList.size() > 0)
                                    messageActivity.GetGMList(tempView.GroupCRList.get(0).GroupId, tempView.GroupCRList.get(0).GName);
                                rv_GroupChatRoomList.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
                                groupCRListAdapter = new GroupCRListAdapter();
                                rv_GroupChatRoomList.setAdapter(groupCRListAdapter);
                            } else {
                                groupCRListAdapter.notifyDataSetChanged();
                            }

                            if (tempView.GroupCRList.size() < tempView.ARequestCount) {
                                //最後一次載入
                                isFinishLoad = true;
                                View footer = LayoutInflater.from(getActivity()).inflate(R.layout.footer, rv_GroupChatRoomList, false);
                                groupCRListAdapter.setFooterView(footer);
                            }
                        } else {
                            SharedService.ShowErrorDialog(StatusCode + "", getActivity());
                        }
                    }
                });

            }
        });
    }

    public class GroupCRListAdapter extends RecyclerView.Adapter<GroupCRListAdapter.ViewHolder> {

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
            View view = LayoutInflater.from(context).inflate(R.layout.groupchatroom_block, parent, false);
            ViewHolder viewHolder = new ViewHolder(view);
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, final int position) {
            if (getItemViewType(position) == TYPE_FOOTER) {
                if (groupChatRoomView.GroupCRList.size() == 0)
                    holder.tv_Footer.setText("快加入群組開始聊天吧!");
                else
                    holder.tv_Footer.setText("沒有更多群組聊天室囉!");
                return;
            }
            final GroupChatRoomView.GroupChatRoom groupChatRoom = groupChatRoomView.GroupCRList.get(position);
            holder.tv_GName.setText(groupChatRoom.GName);
            holder.tv_LastCTime.setText(groupChatRoomView.LastCTimeList.get(position));
            holder.tv_LastContent.setText(groupChatRoom.LastMemberName + " : " + groupChatRoom.LastContent);

            if (groupChatRoomView.GroupCRList.get(position).GImgName != null) {
                holder.iv_GImg.setImageDrawable(null);
                holder.iv_GImg.setTag(groupChatRoomView.GroupCRList.get(position).GImgName);
                showImage(holder.iv_GImg, groupChatRoomView.GroupCRList.get(position).GImgName, "G");
            } else {
                holder.iv_GImg.setTag("");
                holder.iv_GImg.setImageResource(R.drawable.defaultgimg);
            }

            holder.ll_GroupChatRoomBlock.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    messageActivity.GetGMList(groupChatRoom.GroupId, groupChatRoom.GName);
                }
            });

            //避免重複請求
            if (position > groupChatRoomView.GroupCRList.size() * 0.6 && !isFinishLoad && !isLoading) {
                isLoading = true;
                GetGroupCRList();
            }
        }

        @Override
        public int getItemCount() {
            int NormalCount = groupChatRoomView.GroupCRList.size();
            if (mHeaderView != null)
                NormalCount++;
            if (mFooterView != null)
                NormalCount++;
            return NormalCount;
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            private TextView tv_GName;
            private TextView tv_LastCTime;
            private TextView tv_LastContent;
            private ImageView iv_GImg;
            private LinearLayout ll_GroupChatRoomBlock;
            private TextView tv_Footer;

            public ViewHolder(View itemView) {
                super(itemView);
                if (itemView == mFooterView) {
                    tv_Footer = (TextView) itemView.findViewById(R.id.tv_Footer);
                    return;
                }
                tv_GName = (TextView) itemView.findViewById(R.id.tv_GName);
                tv_LastCTime = (TextView) itemView.findViewById(R.id.tv_LastCTime);
                tv_LastContent = (TextView) itemView.findViewById(R.id.tv_LastContent);
                iv_GImg = (ImageView) itemView.findViewById(R.id.iv_GImg);
                ll_GroupChatRoomBlock = (LinearLayout) itemView.findViewById(R.id.ll_GroupChatRoomBlock);
            }
        }
    }

}
