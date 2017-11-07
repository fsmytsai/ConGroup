package ViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by user on 2017/4/19.
 */

public class GroupMessageView {

    public List<GroupMessages> GroupMessageList;
    public int ARequestCount;
    public List<String> MemberNameList;
    public List<String> MImgNameList;
    public List<String> CTimeList;

    public static class GroupMessages {
        public int GMessId;
        public int GroupId;
        public String SendAccount;
        public String Content;
        public String CreateTime;
    }

    public GroupMessageView() {
        GroupMessageList = new ArrayList<>();
        MemberNameList = new ArrayList<>();
        MImgNameList = new ArrayList<>();
        CTimeList = new ArrayList<>();
    }
}
