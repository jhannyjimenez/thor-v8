package org.mitre.thor.math;

import org.ejml.simple.SimpleMatrix;

import java.text.DecimalFormat;

public class MatrixUtil {

    private static final DecimalFormat idDF = new DecimalFormat("0.000####");

    public static SimpleMatrix createTMatrixFromVectorData(double[] vectorData){

        double[][] matrixData = new double[vectorData.length][1];
        for(int i = 0; i < vectorData.length; i++){
            matrixData[i][0] = vectorData[i];
        }

        return new SimpleMatrix(matrixData);
    }

    public static double[] createVectorFromMatrixData(SimpleMatrix matrix){

        double[] vector = new double[matrix.numRows()];
        for(int i = 0; i < matrix.numRows(); i++){
            vector[i] = matrix.get(i, 0);
        }
        return vector;
    }

    public static void visualizeMatrixData(SimpleMatrix matrix, String matrixTitle){
        System.out.print("\n");
        System.out.println("----------------------------------------------------------------------------------------");
        System.out.println(matrixTitle);
        System.out.print("----------------------------------------------------------------------------------------");
        for(int r = 0; r < matrix.numRows(); r++){
            System.out.print("\n|");
            for(int c = 0; c < matrix.numCols(); c++){
                System.out.print(idDF.format(matrix.get(r, c)) + " ");
            }
            System.out.print("|");
        }
    }

    public static void writeMatrixToMathematica(SimpleMatrix matrix, int n,  String matrixName){
        System.out.println("(* " + matrixName + " - " + n + " Nodes *)") ;
        System.out.println(getMathematicaMatrixString(matrix, 0, matrix.numRows(), 0, matrix.numCols(), "m1"));
        System.out.println("MatrixForm[N[m1]]");
    }

    //TODO: Fix with change to getMathemetiacStringFunction
    public static void writeMatrixToMathematicaPairs(SimpleMatrix matrix, int n, String matrixName){

        int midX = matrix.numCols() - 1;
        int midY = matrix.numRows() - 1;
        int lastX = matrix.numCols();
        int lastY = matrix.numRows();

        System.out.println("(* " + matrixName + " - " + n + " Nodes *)") ;
        System.out.println(getMathematicaMatrixString(matrix, 0, 0, lastX, lastY, "m1"));

        System.out.println(getMathematicaMatrixString(matrix, 0, 0, n, n, "m2"));
        System.out.println(getMathematicaMatrixString(matrix, n, 0, midX, n, "m3"));
        System.out.println(getMathematicaMatrixString(matrix, midX, 0, lastX, n, "m4"));
        System.out.println(getMathematicaMatrixString(matrix, 0, n, n, midY, "m5"));
        System.out.println(getMathematicaMatrixString(matrix, n, n, midX, midY, "m6"));
        System.out.println(getMathematicaMatrixString(matrix, midX, n, lastX, midY, "m7"));
        System.out.println(getMathematicaMatrixString(matrix, 0, midY, n, lastY, "m8"));
        System.out.println(getMathematicaMatrixString(matrix, n, midY, midX, lastY, "m9"));
        System.out.println(getMathematicaMatrixString(matrix, midX, midY, lastX, lastY, "m10"));

        System.out.println("MatrixForm[N[m1]]");
        System.out.println("MatrixForm[N[m2]]");
        System.out.println("MatrixForm[N[m3]]");
        System.out.println("MatrixForm[N[m4]]");
        System.out.println("MatrixForm[N[m5]]");
        System.out.println("MatrixForm[N[m6]]");
        System.out.println("MatrixForm[N[m7]]");
        System.out.println("MatrixForm[N[m8]]");
        System.out.println("MatrixForm[N[m9]]");
        System.out.println("MatrixForm[N[m10]]");

    }

    public static void writeMatrixToMathematicaCombo(SimpleMatrix matrix, int n, int d, int c, String matrixName){

        int p = n + d;
        int t = p + c;
        int l = matrix.numCols();

        System.out.println("(* " + matrixName + " - " + n + " Nodes *)") ;
        System.out.println(getMathematicaMatrixString(matrix, 0, l, 0, l, "m1"));

        System.out.println(getMathematicaMatrixString(matrix, 0, n, 0, 3, "m2"));
        System.out.println(getMathematicaMatrixString(matrix, n, p, 0, 3, "m3"));
        System.out.println(getMathematicaMatrixString(matrix, p, t, 0, 3, "m4"));
        System.out.println(getMathematicaMatrixString(matrix, t, l, 0, 3, "m5"));

        System.out.println(getMathematicaMatrixString(matrix, 0, n, n, p, "m6"));
        System.out.println(getMathematicaMatrixString(matrix, n, p, n, p, "m7"));
        System.out.println(getMathematicaMatrixString(matrix, p, t, n, p, "m8"));
        System.out.println(getMathematicaMatrixString(matrix, t, l, n, p, "m9"));

        System.out.println(getMathematicaMatrixString(matrix, 0, n, p, t, "m10"));
        System.out.println(getMathematicaMatrixString(matrix, n, p, p, t, "m11"));
        System.out.println(getMathematicaMatrixString(matrix, p, t, p, t, "m12"));
        System.out.println(getMathematicaMatrixString(matrix, t, l, p, t, "m13"));

        System.out.println(getMathematicaMatrixString(matrix, 0, n, t, l, "m14"));
        System.out.println(getMathematicaMatrixString(matrix, n, p, t, l, "m15"));
        System.out.println(getMathematicaMatrixString(matrix, p, t, t, l, "m16"));
        System.out.println(getMathematicaMatrixString(matrix, t, l, t, l, "m17"));


        System.out.println("MatrixForm[N[m1]]");
        System.out.println("MatrixForm[N[m2]]");
        System.out.println("MatrixForm[N[m3]]");
        System.out.println("MatrixForm[N[m4]]");
        System.out.println("MatrixForm[N[m5]]");
        System.out.println("MatrixForm[N[m6]]");
        System.out.println("MatrixForm[N[m7]]");
        System.out.println("MatrixForm[N[m8]]");
        System.out.println("MatrixForm[N[m9]]");
        System.out.println("MatrixForm[N[m10]]");
        System.out.println("MatrixForm[N[m11]]");
        System.out.println("MatrixForm[N[m12]]");
        System.out.println("MatrixForm[N[m13]]");
        System.out.println("MatrixForm[N[m14]]");
        System.out.println("MatrixForm[N[m15]]");
        System.out.println("MatrixForm[N[m16]]");
        System.out.println("MatrixForm[N[m17]]");


    }

    private static String getMathematicaMatrixString(SimpleMatrix matrix, int rowS, int rowR, int colS, int colR, String name){
        StringBuilder sB = new StringBuilder();
        sB.append(name);
        sB.append(" = {");

        for(int row = rowS; row < rowR; row++){
            sB.append("{");
            for(int col = colS; col < colR; col++){
                sB.append(idDF.format(matrix.get(row, col)));
                sB.append(",");
            }
            sB.deleteCharAt(sB.length() - 1);
            sB.append("}, ");
        }
        sB.deleteCharAt(sB.length() - 1);
        sB.deleteCharAt(sB.length() - 1);
        sB.append("};");
        return sB.toString();
    }
}
