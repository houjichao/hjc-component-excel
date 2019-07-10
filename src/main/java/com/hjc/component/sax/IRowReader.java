package com.hjc.component.sax;

import java.util.List;

public interface IRowReader {

    /**业务逻辑实现方法
	 * @param sheetIndex
	 * @param curRow
	 * @param rowList
	 */
	void getRows(int sheetIndex, int curRow, List<String> rowList);
}