package com.hjc.component.service.impl;

import com.hjc.component.service.BaseDataTypeHandleCommand;
import com.hjc.component.util.PatternUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

/**
 * @author by hjc
 * @Classname BigDecimalHandleCommand
 * @Description 大数数据处理
 * @Date 2019/7/9 21:36
 */
@Service
public class BigDecimalHandleCommand implements BaseDataTypeHandleCommand {

    @Override
    public Object process(String title, String value, Map<String, String> errMsg,String format) {
        //大数
        if (StringUtils.isNotBlank(value) && PatternUtil.PATTERN_DECIMAL.matcher(value).matches()) {
            return new BigDecimal(value);
        } else if (StringUtils.isNotBlank(value) && !PatternUtil.PATTERN_DECIMAL.matcher(value).matches()) {
            errMsg.put(title, "请输入小数");
        }
        return null;
    }
}