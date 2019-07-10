package com.hjc.component.util;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hjc.component.annotation.ExcelCell;
import com.hjc.component.service.BaseDataTypeHandleCommand;
import com.hjc.component.service.BaseDataTypeHandleCommandContext;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.POIXMLDocument;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.streaming.SXSSFCell;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author by hjc
 * @Classname ExcelConverter
 * @Description 通用excel转换器
 * @Date 2019/7/9 21:36
 */
@Slf4j
@Component
public class ExcelConverter {

    @Autowired
    private BaseDataTypeHandleCommandContext context;

    /**
     * 未通过校验行及错误信息
     */
    @Data
    @AllArgsConstructor
    public static class InvalidRow {
        private Row row;
        private Map<String, String> errMsg;

        public String getFormatErrorMsg() {
            return errMsg.keySet().stream().map(title -> {
                StringBuffer buff = new StringBuffer();
                buff.append("【");
                buff.append(title);
                buff.append("】");
                buff.append(errMsg.get(title));
                return buff.toString();
            }).collect(Collectors.joining("\r\n"));
        }
    }

    /**
     * 导入结果包装
     *
     * @param <T>
     */
    @Data
    @AllArgsConstructor
    public static class ExcelConvertResult<T> {
        private Map<String, List<T>> records;
        private Map<String, List<InvalidRow>> invalidRows;
    }

    /**
     * 表头行号
     **/
    private static final int TITLE_ROW_NUM = 0;
    /**
     * 数据起始行号
     **/
    private static final int DATA_START_ROW_NUM = 1;

    /**
     * 将excel内容转换为bean集合
     *
     * @param fileName 文件名（用来判断excel版本）
     * @param is       输入流
     * @param clazzList    目标类型
     * @return
     */
    public <T> ExcelConvertResult convert(String fileName, InputStream is, List<Class> clazzList,Integer orgType) {
        Map<String, List<T>> result = Maps.newHashMap();
        Map<String, List<InvalidRow>> unValidRows = Maps.newHashMap();
        Workbook wb = null;
        try {
            //判断excel版本
            if (!is.markSupported()) {
                is = new PushbackInputStream(is, 8);
            }
            if (POIFSFileSystem.hasPOIFSHeader(is)) {
                wb = new HSSFWorkbook(is);
                log.info("2003版本及以下");
            } else if (POIXMLDocument.hasOOXMLHeader(is)) {
                wb = new XSSFWorkbook(is);
                log.info("2007版本及以上");
            } else {
                throw new RuntimeException("Excel格式有误", new Throwable());
            }
        } catch (IOException e) {
            log.error("excel文件不合法，转换失败,exception is:{}", e);
        }
        int count = 0;
        int sheetNum = wb.getNumberOfSheets();
        for (Class clzz : clazzList) {
            if (count < sheetNum) {
                convert(clzz, wb.getSheetAt(count), result, unValidRows, orgType);
                count++;
            }
        }
        return new ExcelConvertResult(result, unValidRows);
    }

    public <T> ExcelConvertResult convert(Class<T> clazz, Sheet sheet, Map<String,List<T>> result, Map<String,List<InvalidRow>> unValidRows, Integer orgType) {
        if (sheet == null) {
            return null;
        }
        Map<Integer, Field> indexField = generateColumns(sheet.getRow(TITLE_ROW_NUM), clazz);
        if (indexField == null || indexField.size() == 0) {
            return null;
        }
        IntStream.rangeClosed(DATA_START_ROW_NUM, sheet.getLastRowNum()).forEach(rIndex -> {
            Row row = sheet.getRow(rIndex);
            if (isNullRow(row)) {
                return;
            }
            try {
                T instance = clazz.newInstance();
                Map<String, String> errMsg = Maps.newHashMap();
                indexField.keySet().stream().forEach(cIndex -> {
                    Field f = indexField.get(cIndex);
                    Cell cell = row.getCell(cIndex);
                    if (cell != null) {
                        try {
                            if (sheet.getClass().getName().equals(HSSFSheet.class.getName())) {
                                cell.setCellType(HSSFCell.CELL_TYPE_STRING);
                            }
                            String value = cell.getStringCellValue();
                            setValueToField(instance, f, value, errMsg, orgType);
                        } catch (Exception e) {
                            log.error("单元格取值解析异常，单元格内容为{}", cell);
                            throw new RuntimeException("单元格取值解析异常，单元格内容为:" + cell);
                        }
                    } else {
                        cell = row.createCell(cIndex);
                        String value = cell.getStringCellValue();
                        if (sheet.getClass().getName().equals(HSSFSheet.class.getName())) {
                            cell.setCellType(HSSFCell.CELL_TYPE_STRING);
                        }
                        if (StringUtils.isBlank(value)) {
                            value = cell.getStringCellValue();
                        }
                        setValueToField(instance, f, value, errMsg, orgType);
                    }
                });
                if (errMsg.keySet().size() > 0) {
                    if (!unValidRows.containsKey(clazz.getName())) {
                        unValidRows.put(clazz.getName(), Lists.newArrayList());
                    }
                    List<InvalidRow> unVaildRowsList = unValidRows.get(clazz.getName());
                    unVaildRowsList.add(new InvalidRow(row, errMsg));
                } else {
                    if (!result.containsKey(clazz.getName())) {
                        result.put(clazz.getName(), Lists.newArrayList());
                    }
                    List<T> resultList = result.get(clazz.getName());
                    resultList.add(instance);
                }
            } catch (InstantiationException | IllegalAccessException e) {
                log.warn("excel导入，创建实例失败：" + e);
                return;
            }
        });
        return null;
    }

    public static String getStringValueFromCell(Cell cell) {
        SimpleDateFormat sFormat = new SimpleDateFormat("MM/dd/yyyy");
        DecimalFormat decimalFormat = new DecimalFormat("#.#");
        String cellValue = "";
        if (cell == null) {
            return cellValue;
        } else if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
            cellValue = cell.getStringCellValue();
        } else if (cell.getCellType() == XSSFCell.CELL_TYPE_NUMERIC) {
            if (HSSFDateUtil.isCellDateFormatted(cell)) {
                double d = cell.getNumericCellValue();
                Date date = HSSFDateUtil.getJavaDate(d);
                cellValue = sFormat.format(date);
            } else {
                cellValue = decimalFormat.format((cell.getNumericCellValue()));
            }
        } else if (cell.getCellType() == Cell.CELL_TYPE_BLANK) {
            cellValue = "";
        } else if (cell.getCellType() == Cell.CELL_TYPE_BOOLEAN) {
            cellValue = String.valueOf(cell.getBooleanCellValue());
        } else if (cell.getCellType() == Cell.CELL_TYPE_ERROR) {
            cellValue = "";
        } else if (cell.getCellType() == Cell.CELL_TYPE_FORMULA) {
            cellValue = cell.getCellFormula().toString();
        }
        return cellValue;
    }


    /**
     * 将实例集合导出为excel
     *
     * @param response http响应
     * @param fileName 下载文件名
     * @param records  数据集合
     * @param clazzList    目标类型
     * @param <T>
     */
    public <T> void exportRecords(HttpServletResponse response, String fileName, List<T> records, List<Class> clazzList, List<String> titleList) {
        HSSFWorkbook wb = new HSSFWorkbook();
        IntStream.range(0, clazzList.size()).forEach(cIndex ->
                exportSheet(records, wb, cIndex, clazzList.get(cIndex), titleList.get(cIndex))
        );
        exportExcel(response, fileName, wb);
    }

    public <T> Workbook exportSheet(List<T> records, Workbook wb, int sheetNum, Class tClass, String sheetTitle) {
        Sheet sheet = wb.createSheet();
        wb.setSheetName(sheetNum, sheetTitle);
        sheet.setDefaultColumnWidth(20);
        Drawing p = sheet.createDrawingPatriarch();

        Map<String, Field> titleField = generateFieldMap(tClass);
        List<String> titles = new ArrayList(titleField.keySet());
        titles.sort((a, b) -> {
            int aOrder = titleField.get(a).getAnnotation(ExcelCell.class).order();
            int bOrder = titleField.get(b).getAnnotation(ExcelCell.class).order();
            return aOrder < bOrder ? -1 : (aOrder > bOrder ? 1 : 0);
        });
        CellStyle titleStyleUnLocked = generateTitleStyle(wb, HSSFColor.LIGHT_BLUE.index, false);
        CellStyle titleStyleRequired = generateTitleStyle(wb, HSSFColor.RED.index, false);
        Row titleRow = sheet.createRow(TITLE_ROW_NUM);
        set100RowStyle(wb, sheet, titles.size());
        //设置下拉列表
        int count = 0;
        List<Field> fieldList = titleField.values().stream().collect(Collectors.toList());
        fieldList.sort(Comparator.comparingInt(m -> m.getAnnotation(ExcelCell.class).order()));
        for (Field field : fieldList) {
            ExcelCell ant = field.getAnnotation(ExcelCell.class);
            String[] dlData = ant.enums();
            if (dlData != null && dlData.length > 0) {
                sheet.addValidationData(setDataValidation(sheet, dlData, 1, 65535, count, count));
            }
            Comment comment = null;
            if (StringUtils.isNotBlank(ant.comment())) {
                //前四个参数是坐标点,后四个参数是编辑和显示批注时的大小.
                comment = p.createCellComment(new HSSFClientAnchor(0, 0, 0, count, (short) 4, 2, (short) 9, 7));
                //输入批注信息
                comment.setString(new HSSFRichTextString(ant.comment()));
            }
            Cell cell = titleRow.createCell(count);
            if (ant.notNull()) {
                cell.setCellStyle(titleStyleRequired);
            } else {
                cell.setCellStyle(titleStyleUnLocked);
            }
            setColumnValidate(wb, sheet, titleField.get(titles.get(count)));
            setValueToCell(cell, titleField.get(titles.get(count)), titles.get(count));
            cell.setCellComment(comment);
            count++;
        }
        //数据
        if (records != null && records.size() > 0) {
            IntStream.range(0, records.size()).forEach(index -> {
                T instance = records.get(index);
                Row row = sheet.createRow(index + DATA_START_ROW_NUM);
                IntStream.range(0, titles.size()).forEach(cIndex -> {
                    Cell cell = row.createCell(cIndex);
                    Field field = titleField.get(titles.get(cIndex));
                    try {
                        setValueToCell(cell, field, field.get(instance));
                    } catch (IllegalAccessException e) {
                        log.warn("excel 数据转换失败：" + e);
                        e.printStackTrace();
                    }
                });
            });
        }
        return wb;
    }

    /**
     *
     * 大批量导出
     *
     * @param response
     * @param fileName
     * @param records
     * @param clazzList
     * @param titleList
     * @param <T>
     */
    public <T> void exportBigRecords(HttpServletResponse response, String fileName, Map<String,List<T>> records, List<Class> clazzList, List<String> titleList) {
        Workbook wb = new SXSSFWorkbook();
        IntStream.range(0, clazzList.size()).forEach(cIndex ->
                exportBigSheet(records.get(clazzList.get(cIndex).getName()), wb, cIndex, clazzList.get(cIndex), titleList.get(cIndex))
        );
        exportExcel(response, fileName, wb);
    }

    public <T> Workbook exportBigSheet(List<T> records, Workbook wb, int sheetNum, Class tClass, String sheetTitle) {
        Sheet sheet = wb.createSheet();
        wb.setSheetName(sheetNum, sheetTitle);
        sheet.setDefaultColumnWidth(20);
        Map<String, Field> titleField = generateFieldMap(tClass);
        List<String> titles = new ArrayList(titleField.keySet());
        titles.sort((a, b) -> {
            int aOrder = titleField.get(a).getAnnotation(ExcelCell.class).order();
            int bOrder = titleField.get(b).getAnnotation(ExcelCell.class).order();
            return aOrder < bOrder ? -1 : (aOrder > bOrder ? 1 : 0);
        });
        CellStyle titleStyleUnLocked = generateTitleStyle(wb, HSSFColor.LIGHT_BLUE.index, false);
        CellStyle titleStyleRequired = generateTitleStyle(wb, HSSFColor.RED.index, false);
        Row titleRow = sheet.createRow(TITLE_ROW_NUM);
        //设置下拉列表
        int count = 0;
        List<Field> fieldList = titleField.values().stream().collect(Collectors.toList());
        fieldList.sort(Comparator.comparingInt(m -> m.getAnnotation(ExcelCell.class).order()));
        for (Field field : fieldList) {
            ExcelCell ant = field.getAnnotation(ExcelCell.class);
            Cell cell = titleRow.createCell(count);
            if (ant.notNull()) {
                cell.setCellStyle(titleStyleRequired);
            } else {
                cell.setCellStyle(titleStyleUnLocked);
            }
            setValueToCell(cell, field, titles.get(count));
            cell.setCellType(SXSSFCell.CELL_TYPE_STRING);
            count++;
        }
        //数据
        if (records != null && records.size() > 0) {
            IntStream.range(0, records.size()).forEach(index -> {
                T instance = records.get(index);
                Row row = sheet.createRow(index + DATA_START_ROW_NUM);
                IntStream.range(0, titles.size()).forEach(cIndex -> {
                    Cell cell = row.createCell(cIndex);
                    Field field = titleField.get(titles.get(cIndex));
                    try {
                        setValueToCell(cell, field, field.get(instance));
                    } catch (IllegalAccessException e) {
                        log.warn("excel 数据转换失败：" + e);
                    }
                });
            });
        }
        return wb;
    }

    /**
     *
     * 生成下拉列表
     *
     * @param sheet
     * @param textList
     * @param firstRow
     * @param endRow
     * @param firstCol
     * @param endCol
     * @return
     */
    private static DataValidation setDataValidation(Sheet sheet, String[] textList, int firstRow, int endRow, int firstCol, int endCol) {
        DataValidationHelper helper = sheet.getDataValidationHelper();
        //加载下拉列表内容
        DataValidationConstraint constraint = helper.createExplicitListConstraint(textList);
        constraint.setExplicitListValues(textList);
        //设置数据有效性加载在哪个单元格上。四个参数分别是：起始行、终止行、起始列、终止列
        CellRangeAddressList regions = new CellRangeAddressList((short) firstRow, (short) endRow, (short) firstCol, (short) endCol);
        //数据有效性对象
        DataValidation data_validation = helper.createValidation(constraint, regions);
        return data_validation;
    }

    /**
     * 将未通过校验的行导出为excel
     *
     * @param response
     * @param fileName
     * @param rows
     * @param clazzList
     */
    public static void exportInvalidRows(HttpServletResponse response, String fileName, Map<String,List<InvalidRow>> rows, List<Class> clazzList,List<String> sheetTitleList) {
        HSSFWorkbook wb = new HSSFWorkbook();
        IntStream.range(0, clazzList.size()).forEach( cIndex -> {
            HSSFSheet sheet = wb.createSheet();
            if(sheet!=null) {
                sheet.setDefaultColumnWidth(20);
                wb.setSheetName(cIndex, sheetTitleList.get(cIndex));
                Class clazz = clazzList.get(cIndex);
                exportInvalidRowsSheet(response, fileName, wb, sheet, rows.get(clazz.getName()), clazz);
            }
        });
        exportExcel(response, fileName, wb);
    }

    public static void exportInvalidRowsSheet(HttpServletResponse response, String fileName, HSSFWorkbook wb, Sheet sheet, List<InvalidRow> rows, Class clazz) {
        Map<String, Field> titleField = generateFieldMap(clazz);
        List<String> titles = new ArrayList(titleField.keySet());
        titles.sort((a, b) -> {
            int aOrder = titleField.get(a).getAnnotation(ExcelCell.class).order();
            int bOrder = titleField.get(b).getAnnotation(ExcelCell.class).order();
            return aOrder < bOrder ? -1 : (aOrder > bOrder ? 1 : 0);
        });
        CellStyle errorCellStyle = generateErrorCellStyle(wb);
        //表头
        CellStyle titleStyleLocked = generateTitleStyle(wb, HSSFColor.LIGHT_BLUE.index, true);
        CellStyle titleStyleUnLocked = generateTitleStyle(wb, HSSFColor.LIGHT_BLUE.index, false);
        Row titleRow = sheet.createRow(TITLE_ROW_NUM);
        set100RowStyle(wb, sheet, titles.size());
        int tempCount = 0;
        IntStream.range(0, titles.size()).forEach(cIndex -> {
            Cell cell = titleRow.createCell(cIndex);
            ExcelCell ant = titleField.get(titles.get(tempCount)).getAnnotation(ExcelCell.class);
            if (ant.enums() != null && ant.enums().length > 0) {
                cell.setCellStyle(titleStyleLocked);
            } else {
                cell.setCellStyle(titleStyleUnLocked);
            }
            setColumnValidate(wb, sheet, titleField.get(titles.get(cIndex)));
            setValueToCell(cell, titleField.get(titles.get(cIndex)), titles.get(cIndex));
        });
        Cell titleErrorCell = titleRow.createCell(titles.size());
        titleErrorCell.setCellValue("错误信息");
        titleErrorCell.setCellStyle(generateTitleStyle(wb, HSSFColor.LIGHT_ORANGE.index, false));
        //数据
        if (rows != null && rows.size() > 0) {
            CellStyle textCellStyle = generateTextCellStyle(wb);
            IntStream.range(0, rows.size()).forEach(index -> {
                InvalidRow invalidRowPack = rows.get(index);
                Row invalidRow = invalidRowPack.getRow();
                Map<String, String> errMsg = invalidRowPack.getErrMsg();
                Row row = sheet.createRow(index + DATA_START_ROW_NUM);
                IntStream.range(0, titles.size()).forEach(cIndex -> {
                    Cell cell = row.createCell(cIndex);
                    if (invalidRow.getCell(cIndex)!=null) {
                        String value = invalidRow.getCell(cIndex).getStringCellValue();
                        invalidRow.getCell(cIndex).setCellType(Cell.CELL_TYPE_STRING);
                        cell.setCellValue(value);
                        if (errMsg.containsKey(titles.get(cIndex))) {
                            cell.setCellStyle(errorCellStyle);
                        } else {
                            cell.setCellStyle(textCellStyle);
                        }
                    }
                });
                Cell errorMsgCell = row.createCell(titles.size());
                errorMsgCell.setCellValue(new HSSFRichTextString(invalidRowPack.getFormatErrorMsg()));
            });
        }
    }

    /**
     * 导出excel
     *
     * @param response http响应
     * @param fileName 导出文件名
     * @param wb       excel
     */
    private static void exportExcel(HttpServletResponse response, String fileName, Workbook wb) {
        try {
            response.setHeader("content-Type", "application/vnd.ms-excel");
            response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileName, "utf-8"));
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            wb.write(os);
            byte[] bytes = os.toByteArray();
            ByteArrayInputStream in = new ByteArrayInputStream(bytes);
            response.setHeader("Content-Length", String.valueOf(in.available()));
            response.getOutputStream().write(bytes);
            response.getOutputStream().flush();
            response.getOutputStream().close();
        } catch (Exception e) {
            log.warn("excel导出失败：" + e);
            e.printStackTrace();
        }
    }


    /**
     * 将字符串型值根据属性类型转换后填充
     *
     * @param instance 实例
     * @param field    属性
     * @param value    值
     */
    private void setValueToField(Object instance, Field field, String value, Map<String, String> errMsg,Integer orgType) {
        Class fclazz = field.getType();
        ExcelCell ant = field.getAnnotation(ExcelCell.class);
        String pattern = ant.pattern();
        Object formatValue = null;
        String title = ant.title();
        if (value == null) {
            value = "";
        }
        value = value.trim();
        if (ant.notNull() && StringUtils.isBlank(value)) {
            //非空校验
            errMsg.put(title, "不能为空");
        }
        //正则校验
        if (ant.notNull() && StringUtils.isNotBlank(pattern)) {
            if ("longitude".equals(field.getName())) {
                if (!PatternUtil.PATTERN_LONGITUDE.matcher(value).matches()) {
                    errMsg.put(title, "格式不合法：" + pattern);
                }
            } else if ("latitude".equals(field.getName())) {
                if (!PatternUtil.PATTERN_LATITUDE.matcher(value).matches()) {
                    errMsg.put(title, "格式不合法：" + pattern);
                }
            } else if ("angle".equals(field.getName())) {
                if (!PatternUtil.PATTERN_LATITUDE.matcher(value).matches()) {
                    errMsg.put(title, "格式不合法：" + pattern);
                }
            } else if ("angle".equals(field.getName())) {
                if (!PatternUtil.PATTERN_ANGLE.matcher(value).matches()) {
                    errMsg.put(title, "格式不合法：" + pattern);
                }
            } else if ("maxSpeed".equals(field.getName()) || "minSpeed".equals(field.getName())) {
                if (!PatternUtil.PATTERN_SPEED.matcher(value).matches()) {
                    errMsg.put(title, "格式不合法：" + pattern);
                }
            } else {
                Pattern patternTemp = Pattern.compile(pattern);
                if (!patternTemp.matcher(value).matches()) {
                    errMsg.put(title, "格式不合法：" + pattern);
                }
            }
        }
        //长度校验
        int length = ant.length();
        if (StringUtils.isNotBlank(value) && length > 0) {
            if (value.length() > length) {
                errMsg.put(title, "长度不合法：" + value);
            }
        }
        //边界值校验
        if (StringUtils.isNotBlank(value) && null != ant.enums() && ant.enums().length > 0) {
            String[] enums = ant.enums();
            if (!Arrays.asList(enums).contains(value)) {
                errMsg.put(title, "边界值不合法：" + value);
            }
        }
        //通过策略模式转换不同类型的值
        BaseDataTypeHandleCommand execute = context.getInstance(fclazz.getName());
        formatValue = execute.process(title, value, errMsg, ant.format());
        try {
            field.set(instance, formatValue);
        } catch (IllegalAccessException e) {
            errMsg.put(title, "格式不合法");
            e.printStackTrace();
        }
    }

    /**
     * 将值设置到单元格中
     *
     * @param cell  单元格
     * @param field 属性
     * @param value 值
     */
    private static void setValueToCell(Cell cell, Field field, Object value) {
        ExcelCell ant = field.getAnnotation(ExcelCell.class);
        String formatValue = null;
        if (value == null) {
            formatValue = "";
        }
        if (value instanceof String) {
            //字符串型
            formatValue = (String) value;
        } else if (value instanceof Integer) {
            //整型
            formatValue = String.valueOf(value);
        } else if (value instanceof Float) {
            //浮点
            formatValue = String.valueOf(value);
        } else if (value instanceof Double) {
            //双浮点
            formatValue = String.valueOf(value);
        } else if (value instanceof BigDecimal) {
            //大数
            formatValue = ((BigDecimal) value).toString();
        } else if (value instanceof Boolean) {
            //布尔
            formatValue = String.valueOf(value);
        } else if (value instanceof Timestamp) {
            //时间戳
            SimpleDateFormat sdf = new SimpleDateFormat(ant.format());
            formatValue = sdf.format(new Date(((Timestamp) value).getTime()));
        } else if (value instanceof Date) {
            //日期
            SimpleDateFormat sdf = new SimpleDateFormat(ant.format());
            formatValue = sdf.format((Date) value);
        }
        cell.setCellValue(formatValue);
    }


    /**
     * 获取【索引->属性列】对应关系
     *
     * @param row   excel表头行
     * @param clazz 目标类
     * @return 索引_字段映射表
     */
    private static Map<Integer, Field> generateColumns(Row row, Class clazz) {
        if (row == null) {
            return null;
        }
        Map<String, Field> nameField = generateFieldMap(clazz);
        Map<Integer, Field> indexField = Maps.newHashMap();
        IntStream.range(0, row.getLastCellNum()).forEach(index -> {
            Cell cell = row.getCell(index);
            if (cell == null) {
                return;
            }
            String title = cell.getStringCellValue();
            if (nameField.containsKey(title)) {
                indexField.put(index, nameField.get(title));
            }
        });
        return indexField;
    }

    /**
     * 构建【标题->属性列】映射表
     *
     * @return
     */
    private static Map<String, Field> generateFieldMap(Class clazz) {
        Map<String, Field> nameField = Maps.newHashMap();
        Arrays.stream(clazz.getDeclaredFields()).filter(f -> f.isAnnotationPresent(ExcelCell.class)).forEach(f -> {
            String colName = f.getAnnotation(ExcelCell.class).title();
            f.setAccessible(true);
            nameField.put(colName, f);
        });
        return nameField;
    }


    /**
     * 判断行为空
     *
     * @param row 数据行
     * @return
     */
    private static boolean isNullRow(Row row) {
        boolean isNull = true;
        if (null == row) {
            return isNull;
        }
        Iterator<Cell> cellItr = row.iterator();
        while (cellItr.hasNext()) {
            Cell c = cellItr.next();
            if (c.getCellType() != Cell.CELL_TYPE_BLANK) {
                isNull = false;
                break;
            }
        }
        return isNull;
    }

    /**
     * 生成表头样式
     *
     * @param wb excel对象
     * @return
     */
    private static CellStyle generateTitleStyle(Workbook wb, short color, boolean lockFlag) {
        Font headFont = wb.createFont();
        headFont.setBoldweight(Font.BOLDWEIGHT_BOLD);
        headFont.setFontName("宋体");
        headFont.setFontHeightInPoints((short) 11);
        headFont.setColor(HSSFColor.WHITE.index);
        CellStyle headStyle = wb.createCellStyle();
        headStyle.setFont(headFont);
        headStyle.setBorderTop((short) 1);
        headStyle.setBorderRight((short) 1);
        headStyle.setBorderBottom((short) 1);
        headStyle.setBorderLeft((short) 1);
        headStyle.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
        headStyle.setFillForegroundColor(color);
        headStyle.setAlignment(CellStyle.ALIGN_CENTER);
        if (lockFlag){
            headStyle.setLocked(true);
        }
        return headStyle;
    }

    /**
     * 生成未通过单元格校验样式
     *
     * @param wb excel对象
     * @return
     */
    private static CellStyle generateErrorCellStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        DataFormat format = wb.createDataFormat();
        style.setDataFormat(format.getFormat("@"));
        style.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
        style.setFillForegroundColor(HSSFColor.LIGHT_ORANGE.index);
        return style;
    }

    /**
     * 生成文本样式
     *
     * @return
     */
    private static CellStyle generateTextCellStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        DataFormat format = wb.createDataFormat();
        style.setDataFormat(format.getFormat("@"));
        return style;
    }


    /**
     * 设置前100行样式为文本型
     *
     * @param wb      excel
     * @param sheet   sheet页
     * @param colSize 总列数
     */
    private static void set100RowStyle(Workbook wb, Sheet sheet, int colSize) {
        CellStyle style = generateTextCellStyle(wb);
        int count = 100;
        for (int i = 1; i <= count; i++) {
            Row rowID = sheet.createRow(i);
            IntStream.range(0, colSize).forEach(cIndex -> {
                Cell cell = rowID.createCell(cIndex);
                cell.setCellStyle(style);
            });
        }
    }


    /**
     * 设置单元格验证规则
     *
     * @param wb    excel
     * @param sheet sheet页
     * @param field 属性列
     */
    private static void setColumnValidate(Workbook wb, Sheet sheet, Field field) {
        ExcelCell ant = field.getAnnotation(ExcelCell.class);
        int colIndex = ant.order();
        //枚举
        String[] enums = ant.enums();
        if (enums.length > 0) {
            DVConstraint constraint = DVConstraint.createExplicitListConstraint(enums);
            CellRangeAddressList regions = new CellRangeAddressList(DATA_START_ROW_NUM,
                    10000, colIndex, colIndex);
            HSSFDataValidation dataValidationList = new HSSFDataValidation(regions, constraint);
            sheet.addValidationData(dataValidationList);
        }
    }
}