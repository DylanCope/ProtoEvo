package com.protoevo.input;

import com.badlogic.gdx.Input;
import com.protoevo.biology.cells.Cell;
import com.protoevo.core.EnvFileIO;

public class SaveCellTextListener implements Input.TextInputListener {

    private final Cell cell;

    public SaveCellTextListener(Cell cell) {
        this.cell = cell;
    }

    @Override
    public void input(String text) {
        EnvFileIO.saveCell(cell, text);
    }

    @Override
    public void canceled() {

    }
}
