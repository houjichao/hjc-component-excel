package com.hjc.component.model;

import com.hjc.component.annotation.ExcelCell;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author by hjc
 * @Classname DateUtil
 * @Description TODO
 * @Date 2019/7/9 21:36
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExcelDemo {

    @ExcelCell(title = "ID(必填)", order = 0, notNull = true, length = 36)
    private String id;

}