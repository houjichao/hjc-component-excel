package com.hjc.component.sax;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hjc.component.annotation.ExcelCell;
import com.hjc.component.service.BaseDataTypeHandleCommand;
import com.hjc.component.service.BaseDataTypeHandleCommandContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.POIXMLDocument;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author by hjc
 * @Classname ExcelReaderUtil
 * @Description Excel读取工具类
 * @Date 2019/7/9 21:36
 */
@Slf4j
@Component
public class ExcelReaderUtil {

    @Autowired
    ExcelXlsxReader excelXlsxReader;

    @Autowired
    private BaseDataTypeHandleCommandContext context;

    /**
     * 每获取一条记录，即打印
     * 在flume里每获取一条记录即发送，而不必缓存起来，可以大大减少内存的消耗，这里主要是针对flume读取大数据量excel来说的
     * @param sheetName
     * @param sheetIndex
     * @param curRow
     * @param cellList
     */
    public <T> void sendRows(String filePath, String sheetName, int sheetIndex, int curRow, Map<Integer,String> cellList,
                                Class<T> clazz,Map<String, List<T>> result,Map<String, List<ExcelXlsxReader.InvalidRow>> unValidRows) {
        if (!result.containsKey(clazz.getName())) {
            result.put(clazz.getName(), Lists.newArrayList());
        }
        T instance = null;
        Map<String, String> errMsg = Maps.newHashMap();
        try {
            instance = clazz.newInstance();
            Field[] fields = instance.getClass().getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                ExcelCell ant = field.getAnnotation(ExcelCell.class);
                String title = ant.title();
                Integer order = ant.order();
                String value = cellList.get(order);
                String pattern = ant.pattern();
                Pattern patternTemp = Pattern.compile(pattern);
                Object formatValue = null;
                if (ant.notNull() && StringUtils.isBlank(value)) {
                    //非空校验
                    //errMsg.put(title, "不能为空");
                }
                if (ant.notNull() && !"".equals(pattern)) {
                    //正则校验
                    if (!patternTemp.matcher(value).matches()) {
                        //errMsg.put(title, "格式不合法：" + pattern);
                    }
                }
                //通过策略模式转换不同类型的值
                Class fclazz = field.getType();
                BaseDataTypeHandleCommand execute = context.getInstance(fclazz.getName());
                formatValue = execute.process(title, value, errMsg, ant.format());
                field.set(instance, formatValue);
            }
            List<T> resultList = result.get(clazz.getName());
            resultList.add(instance);
        } catch (Exception ex) {
            log.error("ex,{}",ex);
        }
    }

    public void readExcel(InputStream inputStream,List<Class> classList) {
        long start = System.currentTimeMillis();
        int totalRows = 0;
        try {
            //判断excel版本
            if (!inputStream.markSupported()) {
                inputStream = new PushbackInputStream(inputStream, 8);
            }
            if (POIFSFileSystem.hasPOIFSHeader(inputStream)) {
                ExcelXlsReader excelXls = new ExcelXlsReader();
                totalRows = excelXls.process(inputStream);
                log.info("2003版本及以下");
            } else if (POIXMLDocument.hasOOXMLHeader(inputStream)) {
                ExcelXlsxReader.ExcelConvertResult result = excelXlsxReader.process(inputStream, classList);
                log.info("2007版本及以上");
            } else {
                throw new RuntimeException("Excel格式有误", new Throwable());
            }
        } catch (IOException e) {
            log.error("excel文件不合法，转换失败,exception is:{}", e);
            throw new RuntimeException(String.format("excel文件不合法，转换失败,exception is:%s", e));
        }
        long end = System.currentTimeMillis();
        log.info("批量导入数据花费时间：" + (end - start));
        log.info("发送的总行数：" + totalRows);
    }

}