package com.hjc.component.service.impl;

import com.hjc.component.service.BaseDataTypeHandleCommand;
import com.hjc.component.util.PatternUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @author by hjc
 * @Classname DoubleHandleCommand
 * @Description 双精度型数据处理
 * @Date 2019/7/9 21:36
 */
@Service
public class FloatHandleCommand implements BaseDataTypeHandleCommand {

    @Override
    public Object process(String title, String value, Map<String, String> errMsg,String format) {
        if (StringUtils.isNotBlank(value) && PatternUtil.PATTERN_DECIMAL.matcher(value).matches()) {
            return Float.parseFloat(value);
        } else if (StringUtils.isNotBlank(value) && !PatternUtil.PATTERN_DECIMAL.matcher(value).matches()) {
            errMsg.put(title, "请输入小数");
        }
        return null;
    }
}