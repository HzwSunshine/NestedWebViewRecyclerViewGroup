package com.hzw.nested;

import android.content.res.Resources;
import android.os.Build;

/**
 * author: hzw
 * time: 2019/4/8 下午5:03
 * description:util
 */
class Util {
    static int dip2px(float dipValue) {
        final float scale = Resources.getSystem().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

    /**
     * @return 系统版本是否高于Android 5.0（API 21）
     */
    static boolean isAboveL() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }
}
