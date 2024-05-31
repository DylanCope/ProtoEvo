package com.protoevo.ui.plotting;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.protoevo.ui.UIStyle;
import space.earlygrey.shapedrawer.ShapeDrawer;

import java.util.ArrayList;

public class PlotGrid extends Widget {

    ShapeDrawer drawer;
    private float xMin, xMax, yMin, yMax;
    private float xMajorTick, yMajorTick, xMinorTick, yMinorTick;
    private int majorTickWidth = 10;
    private float pixelWidth, pixelHeight;

    // offset from the center of the widget
    private final Vector2 originPositionOffset = new Vector2(0, 0);
    private float pixelLengthOfXUnit = 1f, pixelLengthOfYUnit = 1f;
    private final ArrayList<PlotElement> plotElements;

    public PlotGrid(Stage stage) {
        drawer = new ShapeDrawer(stage.getBatch(), new TextureRegion(UIStyle.getWhite1x1()));
        xMajorTick = yMajorTick = 1f;
        xMinorTick = yMinorTick = 0.25f;
        plotElements = new ArrayList<>();
    }

    public void add(PlotElement element) {
        plotElements.add(element);
    }

    public void setSize(float w, float h) {
        pixelWidth = w;
        pixelHeight = h;
    }

    public float getPixelWidth() {
        return pixelWidth;
    }

    public float getPixelHeight() {
        return pixelHeight;
    }

    public void setPlotBounds(float xMin, float xMax, float yMin, float yMax) {

         this.xMin = xMin;
         this.yMin = yMin;
         this.xMax = xMax;
         this.yMax = yMax;

        if (xMin >= xMax - 1e-12) {
            throw new RuntimeException("xMin must be less than xMax");
        }
        if (yMin >= yMax - 1e-12) {
            throw new RuntimeException("yMin must be less than yMax");
        }

        pixelLengthOfXUnit = pixelWidth / (xMax - xMin);
        pixelLengthOfYUnit = pixelHeight / (yMax - yMin);

        float xOriginOffset, yOriginOffset;
        if (xMin > 0) {
            xOriginOffset = -pixelWidth / 2f;
        }
        else if (xMax < 0) {
            xOriginOffset = pixelWidth / 2f;
        }
        else if (Math.abs(xMax) > Math.abs(xMin)) {
            xOriginOffset = -(pixelLengthOfXUnit * Math.abs(xMax) - pixelWidth / 2);
        } else {
            xOriginOffset = pixelLengthOfXUnit * Math.abs(xMin) - pixelWidth / 2;
        }

        if (yMin > 0) {
            yOriginOffset = -pixelHeight / 2f;
        }
        else if (xMax < 0) {
            yOriginOffset = pixelHeight / 2f;
        }
        else if (Math.abs(xMax) > Math.abs(yMin)) {
            yOriginOffset = -(pixelLengthOfYUnit * Math.abs(yMax) - pixelHeight / 2);
        } else {
            yOriginOffset = pixelLengthOfYUnit * Math.abs(yMin) - pixelHeight / 2;
        }

        originPositionOffset.set(xOriginOffset, yOriginOffset);
    }

    public void setPlotBoundsX(float xMin, float xMax) {
        pixelLengthOfYUnit = pixelLengthOfXUnit = (xMax - xMin) / getWidth();
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        super.draw(batch, parentAlpha);
        drawer.setColor(Color.WHITE.cpy().mul(1f, 1f, 1f, 0.25f));
        drawer.filledRectangle(getX(), getY(), getWidth(), getHeight());
        drawAxis(batch);
        drawer.setColor(Color.WHITE);

        for (PlotElement element : plotElements)
            element.draw(this, drawer);

        // border
        drawer.setColor(Color.WHITE);
        drawer.rectangle(getX(), getY(), getWidth(), getHeight(), 6);
    }

    public void drawAxis(Batch batch) {

        // draw grid
        for (float x = 0; x <= xMax; x += xMajorTick) {
            drawMajorXGridLine(x);
        }
        for (float x = 0; x >= xMin; x -= xMajorTick) {
            drawMajorXGridLine(x);
        }
        for (float y = 0; y <= yMax; y += yMajorTick) {
            drawMajorYGridLine(y);
        }
        for (float y = 0; y >= yMin; y -= yMajorTick) {
            drawMajorYGridLine(y);
        }

        // draw ticks on axis
        for (float x = 0; x <= xMax; x += xMajorTick) {
            drawMajorXTick(x);
        }
        for (float x = 0; x >= xMin; x -= xMajorTick) {
            drawMajorXTick(x);
        }
        for (float y = 0; y <= yMax; y += yMajorTick) {
            drawMajorYTick(y);
        }
        for (float y = 0; y >= yMin; y -= yMajorTick) {
            drawMajorYTick(y);
        }

        // draw main axis
        drawer.setColor(Color.WHITE);
        drawer.line(
                getX() + pixelWidth / 2f + originPositionOffset.x,
                getY(),
                getX() + pixelWidth / 2f + originPositionOffset.x,
                getY() + getHeight(),
                3
        );
        drawer.line(
                getX(),
                getY() + pixelHeight / 2f + originPositionOffset.y,
                getX() + getWidth(),
                getY() + pixelHeight / 2f + originPositionOffset.y,
                3
        );
    }

    public float getScreenX(float plotSpaceX) {
        return getX() + pixelWidth / 2f + originPositionOffset.x + plotSpaceX * pixelLengthOfXUnit;
    }

    public float getScreenY(float plotSpaceY) {
        return getY() + pixelHeight / 2f + originPositionOffset.y + plotSpaceY * pixelLengthOfYUnit;
    }

    public Vector2 toScreenSpace(Vector2 plotCoord) {
        return new Vector2(getScreenX(plotCoord.x), getScreenY(plotCoord.y));
    }

    public boolean inScreenBounds(Vector2 plotCoord) {
        Vector2 screenCoord = toScreenSpace(plotCoord);
        return (
            (getX() <= screenCoord.x) &&
            (screenCoord.x <= getX() + getWidth()) &&
            (getY() <= screenCoord.y) &&
            (screenCoord.y <= getY() + getHeight())
        );
    }

    public void drawMajorXGridLine(float x) {
        float lineX = getScreenX(x);
        if (lineX < getX() || lineX >= getX() + getWidth())
            return;
        drawer.setColor(Color.WHITE.cpy().mul(1f, 1f, 1f, 0.25f));
        drawer.line(lineX, getY(), lineX, getY() + getHeight(), 3);
    }

    public void drawMajorYGridLine(float y) {
        float lineY = getScreenY(y);
        if (lineY < getY() || lineY >= getY() + getHeight())
            return;
        drawer.setColor(Color.WHITE.cpy().mul(1f, 1f, 1f, 0.25f));
        drawer.line(getX(), lineY, getX() + getWidth(), lineY, 3);
    }

    public void drawMajorXTick(float x) {
        float lineX = getX() + pixelWidth / 2f + originPositionOffset.x + x * pixelLengthOfXUnit;
        if (lineX < getX() || lineX >= getX() + getWidth())
            return;
        drawer.setColor(Color.WHITE);
        drawer.line(
                lineX,
                getY() + getHeight() / 2 + originPositionOffset.y - majorTickWidth,
                lineX,
                getY() + getHeight() / 2 + originPositionOffset.y + majorTickWidth,
                3
        );
    }

    public void drawMajorYTick(float y) {
        float lineY = getY() + pixelHeight / 2f + y * pixelLengthOfYUnit + originPositionOffset.y;
        if (lineY < getY() || lineY >= getY() + getHeight())
            return;
        drawer.setColor(Color.WHITE);
        drawer.line(
                getX() + getWidth() / 2 + originPositionOffset.x - majorTickWidth,
                lineY,
                getX() + getWidth() / 2 + originPositionOffset.x + majorTickWidth,
                lineY,
                3
        );
    }


}
