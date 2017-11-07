package ViewModel;

/**
 * Created by user on 2017/4/13.
 */

public class GroupView {
    public Groups Group;
    public String CTime;
    public int Character;
    public boolean IsJoin;
    public boolean IsApply;
    public boolean IsMark;

    public static class Groups {
        public int GroupId;
        public String GImgName;
        public String GName;
        public String GIntroduction;
        public byte GType;
        public byte JoinType;
        public byte InviteType;
    }
}
