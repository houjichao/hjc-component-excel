package com.hjc.component.service.impl;

import com.hjc.component.service.BaseDataTypeHandleCommand;
import com.hjc.component.util.PatternUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author by hjc
 * @Classname DoubleHandleCommand
 * @Description 双精度型数据处理
 * @Date 2019/7/9 21:36
 */
@Service
public class DoubleHandleCommand implements BaseDataTypeHandleCommand {
    public static final Pattern PATTERN_DECIMAL = Pattern.compile("^[\\-+]?(0(\\.\\d*)?|([1-9](\\d)?)(\\.\\d*)?|1[0-7]\\d(\\.\\d*)?|180\\.0*)$");
    public static final Pattern PATTERN_1 = Pattern.compile("^[\\-+]?((0{1,3}|([1-8]\\d?))(\\.\\d*)?|90(\\.0*)?)$");
    public static final Pattern PATTERN_2 = Pattern.compile("^([-+])?(((\\d|[1-9]\\d|1[0-7]\\d|0{1,3})\\.\\d*)|(\\d|[1-9]\\d|1[0-7]\\d|0{1,3})|180\\.0*|180)$");

    @Override
    public Object process(String title, String value, Map<String, String> errMsg, String format) {
        //双浮点
        if (StringUtils.isNotBlank(value) && PatternUtil.PATTERN_DECIMAL.matcher(value).matches()) {
            Double number = Double.parseDouble(value);
            BigDecimal bigDecimal = new BigDecimal(number);
            double bdfScale = bigDecimal.setScale(6, BigDecimal.ROUND_DOWN).doubleValue();
            return bdfScale;
        } else if (StringUtils.isNotBlank(value) && !PatternUtil.PATTERN_DECIMAL.matcher(value).matches()) {
            errMsg.put(title, "请输入小数");
        }
        return null;
    }

    public static void main(String[] args) {
        String value = "000.000000";
        if (StringUtils.isNotBlank(value) && PATTERN_1.matcher(value).matches()) {
            Double number = Double.parseDouble(value);
            BigDecimal bigDecimal = new BigDecimal(number);
            double bdfScale = bigDecimal.setScale(6, BigDecimal.ROUND_DOWN).doubleValue();
            System.out.println(bdfScale);
        }
    }
}