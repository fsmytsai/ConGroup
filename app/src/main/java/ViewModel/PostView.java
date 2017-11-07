package ViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by user on 2016/11/18.
 */

public class PostView {

    public List<Posts> PostList;
    public int ARequestCount;
    public List<String> MemberNameList;
    public List<String> MImgNameList;
    public List<String> GNameList;
    public List<String> CTimeList;
    public List<Boolean> IsMarkList;
    public List<Boolean> IsLikeList;
    public List<Integer> LikeNumList;
    public List<Integer> CommentNumList;

    public static class Posts {
        public List<PostImage> PostImages;
        public List<PostLocation> PostLocations;
        public int PostId;
        public int GroupId;
        public String Account;
        public String Content;
        public String CreateTime;
        public String LatestTime;
        public boolean IsTop;
    }

    public static class PostImage{
        public int ImgNo;
        public String ImgName;
    }

    public static class PostLocation{
        public String LocationName;
        public String PlaceId;
    }

    public PostView() {
        PostList = new ArrayList<>();
        MemberNameList = new ArrayList<>();
        MImgNameList = new ArrayList<>();
        GNameList = new ArrayList<>();
        CTimeList = new ArrayList<>();
        IsMarkList = new ArrayList<>();
        IsLikeList = new ArrayList<>();
        LikeNumList = new ArrayList<>();
        CommentNumList = new ArrayList<>();
    }
}
