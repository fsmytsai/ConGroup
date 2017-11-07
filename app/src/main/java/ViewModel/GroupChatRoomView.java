package ViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by user on 2017/4/19.
 */

public class GroupChatRoomView {
    public List<GroupChatRoom> GroupCRList;
    public List<String> LastCTimeList;
    public int ARequestCount;

    public static class GroupChatRoom {
        public int GroupId;
        public String GName;
        public String GImgName;
        public String LastMemberName;
        public String LastContent;
        public String LastCreateTime;
    }

    public GroupChatRoomView() {
        GroupCRList = new ArrayList<>();
        LastCTimeList = new ArrayList<>();
    }
}
