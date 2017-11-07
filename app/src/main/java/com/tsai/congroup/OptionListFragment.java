package com.tsai.congroup;


import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.IOException;

import MyMethod.SharedService;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static android.app.Activity.RESULT_OK;


/**
 * A simple {@link Fragment} subclass.
 */
public class OptionListFragment extends MySharedFragment {
    private final int ADDGROUP_CODE = 96;
    //    private String[] optionList = {"個人頁面", "新增群組", "介紹頁面", "登出"};
//    public RecyclerView rv_OptionList;
//    private OptionListAdapter optionListAdapter;
    private MainActivity mainActivity;
    private ConGroupFragment conGroupFragment;


    private LinearLayout ll_ProFile;
    private LinearLayout ll_GroupMemberList;
    private LinearLayout ll_AddGroup;
    private LinearLayout ll_Introduction;
    private LinearLayout ll_Logout;

    public OptionListFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_option_list, container, false);
        mainActivity = (MainActivity) getActivity();
        conGroupFragment = (ConGroupFragment) getParentFragment();
        super.client = mainActivity.client;
//        rv_OptionList = (RecyclerView) view.findViewById(R.id.rv_OptionList);
//        rv_OptionList.setLayoutManager(new LinearLayoutManager(getActivity()));
//        optionListAdapter = new OptionListAdapter();
//
//        rv_OptionList.setAdapter(optionListAdapter);
        initView(view);
        return view;
    }

    private void initView(View v) {
        ll_ProFile = (LinearLayout) v.findViewById(R.id.ll_ProFile);
        ll_GroupMemberList = (LinearLayout) v.findViewById(R.id.ll_GroupMemberList);
        ll_AddGroup = (LinearLayout) v.findViewById(R.id.ll_AddGroup);
        ll_Introduction = (LinearLayout) v.findViewById(R.id.ll_Introduction);
        ll_Logout = (LinearLayout) v.findViewById(R.id.ll_Logout);

        ll_ProFile.setOnClickListener(MyClick);
        ll_GroupMemberList.setOnClickListener(MyClick);
        ll_AddGroup.setOnClickListener(MyClick);
        ll_Introduction.setOnClickListener(MyClick);
        ll_Logout.setOnClickListener(MyClick);

        SetVisibility();
    }

    View.OnClickListener MyClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.ll_ProFile:
                    OpenProFile();
                    break;
                case R.id.ll_GroupMemberList:
                    GetGroupMembers();
                    break;
                case R.id.ll_AddGroup:
                    AddGroup();
                    break;
                case R.id.ll_Introduction:
                    GoIntroduction();
                    break;
                case R.id.ll_Logout:
                    Logout();
                    break;
            }
        }
    };

    public void SetVisibility() {
        if (conGroupFragment.groupView != null) {
            ll_GroupMemberList.setVisibility(View.VISIBLE);
            ll_AddGroup.setVisibility(View.GONE);
            ll_Introduction.setVisibility(View.GONE);
        } else {
            ll_GroupMemberList.setVisibility(View.GONE);
            ll_AddGroup.setVisibility(View.VISIBLE);
            ll_Introduction.setVisibility(View.VISIBLE);
        }
    }

    //    public class OptionListAdapter extends RecyclerView.Adapter<OptionListAdapter.ViewHolder> {
//
//        @Override
//        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
//            Context context = parent.getContext();
//            View view = LayoutInflater.from(context).inflate(R.layout.groupmember_block, parent, false);
//            ViewHolder viewHolder = new ViewHolder(view);
//            return viewHolder;
//        }
//
//        @Override
//        public void onBindViewHolder(ViewHolder holder, final int position) {
//            holder.tv_Option.setText(optionList[position]);
//            holder.tv_Option.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    if (position == 0) {
//                        Intent intent = new Intent(getActivity(), ProFileActivity.class);
//                        intent.putExtra("Account", SharedService.identityView.Account);
//                        startActivity(intent);
//                    }
//                    if (position == 1) {
//                        Intent intent = new Intent(getActivity(), AddGroupActivity.class);
//                        startActivityForResult(intent, ADDGROUP_CODE);
//                    }
//                    if (position == 2) {
//                        mainActivity.InitView(false, "ConGroup");
//                        getActivity().getSupportFragmentManager()
//                                .beginTransaction()
//                                .replace(R.id.MainFrameLayout, new IntroductionFragment(), "IntroductionFragment")
//                                .commit();
//                    }
//                    if (position == 3) {
//                        Logout();
//                    }
//                }
//            });
//        }
//
//        @Override
//        public int getItemCount() {
//            return optionList.length;
//        }
//
//        public class ViewHolder extends RecyclerView.ViewHolder {
//            private final TextView tv_Option;
//
//            public ViewHolder(View itemView) {
//                super(itemView);
//                tv_Option = (TextView) itemView.findViewById(R.id.tv_Option);
//            }
//        }
//    }
    public void OpenProFile() {
        Intent intent = new Intent(getActivity(), ProFileActivity.class);
        intent.putExtra("Account", SharedService.identityView.Account);
        startActivity(intent);
    }

    public void GetGroupMembers() {
        Intent intent = new Intent(getActivity(), GroupMemberListActivity.class);
        intent.putExtra("GroupId", conGroupFragment.groupView.Group.GroupId);
        intent.putExtra("Character", conGroupFragment.groupView.Character);
        intent.putExtra("GName", conGroupFragment.groupView.Group.GName);
        startActivity(intent);
    }

    public void AddGroup() {
        Intent intent = new Intent(getActivity(), AddGroupActivity.class);
        startActivityForResult(intent, ADDGROUP_CODE);
    }

    public void GoIntroduction() {
        mainActivity.InitView(false, "ConGroup");
        getActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.MainFrameLayout, new IntroductionFragment(), "IntroductionFragment")
                .commit();
    }

    public void Logout() {
        SharedService.sp_httpData.edit().putString("Cookie", "").apply();
        SharedService.identityView = null;
        mainActivity.rl_toolBar.removeAllViews();
        getActivity().stopService(new Intent(getActivity(), SignalrService.class));
        mainActivity.CheckLogon();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ADDGROUP_CODE && resultCode == RESULT_OK) {
            int GroupId = data.getIntExtra("GroupId", -1);
            if (GroupId == -1)
                return;
            ConGroupFragment conGroupFragment = (ConGroupFragment) getParentFragment();
            conGroupFragment.GetGPosts(GroupId);
            conGroupFragment.getGroupListFragment().Refresh();
        }
    }
}
