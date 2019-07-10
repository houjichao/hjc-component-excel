package com.hjc.component.service.impl;

import com.hjc.component.enums.BaseDataTypeHandleCommandEnum;
import com.hjc.component.service.BaseDataTypeHandleCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @author by hjc
 * @Classname PrintAllCommand
 * @Description 打印所有
 * @Date 2019/7/9 21:36
 */
@Service
public class PrintAllCommand implements BaseDataTypeHandleCommand {
    private final static Logger LOGGER = LoggerFactory.getLogger(PrintAllCommand.class);

    @Override
    public Object process(String title, String formatValue, Map<String, String> errMsg,String format) {
        Map<String, String> allStatusCode = BaseDataTypeHandleCommandEnum.getAllStatusCode();
        LOGGER.warn("====================================");
        for (Map.Entry<String, String> stringStringEntry : allStatusCode.entrySet()) {
            String key = stringStringEntry.getKey();
            String value = stringStringEntry.getValue();
            LOGGER.warn(key + "----->" + value);
        }
        LOGGER.warn("====================================");
        return null;
    }
}
