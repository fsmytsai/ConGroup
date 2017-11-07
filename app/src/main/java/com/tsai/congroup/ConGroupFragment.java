package com.tsai.congroup;


import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import MyMethod.SharedService;
import MyMethod.ViewPagerAdapter;
import ViewModel.GroupView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;


/**
 * A simple {@link Fragment} subclass.
 */
public class ConGroupFragment extends Fragment {

    private MainActivity mainActivity;
    public ViewPager viewPager;
    private ViewPagerAdapter viewPagerAdapter;
    private TabLayout tabs;
    public GroupView groupView;

    public ConGroupFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_con_group, container, false);
        mainActivity = (MainActivity) getActivity();
        initView(view);
        return view;
    }

    private void initView(View v) {
        List<Fragment> fragments = new ArrayList<Fragment>();
        fragments.add(new GroupListFragment());
        fragments.add(new PostListFragment());
        fragments.add(new NotifiListFragment());
        fragments.add(new OptionListFragment());

        viewPager = (ViewPager) v.findViewById(R.id.viewpager);
        viewPagerAdapter = new ViewPagerAdapter(getChildFragmentManager(), fragments, getActivity());
//        viewPagerAdapter.tabTitles = new String[]{"群組列", "所有貼文", "通知", "功能"};
        viewPagerAdapter.tabTitles = new String[]{"", "", "", ""};
        viewPagerAdapter.tabIcons = new int[]{R.drawable.group, R.drawable.article, R.drawable.notifi, R.drawable.function};
        viewPager.setAdapter(viewPagerAdapter);
        //使用getActivity()從MainActivity取值
        viewPager.setCurrentItem(getActivity().getIntent().getIntExtra("ViewItem", 1));
        viewPager.setOffscreenPageLimit(3);

        tabs = (TabLayout) v.findViewById(R.id.tabs);
        tabs.setTabMode(TabLayout.MODE_FIXED);
        tabs.setTabGravity(TabLayout.GRAVITY_FILL);
        tabs.setupWithViewPager(viewPager);
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                if (tab.getPosition() == 1) {
                    final RecyclerView rv_PostList = getPostListFragment().rv_PostList;
                    rv_PostList.smoothScrollToPosition(0);
                } else if (tab.getPosition() == 0) {
                    final RecyclerView rv_GroupList = getGroupListFragment().rv_GroupList;
                    rv_GroupList.smoothScrollToPosition(0);
                } else if (tab.getPosition() == 2) {
                    final RecyclerView rv_NotifiList = getNotifiListFragment().rv_NotifiList;
                    rv_NotifiList.smoothScrollToPosition(0);
                }
            }
        });
    }

    public GroupListFragment getGroupListFragment() {
        return (GroupListFragment) getChildFragmentManager()
                .findFragmentByTag("android:switcher:" + R.id.viewpager + ":0");
    }

    public PostListFragment getPostListFragment() {
        return (PostListFragment) getChildFragmentManager()
                .findFragmentByTag("android:switcher:" + R.id.viewpager + ":1");
    }

    public NotifiListFragment getNotifiListFragment() {
        return (NotifiListFragment) getChildFragmentManager()
                .findFragmentByTag("android:switcher:" + R.id.viewpager + ":2");
    }

    public OptionListFragment getOptionListFragment() {
        return (OptionListFragment) getChildFragmentManager()
                .findFragmentByTag("android:switcher:" + R.id.viewpager + ":3");
    }

    public void GetGPosts(final int GroupId) {
        getPostListFragment().ClearAllImageRequest();

        Request request = new Request.Builder()
                .url(getString(R.string.BackEndPath) + "Api/GroupApi/GetGroupByGroupId?groupId=" + GroupId)
                .build();

        mainActivity.client.newCall(request).enqueue(new Callback() {
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
                            Gson gson = new Gson();
                            groupView = gson.fromJson(ResMsg, GroupView.class);
                            getOptionListFragment().SetVisibility();
                            if (mainActivity.RecordGroupIdList.contains(GroupId)) {
                                mainActivity.RecordGroupIdList.remove(new Integer(GroupId));
                            }
                            mainActivity.RecordGroupIdList.add(GroupId);
//                            viewPagerAdapter.tabTitles[1] = groupView.Group.GName;
//                            //重設Title
//                            tabs.setupWithViewPager(viewPager);
                            viewPager.setCurrentItem(1);
                            PostListFragment postListFragment = getPostListFragment();
                            postListFragment.postType = true;
                            postListFragment.groupId = GroupId;
                            postListFragment.Refresh();
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

    public void GetMPosts() {
        //清除群組資料
        groupView = null;

//        viewPagerAdapter.tabTitles[1] = "所有貼文";
//        //重設Title
//        tabs.setupWithViewPager(viewPager);
        viewPager.setCurrentItem(1);
        PostListFragment postListFragment = getPostListFragment();
        postListFragment.postType = false;
        postListFragment.groupId = -1;
        postListFragment.Refresh();
        getOptionListFragment().SetVisibility();
    }

//    public void SetNotifiUnReadCount(int UnReadCount) {
//        i-f (UnReadCount == 0)
//            viewPagerAdapter.tabTitles[2] = "通知";
//        else
//            viewPagerAdapter.tabTitles[2] = "通知(" + UnReadCount + ")";
//        tabs.setupWithViewPager(viewPager);
//    }

    public void GotNewNotifi(int UnReadCount) {
        if (UnReadCount == 0)
            viewPagerAdapter.tabIcons[2] = R.drawable.notifi;
        else
            viewPagerAdapter.tabIcons[2] = R.drawable.notifil;
        tabs.setupWithViewPager(viewPager);
    }
}
