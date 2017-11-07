package ViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by user on 2017/4/10.
 */

public class MemberAllGroupView {
    public List<SimpleGroup> SimpleGroupList;

    public class SimpleGroup {
        public int GroupId;
        public String GImgName;
        public String GName;
    }

    public MemberAllGroupView() {
        SimpleGroupList = new ArrayList();
    }
}
