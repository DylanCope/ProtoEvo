package com.protoevo.ui.plotting;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
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
    private final GlyphLayout layout = new GlyphLayout();
    private BitmapFont axisFont;

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
        axisFont = UIStyle.createFiraCode((int) (pixelHeight / 25f));
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

        if (xMin > xMax) {
            throw new RuntimeException("xMin must be less than xMax");
        }
        if (yMin > yMax) {
            throw new RuntimeException("yMin must be less than yMax");
        }

        if (this.xMin == this.xMax) {
            this.xMin -= 1;
            this.xMax += 1;
        }
        if (this.yMin == this.yMax) {
            this.yMin -= 1;
            this.yMax += 1;
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
        else if (yMax < 0) {
            yOriginOffset = pixelHeight / 2f;
        }
        else if (Math.abs(yMax) > Math.abs(yMin)) {
            yOriginOffset = -(pixelLengthOfYUnit * Math.abs(yMax) - pixelHeight / 2);
        } else {
            yOriginOffset = pixelLengthOfYUnit * Math.abs(yMin) - pixelHeight / 2;
        }

        originPositionOffset.set(xOriginOffset, yOriginOffset);
    }

    public void setPlotBoundsX(float xMin, float xMax) {
        pixelLengthOfYUnit = pixelLengthOfXUnit = (xMax - xMin) / getWidth();
    }

    public void setMajorTicks(float xMajorTick, float yMajorTick) {
        this.xMajorTick = xMajorTick;
        this.yMajorTick = yMajorTick;
    }

    public void setMinorTicks(float xMinorTick, float yMinorTick) {
        this.xMinorTick = xMinorTick;
        this.yMinorTick = yMinorTick;
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

    private void drawAxisXTickLabel(Batch batch, float x) {
        batch.setColor(Color.WHITE);
        String text = String.format("%.2f", x);
        layout.setText(axisFont, text);
        float screenX = getScreenX(x) - layout.width / 2;
        float screenY = getScreenY(yMin) - layout.height * 1.25f;
        if (screenX < getX() - layout.width || screenX >= getX() + pixelWidth)
            return;
        axisFont.draw(batch, text, screenX, screenY);
    }

    private void drawAxisYTickLabel(Batch batch, float y) {
        batch.setColor(Color.WHITE);
        String text = String.format("%.2f", y);
        layout.setText(axisFont, text);
        float screenX = getScreenX(xMin) - layout.width * 2f;
        float screenY = getScreenY(y) + layout.height / 2f;
        if (screenY < getY() - layout.height || screenY >= getY() + pixelHeight)
            return;
        axisFont.draw(batch, text, screenX, screenY);
    }


    public void drawAxis(Batch batch) {

        // draw grid
        for (float x = 0; x <= xMax; x += xMajorTick) {
            drawMajorXGridLine(x);
            drawAxisXTickLabel(batch, x);
        }
        for (float x = 0; x >= xMin; x -= xMajorTick) {
            drawMajorXGridLine(x);
            drawAxisXTickLabel(batch, x);
        }
        for (float y = 0; y <= yMax; y += yMajorTick) {
            drawMajorYGridLine(y);
            drawAxisYTickLabel(batch, y);
        }
        for (float y = 0; y >= yMin; y -= yMajorTick) {
            drawMajorYGridLine(y);
            drawAxisYTickLabel(batch, y);
        }

//        // draw ticks on axis
//        for (float x = 0; x <= xMax; x += xMajorTick) {
//            drawMajorXTick(x);
//        }
//        for (float x = 0; x >= xMin; x -= xMajorTick) {
//            drawMajorXTick(x);
//        }
//        for (float y = 0; y <= yMax; y += yMajorTick) {
//            drawMajorYTick(y);
//        }
//        for (float y = 0; y >= yMin; y -= yMajorTick) {
//            drawMajorYTick(y);
//        }
//
//        // draw main axis
//        drawer.setColor(Color.WHITE);
//        drawer.line(
//                getX() + pixelWidth / 2f + originPositionOffset.x,
//                getY(),
//                getX() + pixelWidth / 2f + originPositionOffset.x,
//                getY() + getHeight(),
//                3
//        );
//        drawer.line(
//                getX(),
//                getY() + pixelHeight / 2f + originPositionOffset.y,
//                getX() + getWidth(),
//                getY() + pixelHeight / 2f + originPositionOffset.y,
//                3
//        );
    }

    public float getScreenX(float plotSpaceX) {
        float relXInBounds = (plotSpaceX - xMin) / (xMax - xMin);
        float pixelRelX = relXInBounds * pixelWidth;
        return getX() + pixelWidth / 2f + originPositionOffset.x + pixelRelX;
    }

    public float getScreenY(float plotSpaceY) {
        float relYInBounds = (plotSpaceY - yMin) / (yMax - yMin);
        float pixelRelY = relYInBounds * pixelHeight;
        return getY() + pixelHeight / 2f + originPositionOffset.y + pixelRelY;
    }

    public Vector2 toScreenSpace(Vector2 plotCoord) {
        return new Vector2(getScreenX(plotCoord.x), getScreenY(plotCoord.y));
    }

    public boolean inScreenBounds(Vector2 plotCoord) {
        Vector2 screenCoord = toScreenSpace(plotCoord);
        return (
            (getX() <= screenCoord.x) &&
            (screenCoord.x <= getX() + pixelWidth) &&
            (getY() <= screenCoord.y) &&
            (screenCoord.y <= getY() + pixelHeight)
        );
    }

    public void drawMajorXGridLine(float x) {
        float lineX = getScreenX(x);
        if (lineX < getX() || lineX >= getX() + pixelWidth)
            return;
        drawer.setColor(Color.WHITE.cpy().mul(1f, 1f, 1f, 0.25f));
        drawer.line(lineX, getY(), lineX, getY() + pixelHeight, 3);
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
