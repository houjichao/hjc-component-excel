package com.hjc.component.service;

import com.hjc.component.enums.BaseDataTypeHandleCommandEnum;
import com.hjc.component.service.impl.PrintAllCommand;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Function:
 *
 * @author hjc
 */
@Component
@Slf4j
public class BaseDataTypeHandleCommandContext {

    /**
     * 获取执行器实例
     * @param command 执行器实例
     * @return
     */
    public BaseDataTypeHandleCommand getInstance(String command) {

        Map<String, String> allClazz = BaseDataTypeHandleCommandEnum.getAllClazz();

        //兼容需要命令后接参数的数据 :q cross
        String[] trim = command.trim().split(" ");
        String clazz = allClazz.get(trim[0]);
        BaseDataTypeHandleCommand innerCommand = null;
        try {
            if (StringUtils.isEmpty(clazz)){
                clazz = PrintAllCommand.class.getName() ;
            }
            innerCommand = (BaseDataTypeHandleCommand) SpringBeanFactory.getBean(Class.forName(clazz));
        } catch (Exception e) {
            log.error("Exception", e);
        }
        return innerCommand;
    }

}
