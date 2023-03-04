package com.protoevo.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.Pixmap;

public class CursorUtils {

    public enum CursorState {
        DEFAULT, OPEN_HAND, CLOSED_HAND, MAGNIFYING_GLASS, LIGHTNING
    }

    public static Cursor createCursor(String path, int xHotspot, int yHotspot) {
        Pixmap pixmap = new Pixmap(Gdx.files.internal(path));
        int size = 32;
        Pixmap cursorPixmap = new Pixmap(size, size, pixmap.getFormat());
        cursorPixmap.drawPixmap(
                pixmap, 0, 0,
                pixmap.getWidth(), pixmap.getHeight(),
                0, 0,
                cursorPixmap.getWidth(), cursorPixmap.getHeight());
        pixmap.dispose();
        Cursor cursor = Gdx.graphics.newCursor(cursorPixmap, xHotspot, yHotspot);
        cursorPixmap.dispose();
        return cursor;
    }

    private static class CursorStateHolder {
        private final Cursor defaultCursorPixmap;
        private final Cursor openHandCursorPixmap;
        private final Cursor closedHandCursorPixmap;
        private final Cursor magnifyingGlassCursorPixmap;
        private final Cursor lightningCursor;
        private CursorState cursorState;

        public CursorStateHolder() {
            defaultCursorPixmap = createCursor("cursors/cursor.png", 0, 0);
            openHandCursorPixmap = createCursor("cursors/hand_open_behind.png", 16, 12);
            closedHandCursorPixmap = createCursor("cursors/hand_closed_behind.png", 16, 16);
            magnifyingGlassCursorPixmap = createCursor("cursors/magnifying_glass.png", 0, 0);
            lightningCursor = createCursor("cursors/lightning.png", 0, 0);
        }

        public void setCursor(CursorState cursorState) {
            if (this.cursorState == cursorState) return;
            this.cursorState = cursorState;
            try {
                switch (cursorState) {
                    case DEFAULT:
                        Gdx.graphics.setCursor(defaultCursorPixmap);
                        break;
                    case OPEN_HAND:
                        Gdx.graphics.setCursor(openHandCursorPixmap);
                        break;
                    case CLOSED_HAND:
                        Gdx.graphics.setCursor(closedHandCursorPixmap);
                        break;
                    case MAGNIFYING_GLASS:
                        Gdx.graphics.setCursor(magnifyingGlassCursorPixmap);
                        break;
                    case LIGHTNING:
                        Gdx.graphics.setCursor(lightningCursor);
                        break;
                }
            }
            catch (Exception ignored) {}
        }

        public void dispose() {
            defaultCursorPixmap.dispose();
            openHandCursorPixmap.dispose();
            closedHandCursorPixmap.dispose();
            magnifyingGlassCursorPixmap.dispose();
            lightningCursor.dispose();
        }
    }

    private static CursorStateHolder cursorStateHolder = null;

    private static CursorStateHolder getCursorStateHolder() {
        if (cursorStateHolder == null) {
            cursorStateHolder = new CursorStateHolder();
        }
        return cursorStateHolder;
    }

    public static void setDefaultCursor() {
        getCursorStateHolder().setCursor(CursorState.DEFAULT);
    }

    public static void setMagnifyingGlassCursor() {
        getCursorStateHolder().setCursor(CursorState.MAGNIFYING_GLASS);
    }

    public static void setOpenHandCursor() {
        getCursorStateHolder().setCursor(CursorState.OPEN_HAND);
    }

    public static void setClosedHandCursor() {
        getCursorStateHolder().setCursor(CursorState.CLOSED_HAND);
    }

    public static void setLightningCursor() {
        getCursorStateHolder().setCursor(CursorState.LIGHTNING);
    }

    public static void dispose() {
        if (cursorStateHolder != null) {
            cursorStateHolder.dispose();
            cursorStateHolder = null;
        }
    }
}
