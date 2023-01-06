package com.protoevo.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;

public class CursorUtils {

    public enum CursorState {
        DEFAULT,
        OPEN_HAND,
        CLOSED_HAND,
        MAGNIFYING_GLASS
    }

    public static CursorState cursorState = null;

    private static Pixmap defaultCursorPixmap;
    private static Pixmap openHandCursorPixmap;
    private static Pixmap closedHandCursorPixmap;
    private static Pixmap magnifyingGlassCursorPixmap;


    public static Pixmap createCursorPixmap(String path) {
        Pixmap pixmap = new Pixmap(Gdx.files.internal(path));
        int size = 32;
        Pixmap cursorPixmap = new Pixmap(size, size, pixmap.getFormat());
        cursorPixmap.drawPixmap(
                pixmap, 0, 0,
                pixmap.getWidth(), pixmap.getHeight(),
                0, 0,
                cursorPixmap.getWidth(), cursorPixmap.getHeight());
        pixmap.dispose();
        return cursorPixmap;
    }

    public static void setDefaultCursor() {
        if (cursorState == CursorState.DEFAULT) return;

        if (defaultCursorPixmap == null)
            defaultCursorPixmap = createCursorPixmap("cursors/cursor.png");

        int xHotspot = 7, yHotspot = 6;
        Gdx.graphics.setCursor(Gdx.graphics.newCursor(defaultCursorPixmap, xHotspot, yHotspot));
        cursorState = CursorState.DEFAULT;
    }

    public static void setMagnifyingGlassCursor() {
        if (cursorState == CursorState.MAGNIFYING_GLASS) return;

        if (magnifyingGlassCursorPixmap == null)
            magnifyingGlassCursorPixmap = createCursorPixmap("cursors/magnifier.png");

        int xHotspot = 7, yHotspot = 6;
        Gdx.graphics.setCursor(Gdx.graphics.newCursor(magnifyingGlassCursorPixmap, xHotspot, yHotspot));

        cursorState = CursorState.MAGNIFYING_GLASS;
    }

    public static void setOpenHandCursor() {
        if (cursorState == CursorState.OPEN_HAND) return;

        if (openHandCursorPixmap == null)
            openHandCursorPixmap = createCursorPixmap("cursors/hand_open_behind.png");

        int xHotspot = 16, yHotspot = 12;
        Gdx.graphics.setCursor(Gdx.graphics.newCursor(openHandCursorPixmap, xHotspot, yHotspot));

        cursorState = CursorState.OPEN_HAND;
    }

    public static void setClosedHandCursor() {
        if (cursorState == CursorState.CLOSED_HAND) return;

        if (closedHandCursorPixmap == null)
            closedHandCursorPixmap = createCursorPixmap("cursors/hand_closed_behind.png");

        int xHotspot = 16, yHotspot = 16;
        Gdx.graphics.setCursor(Gdx.graphics.newCursor(closedHandCursorPixmap, xHotspot, yHotspot));

        cursorState = CursorState.CLOSED_HAND;
    }
}
