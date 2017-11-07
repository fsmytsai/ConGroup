package com.tsai.congroup;


import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.List;

import MyMethod.SharedService;
import ViewModel.GroupView;
import ViewModel.MemberAllGroupView;
import ViewModel.PostView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


/**
 * A simple {@link Fragment} subclass.
 */
public class PostListFragment extends MySharedFragment {
    private final int Comment_CODE = 89;
    private final int AddPost_CODE = 666;
    private final int EDITPOST_CODE = 78;
    private final int EDITGROUP_CODE = 423;

    private MainActivity mainActivity;
    private ConGroupFragment conGroupFragment;
    public SwipeRefreshLayout mSwipeLayout;

    private PostView postView;
//    private PostView tempView;

    private boolean isFirstLoad = true;
    private boolean isLoading = true;
    private boolean isFinishLoad = false;
    private boolean isLiking = false;
    //    private boolean toLoadAllImages = false;
    private String PostIdString = "";

    public RecyclerView rv_PostList;
    private PostListAdapter postListAdapter;

    //false=MPost  true=GPost
    public boolean postType = false;
    public int groupId = -1;

    //ajs使用
    private int nowPosition = -1;

    public PostListFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_post_list, container, false);

        mainActivity = (MainActivity) getActivity();
        conGroupFragment = (ConGroupFragment) getParentFragment();
        super.client = mainActivity.client;
        super.imageClient = SharedService.GetClient(getActivity());
        SetCache((int) Runtime.getRuntime().maxMemory() / 5);

        rv_PostList = (RecyclerView) view.findViewById(R.id.rv_PostList);
        setSwipeRefresh(view);
        return view;
    }

    private void setSwipeRefresh(View view) {
        mSwipeLayout = (SwipeRefreshLayout) view.findViewById(R.id.srl_PostList);
        mSwipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                //檢查網路連線
                if (!SharedService.CheckNetWork(getActivity())) {
                    SharedService.ShowTextToast("請檢察網路連線", getActivity());
                    mSwipeLayout.setRefreshing(false);
                    return;
                }

                //ClearAllImageRequest();
                if (postType)
                    conGroupFragment.GetGPosts(conGroupFragment.groupView.Group.GroupId);
                else
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

    View.OnClickListener MyClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.bt_Like:
                    if (!isLiking)
                        Like(v);
                    break;
                case R.id.ib_PostBox:
                    SetPostPopupMenu(v);
                    break;
                case R.id.bt_Comment:
                    OpenComment(v);
                    break;
                case R.id.tv_GName:
                    conGroupFragment.GetGPosts(postView.PostList.get((int) v.getTag()).GroupId);
                    break;
                case R.id.tv_LocationName:
                    OpenMap(v);
                    break;
                case R.id.tv_MemberName:
                    OpenProFile((int) v.getTag());
                    break;
                case R.id.bt_JoinGroup:
                    JoinGroup();
                    break;
                case R.id.ib_GroupBox:
                    SetGroupPopupMenu(v);
                    break;
            }
        }
    };

    public void Refresh() {
        mSwipeLayout.setRefreshing(true);
        clearLruCache();
        postView = new PostView();
        PostIdString = "";
        //避免重新接上網路時重整導致崩潰
        if (!isFirstLoad) {
            isFinishLoad = false;
            postListAdapter.notifyDataSetChanged();
        }
        GetPosts();
    }

    public void GetPosts() {
        String url;
        if (postType) {
            //群組貼文

            //還沒加入
            if (!conGroupFragment.groupView.IsJoin) {
                // 停止刷新
                mSwipeLayout.setRefreshing(false);
                if (postListAdapter.mFooterView == null) {
                    View footer = LayoutInflater.from(getActivity()).inflate(R.layout.footer, rv_PostList, false);
                    postListAdapter.setFooterView(footer);
                    postListAdapter.notifyDataSetChanged();
                }
                return;
            }
            url = getString(R.string.BackEndPath) + "Api/PostApi/DisplayGPosts?groupId=" + groupId + "&" + PostIdString;
        } else {
            //會員貼文
            url = getString(R.string.BackEndPath) + "Api/PostApi/DisplayMPosts?" + PostIdString;
        }
        Request request = new Request.Builder()
                .url(url)
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
                            PostView tempView = gson.fromJson(ResMsg, PostView.class);

                            //預先載入全部圖片
                            loadImages(tempView);
//                            toLoadAllImages = true;

                            for (int i = 0; i < tempView.PostList.size(); i++) {
                                PostIdString += "PostIdList=";
                                PostIdString += String.valueOf(tempView.PostList.get(i).PostId);
                                PostIdString += "&";
                            }

                            postView.PostList.addAll(tempView.PostList);
                            postView.MemberNameList.addAll(tempView.MemberNameList);
                            postView.MImgNameList.addAll(tempView.MImgNameList);
                            postView.GNameList.addAll(tempView.GNameList);
                            postView.CTimeList.addAll(tempView.CTimeList);
                            postView.IsMarkList.addAll(tempView.IsMarkList);
                            postView.IsLikeList.addAll(tempView.IsLikeList);
                            postView.LikeNumList.addAll(tempView.LikeNumList);
                            postView.CommentNumList.addAll(tempView.CommentNumList);

                            if (isFirstLoad) {
                                isFirstLoad = false;
                                rv_PostList.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
                                postListAdapter = new PostListAdapter();

                                View header = LayoutInflater.from(getActivity()).inflate(R.layout.post_head, rv_PostList, false);
                                postListAdapter.setHeaderView(header);

                                rv_PostList.setAdapter(postListAdapter);
                            } else {
                                postListAdapter.notifyDataSetChanged();
                            }

                            if (tempView.PostList.size() < tempView.ARequestCount) {
                                //最後一次載入
                                isFinishLoad = true;
                                View footer = LayoutInflater.from(getActivity()).inflate(R.layout.footer, rv_PostList, false);
                                postListAdapter.setFooterView(footer);
                            }
                        } else {
                            SharedService.ShowErrorDialog(StatusCode + "", getActivity());
                        }
                    }
                });
            }
        });
    }

    public void loadImages(PostView tempView) {

        for (int i = 0; i < tempView.PostList.size(); i++) {
            //大頭貼
            String MImgName = tempView.MImgNameList.get(i);
            if (MImgName != null) {
                showImage(null, MImgName, "M");
            }

            //貼文圖片
            if (tempView.PostList.get(i).PostImages.size() > 0) {
                for (PostView.PostImage postImage : tempView.PostList.get(i).PostImages) {
                    showImage(null, postImage.ImgName, "P");
                }
            }
        }
    }

    public class PostListAdapter extends RecyclerView.Adapter<PostListAdapter.ViewHolder> {
        public final int TYPE_HEADER = 0;  //说明是带有Header的
        public final int TYPE_FOOTER = 1;  //说明是带有Footer的
        public final int TYPE_NORMAL = 2;  //说明是不带有header和footer的
        private View mHeaderView;
        public View mFooterView;

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

//        public PostListAdapter() {
//            linearLayoutManager = (LinearLayoutManager) rv_PostList.getLayoutManager();
//            rv_PostList.addOnScrollListener(new RecyclerView.OnScrollListener() {
//                @Override
//                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
//                    super.onScrollStateChanged(recyclerView, newState);
//
//                    if (toLoadAllImages) {
//                        toLoadAllImages = false;
//                        loadImages();
//                        tempView = null;
//                    }
////                    //避免下拉刷新也加載圖片，避免RecyclerView為空時依然加載圖片
//                    if (linearLayoutManager.findFirstVisibleItemPosition() != 0 && postView.PostList.size() != 0) {
//                        if (newState == RecyclerView.SCROLL_STATE_IDLE) {
//                            mStart = linearLayoutManager.findFirstVisibleItemPosition();
//                            mEnd = linearLayoutManager.findLastVisibleItemPosition();
//
//                            if (mHeaderView != null) {
//                                mEnd--;
//                                if (mStart != 0) {
//                                    mStart--;
//                                }
//                            }
//                            Log.d("Start", mStart + "");
//                            Log.d("mEnd", mEnd + "");
//                            imageLoader.loadImages(mStart, mEnd);
//                        } else {
//                            imageLoader.cancelAllAsyncTask();
//                        }
//                    }
//                }
//
//                @Override
//                public void onScrolled(RecyclerView recyclerView,
//                                       int dx, int dy) {
//                    super.onScrolled(recyclerView, dx, dy);
//                    if (toLoadAllImages) {
//                        toLoadAllImages = false;
//                        loadImages();
//                        tempView = null;
//                    }
//                    mStart = linearLayoutManager.findFirstVisibleItemPosition();
//                    mEnd = linearLayoutManager.findLastVisibleItemPosition();
//
//                    if (mHeaderView != null) {
//                        mEnd--;
//                        if (mStart != 0) {
//                            mStart--;
//                        }
//                    }
//                    if (isFirstIn && mStart != -1) {
//                        imageLoader.loadImages(mStart, mEnd);
//                        isFirstIn = false;
//                    }
//                }
//            });
//        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            Context context = parent.getContext();

            if (mHeaderView != null && viewType == TYPE_HEADER) {
                return new ViewHolder(mHeaderView);
            }
            if (mFooterView != null && viewType == TYPE_FOOTER) {
                return new ViewHolder(mFooterView);
            }
            View view = LayoutInflater.from(context).inflate(R.layout.post_block, parent, false);
            ViewHolder viewHolder = new ViewHolder(view);
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            if (getItemViewType(position) == TYPE_FOOTER) {
                if (postType) {
                    if (conGroupFragment.groupView.IsJoin)
                        holder.tv_Footer.setText("沒有更多貼文囉!");
                    else
                        holder.tv_Footer.setText("加入 " + conGroupFragment.groupView.Group.GName + " 以查看貼文");
                } else {
                    if (conGroupFragment.getGroupListFragment().memberAllGroupView.SimpleGroupList.size() == 0)
                        holder.tv_Footer.setText("加入群組才會有貼文哦!");
                    else
                        holder.tv_Footer.setText("沒有更多貼文囉!");
                }
                return;
            }

            if (getItemViewType(position) == TYPE_HEADER) {
                if (postType) {
                    holder.ll_GroupHead.setVisibility(View.VISIBLE);
                    GroupView groupView = conGroupFragment.groupView;
                    if (groupView.Group.GImgName != null) {
                        holder.iv_GImg.setImageDrawable(null);
                        holder.iv_GImg.setTag(groupView.Group.GImgName);
                        showBigImage(holder.iv_GImg, groupView.Group.GImgName, "G");
                    } else
                        holder.iv_GImg.setImageResource(R.drawable.defaultgimg);

                    holder.tv_GName.setText(groupView.Group.GName);
                    String[] GTypes = {"同好會", "朋友圈", "公司"};
                    holder.tv_GType.setText(GTypes[groupView.Group.GType - 1]);
                    holder.tv_GIntro.setText("群組簡介 : " + groupView.Group.GIntroduction);

                    if (groupView.IsJoin) {
                        holder.ll_AddPost.setVisibility(View.VISIBLE);
                        holder.bt_JoinGroup.setVisibility(View.GONE);
                    } else {
                        holder.ll_AddPost.setVisibility(View.GONE);
                        holder.bt_JoinGroup.setVisibility(View.VISIBLE);
                        holder.bt_JoinGroup.setOnClickListener(MyClick);
                        if (groupView.Group.JoinType == 1)
                            holder.bt_JoinGroup.setText("加入社團");
                        else {
                            if (groupView.IsApply)
                                holder.bt_JoinGroup.setText("取消申請");
                            else
                                holder.bt_JoinGroup.setText("申請加入");
                        }
                    }
                    holder.ib_GroupBox.setOnClickListener(MyClick);
                } else {
                    if (conGroupFragment.getGroupListFragment().memberAllGroupView.SimpleGroupList.size() == 0)
                        holder.ll_AddPost.setVisibility(View.GONE);
                    else
                        holder.ll_AddPost.setVisibility(View.VISIBLE);
                    holder.ll_GroupHead.setVisibility(View.GONE);
                }

                holder.tv_AddPost.setText(SharedService.identityView.MemberName + "，發佈貼文吧!");

                if (SharedService.identityView.MImgName != null) {
                    holder.iv_MImg.setImageDrawable(null);
                    holder.iv_MImg.setTag(SharedService.identityView.MImgName);
                    showImage(holder.iv_MImg, SharedService.identityView.MImgName, "M");
                } else
                    holder.iv_MImg.setImageResource(R.drawable.defaultmimg);
                return;
            }

            if (mHeaderView != null)
                position--;

            holder.tv_MemberName.setText(postView.MemberNameList.get(position));

            if (postType) {
                holder.tv_PostAt.setText("");
                holder.tv_GName.setVisibility(View.GONE);
            } else {
                holder.tv_PostAt.setText("發佈於");
                holder.tv_GName.setVisibility(View.VISIBLE);
                holder.tv_GName.setText(postView.GNameList.get(position));
            }
            holder.tv_CTime.setText(postView.CTimeList.get(position));
            if (postView.PostList.get(position).PostLocations.size() > 0) {
                holder.tv_LocationName.setText(postView.PostList.get(position).PostLocations.get(0).LocationName);
                holder.tv_LocationName.setTag(position);
                holder.tv_LocationName.setOnClickListener(MyClick);
            } else {
                holder.tv_LocationName.setText("");
            }
            holder.tv_Content.setText(postView.PostList.get(position).Content);

            if (postView.MImgNameList.get(position) != null) {
                holder.iv_MImg.setImageDrawable(null);
                holder.iv_MImg.setTag(postView.MImgNameList.get(position));
                showImage(holder.iv_MImg, postView.MImgNameList.get(position), "M");
            } else {
                holder.iv_MImg.setTag("");
                holder.iv_MImg.setImageResource(R.drawable.defaultmimg);
            }

            holder.ll_PostImageList.removeAllViews();
            if (postView.PostList.get(position).PostImages.size() > 0) {
                holder.ll_PostImageList.setPadding(0, 0, 0, 20);
                for (PostView.PostImage postImage : postView.PostList.get(position).PostImages) {
                    ImageView imageView = new ImageView(getActivity());
                    imageView.setAdjustViewBounds(true);
                    imageView.setTag(postImage.ImgName);
                    holder.ll_PostImageList.addView(imageView);
                    showImage(imageView, postImage.ImgName, "P");
                }
            } else {
                holder.ll_PostImageList.setPadding(0, 0, 0, 0);
            }

            if (postView.PostList.get(position).IsTop) {
                holder.tv_IsTop.setVisibility(View.VISIBLE);
            } else {
                holder.tv_IsTop.setVisibility(View.GONE);
            }

            if (postView.LikeNumList.get(position) > 0) {
                holder.bt_Like.setText(String.valueOf(postView.LikeNumList.get(position)) + "個蚌");
            } else {
                holder.bt_Like.setText("蒸蚌");
            }
            if (postView.IsLikeList.get(position)) {
//                holder.bt_Like.setBackgroundColor(Color.parseColor("#efb175"));
                holder.bt_Like.setBackgroundResource(R.drawable.button_rounde2);
            } else {
//                holder.bt_Like.setBackgroundColor(Color.parseColor("#efe0d1"));
                holder.bt_Like.setBackgroundResource(R.drawable.button_rounde);
            }

            if (postView.CommentNumList.get(position) > 0) {
                holder.bt_Comment.setText(postView.CommentNumList.get(position) + "則留言");
            } else {
                holder.bt_Comment.setText("留言");
            }
//            holder.bt_Comment.setBackgroundColor(Color.parseColor("#efe0d1"));
            holder.bt_Comment.setBackgroundResource(R.drawable.button_rounde);

            //點擊事件
            holder.bt_Like.setTag(position);
            holder.bt_Like.setOnClickListener(MyClick);
            holder.bt_Comment.setTag(position);
            holder.bt_Comment.setOnClickListener(MyClick);
            holder.ib_PostBox.setTag(position);
            holder.ib_PostBox.setOnClickListener(MyClick);
            holder.tv_GName.setTag(position);
            holder.tv_GName.setOnClickListener(MyClick);
            holder.tv_MemberName.setTag(position);
            holder.tv_MemberName.setOnClickListener(MyClick);


//            holder.iv_MImg.setTag(position);//避免修改到圖片的Tag使之無法正確載入圖片
            final int mPosition = position;
            holder.iv_MImg.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    OpenProFile(mPosition);
                }
            });

            //避免重複請求
            if (position > postView.PostList.size() * 0.6 && !isFinishLoad && !isLoading) {
                isLoading = true;
                GetPosts();
            }
        }

        @Override
        public int getItemCount() {
            int NormalCount = postView.PostList.size();
            if (mHeaderView != null)
                NormalCount++;
            if (mFooterView != null)
                NormalCount++;
            return NormalCount;
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            private TextView tv_MemberName;
            private TextView tv_GName;
            private TextView tv_LocationName;
            private TextView tv_CTime;
            private TextView tv_Content;
            private Button bt_Like;
            private Button bt_Comment;
            private ImageView iv_MImg;
            private LinearLayout ll_PostImageList;
            private ImageButton ib_PostBox;
            private ImageView iv_GImg;
            private LinearLayout ll_GroupHead;
            private TextView tv_GType;
            private TextView tv_GIntro;
            private Button bt_JoinGroup;
            private TextView tv_AddPost;
            private LinearLayout ll_AddPost;
            private ImageButton ib_GroupBox;
            private TextView tv_Footer;
            private TextView tv_PostAt;
            private TextView tv_IsTop;

            public ViewHolder(View itemView) {
                super(itemView);
                //如果是headerview或者是footerview,直接返回
                if (itemView == mFooterView) {
                    tv_Footer = (TextView) itemView.findViewById(R.id.tv_Footer);
                    return;
                }

                tv_MemberName = (TextView) itemView.findViewById(R.id.tv_MemberName);
                tv_GName = (TextView) itemView.findViewById(R.id.tv_GName);
                tv_CTime = (TextView) itemView.findViewById(R.id.tv_CTime);
                tv_LocationName = (TextView) itemView.findViewById(R.id.tv_LocationName);
                tv_Content = (TextView) itemView.findViewById(R.id.tv_Content);
                bt_Like = (Button) itemView.findViewById(R.id.bt_Like);
                bt_Comment = (Button) itemView.findViewById(R.id.bt_Comment);
                iv_MImg = (ImageView) itemView.findViewById(R.id.iv_MImg);
                ll_PostImageList = (LinearLayout) itemView.findViewById(R.id.ll_PostImageList);
                ib_PostBox = (ImageButton) itemView.findViewById(R.id.ib_PostBox);
                tv_PostAt = (TextView) itemView.findViewById(R.id.tv_PostAt);
                tv_IsTop = (TextView) itemView.findViewById(R.id.tv_IsTop);

                if (itemView == mHeaderView) {
                    ll_GroupHead = (LinearLayout) itemView.findViewById(R.id.ll_GroupHead);
                    iv_GImg = (ImageView) itemView.findViewById(R.id.iv_GImg);
                    tv_GType = (TextView) itemView.findViewById(R.id.tv_GType);
                    tv_GIntro = (TextView) itemView.findViewById(R.id.tv_GIntro);
                    bt_JoinGroup = (Button) itemView.findViewById(R.id.bt_JoinGroup);
                    tv_AddPost = (TextView) itemView.findViewById(R.id.tv_AddPost);
                    ll_AddPost = (LinearLayout) itemView.findViewById(R.id.ll_AddPost);
                    ib_GroupBox = (ImageButton) itemView.findViewById(R.id.ib_GroupBox);
                    ll_AddPost.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            List<MemberAllGroupView.SimpleGroup> tempList = conGroupFragment.getGroupListFragment().memberAllGroupView.SimpleGroupList;
                            String[] GNameArr = new String[tempList.size()];
                            int[] GroupIdArr = new int[tempList.size()];
                            for (int i = 0; i < tempList.size(); i++) {
                                GNameArr[i] = tempList.get(i).GName;
                                GroupIdArr[i] = tempList.get(i).GroupId;
                            }

                            Intent intent = new Intent(getActivity(), AddPostActivity.class);
                            intent.putExtra("GNameArr", GNameArr);
                            intent.putExtra("GroupIdArr", GroupIdArr);
                            intent.putExtra("groupId", groupId);
                            startActivityForResult(intent, AddPost_CODE);
                        }
                    });
                }
            }
        }
    }

//    private class ImageLoader {
//        private OkHttpClient LoadImgClient = new OkHttpClient();
//        private RecyclerView rv_PostList;
//
//        public ImageLoader(RecyclerView recyclerView) {
//            rv_PostList = recyclerView;
//        }
//
//        public void showImage(ImageView imageView, String ImgName, String Type) {
//
//            Bitmap bitmap = getBitmapFromLrucache(ImgName);
//            //防止動態刪除貼文導致ImageView抓不到、滾動狀態下請求圖片
//            if (bitmap == null && rv_PostList.getScrollState() == RecyclerView.SCROLL_STATE_IDLE) {
//                LoadImgByOkHttp(imageView, ImgName, getString(R.string.BackEndPath) + "Thumb" + Type + "Images/" + ImgName);
//            } else if (bitmap != null) {
//                imageView.setImageBitmap(bitmap);
//            }
//        }
//
//        public void loadImages(int start, int end) {
//
//            for (int i = start; i <= end; i++) {
//                //大頭貼
//                String MImgName = postView.MImgNameList.get(i);
//                if (MImgName != null) {
//                    ImageView MImageView = (ImageView) rv_PostList.findViewWithTag("M1");
//                    if (MImageView != null) {
//                        if (getBitmapFromLrucache(MImgName) != null) {
//                            MImageView.setImageBitmap(getBitmapFromLrucache(MImgName));
//                        } else {
//                            LoadImgByOkHttp(MImageView, MImgName, getString(R.string.BackEndPath) + "ThumbMImages/" + MImgName);
//                        }
//                    } else {
//                        SharedService.ShowTextToast("MImageView為null", getActivity());
//                    }
//                }
//
//                //貼文圖片
//                if (postView.PostList.get(i) != null && postView.PostList.get(i).PostImages.size() > 0) {
//                    for (PostView.PostImage postImage : postView.PostList.get(i).PostImages) {
//                        ImageView PostImageView = (ImageView) rv_PostList.findViewWithTag(postImage.ImgName);
//                        if (PostImageView != null) {
//                            if (getBitmapFromLrucache(postImage.ImgName) != null)
//                                PostImageView.setImageBitmap(getBitmapFromLrucache(postImage.ImgName));
//                            else
//                                LoadImgByOkHttp(PostImageView, postImage.ImgName, getString(R.string.BackEndPath) + "ThumbPImages/" + postImage.ImgName);
//                        } else {
//                            SharedService.ShowTextToast("PostImageView為null", getActivity());
//                        }
//                    }
//                }
//            }
//        }
//
//        public void LoadImgByOkHttp(final ImageView imageView, final String ImgName, final String url) {
//            Request request = new Request.Builder()
//                    .url(url)
//                    .build();
//
//            LoadImgClient.newCall(request).enqueue(new Callback() {
//                @Override
//                public void onFailure(Call call, IOException e) {
//                    getActivity().runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            SharedService.ShowTextToast("請檢察網路連線", getActivity());
//                        }
//                    });
//                }
//
//
//                @Override
//                public void onResponse(Call call, Response response) throws IOException {
//                    InputStream inputStream = response.body().byteStream();
//                    final Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
//                    getActivity().runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            if (bitmap != null) {
//                                addBitmapToLrucaches(ImgName, bitmap);
//                                imageView.setImageBitmap(bitmap);
//                            }
////                            if (imageView.getTag().equals(ImgName + nowPosition)) {
////                                imageView.setImageBitmap(bitmap);
////                            } else {
////                                SharedService.ShowTextToast("test", getActivity());
////                            }
//                        }
//                    });
//                }
//            });
//        }
//
//        public void cancelAllAsyncTask() {
//            LoadImgClient.dispatcher().cancelAll();
//        }
//    }

    private void SetPostPopupMenu(View v) {
        nowPosition = (Integer) v.getTag();
        if (nowPosition != -1) {
            final PopupMenu popupmenu = new PopupMenu(getActivity(), v);
            popupmenu.getMenuInflater().inflate(R.menu.post_popup, popupmenu.getMenu());

            final PostView.Posts post = postView.PostList.get(nowPosition);
            if (!post.Account.equals(SharedService.identityView.Account)) {
                popupmenu.getMenu().findItem(R.id.item_EditPost).setVisible(false);
                popupmenu.getMenu().findItem(R.id.item_DeletePost).setVisible(false);
            }
            if (SharedService.identityView.Roles.contains("GAdmin" + postView.PostList.get(nowPosition).GroupId))
                popupmenu.getMenu().findItem(R.id.item_DeletePost).setVisible(true);
            if (postView.IsMarkList.get(nowPosition)) {
                popupmenu.getMenu().findItem(R.id.item_Mark).setVisible(false);
            } else {
                popupmenu.getMenu().findItem(R.id.item_DeleteMark).setVisible(false);
            }

            popupmenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() { // 設定popupmenu項目點擊傾聽者.

                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    switch (item.getItemId()) { // 取得被點擊的項目id.
                        case R.id.item_EditPost:
                            EditPost();
                            break;
                        case R.id.item_DeletePost:
                            DeletePost();
                            break;
                        case R.id.item_Mark:
                            Mark(true, 2, post.PostId);
                            break;
                        case R.id.item_DeleteMark:
                            Mark(false, 2, post.PostId);
                            break;
                    }
                    return true;
                }

            });

            popupmenu.show();
        }
    }

    private void SetGroupPopupMenu(View v) {
        final PopupMenu popupmenu = new PopupMenu(getActivity(), v);
        popupmenu.getMenuInflater().inflate(R.menu.group_popup, popupmenu.getMenu());
        final GroupView groupView = conGroupFragment.groupView;
        if (!groupView.IsJoin) {
            popupmenu.getMenu().findItem(R.id.item_Quit).setVisible(false);
        }
        if (conGroupFragment.groupView.Character != 1) {
            popupmenu.getMenu().findItem(R.id.item_EditGroup).setVisible(false);
            popupmenu.getMenu().findItem(R.id.item_DeleteGroup).setVisible(false);
        }
        if (groupView.IsMark) {
            popupmenu.getMenu().findItem(R.id.item_Mark).setVisible(false);
        } else {
            popupmenu.getMenu().findItem(R.id.item_DeleteMark).setVisible(false);
        }
        popupmenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() { // 設定popupmenu項目點擊傾聽者.

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) { // 取得被點擊的項目id.
                    case R.id.item_EditGroup:
                        EditGroup();
                        break;
                    case R.id.item_Quit:
                        QuitGroup();
                        break;
                    case R.id.item_DeleteGroup:
                        DeleteGroup();
                        break;
                    case R.id.item_Mark:
                        Mark(true, 1, groupView.Group.GroupId);
                        break;
                    case R.id.item_DeleteMark:
                        Mark(false, 1, groupView.Group.GroupId);
                        break;
                }
                return true;
            }

        });
        popupmenu.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case AddPost_CODE:
                if (resultCode == getActivity().RESULT_OK) {
                    int GroupId = data.getIntExtra("groupId", -1);
                    if (GroupId != groupId)
                        conGroupFragment.GetGPosts(GroupId);
                    else {
                        int PostId = data.getIntExtra("PostId", -1);
                        if (PostId != -1)
                            InsertPostByPostId(PostId);
                    }
                }
                return;
            case Comment_CODE:
                if (resultCode == getActivity().RESULT_OK) {
                    GetPostByPostId();
                }
                return;
            case EDITPOST_CODE:
                if (resultCode == getActivity().RESULT_OK) {
                    GetPostByPostId();
                }
                return;
            case EDITGROUP_CODE:
                if (resultCode == getActivity().RESULT_OK) {
                    conGroupFragment.GetGPosts(conGroupFragment.groupView.Group.GroupId);
                }
                return;
        }
    }

    private void Like(View v) {
        isLiking = true;
        nowPosition = (Integer) v.getTag();
        Request request = new Request.Builder()
                .url(getString(R.string.BackEndPath) + "Api/PostApi/Like?PostId=" + postView.PostList.get(nowPosition).PostId)
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
                            GetPostByPostId();
                        } else if (StatusCode == 400) {
                            SharedService.ShowErrorDialog(ResMsg, getActivity());
                        } else {
                            SharedService.ShowErrorDialog(StatusCode + "", getActivity());
                        }
                    }
                });
            }
        });
    }

    private void OpenComment(View v) {
        nowPosition = (Integer) v.getTag();
        Intent intent = new Intent(getActivity(), CommentActivity.class);
        intent.putExtra("PostId", postView.PostList.get(nowPosition).PostId);
        intent.putExtra("IsGAdmin", SharedService.identityView.Roles.contains("GAdmin" + postView.PostList.get(nowPosition).GroupId));
        startActivityForResult(intent, Comment_CODE);
        getActivity().overridePendingTransition(R.animator.opencomment, 0);
    }

    private void OpenProFile(int Position) {
        Intent intent = new Intent(getActivity(), ProFileActivity.class);
        intent.putExtra("Account", postView.PostList.get(Position).Account);
        startActivity(intent);
    }

    private void EditPost() {
        if (nowPosition == -1)
            return;
        Intent intent = new Intent(getActivity(), EditPostActivity.class);
        intent.putExtra("PostId", postView.PostList.get(nowPosition).PostId);
        intent.putExtra("Content", postView.PostList.get(nowPosition).Content);
        if (postView.PostList.get(nowPosition).PostLocations.size() > 0)
            intent.putExtra("LocationName", postView.PostList.get(nowPosition).PostLocations.get(0).LocationName);
        String JsonPostImages = new Gson().toJson(postView.PostList.get(nowPosition).PostImages);
        intent.putExtra("JsonPostImages", JsonPostImages);
        startActivityForResult(intent, EDITPOST_CODE);
    }

    private void DeletePost() {
        if (nowPosition == -1)
            return;

        new AlertDialog.Builder(getActivity())
                .setMessage("確定要刪除此貼文嗎?")
                .setNeutralButton("取消", null)
                .setPositiveButton("確定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Request request = new Request.Builder()
                                .url(getString(R.string.BackEndPath) + "Api/PostApi/Delete?PostId=" + postView.PostList.get(nowPosition).PostId)
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
                                            SharedService.ShowTextToast("刪除成功", getActivity());
                                            postView.PostList.remove(nowPosition);
                                            postView.MemberNameList.remove(nowPosition);
                                            postView.MImgNameList.remove(nowPosition);
                                            postView.GNameList.remove(nowPosition);
                                            postView.CTimeList.remove(nowPosition);
                                            postView.IsMarkList.remove(nowPosition);
                                            postView.IsLikeList.remove(nowPosition);
                                            postView.LikeNumList.remove(nowPosition);
                                            postView.CommentNumList.remove(nowPosition);
                                            if (postListAdapter.mHeaderView != null)
                                                nowPosition++;
                                            postListAdapter.notifyItemRemoved(nowPosition);
                                            int dItemCount = postView.PostList.size() - nowPosition + 1;
                                            postListAdapter.notifyItemRangeChanged(nowPosition, dItemCount);
                                            nowPosition = -1;
                                        } else if (StatusCode == 400) {
                                            SharedService.ShowErrorDialog(ResMsg, getActivity());
                                        } else {
                                            SharedService.ShowErrorDialog(StatusCode + "", getActivity());
                                        }
                                    }
                                });
                            }
                        });
                    }
                })
                .show();
    }

    private void GetPostByPostId() {
        if (nowPosition != -1) {
            Request request = new Request.Builder()
                    .url(getString(R.string.BackEndPath) + "Api/PostApi/GetPostByPostId?PostId=" + postView.PostList.get(nowPosition).PostId)
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
                                Gson gson = new Gson();
                                PostView tempData = gson.fromJson(ResMsg, PostView.class);
                                postView.PostList.set(nowPosition, tempData.PostList.get(0));
                                postView.CTimeList.set(nowPosition, tempData.CTimeList.get(0));
                                postView.IsMarkList.set(nowPosition, tempData.IsMarkList.get(0));
                                postView.IsLikeList.set(nowPosition, tempData.IsLikeList.get(0));
                                postView.LikeNumList.set(nowPosition, tempData.LikeNumList.get(0));
                                postView.CommentNumList.set(nowPosition, tempData.CommentNumList.get(0));
//                                if (postListAdapter.mHeaderView != null)
//                                    nowPosition++;
                                postListAdapter.notifyDataSetChanged();
                                nowPosition = -1;
                                isLiking = false;
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
            SharedService.ShowTextToast("更新資料失敗", getActivity());
        }

    }

    private void InsertPostByPostId(final int PostId) {
        Request request = new Request.Builder()
                .url(getString(R.string.BackEndPath) + "Api/PostApi/GetPostByPostId?PostId=" + PostId)
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
                            Gson gson = new Gson();
                            PostView tempData = gson.fromJson(ResMsg, PostView.class);
                            postView.PostList.add(0, tempData.PostList.get(0));
                            postView.MemberNameList.add(0, tempData.MemberNameList.get(0));
                            postView.MImgNameList.add(0, tempData.MImgNameList.get(0));
                            postView.GNameList.add(0, tempData.GNameList.get(0));
                            postView.CTimeList.add(0, tempData.CTimeList.get(0));
                            postView.IsMarkList.add(0, tempData.IsMarkList.get(0));
                            postView.IsLikeList.add(0, tempData.IsLikeList.get(0));
                            postView.LikeNumList.add(0, tempData.LikeNumList.get(0));
                            postView.CommentNumList.add(0, tempData.CommentNumList.get(0));

                            if (postListAdapter.mHeaderView != null) {
                                postListAdapter.notifyItemInserted(1);
                                postListAdapter.notifyItemRangeChanged(1, postView.PostList.size());
                            } else {
                                postListAdapter.notifyItemInserted(0);
                                postListAdapter.notifyItemRangeChanged(0, postView.PostList.size());
                            }
                        } else if (StatusCode == 400) {
                            SharedService.ShowErrorDialog(ResMsg, getActivity());
                        } else {
                            SharedService.ShowErrorDialog(StatusCode + "", getActivity());
                        }
                    }
                });
            }
        });

    }

    private void OpenMap(View v) {
        Intent intent = new Intent(getActivity(), MapsActivity.class);
        intent.putExtra("PlaceId", postView.PostList.get((int) v.getTag()).PostLocations.get(0).PlaceId);
        startActivity(intent);
    }

    private void JoinGroup() {
        final GroupView groupView = conGroupFragment.groupView;
        Request request;
        if (groupView.Group.JoinType == 1) {
            RequestBody formBody = new FormBody.Builder()
                    .add("GroupId", groupId + "")
                    .build();

            request = new Request.Builder()
                    .url(getString(R.string.BackEndPath) + "Api/GroupMemberApi/Add")
                    .post(formBody)
                    .build();
        } else {
            if (groupView.IsApply) {
                request = new Request.Builder()
                        .url(getString(R.string.BackEndPath) + "Api/GroupApplicationApi/DeleteGA?GroupId=" + groupId)
                        .delete()
                        .build();
            } else {
                RequestBody formBody = new FormBody.Builder()
                        .add("GroupId", groupId + "")
                        .build();

                request = new Request.Builder()
                        .url(getString(R.string.BackEndPath) + "Api/GroupApplicationApi/CreateGA")
                        .post(formBody)
                        .build();
            }
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

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (StatusCode == 200) {
                            if (groupView.Group.JoinType == 1) {
                                conGroupFragment.getGroupListFragment().Refresh();
                                conGroupFragment.GetGPosts(groupId);
                            } else {
                                groupView.IsApply = !groupView.IsApply;
                                postListAdapter.notifyDataSetChanged();
                            }
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

    private void EditGroup() {
        String JsonGroupView = new Gson().toJson(conGroupFragment.groupView);
        Intent intent = new Intent(getActivity(), EditGroupActivity.class);
        intent.putExtra("JsonGroupView", JsonGroupView);
        startActivityForResult(intent, EDITGROUP_CODE);
    }

    private void QuitGroup() {
        new AlertDialog.Builder(getActivity())
                .setTitle("確認退出")
                .setMessage("您確定要退出 " + conGroupFragment.groupView.Group.GName + " 嗎?")
                .setNegativeButton("取消", null)
                .setPositiveButton("確定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Request request = new Request.Builder()
                                .url(getString(R.string.BackEndPath) + "Api/GroupMemberApi/Quit?GroupId=" + groupId)
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
                                            conGroupFragment.getGroupListFragment().Refresh();
                                            conGroupFragment.GetGPosts(groupId);
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
                })
                .show();
    }

    private void DeleteGroup() {
        new AlertDialog.Builder(getActivity())
                .setTitle("確認刪除")
                .setMessage("您確定要刪除 " + conGroupFragment.groupView.Group.GName + " 嗎?")
                .setNegativeButton("取消", null)
                .setPositiveButton("確定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Request request = new Request.Builder()
                                .url(getString(R.string.BackEndPath) + "Api/GroupApi/Delete?GroupId=" + groupId)
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
                                            mainActivity.onBackPressed();
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
                })
                .show();
    }

    private void Mark(boolean IsMark, final int Resource, int ResourceId) {
        Request request;
        if (IsMark) {
            RequestBody formBody = new FormBody.Builder()
                    .add("Resource", Resource + "")
                    .add("ResourceId", ResourceId + "")
                    .build();

            request = new Request.Builder()
                    .url(getString(R.string.BackEndPath) + "Api/MarkApi/CreateMark")
                    .post(formBody)
                    .build();
        } else {
            request = new Request.Builder()
                    .url(getString(R.string.BackEndPath) + "Api/MarkApi/DeleteMark?Resource=" + Resource + "&ResourceId=" + ResourceId)
                    .delete()
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

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (StatusCode == 200) {
                            if (Resource == 1) {
                                conGroupFragment.groupView.IsMark = !conGroupFragment.groupView.IsMark;
                            } else {
                                postView.IsMarkList.set(nowPosition, !postView.IsMarkList.get(nowPosition));
                                nowPosition = -1;
                            }
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
}
