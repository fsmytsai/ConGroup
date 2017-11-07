package ViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by user on 2017/4/18.
 */

public class NearbySearchView {
    public List<NearbySearchResult> results;
    public String next_page_token;

    public static class NearbySearchResult {
        public String name;
        public String place_id;
    }

    public NearbySearchView() {
        results = new ArrayList<>();
    }
}
