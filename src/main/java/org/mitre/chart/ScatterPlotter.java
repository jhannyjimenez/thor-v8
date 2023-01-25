package org.mitre.chart;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;

/**
 * Draws a LineChart to the screen and handles its data. This is used for the Operability Analysis
 */
public class ScatterPlotter extends Application {

    public Stage plotterStage;
    private final NumberAxis xAxis = new NumberAxis();
    private final NumberAxis yAxis = new NumberAxis();
    private final ScatterChart<Number, Number> lineChart = new ScatterChart<>(xAxis, yAxis);

    //set up the line chart
    @Override
    public void start(Stage primaryStage){
        this.plotterStage = primaryStage;
        plotterStage.setTitle("Plot");
        Scene scene = new Scene(lineChart, 800, 600);
        plotterStage.setScene(scene);
        this.plotterStage = primaryStage;
        lineChart.applyCss();

        xAxis.setAutoRanging(false);
        yAxis.setAutoRanging(false);
    }

    public void show(){
        plotterStage.show();
    }

    public void setTitle(String title){
        plotterStage.setTitle(title);
    }

    public void addSeries(XYChart.Series<Number, Number> series){
        this.lineChart.getData().add(series);
    }

    public void setLineChartTitle(String title){
        this.lineChart.setTitle(title);
    }

    public void setXAxisLabel(String labelName){
        this.xAxis.setLabel(labelName);
    }

    public void setYAxisLabel(String labelName){
        this.yAxis.setLabel(labelName);
    }

    public void setXStep(double step){
        this.xAxis.setTickUnit(step);
    }

    public void setYStep(double step){
        this.yAxis.setTickUnit(step);
    }

    public void setXAxisBounds(double lowerBound, double upperBound){
        xAxis.setLowerBound(lowerBound);
        xAxis.setUpperBound(upperBound);
    }

    public void setYAxisBounds(double lowerBound, double upperBound){
        yAxis.setLowerBound(lowerBound);
        yAxis.setUpperBound(upperBound);
    }
}
