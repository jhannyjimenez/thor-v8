package org.mitre.transitions;

import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.util.Duration;

/**
 * Handles any GUI transitions
 */
public class FxTransitions {
    /**
     * Slides the current pane out to the right side and simultaneously slides a pane in from the left side
     *
     * @param nodeIn the node that is to slide in from the left side
     * @param nodeOut the node that is to slide out to the right side
     * @param durationInSeconds the duration of the animation
     */
    public static void slideNodeInAndNodeOut(Node nodeIn, Node nodeOut, double durationInSeconds){
        nodeIn.toFront();
        nodeIn.setTranslateX(-nodeIn.getBoundsInParent().getWidth());

        TranslateTransition slideIn = new TranslateTransition(Duration.seconds(durationInSeconds), nodeIn);
        slideIn.setByX(nodeIn.getBoundsInParent().getWidth());
        slideIn.setInterpolator(Interpolator.LINEAR);

        TranslateTransition slideOut = new TranslateTransition(Duration.seconds(durationInSeconds), nodeOut);
        slideOut.setByX(nodeOut.getBoundsInParent().getWidth());
        slideOut.setInterpolator(Interpolator.LINEAR);

        slideIn.play();
        slideOut.play();
    }
}
