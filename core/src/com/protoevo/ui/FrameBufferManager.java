package com.protoevo.ui;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;

import java.util.Stack;

public class FrameBufferManager {
    private final Stack<FrameBuffer> stack = new Stack<FrameBuffer>();

    private static FrameBufferManager instance;

    public FrameBufferManager getInstance() {
        if (instance == null) {
            instance = new FrameBufferManager();
        }
        return instance;
    }

    public static void dispose() {
        if (instance != null)
            for (FrameBuffer buffer : instance.stack) {
                buffer.dispose();
            }
        instance = null;
    }

    public void begin(FrameBuffer buffer) {
        if (!stack.isEmpty()) {
            stack.peek().end();
        }
        stack.push(buffer).begin();
    }

    public void end() {
        stack.pop().end();
        if (!stack.isEmpty()) {
            stack.peek().begin();
        }
    }
}
