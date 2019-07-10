package com.hjc.component.enums;

import java.util.HashMap;
import java.util.Map;

/**
 * @author by hjc
 * @Classname BaseDataTypeHandleCommandEnum
 * @Description 基本类型处理类枚举
 * @Date 2019/7/9 21:36
 */
public enum BaseDataTypeHandleCommandEnum {

    ALL("all", "获取所有命令", "PrintAllCommand"),
    STRING("java.lang.String", "String处理", "StringHandleCommand"),
    INTEGER("java.lang.Integer", "Integer处理", "IntegerHandleCommand"),
    FLOAT("java.lang.Float", "FLOAT处理", "FloatHandleCommand"),
    DOUBLE("java.lang.Double", "Double处理", "DoubleHandleCommand"),
    BIG_DECIMAL("java.math.BigDecimal", "BigInt处理", "BigDecimalHandleCommand"),
    BOOLEAN("java.lang.Boolean", "Boolean处理", "BooleanHandleCommand"),
    TIMESTAMP("java.sql.Timestamp", "TimeStamp处理", "TimestampHandleCommand"),
    DATE("java.util.Date", "Date处理", "DateHandleCommand");


    /**
     * 枚举值码
     */
    private final String commandType;
    /**
     * 枚举描述
     */
    private final String desc;
    /**
     * 实现类
     */
    private final String clazz;


    /**
     * 构建一个 。
     *
     * @param commandType 枚举值码。
     * @param desc        枚举描述。
     */
    private BaseDataTypeHandleCommandEnum(String commandType, String desc, String clazz) {
        this.commandType = commandType;
        this.desc = desc;
        this.clazz = clazz;
    }

    /**
     * 得到枚举值码。
     *
     * @return 枚举值码。
     */
    public String getCommandType() {
        return commandType;
    }

    /**
     * 获取 class。
     *
     * @return class。
     */
    public String getClazz() {
        return clazz;
    }

    /**
     * 得到枚举描述。
     *
     * @return 枚举描述。
     */
    public String getDesc() {
        return desc;
    }

    /**
     * 得到枚举值码。
     *
     * @return 枚举值码。
     */
    public String code() {
        return commandType;
    }

    /**
     * 得到枚举描述。
     *
     * @return 枚举描述。
     */
    public String message() {
        return desc;
    }

    /**
     * 获取全部枚举值码。
     *
     * @return 全部枚举值码。
     */
    public static Map<String, String> getAllStatusCode() {
        Map<String, String> map = new HashMap<String, String>(16);
        for (BaseDataTypeHandleCommandEnum status : values()) {
            map.put(status.getCommandType(), status.getDesc());
        }
        return map;
    }

    public static Map<String, String> getAllClazz() {
        Map<String, String> map = new HashMap<String, String>(16);
        for (BaseDataTypeHandleCommandEnum status : values()) {
            map.put(status.getCommandType().trim(), "com.hjc.component.service.impl." + status.getClazz());
        }
        return map;
    }


}