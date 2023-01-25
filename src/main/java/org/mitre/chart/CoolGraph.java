package org.mitre.chart;

import javafx.application.Platform;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import org.mitre.thor.network.nodes.Node;

import java.util.*;

public class CoolGraph extends StackedBarPlotter{

    public List<Integer> divisions;
    List<int[]> divisionColors;

    public CoolGraph(List<Integer> divisions, List<int[]> divisionColors){
        super.setBarChartTitle("Average Operability of End Nodes give X-Axis Failure Sets");
        super.setYAxisLabel("Self-Effectiveness of Node(s) in the X Set");
        super.setYAxisBounds(0,100);
        super.setYStep(10);

        this.divisions = divisions;
        this.divisionColors = divisionColors;
        Platform.runLater(() -> super.start(new Stage()));
        Platform.runLater(() -> super.setTitle("CG"));

        if(divisions.get(divisions.size() - 1) < 100){
            divisions.add(100);
        }
    }

    public void show(){
        Platform.runLater(() ->  super.plotterStage.close());
        Platform.runLater(() ->  super.plotterStage.show());
    }

    public void graphNodes(ArrayList<Node> nodes, int rollUpIndex){
        for (int d = 0; d < divisions.size(); d++) {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            if(d != 0){
                series.setName("Average operability of end nodes (" + divisions.get(d - 1) + " < Op <= " + divisions.get(d) + ")");
            }else{
                series.setName("Average operability of end nodes (0 < Op <= " + divisions.get(d) + ")");
            }

            List<Node> newOrder = new ArrayList<>(nodes);
            newOrder.sort((o1, o2) -> Double.compare(o2.analysisDataHolders.get(rollUpIndex).cgSCore, o1.analysisDataHolders.get(rollUpIndex).cgSCore));
            if(newOrder.size() > 10){
                newOrder =  newOrder.subList(0, 10);
            }

            for (Node node : newOrder) {
                XYChart.Data<String, Number> data;
                data = new XYChart.Data<>(node.getDecorativeName(), node.analysisDataHolders.get(rollUpIndex).sizePerDivision.get(d));
                series.getData().add(data);
            }
            if(series.getData().size() != 0){
                Platform.runLater(() -> super.addSeries(series));
            }
        }

        colorSeries();
        colorLegend();
    }

    private void colorSeries(){
        Platform.runLater(() -> {
            for(int i = 0; i < divisionColors.size(); i++){
                Set<javafx.scene.Node> nodes = super.getChart().lookupAll(".default-color" + i + ".chart-bar");
                for(javafx.scene.Node node : nodes){
                    node.setStyle("-fx-bar-fill: rgb(" + divisionColors.get(i)[0] + "," + divisionColors.get(i)[1] + "," + divisionColors.get(i)[2] + ");");
                }
            }
        });
    }

    private void colorLegend(){
        Platform.runLater(() -> {
           Set<javafx.scene.Node> items = super.getChart().lookupAll("Label.chart-legend-item");
           int i = 0;
           for(javafx.scene.Node item : items){
               Label label = (Label) item;
               Color c = Color.rgb(divisionColors.get(i)[0], divisionColors.get(i)[1], divisionColors.get(i)[2]);
               final Rectangle rectangle = new Rectangle(15, 15, c);
               label.setGraphic(rectangle);
               i++;
           }
        });
    }
}
