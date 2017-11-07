package ViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by user on 2017/4/29.
 */

public class GroupSearchView {

    public List<MemberAllGroupView.SimpleGroup> SimpleGroupList;
    public ForAppend forAppend;

    public class SimpleGroup {
        public int GroupId;
        public String GImgName;
        public String GName;
    }

    public class ForAppend {
        public int NowAppendTimes;

        public int MaxAppendTimes;

        public int ItemNum;
    }

    public GroupSearchView() {
        SimpleGroupList = new ArrayList<>();
    }
}
