package com.tsai.congroup;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import MyMethod.SharedService;
import MyMethod.SpaceItemDecoration;
import ViewModel.GroupSearchView;
import ViewModel.MemberAllGroupView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

public class SearchGroupActivity extends MySharedActivity {
    private GroupSearchView groupSearchView;
    private boolean isFirstLoad = true;
    private boolean isLoading = true;
    private boolean isFinishLoad = false;
    public RecyclerView rv_GroupList;
    private GroupListAdapter groupListAdapter;
    private int appendTimes = 1;
    private String keyWords;
    private EditText editText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_group);

        InitView(true, "");
        SetupUI(findViewById(R.id.activity_Outer));
        SetCache((int) Runtime.getRuntime().maxMemory() / 20);
        initView();
    }

    private void initView() {
        rv_GroupList = (RecyclerView) findViewById(R.id.rv_GroupList);
        editText = new EditText(this);
        editText.setHint("搜尋群組");
        editText.setMaxLines(1);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams((int) (myWidth * 0.7), RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
        params.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        editText.setLayoutParams(params);
        rl_toolBar.addView(editText);

        ImageButton imageButton = new ImageButton(this);
        imageButton.setImageResource(R.drawable.search);
        imageButton.setBackgroundColor(ContextCompat.getColor(this, R.color.colorTransparent));
        imageButton.setAdjustViewBounds(true);
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedService.HideKeyboard(SearchGroupActivity.this);
                activity_Outer.requestFocus();
                Refresh();
            }
        });
        params = new RelativeLayout.LayoutParams(SharedService.toolBarHeight, SharedService.toolBarHeight);
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
        params.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        imageButton.setLayoutParams(params);
        rl_toolBar.addView(imageButton);
    }

    public void Refresh() {
        keyWords = editText.getText().toString();
        groupSearchView = new GroupSearchView();
        clearLruCache();
        //避免重新接上網路時重整導致崩潰
        if (!isFirstLoad) {
            appendTimes = 1;
            isFinishLoad = false;
            groupListAdapter.notifyDataSetChanged();
        }
        SearchGroups();
    }

    public void SearchGroups() {

        Request request = new Request.Builder()
                .url(getString(R.string.BackEndPath) + "Api/GroupApi/SearchGroups?AppendTimes=" + appendTimes + "&KeyWords=" + keyWords)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        SharedService.ShowTextToast("請檢察網路連線", SearchGroupActivity.this);
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

                        isLoading = false;
                        if (StatusCode == 200) {
                            appendTimes++;
                            Gson gson = new Gson();
                            GroupSearchView tempView = gson.fromJson(ResMsg, GroupSearchView.class);
                            if (tempView.SimpleGroupList.size() == 0 && groupSearchView.SimpleGroupList.size() == 0) {
                                //無群組
                                SharedService.ShowTextToast("查無結果", SearchGroupActivity.this);
                            }
                            if (tempView.forAppend.NowAppendTimes == tempView.forAppend.MaxAppendTimes) {
                                isFinishLoad = true;
                                SharedService.ShowTextToast("已加載完畢", SearchGroupActivity.this);
                            }
                            groupSearchView.SimpleGroupList.addAll(tempView.SimpleGroupList);
                            if (isFirstLoad) {
                                isFirstLoad = false;
                                rv_GroupList.setLayoutManager(new LinearLayoutManager(SearchGroupActivity.this, LinearLayoutManager.VERTICAL, false));
                                groupListAdapter = new GroupListAdapter();
                                rv_GroupList.setAdapter(groupListAdapter);
                            } else {
                                groupListAdapter.notifyDataSetChanged();
                            }
                        } else {
                            SharedService.ShowErrorDialog(StatusCode + "", SearchGroupActivity.this);
                        }
                    }
                });

            }

        });
    }

    public class GroupListAdapter extends RecyclerView.Adapter<GroupListAdapter.ViewHolder> {

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            Context context = parent.getContext();
            View view = LayoutInflater.from(context).inflate(R.layout.group_block, parent, false);
            ViewHolder viewHolder = new ViewHolder(view);
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, final int position) {
            final MemberAllGroupView.SimpleGroup simpleGroup = groupSearchView.SimpleGroupList.get(position);
            holder.tv_GName.setText(simpleGroup.GName);

            if (simpleGroup.GImgName != null) {
                holder.iv_GImg.setImageDrawable(null);
                showImage(holder.iv_GImg, simpleGroup.GImgName, "G");
            } else {
                holder.iv_GImg.setImageResource(R.drawable.defaultgimg);
            }

            holder.ll_GroupBlock.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getIntent().putExtra("GroupId", groupSearchView.SimpleGroupList.get(position).GroupId);
                    setResult(RESULT_OK, getIntent());
                    finish();
                }
            });

            //避免重複請求
            if (position > groupSearchView.SimpleGroupList.size() * 0.6 && !isFinishLoad && !isLoading) {
                isLoading = true;
                SearchGroups();
            }
        }

        @Override
        public int getItemCount() {
            return groupSearchView.SimpleGroupList.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            private final TextView tv_GName;
            private final ImageView iv_GImg;
            private final LinearLayout ll_GroupBlock;

            public ViewHolder(View itemView) {
                super(itemView);
                tv_GName = (TextView) itemView.findViewById(R.id.tv_GName);
                iv_GImg = (ImageView) itemView.findViewById(R.id.iv_GImg);
                ll_GroupBlock = (LinearLayout) itemView.findViewById(R.id.ll_GroupBlock);
            }
        }
    }
}
