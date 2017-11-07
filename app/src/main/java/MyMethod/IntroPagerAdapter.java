package MyMethod;

import android.app.Activity;
import android.content.SharedPreferences;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckedTextView;

import com.tsai.congroup.MainActivity;
import com.tsai.congroup.R;

import java.util.List;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by user on 2017/4/28.
 */

public class IntroPagerAdapter extends PagerAdapter {
    private List<View> mListViews;
    private MainActivity mainActivity;

    public IntroPagerAdapter(List<View> mListViews, Activity mActivity) {
        this.mListViews = mListViews;
        mainActivity = (MainActivity) mActivity;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }


    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View view = mListViews.get(position);
        if (position == 8) {

            final SharedPreferences sp_Settings = mainActivity.getSharedPreferences("Settings", MODE_PRIVATE);
            final CheckedTextView ctv_NeverIntro = (CheckedTextView) view.findViewById(R.id.ctv_NeverIntro);
            ctv_NeverIntro.setChecked(sp_Settings.getBoolean("IsNeverIntro", false));
            ctv_NeverIntro.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    ctv_NeverIntro.toggle();
                }
            });

            Button bt_StartConGroup = (Button) view.findViewById(R.id.bt_StartConGroup);
            bt_StartConGroup.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (ctv_NeverIntro.isChecked()) {
                        sp_Settings.edit().putBoolean("IsNeverIntro", true).apply();
                        SharedService.ShowTextToast("可在功能頁面再次開啟", mainActivity);
                    } else {
                        sp_Settings.edit().putBoolean("IsNeverIntro", false).apply();
                    }
                    mainActivity.CheckLogon();
                }
            });
        }
        container.addView(view);
        return view;
    }

    @Override
    public int getCount() {
        return mListViews.size();
    }

    @Override
    public boolean isViewFromObject(View arg0, Object arg1) {
        return arg0 == arg1;
    }
}
