package com.tsai.congroup;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
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
import java.io.InputStream;

import MyMethod.SharedService;
import ViewModel.CommentView;
import ViewModel.PostView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PostActivity extends MySharedActivity {
    private boolean isFirstLoad = true;
    private boolean isUpdate = false;
    private boolean isLoading = true;
    private boolean isFinishLoad = false;

    private int skip = 0;
    private int postId;
    private boolean isGAdmin = false;

    private PostView postView;
    private CommentView commentView;
    public SwipeRefreshLayout mSwipeLayout;
    private RecyclerView rv_CommentList;
    private CommentListAdapter commentListAdapter;

    private EditText et_CommentContent;

    private int firstPosition = -1;
    private int endPosition = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);
        postId = getIntent().getIntExtra("PostId", -1);

        if (postId != -1) {
            //設定Activity
            InitView(true, "貼文");
            SetupUI(findViewById(R.id.activity_Outer));
            SetCache((int) Runtime.getRuntime().maxMemory() / 5);

            setSwipeRefresh();
            rv_CommentList = (RecyclerView) findViewById(R.id.rv_CommentList);
            et_CommentContent = (EditText) findViewById(R.id.et_CommentContent);
            Refresh();
        } else {
            SharedService.ShowTextToast("ERROR", this);
            finish();
        }

    }

    private void setSwipeRefresh() {
        mSwipeLayout = (SwipeRefreshLayout) findViewById(R.id.srl_CommentList);
        mSwipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                //檢查網路連線
                if (!SharedService.CheckNetWork(PostActivity.this)) {
                    SharedService.ShowTextToast("請檢察網路連線", PostActivity.this);
                    mSwipeLayout.setRefreshing(false);
                    return;
                }
                Refresh();
            }
        });
        mSwipeLayout.setProgressBackgroundColorSchemeColor(getResources().getColor(R.color.colorTransparent));
        // 设置下拉圆圈上的颜色，蓝色、绿色、橙色、红色
        mSwipeLayout.setColorSchemeResources(android.R.color.holo_blue_bright, android.R.color.holo_green_light,
                android.R.color.holo_orange_light, android.R.color.holo_red_light);
        mSwipeLayout.setDistanceToTriggerSync(400);// 设置手指在屏幕下拉多少距离会触发下拉刷新
        mSwipeLayout.setSize(SwipeRefreshLayout.DEFAULT);
    }

    public void Refresh() {
        mSwipeLayout.setRefreshing(true);
        //clearLruCache();
        skip = 0;
        postView = new PostView();
        commentView = new CommentView();
        //避免重新接上網路時重整導致崩潰
        if (!isFirstLoad) {
            isFinishLoad = false;
            commentListAdapter.notifyDataSetChanged();
        }
        GetPost();
    }

    private void GetPost() {
        Request request = new Request.Builder()
                .url(getString(R.string.BackEndPath) + "Api/PostApi/GetPostByPostId?PostId=" + postId)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        SharedService.ShowTextToast("請檢察網路連線", PostActivity.this);
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
                        // 停止刷新
                        mSwipeLayout.setRefreshing(false);
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

                            for (String Role : SharedService.identityView.Roles) {
                                if (Role.equals("GAdmin" + postId)) {
                                    isGAdmin = true;
                                    break;
                                }
                            }

//                          if (isFirstLoad) {
                            if (isFirstLoad) {
                                isFirstLoad = false;
                                rv_CommentList.setLayoutManager(new LinearLayoutManager(PostActivity.this, LinearLayoutManager.VERTICAL, false));
                                commentListAdapter = new CommentListAdapter();
                                rv_CommentList.setAdapter(commentListAdapter);
                            } else {
                                isUpdate = true;
                                commentListAdapter.ReSetHeader();
                            }
                            GetComments();
                        } else if (StatusCode == 400) {
                            SharedService.ShowErrorDialog(ResMsg, PostActivity.this);
                        } else {
                            SharedService.ShowErrorDialog(StatusCode + "", PostActivity.this);
                        }
                    }
                });
            }
        });

    }

    View.OnClickListener MyClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.ib_PostBox:
                    SetPostPopupMenu(v);
                    break;
                case R.id.ib_CommentBox:
                    SetCommentPopupMenu(v);
                    break;
                case R.id.bt_Like:
                    Like();
                    break;
                case R.id.bt_Comment:
                    SharedService.ShowKeyboard(PostActivity.this, et_CommentContent);
                    break;
                case R.id.tv_LocationName:
                    OpenMap(v);
                    break;
                case R.id.tv_MemberName:
                    OpenProFile(v);
                    break;
                case R.id.iv_MImg:
                    OpenProFile(v);
                    break;
                case R.id.tv_GName:
                    GoGroup();
                    break;
            }
        }
    };

    private void GetComments() {
        Request request = new Request.Builder()
                .url(getString(R.string.BackEndPath) + "Api/CommentApi/DisplayComments?PostId=" + postId + "&Skip=" + skip)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                SharedService.ShowTextToast("請檢察網路連線", PostActivity.this);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final int StatusCode = response.code();
                final String ResMsg = response.body().string();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (StatusCode == 200) {
                            //請求完畢
                            isLoading = false;
                            Gson gson = new Gson();
                            CommentView tempData = gson.fromJson(ResMsg, CommentView.class);

                            commentView.CommentList.addAll(tempData.CommentList);
                            commentView.MemberNameList.addAll(tempData.MemberNameList);
                            commentView.MImgNameList.addAll(tempData.MImgNameList);
                            commentView.CTimeList.addAll(tempData.CTimeList);
                            skip += tempData.CommentList.size();

                            commentListAdapter.notifyDataSetChanged();

                            if (tempData.CommentList.size() < tempData.ARequestCount) {
                                //最後一次載入
                                isFinishLoad = true;
                                View footer = LayoutInflater.from(PostActivity.this).inflate(R.layout.footer, rv_CommentList, false);
                                commentListAdapter.setFooterView(footer);
                            }
                        } else {
                            SharedService.ShowErrorDialog(StatusCode + "", PostActivity.this);
                        }
                    }
                });
            }
        });
    }


    public class CommentListAdapter extends RecyclerView.Adapter<CommentListAdapter.ViewHolder> {
        private ImageLoader imageLoader;
        public boolean isFirstIn;

        public final int TYPE_HEADER = 0;  //说明是带有Header的
        public final int TYPE_FOOTER = 1;  //说明是带有Footer的
        public final int TYPE_NORMAL = 2;  //说明是不带有header和footer的
        private View mHeaderView;
        private View mFooterView;

        private LinearLayoutManager linearLayoutManager;

        public void setHeaderView(View headerView) {
            mHeaderView = headerView;
            notifyItemInserted(0);
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

        public CommentListAdapter() {
            View header = LayoutInflater.from(PostActivity.this).inflate(R.layout.post_block, rv_CommentList, false);
            setHeaderView(header);
            imageLoader = new ImageLoader();
            isFirstIn = true;

            linearLayoutManager = (LinearLayoutManager) rv_CommentList.getLayoutManager();
            rv_CommentList.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView,
                                       int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    firstPosition = linearLayoutManager.findFirstVisibleItemPosition();
                    endPosition = linearLayoutManager.findLastVisibleItemPosition();
                }
            });
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (mHeaderView != null && viewType == TYPE_HEADER) {
                return new ViewHolder(mHeaderView);
            }
            if (mFooterView != null && viewType == TYPE_FOOTER) {
                return new ViewHolder(mFooterView);
            }

            Context context = parent.getContext();
            View view = LayoutInflater.from(context).inflate(R.layout.comment_block, parent, false);
            ViewHolder viewHolder = new ViewHolder(view);
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(CommentListAdapter.ViewHolder holder, int position) {
            if (getItemViewType(position) == TYPE_FOOTER) {
                if (commentView.CommentList.size() > 0)
                    holder.tv_Footer.setText("沒有更多留言囉!");
                else
                    holder.tv_Footer.setText("成為第一個留言的人吧!");
            }
            if (getItemViewType(position) != TYPE_NORMAL)
                return;

            if (mHeaderView != null)
                position--;

            holder.tv_MemberName.setText(commentView.MemberNameList.get(position));
            holder.tv_CTime.setText(commentView.CTimeList.get(position));
            holder.tv_Content.setText(commentView.CommentList.get(position).Content);

            //每次都要重設，避免刪除單筆後造成Tag混亂
            holder.iv_MImg.setTag("");
            holder.iv_MImg.setImageDrawable(null);
            if (commentView.MImgNameList.get(position) != null) {
                holder.iv_MImg.setTag(commentView.MImgNameList.get(position) + position);
                imageLoader.showImage(holder.iv_MImg, commentView.MImgNameList.get(position), "M");
            } else {
                holder.iv_MImg.setImageResource(R.drawable.defaultmimg);
            }

            if (commentView.CommentList.get(position).Account.equals(SharedService.identityView.Account) || isGAdmin) {
                holder.ib_CommentBox.setTag(position);
                holder.ib_CommentBox.setOnClickListener(MyClick);
                holder.ib_CommentBox.setVisibility(View.VISIBLE);
            } else {
                holder.ib_CommentBox.setVisibility(View.GONE);
            }

            holder.tv_MemberName.setTag(position);
            holder.tv_MemberName.setOnClickListener(MyClick);
            holder.iv_MImg.setTag(position);
            holder.iv_MImg.setOnClickListener(MyClick);

            //避免重複請求
            if (position > commentView.CommentList.size() * 0.6 && !isFinishLoad && !isLoading) {
                isLoading = true;
                GetComments();
            }
        }

        @Override
        public int getItemCount() {
            int NormalCount = commentView.CommentList.size();
            if (mHeaderView != null)
                NormalCount++;
            if (mFooterView != null)
                NormalCount++;
            return NormalCount;
        }

        public void ReSetHeader() {
            new ViewHolder(mHeaderView);
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
            private ImageButton ib_CommentBox;
            private TextView tv_IsTop;
            private TextView tv_Footer;

            public ViewHolder(View itemView) {
                super(itemView);
                //如果是headerview或者是footerview,直接返回

                if (itemView == mFooterView) {
                    tv_Footer = (TextView) itemView.findViewById(R.id.tv_Footer);
                    return;
                }

                tv_MemberName = (TextView) itemView.findViewById(R.id.tv_MemberName);
                tv_CTime = (TextView) itemView.findViewById(R.id.tv_CTime);
                tv_Content = (TextView) itemView.findViewById(R.id.tv_Content);
                iv_MImg = (ImageView) itemView.findViewById(R.id.iv_MImg);
                ib_CommentBox = (ImageButton) itemView.findViewById(R.id.ib_CommentBox);

                if (itemView == mHeaderView && (isFirstIn || isUpdate)) {
                    SetHeaderContent(itemView);
                }
            }

            private void SetHeaderContent(View itemView) {
                tv_GName = (TextView) itemView.findViewById(R.id.tv_GName);
                bt_Like = (Button) itemView.findViewById(R.id.bt_Like);
                bt_Comment = (Button) itemView.findViewById(R.id.bt_Comment);
                ll_PostImageList = (LinearLayout) itemView.findViewById(R.id.ll_PostImageList);
                ib_PostBox = (ImageButton) itemView.findViewById(R.id.ib_PostBox);
                tv_LocationName = (TextView) itemView.findViewById(R.id.tv_LocationName);
                tv_IsTop = (TextView) itemView.findViewById(R.id.tv_IsTop);

                bt_Like.setOnClickListener(MyClick);
                bt_Comment.setOnClickListener(MyClick);
                ib_PostBox.setOnClickListener(MyClick);
                tv_GName.setOnClickListener(MyClick);
                tv_MemberName.setText(postView.MemberNameList.get(0));
                tv_GName.setText(postView.GNameList.get(0));
                tv_CTime.setText(postView.CTimeList.get(0));

                tv_Content.setText(postView.PostList.get(0).Content);
                tv_MemberName.setOnClickListener(MyClick);
                iv_MImg.setOnClickListener(MyClick);

                //每次都要重設，避免刪除單筆後造成Tag混亂
                iv_MImg.setTag("");
                iv_MImg.setImageDrawable(null);
                if (postView.MImgNameList.get(0) != null) {
                    imageLoader.showImage(iv_MImg, postView.MImgNameList.get(0), "M");
                } else {
                    iv_MImg.setImageResource(R.drawable.defaultmimg);
                }

                ll_PostImageList.removeAllViews();
                if (postView.PostList.get(0).PostImages.size() > 0) {
                    ll_PostImageList.setPadding(0, 0, 0, 20);
                    for (PostView.PostImage postImage : postView.PostList.get(0).PostImages) {
                        ImageView imageView = new ImageView(PostActivity.this);
                        imageView.setAdjustViewBounds(true);
                        imageView.setTag(postImage.ImgName);
                        ll_PostImageList.addView(imageView);
                        imageLoader.showImage(imageView, postImage.ImgName, "P");
                    }
                } else {
                    ll_PostImageList.setPadding(0, 0, 0, 0);
                }

                if (postView.PostList.get(0).IsTop) {
                    tv_IsTop.setVisibility(View.VISIBLE);
                } else {
                    tv_IsTop.setVisibility(View.GONE);
                }

                if (postView.PostList.get(0).PostLocations.size() > 0) {
                    tv_LocationName.setText(postView.PostList.get(0).PostLocations.get(0).LocationName);
                    tv_LocationName.setOnClickListener(MyClick);
                } else {
                    tv_LocationName.setText("");
                }

                if (postView.LikeNumList.get(0) > 0) {
                    bt_Like.setText(String.valueOf(postView.LikeNumList.get(0)) + "個蚌");
                } else {
                    bt_Like.setText("蒸蚌");
                }
                if (postView.IsLikeList.get(0)) {
//                    bt_Like.setBackgroundColor(Color.parseColor("#0080ff"));
                    bt_Like.setBackgroundResource(R.drawable.button_rounde2);
                } else {
//                    bt_Like.setBackgroundColor(Color.parseColor("#ffe6e6"));
                    bt_Like.setBackgroundResource(R.drawable.button_rounde);
                }

                if (postView.CommentNumList.get(0) > 0) {
                    bt_Comment.setText(postView.CommentNumList.get(0) + "則留言");
                } else {
                    bt_Comment.setText("留言");
                }
//                bt_Comment.setBackgroundColor(Color.parseColor("#ffe6e6"));
                bt_Comment.setBackgroundResource(R.drawable.button_rounde);
            }
        }
    }

    private class ImageLoader {
        private OkHttpClient LoadImgClient = new OkHttpClient();

        public void showImage(ImageView imageView, String ImgName, String Type) {

            Bitmap bitmap = getBitmapFromLrucache(ImgName);
            if (bitmap == null) {
                LoadImgByOkHttp(imageView, ImgName, getString(R.string.BackEndPath) + "Thumb" + Type + "Images/" + ImgName);
            } else if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            }
        }

        public void LoadImgByOkHttp(final ImageView imageView, final String ImgName, final String url) {
            Request request = new Request.Builder()
                    .url(url)
                    .build();

            LoadImgClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            SharedService.ShowTextToast("請檢察網路連線", PostActivity.this);
                        }
                    });
                }


                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    InputStream inputStream = response.body().byteStream();
                    final Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (bitmap != null) {
                                addBitmapToLrucaches(ImgName, bitmap);
                                imageView.setImageBitmap(bitmap);
                            }
                        }
                    });
                }
            });
        }
    }

    private void SetPostPopupMenu(View v) {
        final PopupMenu popupmenu = new PopupMenu(this, v);
        popupmenu.getMenuInflater().inflate(R.menu.post_popup, popupmenu.getMenu());


        final PostView.Posts post = postView.PostList.get(0);
        if (!post.Account.equals(SharedService.identityView.Account)) {
            popupmenu.getMenu().findItem(R.id.item_EditPost).setVisible(false);
            popupmenu.getMenu().findItem(R.id.item_DeletePost).setVisible(false);
        }
        if (isGAdmin)
            popupmenu.getMenu().findItem(R.id.item_DeletePost).setVisible(true);
        if (postView.IsMarkList.get(0)) {
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
                        Mark(true, post.PostId);
                        break;
                    case R.id.item_DeleteMark:
                        Mark(false, post.PostId);
                        break;
                }
                return true;
            }

        });
        popupmenu.show();
    }

    private void SetCommentPopupMenu(View v) {
        final int position = (int) v.getTag();
        final PopupMenu popupmenu = new PopupMenu(this, v);
        popupmenu.getMenuInflater().inflate(R.menu.comment_popup, popupmenu.getMenu());

        if (postId != -1) {
            if (!commentView.CommentList.get(position).Account.equals(SharedService.identityView.Account)) {
                popupmenu.getMenu().findItem(R.id.item_EditComment).setVisible(false);
                popupmenu.getMenu().findItem(R.id.item_DeleteComment).setVisible(false);
            }
            //管理員或發文者也可刪除留言
            if (isGAdmin || SharedService.identityView.Account.equals(postView.PostList.get(0).Account))
                popupmenu.getMenu().findItem(R.id.item_DeleteComment).setVisible(true);
        }
        popupmenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() { // 設定popupmenu項目點擊傾聽者.

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) { // 取得被點擊的項目id.
                    case R.id.item_EditComment:
                        EditComment(position);
                        break;
                    case R.id.item_DeleteComment:
                        DeleteComment(position);
                        break;
                    default:
                        break;
                }
                return true;
            }

        });
        popupmenu.show();
    }

    private void Like() {
        Request request = new Request.Builder()
                .url(getString(R.string.BackEndPath) + "Api/PostApi/Like?PostId=" + postId)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        SharedService.ShowTextToast("請檢察網路連線", PostActivity.this);
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
                            GetPostByPostId();
                        } else if (StatusCode == 400) {
                            SharedService.ShowErrorDialog(ResMsg, PostActivity.this);
                        } else {
                            SharedService.ShowErrorDialog(StatusCode + "", PostActivity.this);
                        }
                    }
                });
            }
        });
    }

    private void EditPost() {
        Intent intent = new Intent(this, EditPostActivity.class);
        intent.putExtra("PostId", postId);
        intent.putExtra("Content", postView.PostList.get(0).Content);
        if (postView.PostList.get(0).PostLocations.size() > 0)
            intent.putExtra("LocationName", postView.PostList.get(0).PostLocations.get(0).LocationName);
        String JsonPostImages = new Gson().toJson(postView.PostList.get(0).PostImages);
        intent.putExtra("JsonPostImages", JsonPostImages);
        startActivityForResult(intent, 87);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 87 && resultCode == RESULT_OK) {
            GetPostByPostId();
        }
    }

    private void DeletePost() {

        new AlertDialog.Builder(this)
                .setMessage("確定要刪除此貼文嗎?")
                .setNeutralButton("取消", null)
                .setPositiveButton("確定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Request request = new Request.Builder()
                                .url(getString(R.string.BackEndPath) + "Api/PostApi/Delete?PostId=" + postId)
                                .delete()
                                .build();
                        client.newCall(request).enqueue(new Callback() {
                            @Override
                            public void onFailure(Call call, IOException e) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        SharedService.ShowTextToast("請檢察網路連線", PostActivity.this);
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
                                            SharedService.ShowTextToast("刪除成功", PostActivity.this);
                                            finish();
                                        } else if (StatusCode == 400) {
                                            SharedService.ShowErrorDialog(ResMsg, PostActivity.this);
                                        } else {
                                            SharedService.ShowErrorDialog(StatusCode + "", PostActivity.this);
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
        Request request = new Request.Builder()
                .url(getString(R.string.BackEndPath) + "Api/PostApi/GetPostByPostId?PostId=" + postId)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        SharedService.ShowTextToast("請檢察網路連線", PostActivity.this);
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
                            Gson gson = new Gson();
                            PostView tempData = gson.fromJson(ResMsg, PostView.class);
                            postView.PostList.set(0, tempData.PostList.get(0));
                            postView.CTimeList.set(0, tempData.CTimeList.get(0));
                            postView.IsMarkList.set(0, tempData.IsMarkList.get(0));
                            postView.IsLikeList.set(0, tempData.IsLikeList.get(0));
                            postView.LikeNumList.set(0, tempData.LikeNumList.get(0));
                            postView.CommentNumList.set(0, tempData.CommentNumList.get(0));
                            //commentListAdapter.notifyItemChanged(0);
                            isUpdate = true;
                            commentListAdapter.ReSetHeader();
                        } else if (StatusCode == 400) {
                            SharedService.ShowErrorDialog(ResMsg, PostActivity.this);
                        } else {
                            SharedService.ShowErrorDialog(StatusCode + "", PostActivity.this);
                        }
                    }
                });
            }
        });

    }

    public void AddComment(View v) {
        SharedService.HideKeyboard(this);
        activity_Outer.requestFocus();
        final String CommentContent = et_CommentContent.getText().toString();
        if (!CommentContent.trim().equals("")) {
            RequestBody formBody = new FormBody.Builder()
                    .add("PostId", postId + "")
                    .add("Content", CommentContent)
                    .build();

            Request request = new Request.Builder()
                    .url(getString(R.string.BackEndPath) + "Api/CommentApi/Add")
                    .post(formBody)
                    .build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            SharedService.ShowTextToast("請檢察網路連線", PostActivity.this);
                        }
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    final int StatusCode = response.code();
                    final String ResMsg = response.body().string();
                    runOnUiThread(new Runnable() {//这是Activity的方法，会在主线程执行任务
                        @Override
                        public void run() {
                            if (StatusCode == 200) {
                                et_CommentContent.setText("");
                                Gson gson = new Gson();
                                int ComId = gson.fromJson(ResMsg, Integer.class);
                                InsertCommentByComId(ComId);
                            } else if (StatusCode == 400) {
                                SharedService.ShowErrorDialog(ResMsg, PostActivity.this);
                            } else {
                                SharedService.ShowErrorDialog(StatusCode + "", PostActivity.this);
                            }
                        }
                    });
                }
            });

        } else {
            SharedService.ShowTextToast("請輸入內容", this);
        }
    }

    private void InsertCommentByComId(int ComId) {
        Request request = new Request.Builder()
                .url(getString(R.string.BackEndPath) + "Api/CommentApi/GetComment?ComId=" + ComId)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        SharedService.ShowTextToast("請檢察網路連線", PostActivity.this);
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
                            Gson gson = new Gson();
                            CommentView tempData = gson.fromJson(ResMsg, CommentView.class);
                            commentView.CommentList.add(0, tempData.CommentList.get(0));
                            commentView.MemberNameList.add(0, tempData.MemberNameList.get(0));
                            commentView.MImgNameList.add(0, tempData.MImgNameList.get(0));
                            commentView.CTimeList.add(0, tempData.CTimeList.get(0));

                            commentListAdapter.notifyItemInserted(1);
                            commentListAdapter.notifyItemRangeChanged(1, commentView.CommentList.size());
                            if (firstPosition > 1)
                                rv_CommentList.scrollToPosition(1);
                        } else if (StatusCode == 400) {
                            SharedService.ShowErrorDialog(ResMsg, PostActivity.this);
                        } else {
                            SharedService.ShowErrorDialog(StatusCode + "", PostActivity.this);
                        }
                    }
                });
            }
        });
    }

    private void EditComment(final int position) {
        final View EditCommentView = LayoutInflater.from(this).inflate(R.layout.editcomment_view, null);
        final EditText et_EditContent = (EditText) EditCommentView.findViewById(R.id.et_EditContent);
        et_EditContent.setText(commentView.CommentList.get(position).Content);
        et_EditContent.setSelection(et_EditContent.length());
        new AlertDialog.Builder(this)
                .setTitle("修改留言")
                .setView(EditCommentView)
                .setNeutralButton("取消", null)
                .setPositiveButton("修改", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        RequestBody formBody = new FormBody.Builder()
                                .add("ComId", String.valueOf(commentView.CommentList.get(position).ComId))
                                .add("Content", et_EditContent.getText().toString())
                                .build();

                        Request request = new Request.Builder()
                                .url(getString(R.string.BackEndPath) + "Api/CommentApi/Edit")
                                .put(formBody)
                                .build();
                        client.newCall(request).enqueue(new Callback() {
                            @Override
                            public void onFailure(Call call, IOException e) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        SharedService.ShowTextToast("請檢察網路連線", PostActivity.this);
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
                                            SharedService.ShowTextToast("修改成功", PostActivity.this);
                                            CommentView.Comments TempData = commentView.CommentList.get(position);
                                            TempData.Content = et_EditContent.getText().toString();
                                            commentView.CommentList.set(position, TempData);
                                            commentListAdapter.notifyDataSetChanged();
                                        } else if (StatusCode == 400) {
                                            SharedService.ShowErrorDialog(ResMsg, PostActivity.this);
                                        } else {
                                            SharedService.ShowErrorDialog(StatusCode + "", PostActivity.this);
                                        }
                                    }
                                });
                            }
                        });
                    }
                })
                .show();

    }

    private void DeleteComment(final int position) {
        new AlertDialog.Builder(this)
                .setMessage("確定要刪除此留言嗎?")
                .setNeutralButton("取消", null)
                .setPositiveButton("刪除", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Request request = new Request.Builder()
                                .url(getString(R.string.BackEndPath) + "Api/CommentApi/Delete?ComId=" + commentView.CommentList.get(position).ComId)
                                .delete()
                                .build();
                        client.newCall(request).enqueue(new Callback() {
                            @Override
                            public void onFailure(Call call, IOException e) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        SharedService.ShowTextToast("請檢察網路連線", PostActivity.this);
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
                                            commentView.CommentList.remove(position);
                                            commentView.MemberNameList.remove(position);
                                            commentView.MImgNameList.remove(position);
                                            commentView.CTimeList.remove(position);

                                            commentListAdapter.notifyItemRemoved(position + 1);
                                            int dItemCount = commentView.CommentList.size() - position;
                                            commentListAdapter.notifyItemRangeChanged(position + 1, dItemCount);
                                        } else if (StatusCode == 400) {
                                            SharedService.ShowErrorDialog(ResMsg, PostActivity.this);
                                        } else {
                                            SharedService.ShowErrorDialog(StatusCode + "", PostActivity.this);
                                        }
                                    }
                                });
                            }
                        });
                    }
                })
                .show();
    }

    private void OpenMap(View v) {
        Intent intent = new Intent(this, MapsActivity.class);
        intent.putExtra("PlaceId", postView.PostList.get(0).PostLocations.get(0).PlaceId);
        startActivity(intent);
    }

    private void OpenProFile(View v) {
        Object Position = v.getTag();
        Intent intent = new Intent(this, ProFileActivity.class);
        if (Position == null || (Position != null && Position.equals("")))
            intent.putExtra("Account", postView.PostList.get(0).Account);
        else
            intent.putExtra("Account", commentView.CommentList.get((int) Position).Account);
        startActivity(intent);
    }

    private void Mark(boolean IsMark, int ResourceId) {
        Request request;
        if (IsMark) {
            RequestBody formBody = new FormBody.Builder()
                    .add("Resource", "2")
                    .add("ResourceId", ResourceId + "")
                    .build();

            request = new Request.Builder()
                    .url(getString(R.string.BackEndPath) + "Api/MarkApi/CreateMark")
                    .post(formBody)
                    .build();
        } else {
            request = new Request.Builder()
                    .url(getString(R.string.BackEndPath) + "Api/MarkApi/DeleteMark?Resource=2&ResourceId=" + ResourceId)
                    .delete()
                    .build();
        }

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        SharedService.ShowTextToast("請檢察網路連線", PostActivity.this);
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
                            postView.IsMarkList.set(0, !postView.IsMarkList.get(0));
                        } else if (StatusCode == 400) {
                            SharedService.ShowErrorDialog(ResMsg, PostActivity.this);
                        } else {
                            SharedService.ShowTextToast("ERROR:" + StatusCode, PostActivity.this);
                        }
                    }
                });
            }
        });
    }

    private void GoGroup() {
        getIntent().putExtra("GroupId", postView.PostList.get(0).GroupId);
        setResult(RESULT_OK, getIntent());
        finish();
    }
}
