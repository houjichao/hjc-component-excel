package com.hjc.component.service.impl;

import com.hjc.component.service.BaseDataTypeHandleCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @author by hjc
 * @Classname StringHandleCommand
 * @Description 字符串型数据处理
 * @Date 2019/7/9 21:36
 */
@Service
public class StringHandleCommand implements BaseDataTypeHandleCommand {
    private final static Logger LOGGER = LoggerFactory.getLogger(StringHandleCommand.class);

    @Override
    public Object process(String title, String value, Map<String, String> errMsg,String format) {
        //字符串型
        return value;
    }

}