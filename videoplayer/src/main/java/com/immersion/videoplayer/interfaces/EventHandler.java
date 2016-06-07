package com.immersion.videoplayer.interfaces;

/**
 * Interface that implements event handling as triggered by {@link com.immersion.videoplayer.widgets.MediaController}
 */
public interface EventHandler {

    /**
     * Method called to perform back press actions
     */
    void backPressed();

    /**
     * Update the current video position in millisec.
     *
     * @param i
     */
    void updateElapsedTime(int i);
}
