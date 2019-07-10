package com.hjc.component.service.impl;

import com.hjc.component.service.BaseDataTypeHandleCommand;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @author by hjc
 * @Classname BooleanHandleCommand
 * @Description 布尔型数据处理
 * @Date 2019/7/9 21:36
 */
@Service
public class BooleanHandleCommand implements BaseDataTypeHandleCommand {

    @Override
    public Object process(String title, String value, Map<String, String> errMsg,String format) {
        //布尔
        boolean tempFlagOne = StringUtils.isNotBlank(value) && "true".equals(value) || "false".equals(value);
        boolean tempFlagTwo = StringUtils.isNotBlank(value) && (!"true".equals(value) && !"false".equals(value));
        if (tempFlagOne) {
            return Boolean.parseBoolean(value);
        } else if (tempFlagTwo) {
            errMsg.put(title, "请输入true/false");
        }
        return null;
    }

}