package ViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by user on 2017/4/25.
 */

public class PChatRoomView {
    public List<ChatRooms> CRList;
    public int ARequestCount;
    public List<String> LatestContentList;
    public List<String> LTimeList;
    public List<String> MemberNameList;
    public List<String> MImgNameList;
    public List<String> LastOnTimeList;

    public static class ChatRooms {
        public int RoomId;
        public String SendAccount;
        public String ReceiveAccount;
    }

    public PChatRoomView() {
        CRList = new ArrayList<>();
        LatestContentList = new ArrayList<>();
        LTimeList = new ArrayList<>();
        MemberNameList = new ArrayList<>();
        MImgNameList = new ArrayList<>();
        LastOnTimeList = new ArrayList<>();
    }
}
