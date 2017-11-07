package com.tsai.congroup;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.util.LruCache;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.serhatsurguvec.swipablelayout.SwipeableLayout;

import java.io.IOException;
import java.io.InputStream;

import MyMethod.SharedService;
import ViewModel.CommentView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CommentActivity extends MySharedActivity {

    private int postId;
    private int skip = 0;

    private boolean isGAdmin;
    private String account;
    private CommentView commentView;

    private EditText et_CommentContent;
    private RecyclerView rv_CommentList;

    private boolean isFirstLoad = true;
    private boolean isLoading = true;
    private boolean isFinishLoad = false;
    private CommentListAdapter commentListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comment);

        SwipeableLayout swipeableLayout = (SwipeableLayout) findViewById(R.id.sl_Comment);
        swipeableLayout.setOnLayoutCloseListener(new SwipeableLayout.OnLayoutCloseListener() {
            @Override
            public void OnLayoutClosed() {
                //这里调用activity的退出方法。
                finish();
            }
        });
        SetupUI(findViewById(R.id.sl_Comment));
        SetCache((int) Runtime.getRuntime().maxMemory() / 5);

        postId = getIntent().getIntExtra("PostId", -1);
        isGAdmin = getIntent().getBooleanExtra("IsGAdmin", false);
        account = SharedService.identityView.Account;

        et_CommentContent = (EditText) findViewById(R.id.et_CommentContent);

        rv_CommentList = (RecyclerView) findViewById(R.id.rv_CommentList);
        commentView = new CommentView();
        GetComments();
    }

    @Override
    public void finish() {
        setResult(RESULT_OK, getIntent());
        super.finish();
    }

    @Override
    protected void onPause() {
        overridePendingTransition(0, 0);
        super.onPause();
    }

    View.OnClickListener MyClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.tv_MemberName:
                    OpenProFile(v);
                    break;
                case R.id.iv_MImg:
                    OpenProFile(v);
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
                SharedService.ShowTextToast("請檢察網路連線", CommentActivity.this);
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
//                            if (tempData.CommentList.size() == 0) {
//                                return;
//                            }
                            commentView.CommentList.addAll(tempData.CommentList);
                            commentView.MemberNameList.addAll(tempData.MemberNameList);
                            commentView.MImgNameList.addAll(tempData.MImgNameList);
                            commentView.CTimeList.addAll(tempData.CTimeList);
                            skip += tempData.CommentList.size();

                            if (isFirstLoad) {
                                isFirstLoad = false;

                                rv_CommentList.setLayoutManager(new LinearLayoutManager(CommentActivity.this, LinearLayoutManager.VERTICAL, false));
                                commentListAdapter = new CommentListAdapter(rv_CommentList);

                                rv_CommentList.setAdapter(commentListAdapter);
                            } else {
                                commentListAdapter.notifyDataSetChanged();
                            }

                            if (tempData.CommentList.size() < tempData.ARequestCount) {
                                //最後一次載入
                                isFinishLoad = true;
                                View footer = LayoutInflater.from(CommentActivity.this).inflate(R.layout.footer, rv_CommentList, false);
                                commentListAdapter.setFooterView(footer);
                            }
                        } else {
                            SharedService.ShowErrorDialog(StatusCode + "", CommentActivity.this);
                        }
                    }
                });
            }
        });
    }

    public class CommentListAdapter extends RecyclerView.Adapter<CommentListAdapter.ViewHolder> {
        private ImageLoader imageLoader;
        private int mStart;
        private int mEnd;

        public final int TYPE_HEADER = 0;  //说明是带有Header的
        public final int TYPE_FOOTER = 1;  //说明是带有Footer的
        public final int TYPE_NORMAL = 2;  //说明是不带有header和footer的
        private View mHeaderView;
        private View mFooterView;

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

        public CommentListAdapter(RecyclerView recyclerView) {
            imageLoader = new ImageLoader(recyclerView);
            final LinearLayoutManager linearLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    super.onScrollStateChanged(recyclerView, newState);

                    mStart = linearLayoutManager.findFirstVisibleItemPosition();
                    mEnd = linearLayoutManager.findLastVisibleItemPosition();

                    //避免下拉關閉Activity也加載圖片，避免RecyclerView為空時依然加載圖片
                    if (linearLayoutManager.findFirstVisibleItemPosition() != 0 && commentView.CommentList.size() != 0) {
                        if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                            imageLoader.loadImages(mStart, mEnd);
                        } else {
                            imageLoader.cancelAllAsyncTask();
                        }
                    }
                }

//                @Override
//                public void onScrolled(RecyclerView recyclerView,
//                                       int dx, int dy) {
//                    super.onScrolled(recyclerView, dx, dy);
//                    mStart = linearLayoutManager.findFirstVisibleItemPosition();
//                    mEnd = linearLayoutManager.findLastVisibleItemPosition();
//
//                    if (isFirstIn && mStart != -1) {
//                        imageLoader.loadImages(mStart, mEnd);
//                        isFirstIn = false;
//                    }
//                }
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
        public void onBindViewHolder(ViewHolder holder, final int position) {
            if (getItemViewType(position) == TYPE_FOOTER) {
                if (commentView.CommentList.size() > 0)
                    holder.tv_Footer.setText("沒有更多留言囉!");
                else
                    holder.tv_Footer.setText("成為第一個留言的人吧!");
                return;
            }

            holder.tv_MemberName.setText(commentView.MemberNameList.get(position));
            holder.tv_CTime.setText(commentView.CTimeList.get(position));
            holder.tv_Content.setText(commentView.CommentList.get(position).Content);

            //每次都要重設，避免刪除單筆後造成Tag混亂
            holder.iv_MImg.setTag("");
            holder.iv_MImg.setImageResource(R.drawable.defaultmimg);
            if (commentView.MImgNameList.get(position) != null) {
                holder.iv_MImg.setTag(commentView.MImgNameList.get(position) + position);
                imageLoader.showImage(holder.iv_MImg, commentView.MImgNameList.get(position));
            }

            //避免重複請求
            if (position > commentView.CommentList.size() * 0.6 && !isFinishLoad && !isLoading) {
                isLoading = true;
                GetComments();
            }
            if (commentView.CommentList.get(position).Account.equals(account) || isGAdmin) {
                holder.ib_CommentBox.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final PopupMenu popupmenu = new PopupMenu(CommentActivity.this, v);
                        popupmenu.getMenuInflater().inflate(R.menu.comment_popup, popupmenu.getMenu());

                        if (postId != -1) {
                            if (!commentView.CommentList.get(position).Account.equals(account)) {
                                popupmenu.getMenu().findItem(R.id.item_EditComment).setVisible(false);
                                popupmenu.getMenu().findItem(R.id.item_DeleteComment).setVisible(false);
                            }
                            if (isGAdmin)
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
                });
            } else {
                holder.ib_CommentBox.setVisibility(View.INVISIBLE);
            }

            holder.tv_MemberName.setTag(position);
            holder.tv_MemberName.setOnClickListener(MyClick);
            holder.iv_MImg.setTag(position);
            holder.iv_MImg.setOnClickListener(MyClick);
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

        public class ViewHolder extends RecyclerView.ViewHolder {
            private TextView tv_MemberName;
            private TextView tv_CTime;
            private TextView tv_Content;
            private ImageView iv_MImg;
            private ImageButton ib_CommentBox;
            private TextView tv_Footer;

            public ViewHolder(View itemView) {
                super(itemView);

                if (itemView == mFooterView) {
                    tv_Footer = (TextView) itemView.findViewById(R.id.tv_Footer);
                    return;
                }

                tv_MemberName = (TextView) itemView.findViewById(R.id.tv_MemberName);
                tv_CTime = (TextView) itemView.findViewById(R.id.tv_CTime);
                tv_Content = (TextView) itemView.findViewById(R.id.tv_Content);
                iv_MImg = (ImageView) itemView.findViewById(R.id.iv_MImg);
                ib_CommentBox = (ImageButton) itemView.findViewById(R.id.ib_CommentBox);
            }
        }
    }

    private class ImageLoader {
        private OkHttpClient LoadImgClient = new OkHttpClient();
        private RecyclerView rv_CommentList;

        public ImageLoader(RecyclerView recyclerView) {
            rv_CommentList = recyclerView;
        }

        public void showImage(ImageView imageView, String ImgName) {

            Bitmap bitmap = getBitmapFromLrucache(ImgName);
            //防止動態刪除貼文導致ImageView抓不到、滾動狀態下請求圖片
            if (bitmap == null && rv_CommentList.getScrollState() == RecyclerView.SCROLL_STATE_IDLE) {
                LoadImgByOkHttp(imageView, ImgName, getString(R.string.BackEndPath) + "ThumbMImages/" + ImgName);
            } else if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            }
        }

        public void loadImages(int start, int end) {

            for (int i = start; i <= end; i++) {
                //大頭貼
                String MImgName = commentView.MImgNameList.get(i);
                if (MImgName != null) {
                    ImageView MImageView = (ImageView) rv_CommentList.findViewWithTag(MImgName + i);
                    if (MImageView == null)
                        return;
                    if (getBitmapFromLrucache(MImgName) != null) {
                        MImageView.setImageBitmap(getBitmapFromLrucache(MImgName));
                    } else {
                        LoadImgByOkHttp(MImageView, MImgName, getString(R.string.BackEndPath) + "ThumbMImages/" + MImgName);
                    }
                }
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
                            SharedService.ShowTextToast("請檢察網路連線", CommentActivity.this);
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

        public void cancelAllAsyncTask() {
            LoadImgClient.dispatcher().cancelAll();
        }
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
                            SharedService.ShowTextToast("請檢察網路連線", CommentActivity.this);
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
                                SharedService.ShowErrorDialog(ResMsg, CommentActivity.this);
                            } else {
                                SharedService.ShowErrorDialog(StatusCode + "", CommentActivity.this);
                            }
                        }
                    });
                }
            });

        } else {
            SharedService.ShowTextToast("請輸入內容", this);
        }
    }

    public void EditComment(final int position) {
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
                                        SharedService.ShowTextToast("請檢察網路連線", CommentActivity.this);
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
                                            SharedService.ShowTextToast("修改成功", CommentActivity.this);
                                            CommentView.Comments TempData = commentView.CommentList.get(position);
                                            TempData.Content = et_EditContent.getText().toString();
                                            commentView.CommentList.set(position, TempData);
                                            commentListAdapter.notifyDataSetChanged();
                                        } else if (StatusCode == 400) {
                                            SharedService.ShowErrorDialog(ResMsg, CommentActivity.this);
                                        } else {
                                            SharedService.ShowErrorDialog(StatusCode + "", CommentActivity.this);
                                        }
                                    }
                                });
                            }
                        });
                    }
                })
                .show();

    }

    public void DeleteComment(final int position) {
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
                                        SharedService.ShowTextToast("請檢察網路連線", CommentActivity.this);
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

                                            commentListAdapter.notifyItemRemoved(position);
                                            int dItemCount = commentView.CommentList.size() - position;
                                            commentListAdapter.notifyItemRangeChanged(position, dItemCount);
                                        } else if (StatusCode == 400) {
                                            SharedService.ShowErrorDialog(ResMsg, CommentActivity.this);
                                        } else {
                                            SharedService.ShowErrorDialog(StatusCode + "", CommentActivity.this);
                                        }
                                    }
                                });
                            }
                        });
                    }
                })
                .show();
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
                        SharedService.ShowTextToast("請檢察網路連線", CommentActivity.this);
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

                            commentListAdapter.notifyItemInserted(0);
                            commentListAdapter.notifyItemRangeChanged(0, commentView.CommentList.size());
                            rv_CommentList.scrollToPosition(0);
                        } else if (StatusCode == 400) {
                            SharedService.ShowErrorDialog(ResMsg, CommentActivity.this);
                        } else {
                            SharedService.ShowErrorDialog(StatusCode + "", CommentActivity.this);
                        }
                    }
                });
            }
        });
    }

    private void OpenProFile(View v) {
        int Position = (int) v.getTag();
        Intent intent = new Intent(this, ProFileActivity.class);
        intent.putExtra("Account", commentView.CommentList.get(Position).Account);
        startActivity(intent);
    }
}