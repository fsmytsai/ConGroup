package com.tsai.congroup;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.Gson;

import java.io.IOException;

import MyMethod.SharedService;
import ViewModel.NotificationView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static android.app.Activity.RESULT_OK;


/**
 * A simple {@link Fragment} subclass.
 */
public class NotifiListFragment extends MySharedFragment {
    private final int OPENPOST_CODE = 517;
    private MainActivity mainActivity;
    private ConGroupFragment conGroupFragment;
    private SwipeRefreshLayout mSwipeLayout;

    public NotificationView notificationView;

    private boolean isFirstLoad = true;
    private boolean isLoading = true;
    private boolean isFinishLoad = false;

    public RecyclerView rv_NotifiList;
    private NotifiListAdapter notifiListAdapter;

    private int skip = 0;
    private int NowPosition = -1;
    private int UnReadCount = 0;

    public NotifiListFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_notifi_list, container, false);
        mainActivity = (MainActivity) getActivity();
        super.client = mainActivity.client;
        super.imageClient = SharedService.GetClient(getActivity());
        SetCache((int) Runtime.getRuntime().maxMemory() / 20);
        conGroupFragment = (ConGroupFragment) getParentFragment();

        rv_NotifiList = (RecyclerView) view.findViewById(R.id.rv_NotifiList);
        getActivity().registerForContextMenu(rv_NotifiList);
        setSwipeRefresh(view);
        Refresh();
        return view;
    }

    private void setSwipeRefresh(View view) {
        mSwipeLayout = (SwipeRefreshLayout) view.findViewById(R.id.srl_NotifiList);
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
        UnReadCount = 0;
        notificationView = new NotificationView();
        //避免重新接上網路時重整導致崩潰
        if (!isFirstLoad) {
            isFinishLoad = false;
            notifiListAdapter.notifyDataSetChanged();
        }
        GetNList();
    }

    private void GetNList() {
        Request request = new Request.Builder()
                .url(getString(R.string.BackEndPath) + "Api/NotificationApi/GetNList?Skip=" + skip)
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
                            NotificationView tempView = gson.fromJson(ResMsg, NotificationView.class);
                            if (isFirstLoad && tempView.NotificationList.size() == 0) {
                                //無通知
                            }
                            for (NotificationView.Notifications notification : tempView.NotificationList) {
                                if (!notification.IsRead)
                                    UnReadCount++;
                            }
//                            conGroupFragment.SetNotifiUnReadCount(UnReadCount);
                            conGroupFragment.GotNewNotifi(UnReadCount);
                            notificationView.NotificationList.addAll(tempView.NotificationList);
                            notificationView.MImgNameList.addAll(tempView.MImgNameList);
                            notificationView.LTimeList.addAll(tempView.LTimeList);
                            skip += tempView.NotificationList.size();

                            if (isFirstLoad) {
                                isFirstLoad = false;

                                rv_NotifiList.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
                                notifiListAdapter = new NotifiListAdapter();
                                rv_NotifiList.setAdapter(notifiListAdapter);
                            } else {
                                notifiListAdapter.notifyDataSetChanged();
                            }

                            if (tempView.NotificationList.size() < tempView.ARequestCount) {
                                //最後一次載入
                                isFinishLoad = true;
                                View footer = LayoutInflater.from(getActivity()).inflate(R.layout.footer, rv_NotifiList, false);
                                notifiListAdapter.setFooterView(footer);
                            }
                        } else {
                            SharedService.ShowErrorDialog(StatusCode + "", getActivity());
                        }
                    }
                });

            }

        });
    }

    public void GetNFromSignalr(final NotificationView tempView) {
        if (tempView.NotificationList.size() == 0)
            return;

        for (int i = 0; i < tempView.NotificationList.size(); i++) {
            for (int j = 0; j < notificationView.NotificationList.size(); j++) {
                if (notificationView.NotificationList.get(j).NotifiId.equals(tempView.NotificationList.get(i).NotifiId)) {
                    notificationView.NotificationList.remove(j);
                    skip--;
                    break;
                }
            }
        }

        notificationView.NotificationList.addAll(0, tempView.NotificationList);
        notificationView.MImgNameList.addAll(0, tempView.MImgNameList);
        notificationView.LTimeList.addAll(0, tempView.LTimeList);

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                notifiListAdapter.notifyItemRangeInserted(0, tempView.NotificationList.size());
                notifiListAdapter.notifyItemRangeChanged(0, notificationView.NotificationList.size());
                rv_NotifiList.scrollToPosition(0);

                skip += tempView.NotificationList.size();
                UnReadCount++;
//                conGroupFragment.SetNotifiUnReadCount(UnReadCount);
                conGroupFragment.GotNewNotifi(UnReadCount);
            }
        });
    }

    public class NotifiListAdapter extends RecyclerView.Adapter<NotifiListAdapter.ViewHolder> {

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
            View view = LayoutInflater.from(context).inflate(R.layout.notification_block, parent, false);
            ViewHolder viewHolder = new ViewHolder(view);
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, final int position) {
            if (getItemViewType(position) == TYPE_FOOTER) {
                if (notificationView.NotificationList.size() == 0)
                    holder.tv_Footer.setText("您沒有任何通知!");
                else
                    holder.tv_Footer.setText("沒有更多通知囉!");
                return;
            }


            holder.tv_Content.setText(notificationView.NotificationList.get(position).Content);
            holder.tv_LTime.setText(notificationView.LTimeList.get(position));

            if (notificationView.MImgNameList.get(position) != null) {
                holder.iv_MImg.setImageDrawable(null);
                holder.iv_MImg.setTag(notificationView.MImgNameList.get(position));
                showImage(holder.iv_MImg, notificationView.MImgNameList.get(position), "M");
            } else {
                holder.iv_MImg.setTag("");
                holder.iv_MImg.setImageResource(R.drawable.defaultmimg);
            }

            //避免重複請求
            if (position > notificationView.NotificationList.size() * 0.6 && !isFinishLoad && !isLoading) {
                isLoading = true;
                GetNList();
            }

            holder.ll_Notification.setTag(position);
            if (notificationView.NotificationList.get(position).IsRead) {
                holder.ll_Notification.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.colorTransparent));
            } else {
                holder.ll_Notification.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.colorOrange));
            }

            //ClickListener
            holder.ll_Notification.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Read(position, true);
                }
            });

            //ContextMenu
            holder.ll_Notification.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
                public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                    NowPosition = (int) v.getTag();
                    menu.add(0, 0, 0, "標示為已讀");
                    menu.add(0, 1, 0, "删除通知");
                }
            });
        }

        @Override
        public int getItemCount() {
            int NormalCount = notificationView.NotificationList.size();
            if (mHeaderView != null)
                NormalCount++;
            if (mFooterView != null)
                NormalCount++;
            return NormalCount;
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            private TextView tv_Content;
            private TextView tv_LTime;
            private ImageView iv_MImg;
            private LinearLayout ll_Notification;
            private TextView tv_Footer;

            public ViewHolder(View itemView) {
                super(itemView);
                if (itemView == mFooterView) {
                    tv_Footer = (TextView) itemView.findViewById(R.id.tv_Footer);
                    return;
                }
                tv_Content = (TextView) itemView.findViewById(R.id.tv_NotifiContent);
                tv_LTime = (TextView) itemView.findViewById(R.id.tv_LTime);
                iv_MImg = (ImageView) itemView.findViewById(R.id.iv_MImg);
                ll_Notification = (LinearLayout) itemView.findViewById(R.id.ll_Notification);

            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 0:
                Read(NowPosition, false);  //更新事件的方法
                NowPosition = -1;
                return true;
            case 1:
                DeleteNotifi(NowPosition);  //刪除事件的方法
                NowPosition = -1;
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void Read(final int Position, final boolean IsClick) {
        conGroupFragment.getPostListFragment().ClearAllImageRequest();
        final String NotifiId = notificationView.NotificationList.get(Position).NotifiId;
        RequestBody formBody = new FormBody.Builder()
                .add("NotifiId", NotifiId)
                .build();

        Request request = new Request.Builder()
                .url(getString(R.string.BackEndPath) + "Api/NotificationApi/ReadNotification")
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
                            NotificationView.Notifications tempData = notificationView.NotificationList.get(Position);
                            if (!tempData.IsRead) {
                                LinearLayout ll_Notification = (LinearLayout) rv_NotifiList.findViewWithTag(Position);
                                ll_Notification.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.colorTransparent));
                                tempData.IsRead = true;
                                UnReadCount--;
//                                conGroupFragment.SetNotifiUnReadCount(UnReadCount);
                                conGroupFragment.GotNewNotifi(UnReadCount);
                                notificationView.NotificationList.set(Position, tempData);
                            }
                            if (IsClick)
                                DoNotifiClickEvent(NotifiId);
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

    private void DeleteNotifi(final int Position) {
        final String NotifiId = notificationView.NotificationList.get(Position).NotifiId;
        Request request = new Request.Builder()
                .url(getString(R.string.BackEndPath) + "Api/NotificationApi/DeleteNotification?NotifiId=" + NotifiId)
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
                            if (!notificationView.NotificationList.get(Position).IsRead) {
                                UnReadCount--;
//                                conGroupFragment.SetNotifiUnReadCount(UnReadCount);
                                conGroupFragment.GotNewNotifi(UnReadCount);
                            }
                            skip--;
                            notificationView.NotificationList.remove(Position);
                            notificationView.MImgNameList.remove(Position);
                            notificationView.LTimeList.remove(Position);
                            notifiListAdapter.notifyItemRemoved(Position);
                            int dItemCount = notificationView.NotificationList.size() - Position;
                            notifiListAdapter.notifyItemRangeChanged(Position, dItemCount);
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

    private void DoNotifiClickEvent(String NotifiId) {
        int Type = Integer.valueOf(NotifiId.substring(0, 2));
        int Id = Integer.valueOf(NotifiId.substring(2));

        if (Type == 1 || Type == 7) {
            conGroupFragment.GetGPosts(Id);
        } else if (Type == 2 || Type == 3) {
            OpenPostActivity(Id);
        } else {
            SharedService.ShowTextToast("此種類通知暫無點擊事件", getActivity());
        }
    }

    private void OpenPostActivity(int PostId) {
        Intent intent = new Intent(getActivity(), PostActivity.class);
        intent.putExtra("PostId", PostId);
        startActivityForResult(intent, OPENPOST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OPENPOST_CODE && resultCode == RESULT_OK) {
            int GroupId = data.getIntExtra("GroupId", -1);
            if (GroupId != -1)
                conGroupFragment.GetGPosts(GroupId);
        }
    }
}