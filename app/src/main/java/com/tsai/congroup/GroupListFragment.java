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
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import MyMethod.SharedService;
import ViewModel.MemberAllGroupView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;


/**
 * A simple {@link Fragment} subclass.
 */
public class GroupListFragment extends MySharedFragment {

    private MainActivity mainActivity;
    private SwipeRefreshLayout mSwipeLayout;

    public MemberAllGroupView memberAllGroupView;
    private List<MemberAllGroupView.SimpleGroup> recommendGroupList;

    private boolean isFirstLoad = true;

    public RecyclerView rv_GroupList;
    private GroupListAdapter groupListAdapter;

    private boolean isMyGroupOpen = true;
    private boolean isRecomGroupOpen = true;
    private ConGroupFragment conGroupFragment;


    public GroupListFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_group_list, container, false);

        mainActivity = (MainActivity) getActivity();
        conGroupFragment = (ConGroupFragment) getParentFragment();
        super.client = mainActivity.client;
        super.imageClient = SharedService.GetClient(getActivity());
        SetCache((int) Runtime.getRuntime().maxMemory() / 20);

        rv_GroupList = (RecyclerView) view.findViewById(R.id.rv_GroupList);
        setSwipeRefresh(view);
        Refresh();
        return view;
    }

    private void setSwipeRefresh(View view) {
        mSwipeLayout = (SwipeRefreshLayout) view.findViewById(R.id.srl_GroupList);
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
        memberAllGroupView = new MemberAllGroupView();
        recommendGroupList = new ArrayList<>();
        clearLruCache();
        //避免重新接上網路時重整導致崩潰
        if (!isFirstLoad) {
            groupListAdapter.notifyDataSetChanged();
        }
        GetMemberAllGroups();
    }

    public void GetMemberAllGroups() {

        Request request = new Request.Builder()
                .url(getString(R.string.BackEndPath) + "Api/GroupMemberApi/GetMemberAllGroups")
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
                        // 停止刷新
                        mSwipeLayout.setRefreshing(false);
                        if (StatusCode == 200) {
                            Gson gson = new Gson();
                            MemberAllGroupView tempView = gson.fromJson(ResMsg, MemberAllGroupView.class);
                            if (isFirstLoad && tempView.SimpleGroupList.size() == 0) {
                                //無群組
                            }
                            memberAllGroupView.SimpleGroupList.addAll(tempView.SimpleGroupList);
                            GetRecommendGroup();
                        }
                    }
                });

            }
        });
    }

    public void GetRecommendGroup() {

        Request request = new Request.Builder()
                .url(getString(R.string.BackEndPath) + "Api/GroupMemberApi/RecommendGroup")
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
                        if (StatusCode == 200) {
                            Gson gson = new Gson();
                            recommendGroupList = gson.fromJson(ResMsg, new TypeToken<List<MemberAllGroupView.SimpleGroup>>() {
                            }.getType());
                            if (isFirstLoad) {
                                isFirstLoad = false;
                                rv_GroupList.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
                                groupListAdapter = new GroupListAdapter();
                                View header = LayoutInflater.from(getActivity()).inflate(R.layout.group_head, rv_GroupList, false);
                                groupListAdapter.setHeaderView(header);

                                View header2 = LayoutInflater.from(getActivity()).inflate(R.layout.group_head, rv_GroupList, false);
                                groupListAdapter.setRHeaderView(header2);
                                rv_GroupList.setAdapter(groupListAdapter);
                                conGroupFragment.getPostListFragment().Refresh();
                            } else {
                                groupListAdapter.notifyDataSetChanged();
                            }
                        }
                    }
                });

            }
        });
    }

    public class GroupListAdapter extends RecyclerView.Adapter<GroupListAdapter.ViewHolder> {

        public final int TYPE_HEADER = 0;  //说明是带有Header的
        public final int TYPE_RHEADER = 1;  //说明是带有RHeader的
        public final int TYPE_NORMAL = 2;  //说明是不带有header和footer的
        private View mHeaderView;
        private View mRHeaderView;

        public void setHeaderView(View headerView) {
            mHeaderView = headerView;
        }

        public void setRHeaderView(View headerView) {
            mRHeaderView = headerView;
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0) {
                //第一个item应该加载Header
                return TYPE_HEADER;
            }// || (!isMyGroupOpen && position == 1)
            else if (isMyGroupOpen && position == memberAllGroupView.SimpleGroupList.size() + 1) {
                //我的群組結束後第一個
                return TYPE_RHEADER;
            } else if (!isMyGroupOpen && position == 1) {
                return TYPE_RHEADER;
            } else {
                return TYPE_NORMAL;
            }
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            Context context = parent.getContext();
            if (mHeaderView != null && viewType == TYPE_HEADER) {
                return new ViewHolder(mHeaderView);
            }
            if (mRHeaderView != null && viewType == TYPE_RHEADER) {
                return new ViewHolder(mRHeaderView);
            }
            View view = LayoutInflater.from(context).inflate(R.layout.group_block, parent, false);
            ViewHolder viewHolder = new ViewHolder(view);
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, final int position) {
            if (getItemViewType(position) == TYPE_HEADER) {
                holder.iv_Resource.setImageResource(R.drawable.mygroup);
                holder.tv_Resource.setText("我的群組");
                if (isMyGroupOpen)
                    holder.iv_HeadType.setImageResource(R.drawable.groupopen);
                else
                    holder.iv_HeadType.setImageResource(R.drawable.groupclose);

                holder.ll_GroupHead.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (isMyGroupOpen) {
                            isMyGroupOpen = !isMyGroupOpen;
                            groupListAdapter.notifyItemRangeRemoved(1, memberAllGroupView.SimpleGroupList.size());
//                                if (isRecomGroupOpen)
//                                    groupListAdapter.notifyItemRangeChanged(0, getItemCount());
                            holder.iv_HeadType.setImageResource(R.drawable.groupclose);
                        } else {
                            isMyGroupOpen = !isMyGroupOpen;
                            groupListAdapter.notifyItemRangeInserted(1, memberAllGroupView.SimpleGroupList.size());
//                                groupListAdapter.notifyItemRangeChanged(0, getItemCount());
                            holder.iv_HeadType.setImageResource(R.drawable.groupopen);
                        }
//                        isMyGroupOpen = !isMyGroupOpen;
//                        notifyDataSetChanged();
                    }
                });

                return;
            } else if (getItemViewType(position) == TYPE_RHEADER) {
                holder.iv_Resource.setImageResource(R.drawable.recommendgroup);
                holder.tv_Resource.setText("推薦群組");
                if (isRecomGroupOpen)
                    holder.iv_HeadType.setImageResource(R.drawable.groupopen);
                else
                    holder.iv_HeadType.setImageResource(R.drawable.groupclose);
                holder.ll_GroupHead.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (isRecomGroupOpen) {
                            isRecomGroupOpen = !isRecomGroupOpen;
                            if (isMyGroupOpen) {
                                groupListAdapter.notifyItemRangeRemoved(memberAllGroupView.SimpleGroupList.size() + 2, recommendGroupList.size());
//                                groupListAdapter.notifyItemRangeChanged(linearLayoutManager.findFirstVisibleItemPosition(), getItemCount());
                            } else {
                                groupListAdapter.notifyItemRangeRemoved(2, recommendGroupList.size());
//                                groupListAdapter.notifyItemRangeChanged(1, 1);
                            }
                            holder.iv_HeadType.setImageResource(R.drawable.groupclose);
                        } else {
                            isRecomGroupOpen = !isRecomGroupOpen;
                            if (isMyGroupOpen) {
                                groupListAdapter.notifyItemRangeInserted(memberAllGroupView.SimpleGroupList.size() + 2, recommendGroupList.size());
//                                groupListAdapter.notifyItemRangeChanged(memberAllGroupView.SimpleGroupList.size() + 1, recommendGroupList.size());
                            } else {
                                groupListAdapter.notifyItemRangeInserted(2, recommendGroupList.size());
//                                groupListAdapter.notifyItemRangeChanged(1, recommendGroupList.size());
                            }
                            holder.iv_HeadType.setImageResource(R.drawable.groupopen);
                            rv_GroupList.scrollToPosition(getItemCount() - 1);
                        }
//                        isRecomGroupOpen = !isRecomGroupOpen;
//                        notifyDataSetChanged();
                    }
                });

                return;
            }


            final MemberAllGroupView.SimpleGroup simpleGroup;
            if (isMyGroupOpen && position > memberAllGroupView.SimpleGroupList.size()) {
                //推薦群組
                simpleGroup = recommendGroupList.get(position - 2 - memberAllGroupView.SimpleGroupList.size());
                if (isRecomGroupOpen)
                    holder.ll_GroupBlock.setVisibility(View.VISIBLE);
                else
                    holder.ll_GroupBlock.setVisibility(View.GONE);
            } else if (!isMyGroupOpen && position > 1) {
                simpleGroup = recommendGroupList.get(position - 2);
                if (isRecomGroupOpen)
                    holder.ll_GroupBlock.setVisibility(View.VISIBLE);
                else
                    holder.ll_GroupBlock.setVisibility(View.GONE);
            } else {
                //我的群組
                simpleGroup = memberAllGroupView.SimpleGroupList.get(position - 1);
                if (isMyGroupOpen)
                    holder.ll_GroupBlock.setVisibility(View.VISIBLE);
                else
                    holder.ll_GroupBlock.setVisibility(View.GONE);
            }

            holder.tv_GName.setText(simpleGroup.GName);

            if (simpleGroup.GImgName != null) {
                holder.iv_GImg.setImageDrawable(null);
                holder.iv_GImg.setTag(simpleGroup.GImgName);
                showImage(holder.iv_GImg, simpleGroup.GImgName, "G");
            } else {
                holder.iv_GImg.setTag("");
                holder.iv_GImg.setImageResource(R.drawable.defaultgimg);
            }

            holder.ll_GroupBlock.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    conGroupFragment.GetGPosts(simpleGroup.GroupId);
                }
            });
        }

        @Override
        public int getItemCount() {
            int ItemCount = 2;
            if (isMyGroupOpen) {
                ItemCount += memberAllGroupView.SimpleGroupList.size();
            }
            if (isRecomGroupOpen) {
                ItemCount += recommendGroupList.size();
            }
            return ItemCount;
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            private TextView tv_GName;
            private ImageView iv_GImg;
            private LinearLayout ll_GroupBlock;
            private ImageView iv_Resource;
            private TextView tv_Resource;
            private ImageView iv_HeadType;
            private LinearLayout ll_GroupHead;

            public ViewHolder(View itemView) {
                super(itemView);
                tv_GName = (TextView) itemView.findViewById(R.id.tv_GName);
                iv_GImg = (ImageView) itemView.findViewById(R.id.iv_GImg);
                ll_GroupBlock = (LinearLayout) itemView.findViewById(R.id.ll_GroupBlock);

                if (itemView == mHeaderView || itemView == mRHeaderView) {
                    iv_Resource = (ImageView) itemView.findViewById(R.id.iv_Resource);
                    tv_Resource = (TextView) itemView.findViewById(R.id.tv_Resource);
                    iv_HeadType = (ImageView) itemView.findViewById(R.id.iv_HeadType);
                    ll_GroupHead = (LinearLayout) itemView.findViewById(R.id.ll_GroupHead);
                }
            }
        }
    }

}
