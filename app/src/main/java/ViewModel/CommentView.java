package ViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by user on 2016/11/19.
 */

public class CommentView {

    public List<Comments> CommentList;
    public int ARequestCount;
    public List<String> MemberNameList;
    public List<String> MImgNameList;
    public List<String> CTimeList;

    public static class Comments {
        public int ComId;
        public int PostId;
        public String Account;
        public String Content;
        public String CreateTime;
    }

    public CommentView() {
        CommentList = new ArrayList<>();
        MemberNameList = new ArrayList<>();
        MImgNameList = new ArrayList<>();
        CTimeList = new ArrayList<>();
    }
}
