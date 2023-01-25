package org.mitre.chart;


import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.StackedBarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.image.WritableImage;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.mitre.thor.logger.CoreLogger;

import javax.imageio.ImageIO;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

/**
 * Draws a StackedBarChart to the screen and handles its data. This is used for the CG analysis
 */
public class StackedBarPlotter extends Application {
    public Stage plotterStage;
    private final CategoryAxis xAxis = new CategoryAxis();
    private final NumberAxis yAxis = new NumberAxis();
    private final StackedBarChart<String, Number> stackedBarChart = new StackedBarChart<>(xAxis, yAxis);

    @Override
    public void start(Stage primaryStage){
        this.plotterStage = primaryStage;
        plotterStage.setTitle("Plot");
        xAxis.setTickLabelRotation(-45);
        xAxis.setTickLabelFont(Font.font("Century Gothic", FontWeight.BOLD,16));
        yAxis.setTickLabelFont(Font.font("Century Gothic", FontWeight.BOLD,16));
        stackedBarChart.setLegendSide(Side.RIGHT);
        stackedBarChart.setStyle("-fx-font-size: 20px");
        Scene scene = new Scene(stackedBarChart, 1200, 800);
        plotterStage.setScene(scene);
        this.plotterStage = primaryStage;
        //primaryStage.getScene().getStylesheets().add(getClass().getResource("/gui/chart/chart.css").toExternalForm());
        yAxis.setAutoRanging(false);
    }

    public void setTitle(String title){
        plotterStage.setTitle(title);
    }

    public StackedBarChart<String, Number> getChart(){
        return stackedBarChart;
    }

    public void addSeries(XYChart.Series<String, Number> series){
        this.stackedBarChart.getData().add(series);
    }

    public void setBarChartTitle(String title){
        this.stackedBarChart.setTitle(title);
    }

    public void setYAxisLabel(String labelName){
        this.yAxis.setLabel(labelName);
    }

    public void setYStep(double step){
        this.yAxis.setTickUnit(step);
    }

    public void setYAxisBounds(double lowerBound, double upperBound){
        yAxis.setLowerBound(lowerBound);
        yAxis.setUpperBound(upperBound);
    }

    public void captureAndSaveDisplay(){
        Platform.runLater(() -> {
            FileChooser fileChooser = new FileChooser();
            //Set extension filter
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("png files (*.png)", "*.png"));
            //Prompt user to select a file
            File file = fileChooser.showSaveDialog(null);
            if(file != null){
                //Pad the capture area
                WritableImage writableImage = new WritableImage((int)stackedBarChart.getWidth() + 20,
                        (int)stackedBarChart.getHeight() + 20);
                stackedBarChart.snapshot(null, writableImage);
                RenderedImage renderedImage = SwingFXUtils.fromFXImage(writableImage, null);
                //Write the snapshot to the chosen file
                try {
                    ImageIO.write(renderedImage, "png", file);
                    CoreLogger.logger.log(Level.INFO, "Cool Graph image file written");
                } catch (IOException e) {
                    e.printStackTrace();
                    CoreLogger.logger.log(Level.SEVERE, "Cool Graph image file could not be written");
                }
            }
        });
    }
}
