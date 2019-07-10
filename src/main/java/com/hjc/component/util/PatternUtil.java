package com.hjc.component.util;

import java.util.regex.Pattern;

/**
 * @author by hjc
 * @Classname PatternUtil
 * @Description 常用正则
 * @Date 2019/7/9 21:36
 */
public class PatternUtil {

    public static final Pattern PATTERN_INTEGER = Pattern.compile("^[0-9]{1,8}$");
    public static final Pattern PATTERN_DECIMAL = Pattern.compile("^([-+])?\\d+(\\.\\d+)?$");

    /**
     *经度正则
     */
    public static final Pattern PATTERN_LONGITUDE = Pattern.compile("^([-+])?(((\\d|[1-9]\\d|1[0-7]\\d|0{1,3})\\.\\d*)|(\\d|[1-9]\\d|1[0-7]\\d|0{1,3})|180\\.0*|180)$");

    /**
     *纬度正则
     */
    public static final Pattern PATTERN_LATITUDE = Pattern.compile("^[\\-+]?((0{1,3}|([1-8]\\d?))(\\.\\d*)?|90(\\.0*)?)$");

    /**
     * 角度正则
     */
    public static final Pattern PATTERN_ANGLE = Pattern.compile("^[0-9]+([.][0-9]+)?$");


    /**
     * 限速正则
     */
    public static final Pattern PATTERN_SPEED = Pattern.compile("^[0-9]+([.][0-9]+)?$");


}