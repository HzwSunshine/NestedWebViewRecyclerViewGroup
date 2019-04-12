package com.hzw.nested;

import android.content.res.Resources;

/**
 * author: hzw
 * time: 2019/4/8 下午5:03
 * description:
 */
class DpUtil {
    static int dip2px(float dipValue) {
        final float scale = Resources.getSystem().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }
}
