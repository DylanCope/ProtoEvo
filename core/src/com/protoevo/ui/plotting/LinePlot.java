package com.protoevo.ui.plotting;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import space.earlygrey.shapedrawer.ShapeDrawer;

import java.util.ArrayList;

public class LinePlot extends PlotElement {

    private ArrayList<Vector2> data;
    private final Array<Vector2> screenCoords = new Array<>();
    private float lineWidth = 2;
    private Color lineColor = Color.WHITE;

    public LinePlot setData(ArrayList<Vector2> data) {
        this.data = data;
        this.data.sort((a, b) -> Float.compare(a.x, b.x));
        return this;
    }

    public LinePlot setLineWidth(float lineWidth) {
        this.lineWidth = lineWidth;
        return this;
    }

    public LinePlot setLineColor(Color lineColor) {
        this.lineColor = lineColor;
        return this;
    }

    @Override
    public void draw(PlotGrid plot, ShapeDrawer drawer) {
        if (data == null || data.isEmpty()) return;

        screenCoords.clear();
        for (Vector2 point : data) {
            if (plot.inScreenBounds(point))
                screenCoords.add(plot.toScreenSpace(point));
        }
        drawer.setColor(lineColor);
        drawer.path(screenCoords, lineWidth, true);
    }
}
