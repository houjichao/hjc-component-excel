package com.hjc.component.sax;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.BuiltinFormats;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.model.SharedStringsTable;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author hjc
 * @create 2018-01-18 14:28
 * @desc POI读取excel有两种模式，一种是用户模式，一种是事件驱动模式
 * 采用SAX事件驱动模式解决XLSX文件，可以有效解决用户模式内存溢出的问题，
 * 该模式是POI官方推荐的读取大数据的模式，
 * 在用户模式下，数据量较大，Sheet较多，或者是有很多无用的空行的情况下，容易出现内存溢出
 * <p>
 * 用于解决.xlsx2007版本大数据量问题
 **/
@Component
@Slf4j
public class ExcelXlsxReader extends DefaultHandler {

    @Autowired
    ExcelReaderUtil excelReaderUtil;

    /**
     * 单元格中的数据可能的数据类型
     */
    enum CellDataType {
        BOOL, ERROR, FORMULA, INLINESTR, SSTINDEX, NUMBER, DATE, NULL
    }

    /**
     * 共享字符串表
     */
    private SharedStringsTable sst;

    /**
     * 上一次的索引值
     */
    private String lastIndex;

    /**
     * 文件的绝对路径
     */
    private String filePath = "";

    /**
     * 工作表索引
     */
    private int sheetIndex = 0;

    /**
     * sheet名
     */
    private String sheetName = "";

    /**
     * 总行数
     */
    private int totalRows=0;

    /**
     * 一行内cell集合
     */
    private Map<Integer,String> cellList = Maps.newHashMap();

    /**
     * 判断整行是否为空行的标记
     */
    private boolean flag = false;

    /**
     * 当前行
     */
    private int curRow = 1;

    /**
     * 当前列
     */
    private int curCol = 0;

    /**
     * T元素标识
     */
    private boolean isTElement;

    /**
     * 异常信息，如果为空则表示没有异常
     */
    private String exceptionMessage;

    /**
     * 单元格数据类型，默认为字符串类型
     */
    private CellDataType nextDataType = CellDataType.SSTINDEX;

    private final DataFormatter formatter = new DataFormatter();

    /**
     * 单元格日期格式的索引
     */
    private short formatIndex;

    /**
     * 日期格式字符串
     */
    private String formatString;

    //上个有内容的单元格id，判断空单元格
    private String lastCellid;

    //两个不为空的单元格之间隔了多少个空的单元格
    private int countNullCell = 0;

    //定义前一个元素和当前元素的位置，用来计算其中空的单元格数量，如A6和A8等
    private String preRef = null, ref = null;

    //定义该文档一行最大的单元格数，用来补全一行最后可能缺失的单元格
    private String maxRef = null;

    /**
     * 单元格
     */
    private StylesTable stylesTable;

    /**
     * 是否跳过了单元格
     */
    private boolean isSkipCeil;

    Map<String, List<Object>> result = Maps.newHashMap();

    Map<String, List<InvalidRow>> unValidRows = Maps.newHashMap();

    List<Class> classList = Lists.newArrayList();

    /**
     * 遍历工作簿中所有的电子表格
     * 并缓存在mySheetList中
     *
     * @param inputStream
     * @throws Exception
     */
    public <T> ExcelConvertResult process(InputStream inputStream, List<Class> clazzList) {
        classList.addAll(clazzList);
        try {
            OPCPackage pkg = OPCPackage.open(inputStream);
            XSSFReader xssfReader = new XSSFReader(pkg);
            stylesTable = xssfReader.getStylesTable();
            SharedStringsTable sst = xssfReader.getSharedStringsTable();
            XMLReader parser = XMLReaderFactory.createXMLReader("org.apache.xerces.parsers.SAXParser");
            this.sst = sst;
            parser.setContentHandler(this);
            XSSFReader.SheetIterator sheets = (XSSFReader.SheetIterator) xssfReader.getSheetsData();
            while (sheets.hasNext()) { //遍历sheet
                curRow = 1; //标记初始行为第一行
                InputStream sheet = sheets.next(); //sheets.next()和sheets.getSheetName()不能换位置，否则sheetName报错
                sheetName = sheets.getSheetName();
                InputSource sheetSource = new InputSource(sheet);
                parser.parse(sheetSource); //解析excel的每条记录，在这个过程中startElement()、characters()、endElement()这三个函数会依次执行
                sheetIndex++;
                sheet.close();
            }
        } catch (Exception ex) {
            log.error("Excel parse exception:{}", ex);
        }
        classList.clear();
        sheetIndex = 0;
        return new ExcelConvertResult(result, unValidRows); //返回该excel文件的总行数，不包括首列和空行
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
     * 第一个执行
     *
     * @param uri
     * @param localName
     * @param name
     * @param attributes
     * @throws SAXException
     */
    @Override
    public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
        //c => 单元格
        if ("c".equals(name)) {
            //前一个单元格的位置
            if (preRef == null) {
                preRef = attributes.getValue("r");
            } else {
                preRef = ref;
            }
            //当前单元格的位置
            ref = attributes.getValue("r");
            cellList.put(covertRowIdtoInt(ref),null);
            //设定单元格类型
            this.setNextDataType(attributes);
        }

        //当元素为t时
        if ("t".equals(name)) {
            isTElement = true;
        } else {
            isTElement = false;
        }

        //置空
        lastIndex = "";
    }

    /**
     * 第二个执行
     * 得到单元格对应的索引值或是内容值
     * 如果单元格类型是字符串、INLINESTR、数字、日期，lastIndex则是索引值
     * 如果单元格类型是布尔值、错误、公式，lastIndex则是内容值
     * @param ch
     * @param start
     * @param length
     * @throws SAXException
     */
    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        lastIndex += new String(ch, start, length);
    }

    /**
     * 第三个执行
     *
     * @param uri
     * @param localName
     * @param name
     * @throws SAXException
     */
    @Override
    public void endElement(String uri, String localName, String name) throws SAXException {
        //t元素也包含字符串
        if (isTElement) {//这个程序没经过
            //将单元格内容加入rowlist中，在这之前先去掉字符串前后的空白符
            String value = lastIndex.trim();
            cellList.put(covertRowIdtoInt(ref), value);
            curCol++;
            isTElement = false;
            //如果里面某个单元格含有值，则标识该行不为空行
            if (value != null && !"".equals(value)) {
                flag = true;
            }
        } else if ("v".equals(name)) {
            //v => 单元格的值，如果单元格是字符串，则v标签的值为该字符串在SST中的索引
            String valueTemp = lastIndex.trim();
            String value = this.getDataValue(valueTemp, "");//根据索引值获取对应的单元格值
            cellList.put(covertRowIdtoInt(ref), value);
            curCol++;
            //如果里面某个单元格含有值，则标识该行不为空行
            if (value != null && !"".equals(value)) {
                flag = true;
            }
        } else {
            //如果标签名称为row，这说明已到行尾，调用optRows()方法
            if ("row".equals(name)) {
                //默认第一行为表头，以该行单元格数目为最大数目
                if (curRow == 1) {
                    maxRef = ref;
                }
                //补全一行尾部可能缺失的单元格
                if (maxRef != null) {
                    int len = countNullCell(maxRef, ref);
                    for (int i = 0; i <= len; i++) {
                        cellList.put(covertRowIdtoInt(ref), "");
                        curCol++;
                    }
                }

                if (flag && curRow != 1) { //该行不为空行且该行不是第一行，则发送（第一行为列名，不需要）
                    excelReaderUtil.sendRows(filePath, sheetName, sheetIndex, curRow, cellList, classList.get(sheetIndex), result, unValidRows);
                    totalRows++;
                }

                cellList.clear();
                curRow++;
                curCol = 0;
                preRef = null;
                ref = null;
                flag=false;
            }
        }
    }

    /**
     * 处理数据类型
     *
     * @param attributes
     */
    public void setNextDataType(Attributes attributes) {
        nextDataType = CellDataType.NUMBER; //cellType为空，则表示该单元格类型为数字
        formatIndex = -1;
        formatString = null;
        String cellType = attributes.getValue("t"); //单元格类型
        String cellStyleStr = attributes.getValue("s"); //
        String columnData = attributes.getValue("r"); //获取单元格的位置，如A1,B1

        if ("b".equals(cellType)) { //处理布尔值
            nextDataType = CellDataType.BOOL;
        } else if ("e".equals(cellType)) {  //处理错误
            nextDataType = CellDataType.ERROR;
        } else if ("inlineStr".equals(cellType)) {
            nextDataType = CellDataType.INLINESTR;
        } else if ("s".equals(cellType)) { //处理字符串
            nextDataType = CellDataType.SSTINDEX;
        } else if ("str".equals(cellType)) {
            nextDataType = CellDataType.FORMULA;
        }

        if (cellStyleStr != null) { //处理日期
            int styleIndex = Integer.parseInt(cellStyleStr);
            XSSFCellStyle style = stylesTable.getStyleAt(styleIndex);
            formatIndex = style.getDataFormat();
            formatString = style.getDataFormatString();

            if (formatString.contains("m/d/yy")) {
                nextDataType = CellDataType.DATE;
                formatString = "yyyy-MM-dd hh:mm:ss";
            }

            if (formatString == null) {
                nextDataType = CellDataType.NULL;
                formatString = BuiltinFormats.getBuiltinFormat(formatIndex);
            }
        }
    }

    /**
     * 对解析出来的数据进行类型处理
     * @param value   单元格的值，
     *                value代表解析：BOOL的为0或1， ERROR的为内容值，FORMULA的为内容值，INLINESTR的为索引值需转换为内容值，
     *                SSTINDEX的为索引值需转换为内容值， NUMBER为内容值，DATE为内容值
     * @param thisStr 一个空字符串
     * @return
     */
    @SuppressWarnings("deprecation")
    public String getDataValue(String value, String thisStr) {
        switch (nextDataType) {
            // 这几个的顺序不能随便交换，交换了很可能会导致数据错误
            case BOOL: //布尔值
                char first = value.charAt(0);
                thisStr = first == '0' ? "FALSE" : "TRUE";
                break;
            case ERROR: //错误
                thisStr = "\"ERROR:" + value.toString() + '"';
                break;
            case FORMULA: //公式
                thisStr = '"' + value.toString() + '"';
                break;
            case INLINESTR:
                XSSFRichTextString rtsi = new XSSFRichTextString(value.toString());
                thisStr = rtsi.toString();
                rtsi = null;
                break;
            case SSTINDEX: //字符串
                String sstIndex = value.toString();
                try {
                    int idx = Integer.parseInt(sstIndex);
                    XSSFRichTextString rtss = new XSSFRichTextString(sst.getEntryAt(idx));//根据idx索引值获取内容值
                    thisStr = rtss.toString();
                    rtss = null;
                } catch (NumberFormatException ex) {
                    thisStr = value.toString();
                }
                break;
            case NUMBER: //数字
                if (formatString != null) {
                    thisStr = formatter.formatRawCellContents(Double.parseDouble(value), formatIndex, formatString).trim();
                } else {
                    thisStr = value;
                }
                thisStr = thisStr.replace("_", "").trim();
                break;
            case DATE: //日期
                thisStr = formatter.formatRawCellContents(Double.parseDouble(value), formatIndex, formatString);
                // 对日期字符串作特殊处理，去掉T
                thisStr = thisStr.replace("T", " ");
                break;
            default:
                thisStr = " ";
                break;
        }
        return thisStr;
    }

    public int countNullCell(String ref, String preRef) {
        //excel2007最大行数是1048576，最大列数是16384，最后一列列名是XFD
        String xfd = ref.replaceAll("\\d+", "");
        String xfd_1 = preRef.replaceAll("\\d+", "");

        xfd = fillChar(xfd, 3, '@', true);
        xfd_1 = fillChar(xfd_1, 3, '@', true);

        char[] letter = xfd.toCharArray();
        char[] letter_1 = xfd_1.toCharArray();
        int res = (letter[0] - letter_1[0]) * 26 * 26 + (letter[1] - letter_1[1]) * 26 + (letter[2] - letter_1[2]);
        return res - 1;
    }

    public String fillChar(String str, int len, char let, boolean isPre) {
        int len_1 = str.length();
        if (len_1 < len) {
            if (isPre) {
                for (int i = 0; i < (len - len_1); i++) {
                    str = let + str;
                }
            } else {
                for (int i = 0; i < (len - len_1); i++) {
                    str = str + let;
                }
            }
        }
        return str;
    }

    /**
     * 列号转数字   AB7-->28 第28列
     *
     * @param rowId
     * @return
     */
    public static int covertRowIdtoInt(String rowId) {
        int firstDigit = -1;
        for (int c = 0; c < rowId.length(); ++c) {
            if (Character.isDigit(rowId.charAt(c))) {
                firstDigit = c;
                break;
            }
        }
        //AB7-->AB
        //AB是列号, 7是行号
        String newRowId = rowId.substring(0, firstDigit);
        int num = 0;
        int result = 0;
        int length = newRowId.length();
        for (int i = 0; i < length; i++) {
            //先取最低位，B
            char ch = newRowId.charAt(length - i - 1);
            //B表示的十进制2，ascii码相减，以A的ascii码为基准，A表示1，B表示2
            num = (int) (ch - 'A' + 1);
            //列号转换相当于26进制数转10进制
            num *= Math.pow(26, i);
            result += num;
        }
        return result - 1;

    }

    /**
     * @return the exceptionMessage
     */
    public String getExceptionMessage() {
        return exceptionMessage;
    }
}