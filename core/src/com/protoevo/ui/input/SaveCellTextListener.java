package com.protoevo.ui.input;

import com.badlogic.gdx.Input;
import com.protoevo.biology.cells.Cell;
import com.protoevo.env.Serialization;

public class SaveCellTextListener implements Input.TextInputListener {

    private final Cell cell;

    public SaveCellTextListener(Cell cell) {
        this.cell = cell;
    }

    @Override
    public void input(String text) {
        Serialization.saveCell(cell, text);
    }

    @Override
    public void canceled() {

    }
}
