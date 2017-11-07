package ViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by user on 2017/4/25.
 */

public class PrivateMessageView {
    public List<PrivateMessages> PrivateMessageList;
    public int ARequestCount;
    public String Account;
    public String MemberName;
    public String MImgName;
    public List<String> CTimeList;

    public static class PrivateMessages {
        public int PMessId;
        public int RoomId;
        public boolean Sender;
        public String Content;
        public String CreateTime;
    }

    public PrivateMessageView() {
        PrivateMessageList = new ArrayList<>();
        CTimeList = new ArrayList<>();
    }
}
