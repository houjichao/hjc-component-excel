package com.hjc.component.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author by hjc
 * @Classname ExcelCell
 * @Description Excel单元格注解
 * @Date 2019/7/9 21:36
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ExcelCell {

    //标题
    String title();

    //排序
    int order();

    //枚举值
    String[] enums() default {};

    //格式
    String format() default "yyyy-MM-dd HH:mm:ss";

    //是否可空
    boolean notNull() default false;

    //正则
    String pattern() default "";

    //批注信息
    String comment() default "";

    //字段长度
    int length() default 0;
}