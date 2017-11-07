package com.tsai.congroup;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.Gson;

import java.io.IOException;

import MyMethod.SharedService;
import ViewModel.GroupMemberView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GroupMemberListActivity extends MySharedActivity {
    private int groupId;
    private int character;
    private String gName = "";
    private boolean isFirstLoad = true;
    private boolean isLoading = true;
    private boolean isFinishLoad = false;
    private int appendTimes = 1;

    private GroupMemberView groupMemberView;

    private SwipeRefreshLayout mSwipeLayout;
    public RecyclerView rv_GroupMemberList;
    private GroupMemberListAdapter groupMemberListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_member_list);
        groupId = getIntent().getIntExtra("GroupId", -1);
        character = getIntent().getIntExtra("Character", -1);
        gName = getIntent().getStringExtra("GName");
        InitView(true, gName + "的成員");
        SetCache((int) Runtime.getRuntime().maxMemory() / 20);
        rv_GroupMemberList = (RecyclerView) findViewById(R.id.rv_GroupMemberList);
        setSwipeRefresh();
        Refresh();
    }

    private void setSwipeRefresh() {
        mSwipeLayout = (SwipeRefreshLayout) findViewById(R.id.srl_GroupMemberList);
        mSwipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                //檢查網路連線
                if (!SharedService.CheckNetWork(GroupMemberListActivity.this)) {
                    SharedService.ShowTextToast("請檢察網路連線", GroupMemberListActivity.this);
                    mSwipeLayout.setRefreshing(false);
                    return;
                }
                Refresh();
            }
        });
        mSwipeLayout.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(GroupMemberListActivity.this, R.color.colorTransparent));
        // 设置下拉圆圈上的颜色，蓝色、绿色、橙色、红色
        mSwipeLayout.setColorSchemeResources(android.R.color.holo_blue_bright, android.R.color.holo_green_light,
                android.R.color.holo_orange_light, android.R.color.holo_red_light);
        mSwipeLayout.setDistanceToTriggerSync(400);// 设置手指在屏幕下拉多少距离会触发下拉刷新
        mSwipeLayout.setSize(SwipeRefreshLayout.DEFAULT);
    }

    public void Refresh() {
        mSwipeLayout.setRefreshing(true);
        clearLruCache();
        appendTimes = 1;
        groupMemberView = new GroupMemberView();
        //避免重新接上網路時重整導致崩潰
        if (!isFirstLoad) {
            isFinishLoad = false;
            groupMemberListAdapter.notifyDataSetChanged();
        }
        GetGroupMembers();
    }

    private void GetGroupMembers() {
        Request request = new Request.Builder()
                .url(getString(R.string.BackEndPath) + "Api/GroupMemberApi/GetGroupMembers?GroupId=" + groupId + "&AppendTimes=" + appendTimes)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        SharedService.ShowTextToast("請檢察網路連線", GroupMemberListActivity.this);
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final int StatusCode = response.code();
                final String ResMsg = response.body().string();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mSwipeLayout.setRefreshing(false);
                        isLoading = false;
                        if (StatusCode == 200) {
                            appendTimes++;
                            Gson gson = new Gson();
                            GroupMemberView tempView = gson.fromJson(ResMsg, GroupMemberView.class);

                            groupMemberView.GroupMemberList.addAll(tempView.GroupMemberList);
                            groupMemberView.MemberNameList.addAll(tempView.MemberNameList);
                            groupMemberView.MImgNameList.addAll(tempView.MImgNameList);

                            if (isFirstLoad) {
                                isFirstLoad = false;
                                rv_GroupMemberList.setLayoutManager(new LinearLayoutManager(GroupMemberListActivity.this, LinearLayoutManager.VERTICAL, false));
                                groupMemberListAdapter = new GroupMemberListAdapter();
                                rv_GroupMemberList.setAdapter(groupMemberListAdapter);
                            } else {
                                groupMemberListAdapter.notifyDataSetChanged();
                            }

                            if (tempView.forAppend.NowAppendTimes == tempView.forAppend.MaxAppendTimes) {
                                isFinishLoad = true;
                                View footer = LayoutInflater.from(GroupMemberListActivity.this).inflate(R.layout.footer, rv_GroupMemberList, false);
                                groupMemberListAdapter.setFooterView(footer);
                            }
                        } else {
                            SharedService.ShowErrorDialog(StatusCode + "", GroupMemberListActivity.this);
                        }
                    }
                });

            }

        });
    }


    public class GroupMemberListAdapter extends RecyclerView.Adapter<GroupMemberListAdapter.ViewHolder> {

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
            View view = LayoutInflater.from(context).inflate(R.layout.groupmember_block, parent, false);
            ViewHolder viewHolder = new ViewHolder(view);
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, final int position) {
            if (getItemViewType(position) == TYPE_FOOTER) {
                holder.tv_Footer.setText("群組沒有更多成員囉!");
                return;
            }

            if (groupMemberView.MImgNameList.get(position) != null) {
                holder.iv_MImg.setImageDrawable(null);
                holder.iv_MImg.setTag(groupMemberView.MImgNameList.get(position));
                showImage(holder.iv_MImg, groupMemberView.MImgNameList.get(position), "M");
            } else {
                holder.iv_MImg.setTag("");
                holder.iv_MImg.setImageResource(R.drawable.defaultmimg);
            }

            holder.tv_MemberName.setText(groupMemberView.MemberNameList.get(position));

            if (groupMemberView.GroupMemberList.get(position).Character == 1) {
                holder.iv_GroupMemberCharacter.setVisibility(View.VISIBLE);
                holder.iv_GroupMemberCharacter.setImageResource(R.drawable.mainmanager);
            } else if (groupMemberView.GroupMemberList.get(position).Character == 2) {
                holder.iv_GroupMemberCharacter.setVisibility(View.VISIBLE);
                holder.iv_GroupMemberCharacter.setImageResource(R.drawable.manager);
            } else {
                holder.iv_GroupMemberCharacter.setVisibility(View.GONE);
            }

            if ((character == 1 && groupMemberView.GroupMemberList.get(position).Character != 1)
                    || (character == 2 && groupMemberView.GroupMemberList.get(position).Character == 0)) {
                holder.ib_GroupMemberBox.setVisibility(View.VISIBLE);
                holder.ib_GroupMemberBox.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final PopupMenu popupmenu = new PopupMenu(GroupMemberListActivity.this, v);
                        popupmenu.getMenuInflater().inflate(R.menu.groupmember_popup, popupmenu.getMenu());

                        if (character == 2) {
                            popupmenu.getMenu().findItem(R.id.item_UpGAdmin).setVisible(false);
                            popupmenu.getMenu().findItem(R.id.item_DownGAdmin).setVisible(false);
                        }

                        if (groupMemberView.GroupMemberList.get(position).Character > 0) {
                            popupmenu.getMenu().findItem(R.id.item_Kick).setVisible(false);
                            popupmenu.getMenu().findItem(R.id.item_UpGAdmin).setVisible(false);
                        } else {
                            popupmenu.getMenu().findItem(R.id.item_DownGAdmin).setVisible(false);
                        }

                        popupmenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() { // 設定popupmenu項目點擊傾聽者.

                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                switch (item.getItemId()) { // 取得被點擊的項目id.
                                    case R.id.item_Kick:
                                        KickGroupMember(position);
                                        break;
                                    case R.id.item_UpGAdmin:
                                        UpdateGAdmin("true", position);
                                        break;
                                    case R.id.item_DownGAdmin:
                                        UpdateGAdmin("false", position);
                                        break;
                                }
                                return true;
                            }

                        });
                        popupmenu.show();
                    }
                });
            } else {
                holder.ib_GroupMemberBox.setVisibility(View.GONE);
            }

            holder.ll_GroupMember.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(GroupMemberListActivity.this, ProFileActivity.class);
                    intent.putExtra("Account", groupMemberView.GroupMemberList.get(position).Account);
                    startActivity(intent);
                }
            });

            //避免重複請求
            if (position > groupMemberView.GroupMemberList.size() * 0.6 && !isFinishLoad && !isLoading) {
                isLoading = true;
                GetGroupMembers();
            }
        }

        @Override
        public int getItemCount() {
            int NormalCount = groupMemberView.GroupMemberList.size();
            if (mHeaderView != null)
                NormalCount++;
            if (mFooterView != null)
                NormalCount++;
            return NormalCount;
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            private TextView tv_MemberName;
            private ImageView iv_MImg;
            private ImageButton ib_GroupMemberBox;
            private TextView tv_Footer;
            private LinearLayout ll_GroupMember;
            private ImageView iv_GroupMemberCharacter;

            public ViewHolder(View itemView) {
                super(itemView);
                if (itemView == mFooterView) {
                    tv_Footer = (TextView) itemView.findViewById(R.id.tv_Footer);
                    return;
                }
                tv_MemberName = (TextView) itemView.findViewById(R.id.tv_MemberName);
                ib_GroupMemberBox = (ImageButton) itemView.findViewById(R.id.ib_GroupMemberBox);
                iv_MImg = (ImageView) itemView.findViewById(R.id.iv_MImg);
                tv_Footer = (TextView) itemView.findViewById(R.id.tv_Footer);
                ll_GroupMember = (LinearLayout) itemView.findViewById(R.id.ll_GroupMember);
                iv_GroupMemberCharacter = (ImageView) itemView.findViewById(R.id.iv_GroupMemberCharacter);
            }
        }
    }

    private void KickGroupMember(final int Position) {
        new AlertDialog.Builder(this)
                .setTitle("確認踢除")
                .setMessage("您確定要把 " + groupMemberView.MemberNameList.get(Position) + " 踢出 " + gName + " 嗎?")
                .setNegativeButton("取消", null)
                .setPositiveButton("確定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        RequestBody formBody = new FormBody.Builder()
                                .add("Account", groupMemberView.GroupMemberList.get(Position).Account)
                                .add("GroupId", groupId + "")
                                .build();

                        Request request = new Request.Builder()
                                .url(getString(R.string.BackEndPath) + "Api/GroupMemberApi/KickGroupMember")
                                .post(formBody)
                                .build();

                        client.newCall(request).enqueue(new Callback() {
                            @Override
                            public void onFailure(Call call, IOException e) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        SharedService.ShowTextToast("請檢察網路連線", GroupMemberListActivity.this);
                                    }
                                });
                            }

                            @Override
                            public void onResponse(Call call, Response response) throws IOException {
                                final int StatusCode = response.code();
                                final String ResMsg = response.body().string();

                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (StatusCode == 200) {
                                            groupMemberView.GroupMemberList.remove(Position);
                                            groupMemberView.MemberNameList.remove(Position);
                                            groupMemberView.MImgNameList.remove(Position);
                                            groupMemberListAdapter.notifyItemRemoved(Position);
                                            groupMemberListAdapter.notifyItemRangeChanged(Position, groupMemberView.GroupMemberList.size() - Position);
                                        } else if (StatusCode == 400) {
                                            SharedService.ShowErrorDialog(ResMsg, GroupMemberListActivity.this);
                                        } else {
                                            SharedService.ShowTextToast("ERROR:" + StatusCode, GroupMemberListActivity.this);
                                        }
                                    }
                                });
                            }
                        });
                    }
                })
                .show();
    }

    private void UpdateGAdmin(final String IsUp, final int Position) {
        RequestBody formBody = new FormBody.Builder()
                .add("Account", groupMemberView.GroupMemberList.get(Position).Account)
                .add("GroupId", groupId + "")
                .add("IsUp", IsUp)
                .build();

        Request request = new Request.Builder()
                .url(getString(R.string.BackEndPath) + "Api/GroupMemberApi/UpdateGAdmin")
                .put(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        SharedService.ShowTextToast("請檢察網路連線", GroupMemberListActivity.this);
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final int StatusCode = response.code();
                final String ResMsg = response.body().string();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (StatusCode == 200) {
                            if (IsUp.equals("true"))
                                groupMemberView.GroupMemberList.get(Position).Character = 2;
                            else
                                groupMemberView.GroupMemberList.get(Position).Character = 0;
                            groupMemberListAdapter.notifyDataSetChanged();
                        } else if (StatusCode == 400) {
                            SharedService.ShowErrorDialog(ResMsg, GroupMemberListActivity.this);
                        } else {
                            SharedService.ShowTextToast("ERROR:" + StatusCode, GroupMemberListActivity.this);
                        }
                    }
                });
            }
        });
    }
}
