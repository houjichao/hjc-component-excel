package com.hjc.component.service;

import java.util.Map;

/**
 * Function:
 *
 * @author hjc
 */
public interface BaseDataTypeHandleCommand {

    /**
     * 执行
     * @param title
     * @param errMsg
     * @param value
     */
    Object process(String title, String value, Map<String, String> errMsg, String format) ;
}
