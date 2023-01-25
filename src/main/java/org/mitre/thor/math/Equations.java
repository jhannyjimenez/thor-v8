package org.mitre.thor.math;

import org.ejml.simple.SimpleMatrix;
import org.mitre.thor.network.Network;
import org.mitre.thor.network.nodes.Activity;
import org.mitre.thor.network.nodes.Group;
import org.mitre.thor.network.nodes.Node;

import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.ArrayList;

public class Equations {

    //TODO make possibilities divisible by two
    public static SimpleMatrix getMatrix1Answers(Network network, ArrayList<Node> nodes, int rollUpIndex, int trial){

        ArrayList<Node> allNodes = new ArrayList<>(nodes);
        Node global_node = network.getNode(888888);
        if(global_node != null){
            allNodes.add(global_node);
        }

        int N = allNodes.size();
        double possibilitiesCalculated = network.analysisDataHolders.get(rollUpIndex).networkCriticalityTrials[trial].possibilitiesCount.doubleValue();

        double[][] matrixData = new double[N][N];
        for(int n = 0; n < N; n++){
            for(int m = 0; m < N; m++){
                if(m == n){
                    matrixData[n][m] = (Math.pow(allNodes.get(n).A + allNodes.get(n).B, 2)) + (Math.pow(allNodes.get(n).B, 2));
                }else{
                    matrixData[n][m] = (allNodes.get(n).A + 2 * allNodes.get(n).B) * ((allNodes.get(m).A/2) + allNodes.get(m).B);
                }
            }
        }

        double[] capitalPhis = new double[N];
        for(int n = 0; n < N; n++){
            double capitalPhi = ((allNodes.get(n).A + allNodes.get(n).B) * allNodes.get(n).analysisDataHolders.get(rollUpIndex).nodeCriticalityTrials[trial].sumOfGoalWhenON + allNodes.get(n).B * allNodes.get(n).analysisDataHolders.get(rollUpIndex).nodeCriticalityTrials[trial].sumOfGoalWhenOFF) / (possibilitiesCalculated / Math.pow(2, allNodes.get(n).analysisDataHolders.get(rollUpIndex).nodeCriticalityTrials[trial].divisionCount + 1));
            capitalPhis[n] = capitalPhi;
        }

        SimpleMatrix matrixA = new SimpleMatrix(matrixData);
        SimpleMatrix capitalPhisTransposeMatrix = MatrixUtil.createTMatrixFromVectorData(capitalPhis);
        SimpleMatrix matrixAInverse = matrixA.invert();

        return matrixAInverse.mult(capitalPhisTransposeMatrix);
    }

    public static SimpleMatrix getMatrix2Answers(Network network, ArrayList<Node> nodes, int rollUpIndex, int trial){

        ArrayList<Node> allNodes = new ArrayList<>(nodes);
        Node global_node = network.getNode(888888);
        if(global_node != null){
            allNodes.add(global_node);
        }

        int N = allNodes.size();
        double possibilitiesCalculated = network.analysisDataHolders.get(rollUpIndex).networkCriticalityTrials[trial].possibilitiesCount.doubleValue();
        double phisSum = network.analysisDataHolders.get(rollUpIndex).networkCriticalityTrials[trial].phiSum;
        double phiSumDivisionCounts = network.analysisDataHolders.get(rollUpIndex).networkCriticalityTrials[trial].phiSumDivisionCounts;

        double[][] matrixCData = new double[(N + 1)][(N + 1)];
        for (int n = 0; n < (N + 1); n++){
            for(int m = 0; m < (N + 1); m++){
                int min = Math.min(n, m);

                if (n < N && m < N && n != m){
                    matrixCData[n][m] = possibilitiesCalculated * Math.pow((allNodes.get(n).A + 2 * allNodes.get(n).B), 2) / 2;
                }else if(m < N && m == n){
                    matrixCData[n][m] = possibilitiesCalculated * (Math.pow((allNodes.get(n).A + allNodes.get(n).B), 2) + Math.pow(allNodes.get(n).B, 2));
                }else if((m == N && n != m) || (n == N && n != m)){
                    matrixCData[n][m] = possibilitiesCalculated * (allNodes.get(min).A + 2 * allNodes.get(min).B);
                }else if(n == N){
                    matrixCData[n][m] = 2 * possibilitiesCalculated;
                }
            }
        }

        double[] capitalPhis = new double[(N + 1)];
        for(int n = 0; n < (N + 1); n++){
            if(n < N){
                capitalPhis[n] = (2 * (allNodes.get(n).A + allNodes.get(n).B) * allNodes.get(n).analysisDataHolders.get(rollUpIndex).nodeCriticalityTrials[trial].sumOfGoalWhenON + 2 * allNodes.get(n).B * allNodes.get(n).analysisDataHolders.get(rollUpIndex).nodeCriticalityTrials[trial].sumOfGoalWhenOFF) * Math.pow(2, allNodes.get(n).analysisDataHolders.get(rollUpIndex).nodeCriticalityTrials[trial].divisionCount);
            }else if(n == N){
                capitalPhis[n] = (2 * phisSum) * Math.pow(2, phiSumDivisionCounts);

            }
        }

        SimpleMatrix matrixC = new SimpleMatrix(matrixCData);
        SimpleMatrix matrixCInverse = matrixC.invert();
        SimpleMatrix matrixCapitalPhis = MatrixUtil.createTMatrixFromVectorData(capitalPhis);

        return matrixCInverse.mult(matrixCapitalPhis);
    }

    public static SimpleMatrix getCase(Network network, ArrayList<Node> nodes, int caseNumber, int rollUpIndex, int trial) {

        ArrayList<Node> allNodes = new ArrayList<>(nodes);
        Node global_node = network.getNode(888888);
        if(global_node != null){
            allNodes.add(global_node);
        }

        int N = allNodes.size();
        double possibilitiesCalculated = network.analysisDataHolders.get(rollUpIndex).networkCriticalityTrials[trial].possibilitiesCount.doubleValue();

        if(caseNumber == 1){
            double[] answers = new double[N];
            for(int i = 0; i < N; i++){
                double answer;
                //answer = ((allNodes.get(i).analysisDataHolders.get(rollUpIndex).nodeCriticalityTrials[trial].sumOfGoalWhenON / (possibilitiesCalculated / Math.pow(2, allNodes.get(i).analysisDataHolders.get(rollUpIndex).nodeCriticalityTrials[trial].divisionCount + 1))) - (allNodes.get(i).analysisDataHolders.get(rollUpIndex).nodeCriticalityTrials[trial].sumOfGoalWhenOFF / (possibilitiesCalculated / Math.pow(2, allNodes.get(i).analysisDataHolders.get(rollUpIndex).nodeCriticalityTrials[trial].divisionCount + 1))));
                answer = ((allNodes.get(i).analysisDataHolders.get(rollUpIndex).nodeCriticalityTrials[trial].sumOfGoalWhenON / allNodes.get(i).analysisDataHolders.get(rollUpIndex).nodeCriticalityTrials[trial].onCount) - (allNodes.get(i).analysisDataHolders.get(rollUpIndex).nodeCriticalityTrials[trial].sumOfGoalWhenOFF / allNodes.get(i).analysisDataHolders.get(rollUpIndex).nodeCriticalityTrials[trial].onCount));
                answers[i] = answer;
            }
            return MatrixUtil.createTMatrixFromVectorData(answers);
        }

        if(caseNumber == 2){

            double positiveSum = 0;
            for (Node node : allNodes) {
                positiveSum += node.analysisDataHolders.get(rollUpIndex).nodeCriticalityTrials[trial].sumOfGoalWhenON / (possibilitiesCalculated / Math.pow(2, node.analysisDataHolders.get(rollUpIndex).nodeCriticalityTrials[trial].divisionCount + 1));
            }

            double positiveCountAverage = positiveSum * (2.0/(N + 1));

            double[] answers = new double[N];
            for(int i = 0; i < N; i++){
                double answer = ((2 * allNodes.get(i).analysisDataHolders.get(rollUpIndex).nodeCriticalityTrials[trial].sumOfGoalWhenON / (possibilitiesCalculated / Math.pow(2, allNodes.get(i).analysisDataHolders.get(rollUpIndex).nodeCriticalityTrials[trial].divisionCount + 1))) - positiveCountAverage);
                answers[i] = answer;
            }
            return MatrixUtil.createTMatrixFromVectorData(answers);
        }

        if(caseNumber == 3){

            double negativeSum = 0;
            for (Node node : allNodes) {
                negativeSum += node.analysisDataHolders.get(rollUpIndex).nodeCriticalityTrials[trial].sumOfGoalWhenOFF / (possibilitiesCalculated / Math.pow(2, node.analysisDataHolders.get(rollUpIndex).nodeCriticalityTrials[trial].divisionCount + 1));
            }

            double negativeCountsAverage = negativeSum * (2.0/(N + 1));

            double[] answers = new double[N];
            for(int i = 0; i < N; i++){
                double answer = (negativeCountsAverage - (2 * allNodes.get(i).analysisDataHolders.get(rollUpIndex).nodeCriticalityTrials[trial].sumOfGoalWhenOFF / (possibilitiesCalculated / Math.pow(2, allNodes.get(i).analysisDataHolders.get(rollUpIndex).nodeCriticalityTrials[trial].divisionCount + 1))));
                answers[i] = answer;
            }
            return MatrixUtil.createTMatrixFromVectorData(answers);
        }

        return new SimpleMatrix(new double[N][N]);
    }



    //Pairs using Xor Matrix
    public static SimpleMatrix pairsOrTriplesUsingXOR(Network network, ArrayList<Node> nodes, int rollUpIndex, int trial){
        int N = nodes.size();
        int s = 0;
        for(Node node : nodes){
            if(node.getClass().equals(Activity.class)){
                s++;
            }
        }

        int P = network.analysisDataHolders.get(rollUpIndex).possibilitiesTable.size();
        int E = network.analysisDataHolders.get(rollUpIndex).endResults.size();
        double[][] matrixAData = new double[P][N + 1];
        for(int p = 0; p < P; p++){
            for(int n = 0; n < N; n++){
                matrixAData[p][n] = network.analysisDataHolders.get(rollUpIndex).possibilitiesTable.get(p).get(n);
            }
            matrixAData[p][N] = 1;
        }
        double[] phis = new double[E];
        for(int e = 0; e < E; e++){
            phis[e] = network.analysisDataHolders.get(rollUpIndex).endResults.get(e);
        }
        SimpleMatrix XOR = new SimpleMatrix(matrixAData);
        SimpleMatrix XORTranspose = XOR.transpose();
        SimpleMatrix b = MatrixUtil.createTMatrixFromVectorData(phis);


        SimpleMatrix alpha = XORTranspose.mult(XOR);
        alpha = alpha.invert();
        alpha = alpha.mult(XORTranspose);

        MatrixUtil.writeMatrixToMathematica(alpha, s, " ((XOR * XOR_T)^-1 * XOR_T) ");
        System.out.println("Pause");

        alpha = alpha.mult(b);

        SimpleMatrix error = XOR.mult(alpha);
        error = error.minus(b);

        double average = 0.0;
        for(int i = 0; i < error.numRows(); i++){
            average += Math.abs(error.get(i, 0));
        }
        average = average / error.numRows();

        System.out.println("Average of Error: " + average);

        return alpha;
    }

    //Pairs using Sum of L row in Xor Matrix
    /*
    public static SimpleMatrix pairsOrTripleUsingMatrixSum(Network network, ArrayList<Node> nodes, int rollUpIndex, int trial){
        double M = network.analysisDataHolders.get(rollUpIndex).networkCriticalityTrials[trial].possibilitiesCount.doubleValue();
        int N = 0;
        int P = 0;
        int T = 0;
        for(Node node : nodes){
            if(node.getClass().equals(Activity.class)){
                N++;
            }else if(node.getClass().equals(Group.class)){
                Group g = (Group) node;
                if(g.subTag.equals("pair")){
                    P++;
                }else if(g.subTag.equals("triple")){
                    T++;
                }
            }
        }


        SimpleMatrix E = network.analysisDataHolders.get(rollUpIndex).networkCriticalityTrials[trial].E;


        ArrayList<Node> allNodes = new ArrayList<>(nodes);
        Node global_node = network.getNode(8888);
        if(global_node != null){
            allNodes.add(global_node);
        }
        double[][] eData = new double[allNodes.size()][1];
        for(int row = 0; row < allNodes.size(); row++){
            eData[row][0] = allNodes.get(row).analysisDataHolders.get(rollUpIndex).nodeCriticalityTrials[trial].sumOfGoalWhenON;
        }

        SimpleMatrix e = new SimpleMatrix(eData);
        SimpleMatrix EInverse = E.invert();
        SimpleMatrix result = EInverse.mult(e);

        SimpleMatrix coreE = E.divide(M);
        SimpleMatrix coreEInverse = coreE.invert();

        //MatrixUtil.visualizeMatrixData(coreE, "E Core");
        //MatrixUtil.visualizeMatrixData(coreEInverse, "E Core Inverse");

        System.out.println();

        //MatrixUtil.writeMatrixToMathematicaPairs(coreE, N, "E/M");
        //System.out.println();

        System.out.println("N = " + N +";");
        System.out.println("M = " + M + ";");
        //MatrixUtil.writeMatrixToMathematicaCombo(EInverse, N,P,T, "E Inverse");
        //MatrixUtil.writeMatrixToMathematicaCombo(coreEInverse, N,P,T, "Core E Inverse");

        verifyPairsEInverseMatrix(EInverse, nodes, M, N);

        System.out.println("Done Verifications");

        System.out.println("Done");

        return result;
    }
     */

    //Triples and Pairs Combo matrix using the explicit EInverse Equation
    public static SimpleMatrix comboExplicit(Network network, ArrayList<Node> nodes, int rollUpIndex, int trial){

        double M = network.analysisDataHolders.get(rollUpIndex).networkCriticalityTrials[trial].possibilitiesCount.doubleValue();
        int N = 0;
        for(Node node : nodes){
            if(node.getClass().equals(Activity.class)){
                N++;
            }
        }

        double[][] EInverseData = new double[nodes.size() + 1][nodes.size() + 1];
        for(int row = 0; row < nodes.size() + 1; row++){
            for(int col = 0; col < nodes.size() + 1; col++){
                double value = 0.0;

                ArrayList<Node> rowChildren = new ArrayList<>();
                ArrayList<Node> colChildren = new ArrayList<>();
                NodeType rowType = null;
                NodeType colType = null;

                if(row < nodes.size() && nodes.get(row).getClass().equals(Group.class)){
                    Group rowG = (Group) nodes.get(row);
                    rowChildren = rowG.nodes;
                    if(rowG.subTag.equals("pair")){
                        rowType = NodeType.PAIR;
                    }else if(rowG.subTag.equals("triple")){
                        rowType = NodeType.TRIPLE;
                    }
                }else if(row < nodes.size()){
                    rowType = NodeType.NODE;
                }

                if(col < nodes.size() && nodes.get(col).getClass().equals(Group.class)){
                    Group colG = (Group) nodes.get(col);
                    colChildren = colG.nodes;
                    if(colG.subTag.equals("pair")){
                        colType = NodeType.PAIR;
                    }else if(colG.subTag.equals("triple")){
                        colType = NodeType.TRIPLE;
                    }
                }else if(col < nodes.size()){
                    colType = NodeType.NODE;
                }

                if(rowType == NodeType.NODE && colType == NodeType.NODE){
                    //singleton with singleton
                    if(row == col){
                        //same singleton
                        value = ((2*N*(N-1)) + 4)/M;
                    }else{
                        //different singleton
                        value = (4*(N-1))/M;
                    }
                }else if((rowType == NodeType.NODE && colType == NodeType.PAIR) || (rowType == NodeType.PAIR && colType == NodeType.NODE)){
                    //singleton with pair
                    if((col < nodes.size() && rowChildren.contains(nodes.get(col))) || (row < nodes.size() && colChildren.contains(nodes.get(row)))){
                        //pair contains the singleton
                        value = (-8*(N-1))/M;
                    }else{
                        //pair does not contain the singleton
                        value = -8/M;
                    }
                }else if((rowType == NodeType.NODE && colType == NodeType.TRIPLE) || (rowType == NodeType.TRIPLE && colType == NodeType.NODE)){
                    //singleton with triple
                    if((col < nodes.size() && rowChildren.contains(nodes.get(col))) || (row < nodes.size() && colChildren.contains(nodes.get(row)))){
                        //triple contains the singleton
                        value = 16/M;
                    }else{
                        //triple does not contain the singleton
                        value = 0;
                    }
                }else if((rowType == NodeType.NODE && colType == null) || (rowType == null && colType == NodeType.NODE)){
                    //singleton and rightmost column
                    value = (-1 * (N - 3) * (N - 2)) / M;
                }else if(rowType == NodeType.PAIR && colType == NodeType.PAIR){
                    //pair with pairs
                    if(row == col){
                        //same pair
                        value = (16 * (N - 1)) / M;
                    }else if(pairsShareACommonNode(rowChildren, colChildren)){
                        //pairs have one node in common
                        value = 16/M;
                    }else{
                        //pairs have no nodes in common
                        value = 0;
                    }
                }else if((rowType == NodeType.PAIR && colType == NodeType.TRIPLE) || (rowType == NodeType.TRIPLE && colType == NodeType.PAIR)){
                    //pair with triple
                    if((colType == NodeType.TRIPLE && tripleContainsPair(colChildren, rowChildren)) || (rowType == NodeType.TRIPLE && tripleContainsPair(rowChildren, colChildren))){
                        //pair and triple which contains pair
                        value = -32/M;
                    }else{
                        //pair and triple which does not contain pair
                        value = 0;
                    }
                }else if((rowType == NodeType.PAIR && colType == null) || (rowType == null && colType == NodeType.PAIR)){
                    //pair and rightmost column
                    value = (4 * (N - 3)) / M;
                }else if(rowType == NodeType.TRIPLE && colType == NodeType.TRIPLE){
                    //triple with triple
                    if(row == col){
                        //same triple
                        value = 64/M;
                    }else{
                        //different triple
                        value = 0;
                    }
                }else if((rowType == NodeType.TRIPLE && colType == null) || (rowType == null && colType == NodeType.TRIPLE)){
                    value = -8/M;
                }else if(rowType == null && colType == null){
                    //both rightmost column
                    value = (6 + 5 * N + Math.pow(N, 3)) / (6 * M);
                }else{
                    System.out.println("INVALID CASE IN PAIRS 2 EQUATION");
                }

                EInverseData[row][col] = value;
            }
        }

        ArrayList<Node> allNodes = new ArrayList<>(nodes);
        Node global_node = network.getNode(8888);
        if(global_node != null){
            allNodes.add(global_node);
        }
        double[][] eData = new double[allNodes.size()][1];
        for(int row = 0; row < allNodes.size(); row++){
            eData[row][0] = allNodes.get(row).analysisDataHolders.get(rollUpIndex).nodeCriticalityTrials[trial].sumOfGoalWhenON;
        }

        SimpleMatrix EInverse = new SimpleMatrix(EInverseData);
        SimpleMatrix e = new SimpleMatrix(eData);

        return EInverse.mult(e);
    }

    //Pairs matrix
    public static SimpleMatrix pairsExplicit(Network network, ArrayList<Node> nodes, int rollUpIndex, int trial){

        double M = network.analysisDataHolders.get(rollUpIndex).networkCriticalityTrials[trial].possibilitiesCount.doubleValue();
        int N = 0;
        for(Node node : nodes){
            if(node.getClass().equals(Activity.class)){
                N++;
            }
        }

        double[][] EInverseData = new double[nodes.size() + 1][nodes.size() + 1];
        for(int row = 0; row < nodes.size() + 1; row++){
            for(int col = 0; col < nodes.size() + 1; col++){
                double value = 0.0;

                ArrayList<Node> rowChildren = new ArrayList<>();
                ArrayList<Node> colChildren = new ArrayList<>();
                NodeType rowType = null;
                NodeType colType = null;

                if(row < nodes.size() && nodes.get(row).getClass().equals(Group.class)){
                    Group rowG = (Group) nodes.get(row);
                    rowChildren = rowG.nodes;
                    if(rowG.subTag.equals("pair")){
                        rowType = NodeType.PAIR;
                    }
                }else if(row < nodes.size()){
                    rowType = NodeType.NODE;
                }

                if(col < nodes.size() && nodes.get(col).getClass().equals(Group.class)){
                    Group colG = (Group) nodes.get(col);
                    colChildren = colG.nodes;
                    if(colG.subTag.equals("pair")){
                        colType = NodeType.PAIR;
                    }
                }else if(col < nodes.size()){
                    colType = NodeType.NODE;
                }

                if(rowType == NodeType.NODE && colType == NodeType.NODE){
                    //singleton with singleton
                    if(row == col){
                        //same singleton
                        value = (4*N)/M;
                    }else{
                        //different singleton
                        value = 4/M;
                    }
                }else if((rowType == NodeType.NODE && colType == NodeType.PAIR) || (rowType == NodeType.PAIR && colType == NodeType.NODE)){
                    //singleton with pair
                    if((col < nodes.size() && rowChildren.contains(nodes.get(col))) || (row < nodes.size() && colChildren.contains(nodes.get(row)))){
                        //pair contains the singleton
                        value = -8/M;
                    }else{
                        //pair does not contain the singleton
                        value = 0;
                    }
                }else if((rowType == NodeType.NODE && colType == null) || (rowType == null && colType == NodeType.NODE)){
                    //singleton and rightmost column
                    value = (2 * (N - 2))/M;
                }else if(rowType == NodeType.PAIR && colType == NodeType.PAIR){
                    //pair with pairs
                    if(row == col){
                        //same pair
                        value = 16/M;
                    }else{
                        //different pairs
                        value = 0;
                    }
                }else if((rowType == NodeType.PAIR && colType == null) || (rowType == null && colType == NodeType.PAIR)){
                    //pair and rightmost column
                    value = -4/M;
                }else if(rowType == null && colType == null){
                    //both rightmost column
                    value = (1/M) + (((N + 1) * N) / (2*M));
                }else{
                    System.out.println("INVALID CASE");
                }

                EInverseData[row][col] = value;
            }
        }

        ArrayList<Node> allNodes = new ArrayList<>(nodes);
        Node global_node = network.getNode(8888);
        if(global_node != null){
            allNodes.add(global_node);
        }
        double[][] eData = new double[allNodes.size()][1];
        for(int row = 0; row < allNodes.size(); row++){
            eData[row][0] = allNodes.get(row).analysisDataHolders.get(rollUpIndex).nodeCriticalityTrials[trial].sumOfGoalWhenON;
        }

        SimpleMatrix EInverse = new SimpleMatrix(EInverseData);
        SimpleMatrix e = new SimpleMatrix(eData);

        return EInverse.mult(e);
    }



    private static void verifyComboEInverseMatrix(SimpleMatrix matrix, ArrayList<Node> nodes, double M, double N){
        for(int row = 0; row < matrix.numRows(); row++){
            for(int col = 0; col < matrix.numCols(); col++){
                double value = matrix.get(row, col);

                ArrayList<Node> rowChildren = new ArrayList<>();
                ArrayList<Node> colChildren = new ArrayList<>();
                NodeType rowType = null;
                NodeType colType = null;

                if(row < nodes.size() && nodes.get(row).getClass().equals(Group.class)){
                    Group rowG = (Group) nodes.get(row);
                    rowChildren = rowG.nodes;
                    if(rowG.subTag.equals("pair")){
                        rowType = NodeType.PAIR;
                    }else if(rowG.subTag.equals("triple")){
                        rowType = NodeType.TRIPLE;
                    }
                }else if(row < nodes.size()){
                    rowType = NodeType.NODE;
                }

                if(col < nodes.size() && nodes.get(col).getClass().equals(Group.class)){
                    Group colG = (Group) nodes.get(col);
                    colChildren = colG.nodes;
                    if(colG.subTag.equals("pair")){
                        colType = NodeType.PAIR;
                    }else if(colG.subTag.equals("triple")){
                        colType = NodeType.TRIPLE;
                    }
                }else if(col < nodes.size()){
                    colType = NodeType.NODE;
                }

                if(rowType == NodeType.NODE && colType == NodeType.NODE){
                    //singleton with singleton
                    String when = "";
                    if(row == col){
                        //same singleton
                        when = "SAME SINGLETON";
                        vValue(value, ((2*N*(N-1)) + 4)/M,when);
                    }else{
                        //different singleton
                        when = "DIFFERENT SINGLETON";
                        vValue(value, (4*(N-1))/M,when);
                    }
                }else if((rowType == NodeType.NODE && colType == NodeType.PAIR) || (rowType == NodeType.PAIR && colType == NodeType.NODE)){
                    //singleton with pair
                    String when = "";
                    if((col < nodes.size() && rowChildren.contains(nodes.get(col))) || (row < nodes.size() && colChildren.contains(nodes.get(row)))){
                        //pair contains the singleton
                        when = "PAIR CONTAINS SINGLETON";
                        vValue(value, (-8*(N-1))/M,when);
                    }else{
                        //pair does not contain the singleton
                        when = "PAIR DOES NOT CONTAIN SINGLETON";
                        vValue(value, -8/M,when);
                    }
                }else if((rowType == NodeType.NODE && colType == NodeType.TRIPLE) || (rowType == NodeType.TRIPLE && colType == NodeType.NODE)){
                    //singleton with triple
                    String when = "";
                    if((col < nodes.size() && rowChildren.contains(nodes.get(col))) || (row < nodes.size() && colChildren.contains(nodes.get(row)))){
                        //triple contains the singleton
                        when = "TRIPLE CONTAINS SINGLETON";
                        vValue(value, 16/M,when);
                    }else{
                        //triple does not contain the singleton
                        when = "TRIPLE DOES NOT CONTAIN SINGLETON";
                        vValue(value, 0,when);
                    }
                }else if((rowType == NodeType.NODE && colType == null) || (rowType == null && colType == NodeType.NODE)){
                    //singleton and rightmost column
                    String when = "SINGLETON AND RIGHtMOST COLUMN";
                    vValue(value, (-1 * (N - 3) * (N - 2)) / M,when);
                }else if(rowType == NodeType.PAIR && colType == NodeType.PAIR){
                    //pair with pairs
                    String when = "";
                    if(row == col){
                        //same pair
                        when = "SAME PAIR";
                        vValue(value, (16 * (N - 1)) / M,when);
                    }else if(pairsShareACommonNode(rowChildren, colChildren)){
                        //pairs have one node in common
                        when = "PAIR WITH ONE NODE IN COMMON";
                        vValue(value, 16/M,when);
                    }else{
                        //pairs have no nodes in common
                        when = "PAIR WITH NO NODES IN COMMON";
                        vValue(value, 0,when);
                    }
                }else if((rowType == NodeType.PAIR && colType == NodeType.TRIPLE) || (rowType == NodeType.TRIPLE && colType == NodeType.PAIR)){
                    //pair with triple
                    String when = "";
                    if((colType == NodeType.TRIPLE && tripleContainsPair(colChildren, rowChildren)) || (rowType == NodeType.TRIPLE && tripleContainsPair(rowChildren, colChildren))){
                        //pair and triple which contains pair
                        when = "TRIPLE CONTAINS PAIR";
                        vValue(value, -32/M,when);
                    }else{
                        //pair and triple which does not contain pair
                        when = "TRIPLE DOES NOT CONTAIN PAIR";
                        vValue(value, 0,when);
                    }
                }else if((rowType == NodeType.PAIR && colType == null) || (rowType == null && colType == NodeType.PAIR)){
                    //pair and rightmost column
                    String when = "PAIR AND RIGHTMOST COLUMN";
                    vValue(value, (4 * (N - 3)) / M,when);
                }else if(rowType == NodeType.TRIPLE && colType == NodeType.TRIPLE){
                    //triple with triple
                    String when = "";
                    if(row == col){
                        //same triple
                        when = "SAME TRIPLE";
                        vValue(value, 64/M,when);
                    }else{
                        //different triple
                        when = "DIFFERENT TRIPLE";
                        vValue(value, 0,when);
                    }
                }else if((rowType == NodeType.TRIPLE && colType == null) || (rowType == null && colType == NodeType.TRIPLE)){
                    String when = "TRIPLE AND RIGHTMOST COLUMN";
                    vValue(value, -8/M, when);
                }else if(rowType == null && colType == null){
                    //both rightmost column
                    String when = "BOTH RIGHTMOST COLUMN";
                    vValue(value, (6 + 5 * N + Math.pow(N, 3)) / (6 * M),when);
                }else{
                    System.out.println("INVALID CASE");
                }
            }
        }
    }

    private static void verifyPairsEInverseMatrix(SimpleMatrix matrix, ArrayList<Node> nodes, double M, double N){
        for(int row = 0; row < matrix.numRows(); row++){
            for(int col = 0; col < matrix.numCols(); col++){
                double value = matrix.get(row, col);

                ArrayList<Node> rowChildren = new ArrayList<>();
                ArrayList<Node> colChildren = new ArrayList<>();
                NodeType rowType = null;
                NodeType colType = null;

                if(row < nodes.size() && nodes.get(row).getClass().equals(Group.class)){
                    Group rowG = (Group) nodes.get(row);
                    rowChildren = rowG.nodes;
                    if(rowG.subTag.equals("pair")){
                        rowType = NodeType.PAIR;
                    }
                }else if(row < nodes.size()){
                    rowType = NodeType.NODE;
                }

                if(col < nodes.size() && nodes.get(col).getClass().equals(Group.class)){
                    Group colG = (Group) nodes.get(col);
                    colChildren = colG.nodes;
                    if(colG.subTag.equals("pair")){
                        colType = NodeType.PAIR;
                    }
                }else if(col < nodes.size()){
                    colType = NodeType.NODE;
                }

                if(rowType == NodeType.NODE && colType == NodeType.NODE){
                    //singleton with singleton
                    String when = "";
                    if(row == col){
                        //same singleton
                        when = "SAME SINGLETON";
                        vValue(value, (4*N)/M,when);
                    }else{
                        //different singleton
                        when = "DIFFERENT SINGLETON";
                        vValue(value, 4/M,when);
                    }
                }else if((rowType == NodeType.NODE && colType == NodeType.PAIR) || (rowType == NodeType.PAIR && colType == NodeType.NODE)){
                    //singleton with pair
                    String when = "";
                    if((col < nodes.size() && rowChildren.contains(nodes.get(col))) || (row < nodes.size() && colChildren.contains(nodes.get(row)))){
                        //pair contains the singleton
                        when = "PAIR CONTAINS SINGLETON";
                        vValue(value, -8/M,when);
                    }else{
                        //pair does not contain the singleton
                        when = "PAIR DOES NOT CONTAIN SINGLETON";
                        vValue(value, 0,when);
                    }
                }else if((rowType == NodeType.NODE && colType == null) || (rowType == null && colType == NodeType.NODE)){
                    //singleton and rightmost column
                    String when = "SINGLETON AND RIGHtMOST COLUMN";
                    vValue(value, (2 * (N - 2))/M,when);
                }else if(rowType == NodeType.PAIR && colType == NodeType.PAIR){
                    //pair with pairs
                    String when = "";
                    if(row == col){
                        //same pair
                        when = "SAME PAIR";
                        vValue(value, 16/M,when);
                    }else{
                        //different pairs
                        when = "DIFFERENT PAIRS";
                        vValue(value, 0,when);
                    }
                }else if((rowType == NodeType.PAIR && colType == null) || (rowType == null && colType == NodeType.PAIR)){
                    //pair and rightmost column
                    String when = "PAIR AND RIGHTMOST COLUMN";
                    vValue(value, -4/M,when);
                }else if(rowType == null && colType == null){
                    //both rightmost column
                    String when = "BOTH RIGHTMOST COLUMN";
                    vValue(value, (1/M) + (((N + 1) * N) / (2*M)),when);
                }else{
                    System.out.println("INVALID CASE");
                }
            }
        }
    }

    private static void vValue(double testValue, double trueValue, String when){
        DecimalFormat df = new DecimalFormat("0.0000");
        double test = Double.parseDouble(df.format(testValue));
        double actual = Double.parseDouble(df.format(trueValue));
        if(test > actual + 0.0001 && test < actual - 0.0001){
            System.out.println("Invalid value when " + when);
        }
    }

    private static boolean pairsShareACommonNode(ArrayList<Node> pair1Children, ArrayList<Node> pair2Children){
        boolean out = false;
        for(Node pair1Child: pair1Children){
            if(pair2Children.contains(pair1Child)){
                out = true;
                break;
            }
        }
        return out;
    }

    private static boolean tripleContainsPair(ArrayList<Node> tripleChildren, ArrayList<Node> pairChildren){
        boolean out = true;
        for(Node pairChild : pairChildren){
            if(!tripleChildren.contains(pairChild)){
                out = false;
                break;
            }
        }
        return out;
    }

    //function to raise a 'BigInteger' to the power of 'number'
    public static BigInteger powerN(BigInteger number, int power) {
        if(power == 0) return new BigInteger("0");
        BigInteger result = number;

        while(power > 1) {
            result = result.multiply(number);
            power--;
        }

        return result;
    }

    enum NodeType{
        NODE,
        PAIR,
        TRIPLE
    }

}
