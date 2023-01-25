package org.mitre.input_correcter;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.mitre.thor.input.Input;
import org.mitre.thor.network.nodes.Activity;
import org.mitre.thor.network.nodes.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * Used to check for potential user-made errors specific to the Standard Form input
 */
public class StandardInputCorrecter extends InputCorrecter{

    public StandardInputCorrecter(Input input){
        super(input);
    }

    /**
     * Looks over the links sheet for missing information. Missing information includes: parent name, parent id, child name,
     * child id, links alpha value, and links beta value.
     *
     * @param searchForFDNA determines if the program looks for the 'linksAlpha' and 'linksBeta' fields
     */
    public void findLinksSheetErrors(boolean searchForFDNA){
        clear();
        HashMap<Node, Integer> firstIdColumns = new HashMap<>();
        HashMap<Node, Integer> firstIdRows = new HashMap<>();
        if(input.workbook != null){
            XSSFSheet linksSheet = input.workbook.getSheet("Links");
            List<Activity> activities = new ArrayList<>();
            if(linksSheet == null){
                String msg = "Could not find a sheet named 'Links'";
                throwError(msg);
            }else{
                int alphaColumnIndex = -5;
                int betaColumnIndex = -5;
                XSSFRow row0 = linksSheet.getRow(0);
                if(row0 != null){
                    for(int i = 0; i < row0.getLastCellNum(); i++){
                        XSSFCell cellI = row0.getCell(i);
                        if(cellI != null){
                            String cellS = cellI.getStringCellValue().toLowerCase(Locale.ROOT);
                            if(cellS.contains("sod") || cellS.contains("alpha")){
                                alphaColumnIndex = i;
                            }else if(cellS.contains("cod") || cellS.contains("beta")){
                                betaColumnIndex = i;
                            }
                        }
                    }
                }
                for(int i = 0; i < linksSheet.getLastRowNum(); i++){
                    XSSFRow row = linksSheet.getRow(i + 1);
                    if(row == null){
                        String msg = "The row " + (i + 2) + " in the links sheet is null";
                        throwError(msg);
                    }else{
                        XSSFCell columnBCell = row.getCell(1);
                        Activity parentActivity = null;
                        if(isCellEmpty(columnBCell)){
                            String msg = "The cell at " + integerToAlphabetic(1) + (i + 2) + " in the links sheet is empty - Parent Name Cell";
                            throwError(msg);
                        }else{
                            boolean nodeAlreadyExists = false;
                            for(Activity activity : activities){
                                if(activity.name.equals(columnBCell.toString())){
                                    parentActivity = activity;
                                    nodeAlreadyExists = true;
                                    break;
                                }
                            }
                            if(!nodeAlreadyExists){
                                Activity activity = new Activity(columnBCell.toString());
                                parentActivity = activity;
                                activities.add(activity);
                            }
                        }

                        XSSFCell columnCCell = row.getCell(2);
                        if(isCellEmpty(columnCCell)){
                            String msg = "The cell at " + integerToAlphabetic(2) + (i + 2) + " in the links sheet is empty - Parent ID Cell";
                            throwError(msg);
                        }else if(columnCCell.getCellType() != CellType.NUMERIC){
                            String msg = "The cell at " + integerToAlphabetic(2) + (i + 2) + " in the links sheet is not a numeric cell - Parent ID Cell";
                            throwError(msg);
                        }else{
                            double id = Double.NaN;
                            try{
                                id = columnCCell.getNumericCellValue();
                            }catch (Exception e){
                                e.printStackTrace();
                            }
                            if(Double.isNaN(id)){
                                String msg = "Could not get the id at " + integerToAlphabetic(2) + (i + 2) + " in the links sheet";
                                throwError(msg);
                            }else{
                                if(id < 0){
                                    String msg = "The id at " + integerToAlphabetic(2) + (i + 2) + " in the links sheet is less than 0";
                                    throwError(msg);
                                }

                                if(parentActivity != null){
                                    for(Activity activity : activities){
                                        if(activity != parentActivity && activity.id == id){
                                            String msg = "The id: " + (int) id + " is used more than once";
                                            String msg2 = "\t id: " + (int) id + " is used in the links sheet by node " + parentActivity.name + " at " + integerToAlphabetic(2) + (i + 2);
                                            String msg3 = "\t id: " + (int) id + " is used in the links sheet by node " + activity.name + " at " + integerToAlphabetic(firstIdColumns.get(activity)) + firstIdRows.get(activity);

                                            throwError(msg);
                                            throwError(msg2);
                                            throwError(msg3);
                                        }
                                    }

                                    if(parentActivity.id == -1){
                                        parentActivity.id = (int) id;
                                        firstIdColumns.put(parentActivity, 2);
                                        firstIdRows.put(parentActivity, i + 2);
                                    }else if(id != parentActivity.id){
                                        String msg = parentActivity.name + " has two different IDs";
                                        String msg2 = "\t id: " + (int) parentActivity.id + " is found in the links sheet at " + integerToAlphabetic(firstIdColumns.get(parentActivity)) + firstIdRows.get(parentActivity);
                                        String msg3 = "\t id: " + (int) id + " is found in the links sheet at " + integerToAlphabetic(2) + (i + 2);

                                        throwError(msg);
                                        throwError(msg2);
                                        throwError(msg3);
                                    }
                                }
                            }
                        }

                        XSSFCell columnDCell = row.getCell(3);
                        Activity childActivity = null;
                        if(isCellEmpty(columnDCell)){
                            String msg = "The cell at " + integerToAlphabetic(3) + (i + 2) + " in the links sheet is empty - Child Name Cell";
                            throwError(msg);
                        }else{
                            boolean nodeAlreadyExists = false;
                            for(Activity activity : activities){
                                if(activity.name.equals(columnDCell.toString())){
                                    childActivity = activity;
                                    nodeAlreadyExists = true;
                                    break;
                                }
                            }
                            if(!nodeAlreadyExists){
                                Activity activity = new Activity(columnDCell.toString());
                                childActivity = activity;
                                activities.add(activity);
                            }
                        }

                        XSSFCell columnECell = row.getCell(4);
                        if(isCellEmpty(columnECell)){
                            String msg = "The cell at " + integerToAlphabetic(4) + (i + 2) + " in the links sheet is empty - Child ID Cell";
                            throwError(msg);
                        }else if(columnECell.getCellType() != CellType.NUMERIC){
                            String msg = "The cell at " + integerToAlphabetic(4) + (i + 2) + " in the links sheet is not a numeric cell - Child ID Cell";
                            throwError(msg);
                        }else{
                            double id = Double.NaN;
                            try{
                                id = columnECell.getNumericCellValue();
                            }catch (Exception e){
                                e.printStackTrace();
                            }
                            if(Double.isNaN(id)){
                                String msg = "Could not get the id at " + integerToAlphabetic(4) + (i + 2) + " in the links sheet";
                                throwError(msg);
                            }else {
                                if(id < 0){
                                    String msg = "The id at " + integerToAlphabetic(4) + (i + 2) + " in the links sheet is less than 0";
                                    throwError(msg);
                                }

                                if(childActivity != null){
                                    if(childActivity.id == -1){
                                        childActivity.id = (int) id;
                                        firstIdColumns.put(childActivity, 4);
                                        firstIdRows.put(childActivity, i + 2);
                                    }else if(id != childActivity.id){
                                        String msg = childActivity.name + " has two different IDs";
                                        String msg2 = "\t id: " + (int) childActivity.id + " is found in the links sheet at " + integerToAlphabetic(firstIdColumns.get(childActivity)) + firstIdRows.get(childActivity);
                                        String msg3 = "\t id: " + (int) id + " is found in the links sheet at " + integerToAlphabetic(4) + (i + 2);

                                        throwError(msg);
                                        throwError(msg2);
                                        throwError(msg3);
                                    }
                                }
                            }
                        }

                        if(searchForFDNA){
                            if(alphaColumnIndex == -5){
                                String msg = "Could not find a column in the Links sheet with a header cell containing the value 'Alpha'";
                                throwError(msg);
                            }else{
                                XSSFCell columnFCell = row.getCell(alphaColumnIndex);
                                if(isCellEmpty(columnFCell)){
                                    String msg = "The cell at " + integerToAlphabetic(alphaColumnIndex) + (i + 2) + " in the links sheet is empty - Link Alpha Cell";
                                    throwError(msg);
                                }else if(columnFCell.getCellType() != CellType.NUMERIC){
                                    String msg = "The cell at " + integerToAlphabetic(alphaColumnIndex) + (i + 2) + " in the links sheet is not a numeric cell - Link Alpha Cell";
                                    throwError(msg);
                                }
                            }
                            if(betaColumnIndex == -5){
                                String msg = "Could not find a column in the Links sheet with a header cell containing the value 'Beta'";
                                throwError(msg);
                            }else{
                                XSSFCell columnGCell = row.getCell(betaColumnIndex);
                                if(isCellEmpty(columnGCell)){
                                    String msg = "The cell at " + integerToAlphabetic(betaColumnIndex) + (i + 2) + " in the links sheet is empty - Link Beta Cell";
                                    throwError(msg);
                                }else if(columnGCell.getCellType() != CellType.NUMERIC){
                                    String msg = "The cell at " + integerToAlphabetic(betaColumnIndex) + (i + 2) + " in the links sheet is not a numeric cell - Link Beta Cell";
                                    throwError(msg);
                                }
                            }
                        }
                    }
                }
            }
        }else{
            String msg = "Could not load the file at " + input.iConfig.filePath;
            throwError(msg);
        }
    }
}
