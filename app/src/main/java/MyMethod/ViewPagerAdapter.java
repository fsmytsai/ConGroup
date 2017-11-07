package MyMethod;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ImageSpan;

import java.util.List;

/**
 * Created by user on 2016/10/16.
 */

public class ViewPagerAdapter extends FragmentPagerAdapter {

    public String[] tabTitles;
    public int[] tabIcons;
    private List<Fragment> fragments;
    private Context context;

//    public ViewPagerAdapter(FragmentManager fm, List<Fragment> f) {
//        super(fm);
//        fragments = f;
//    }

    public ViewPagerAdapter(FragmentManager fm, List<Fragment> f, Context mContext) {
        super(fm);
        fragments = f;
        context = mContext;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        if (tabIcons == null) {
            return tabTitles[position];
        } else {
            if (!tabTitles[position].equals(""))
                return tabTitles[position];
            SpannableString spannableString = null;
            //use the MrVector library to inflate vector drawable inside tab
            Drawable drawable = ContextCompat.getDrawable(context, tabIcons[position]);
            //set the size of drawable to 36 pixels
            int height = (int) (SharedService.toolBarHeight * 0.7);
//            height = height != 0 ? height : 96;
            drawable.setBounds(0, 0, height, height);
            ImageSpan imageSpan = new ImageSpan(drawable);
            //to make our tabs icon only, set the Text as blank string with white space
            spannableString = new SpannableString(" ");
            spannableString.setSpan(imageSpan, 0, spannableString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            return spannableString;
        }
    }

    @Override
    public int getCount() {
        return fragments.size();
    }

    @Override
    public Fragment getItem(int position) {
        return fragments.get(position); //從上方List<Fragment> fragments取得
    }

}