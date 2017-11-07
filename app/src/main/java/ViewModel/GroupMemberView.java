package ViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by user on 2017/5/11.
 */

public class GroupMemberView {
    public List<GroupMembers> GroupMemberList;
    public List<String> MemberNameList;
    public List<String> JTimeList;
    public List<String> MImgNameList;
    public GroupSearchView.ForAppend forAppend;

    public static class GroupMembers {
        public int GroupId;
        public String Account;
        public int Character;
    }

    public GroupMemberView() {
        GroupMemberList = new ArrayList<>();
        MemberNameList = new ArrayList<>();
        JTimeList = new ArrayList<>();
        MImgNameList = new ArrayList<>();
    }
}
