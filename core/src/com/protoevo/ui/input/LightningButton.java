package com.protoevo.ui.input;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Null;
import com.protoevo.ui.SimulationInputManager;
import com.protoevo.utils.ImageUtils;

public class LightningButton extends ImageButton {

    private static Drawable canStrikeImage, cannotStrikeImage;

    private boolean canStrike = false;

    public LightningButton(SimulationInputManager inputManager, float size) {
        super(getDrawable(false));
        setTouchable(Touchable.enabled);

        addListener(event -> {
            if (event.toString().equals("touchDown")) {
                canStrike = !canStrike;
                inputManager.getParticleTracker().setCanTrack(!canStrike);
            }
            return true;
        });

        setHeight(size);
        setWidth(size);
    }

    public boolean canStrike() {
        return canStrike;
    }

    @Override
    protected @Null Drawable getImageDrawable () {
        return getDrawable(canStrike);
    }

    private static Drawable loadDrawable(String path) {
        return new TextureRegionDrawable(new TextureRegion(ImageUtils.getTexture(path)));
    }

    private static Drawable getDrawable(boolean state) {
        if (state) {
            if (canStrikeImage == null)
                canStrikeImage = loadDrawable("icons/can_strike.png");
            return canStrikeImage;
        } else {
            if (cannotStrikeImage == null)
                cannotStrikeImage = loadDrawable("icons/cannot_strike.png");
            return cannotStrikeImage;
        }
    }

    public static void dispose() {
        if (canStrikeImage != null)
            ((TextureRegionDrawable) canStrikeImage).getRegion().getTexture().dispose();
        if (cannotStrikeImage != null)
            ((TextureRegionDrawable) cannotStrikeImage).getRegion().getTexture().dispose();
        canStrikeImage = null;
        cannotStrikeImage = null;
    }
}
