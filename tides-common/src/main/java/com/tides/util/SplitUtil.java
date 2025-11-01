package com.tides.util;

import static com.tides.constant.Constant.GLIDE_LINE;

/**
 * @description: 分割工具
 * @author: 19continue
 **/
public class SplitUtil {
    
    public static String[] toSplit(String str) {
        return str.split(GLIDE_LINE);
    }
}
