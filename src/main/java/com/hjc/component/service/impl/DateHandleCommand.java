package com.hjc.component.service.impl;

import com.hjc.component.service.BaseDataTypeHandleCommand;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Map;

/**
 * @author by hjc
 * @Classname DateHandleCommand
 * @Description 日期型数据处理
 * @Date 2019/7/9 21:36
 */
@Service
public class DateHandleCommand implements BaseDataTypeHandleCommand {

    @Override
    public Object process(String title, String value, Map<String, String> errMsg, String format) {
        if (StringUtils.isNotBlank(value)) {
            //日期
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(format);
                return sdf.parse(value);
            } catch (ParseException e) {
                errMsg.put(title, "输入格式：" + format);
                return null;
            }
        } else {
            return null;
        }
    }
}