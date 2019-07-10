package com.hjc.component.service.impl;

import com.hjc.component.service.BaseDataTypeHandleCommand;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Map;

/**
 * @author by hjc
 * @Classname StringHandleCommand
 * @Description 时间戳型数据处理
 * @Date 2019/7/9 21:36
 */
@Service
public class TimestampHandleCommand implements BaseDataTypeHandleCommand {

    @Override
    public Object process(String title, String value, Map<String, String> errMsg,String format) {
        if (StringUtils.isNotBlank(value)) {
            //时间戳
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(format);
                return new Timestamp(sdf.parse(value).getTime());
            } catch (ParseException e) {
                errMsg.put(title, "输入格式：" + format);
            }
        }
        return null;
    }
}