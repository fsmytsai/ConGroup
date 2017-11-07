package ViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by user on 2017/4/12.
 */

public class NotificationView {
    public List<Notifications> NotificationList;
    public List<String> MImgNameList;
    public List<String> LTimeList;
    public int ARequestCount;

    public static class Notifications {
        public String NotifiId;
        public String Account;
        public String Trigger;
        public String Content;
        public String LatestTime;
        public boolean IsRead;
        public String LatestReadTime;
    }

    public NotificationView()
    {
        NotificationList = new ArrayList();
        MImgNameList = new ArrayList();
        LTimeList = new ArrayList();
    }
}
