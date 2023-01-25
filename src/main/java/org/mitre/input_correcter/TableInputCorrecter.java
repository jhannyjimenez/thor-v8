package org.mitre.input_correcter;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.mitre.thor.input.Input;

import java.util.ArrayList;
import java.util.List;

/**
 * Used to check for potential user-made errors specific to the Table form input
 */
public class TableInputCorrecter extends InputCorrecter{

    public TableInputCorrecter(Input input){
        super(input);
    }

    /**
     * Looks over the phi sheet for any missing information. Missing information includes: missing node names, blank or
     * non-numeric cells, and missing phi values.
     *
     * @return a list of errors found
     */
    public List<String> findPhiErrors(){
        clear();

        List<String> errors = new ArrayList<>();
        XSSFSheet phiSheet = input.workbook.getSheet("Phi");

        //region getNodes
        int phiColumnIndex = -1;
        if(phiSheet != null){
            XSSFRow tableHeaderRow = phiSheet.getRow(0);
            if(tableHeaderRow != null){
                for(int i = 0; i < tableHeaderRow.getLastCellNum(); i++){
                    XSSFCell nameCell = tableHeaderRow.getCell(i);
                    if(nameCell != null && nameCell.toString().equalsIgnoreCase("phi")){
                        phiColumnIndex = i;
                    }else if(nameCell == null){
                        String msg = "The cell at " + (integerToAlphabetic(i) + 1) + " in the 'Phi' sheet is blank";
                        throwError(msg);
                    }
                }
            }else{
                String msg = "Row " + (1) + " in the 'Phi' sheet is null. It should contain the names of the nodes";
                throwError(msg);
            }
        }

        if(phiColumnIndex == -1){
            String msg = "Could not find a column with the header 'phi' in the table located in the 'Phi' sheet";
            throwError(msg);
        }
        //endregion

        //region getPhis
        if(phiSheet != null){
            for(int i = 1; i < phiSheet.getLastRowNum(); i++){
                XSSFRow row = phiSheet.getRow(i);
                if(row != null){
                    for(int a = 0; a < row.getLastCellNum(); a++){
                        XSSFCell cellStatus = row.getCell(a);
                        if(isCellEmpty(cellStatus)){
                            String msg = "The cell at " + (integerToAlphabetic(a) + (i + 1)) + " in the 'Phi' sheet is blank";
                            throwError(msg);
                        }else if(cellStatus.getCellType() != CellType.NUMERIC){
                            String msg = "The cell at " + (integerToAlphabetic(a) + (i + 1)) + " in the 'Phi' sheet is not numeric";
                            throwError(msg);
                        }else if(a != phiColumnIndex && (cellStatus.getNumericCellValue() != 0 && cellStatus.getNumericCellValue() != 1)){
                            String msg = "The cell at " + (integerToAlphabetic(a) + (i + 1)) + " in the 'Phi' sheet should have a value of 1 or 0";
                            throwError(msg);
                        }
                    }
                }else{
                    String msg = "The row " + (i + 1) + " in the 'Phi' sheet is null";
                    throwError(msg);
                }
            }
        }
        //endregion
        return errors;
    }
}
