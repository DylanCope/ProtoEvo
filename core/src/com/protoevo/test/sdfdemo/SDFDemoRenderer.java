package com.protoevo.test.sdfdemo;

import com.badlogic.gdx.utils.ScreenUtils;
import com.protoevo.ui.rendering.Renderer;

public class SDFDemoRenderer implements Renderer {

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0, 0, 0, 1);
    }

    @Override
    public void dispose() {

    }
}
