package com.immersion.videoplayer.widgets;

/**
 * It represents a scheduled task consisting of the information that allows for delayed invocation
 * of the callback contained.
 */
public class ScheduledTask {

    private final long mDuration;
    private final Runnable mCallback;
    private final boolean mFromEnd;

    /**
     * Creates a ScheduledTask
     * @param what Callback to be invoked.
     * @param beforeEnd Whether the time being supplied accounts before end or from the beginning.
     * @param at Duration to when the callback will be invoked.
     */
    public ScheduledTask(Runnable what, boolean beforeEnd, long at) {
        mCallback = what;
        mDuration = at;
        mFromEnd = beforeEnd;
    }

    public long getDuration() {
        return mDuration;
    }

    public Runnable getCallback() {
        return mCallback;
    }

    public boolean getStartFactor() {
        return mFromEnd;
    }
}
