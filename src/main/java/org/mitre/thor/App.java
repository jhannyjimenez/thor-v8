package org.mitre.thor;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.mitre.thor.logger.CoreLogger;
import org.mitre.thor.gui.Controller;

import java.io.IOException;
import java.util.logging.Level;

/**
 * This is the GUI Application/JavaFX App. It reads the FXML file and assigns controllers. It is important to note that the GUI
 * Application starts on its own thread.
 */
public class App extends Application {

    public Controller controller;
    private double x, y;
    private boolean display = true;

    /**
     * Starts the GUI Application
     */
    public void launchApp(boolean Display)
    {
        this.display = Display;
        launch();
    }

    /**
     * Converts the .fxml (which contains the GUI structure) into a visible GUI and assigns a controller so that
     * one can add functionality.
     *
     * @param primaryStage the primary stage of the JavaFX window
     */
    @Override
    public void start(Stage primaryStage) {
        controller = new Controller(this, primaryStage);

        //create loader that will load the gui layout from the .fxml file
        String guiLayoutLocation = "/gui/vcar-layout.fxml";
        FXMLLoader fxmlLoader;
        fxmlLoader = new FXMLLoader(getClass().getResource(guiLayoutLocation));

        //create instance of the gui controller and assign it to the gui
        fxmlLoader.setController(controller);

        //load gui layout
        Parent root = null;
        try {
            root = fxmlLoader.load();
        } catch (IOException e) {
            e.printStackTrace();
            CoreLogger.logger.log(Level.SEVERE, "Error occurred while trying to load the .fxml file");
        }

        if(root != null){
            //decorate the frame
            primaryStage.setScene(new Scene(root));
            primaryStage.initStyle(StageStyle.UNDECORATED);
            primaryStage.setTitle("THOR");
            primaryStage.show();

            //make the frame draggable
            root.setOnMousePressed((event -> {
                x = event.getSceneX();
                y = event.getSceneY();
            }));
            root.setOnMouseDragged((event -> {
                primaryStage.setX(event.getScreenX() - x);
                primaryStage.setY(event.getScreenY() - y);
            }));
        }else{
            CoreLogger.logger.log(Level.SEVERE, "Could not load the GUI layout at resources/" + guiLayoutLocation);
        }

        if(!display && root != null){
            root.setVisible(false);
            System.out.println("Display should be off");
        }
    }

    /**
     * Sends a non-severe message to the CoreLogger and to the GUI logger
     *
     * @param msg the message to be logged
     */
    public void GAS(String msg, boolean transition){
        CoreLogger.logger.log(Level.INFO, msg);
        if(controller != null){
            controller.throwConsoleStatus(msg, transition);
        }
    }

    /**
     * Sends a sever message to the CoreLogger and the GUI logger
     *
     * @param msg the message to be logged
     * @param transition determines if the GUI should transition to the console pane where the user can see the error
     */
    public void GAE(String msg, boolean transition){
        CoreLogger.logger.log(Level.SEVERE, msg);
        if(controller != null){
            controller.throwConsoleError(msg, transition);
        }
    }
}
