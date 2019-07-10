package com.hjc.component.service.impl;

import com.hjc.component.service.BaseDataTypeHandleCommand;
import com.hjc.component.util.PatternUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @author by hjc
 * @Classname IntegerHandleCommand
 * @Description 整型数据处理
 * @Date 2019/7/9 21:36
 */
@Service
public class IntegerHandleCommand implements BaseDataTypeHandleCommand {


    @Override
    public Object process(String title, String value, Map<String, String> errMsg,String format) {
        //整型
        if (StringUtils.isNotBlank(value) && PatternUtil.PATTERN_INTEGER.matcher(value).matches()) {
            return Integer.parseInt(value);
        } else if (StringUtils.isNotBlank(value) && !PatternUtil.PATTERN_INTEGER.matcher(value).matches()) {
            errMsg.put(title, "请输入整数");
        }
        return null;
    }
}