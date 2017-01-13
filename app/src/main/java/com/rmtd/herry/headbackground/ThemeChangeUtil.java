package com.rmtd.herry.headbackground;

import android.app.Activity;

/**
 * Created by melo on 2017/1/13.
 */
public class ThemeChangeUtil {
    public static boolean isChange = false;
    public static void changeTheme(Activity activity){
        if(isChange){
            activity.setTheme(R.style.NightTheme);
        }else {
            activity.setTheme(R.style.DayTheme);
        }
    }
}
