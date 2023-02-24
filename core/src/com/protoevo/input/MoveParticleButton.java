package com.protoevo.input;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Null;
import com.protoevo.utils.ImageUtils;

public class MoveParticleButton extends ImageButton {

    private static Drawable openGrey, open, closed;

    public enum State {
        HOLDING, CAN_HOLD, CANNOT_HOLD
    }

    private State state, lastState;
    private boolean holdingControl;

    public MoveParticleButton(float size) {
        super(getDrawable(State.CANNOT_HOLD));
        setTouchable(Touchable.enabled);

        addListener(event -> {
            if (state == State.HOLDING)
                return true;

            if (event.toString().equals("touchDown")) {
                if (state == State.CAN_HOLD) {
                    state = State.CANNOT_HOLD;

                } else if (state == State.CANNOT_HOLD) {
                    state = State.CAN_HOLD;
                }
            }
            return true;
        });

        setHeight(size);
        setWidth(size);
        state = State.CANNOT_HOLD;
    }

    public boolean isHolding() {
        return getState() == MoveParticleButton.State.HOLDING;
    }

    public boolean couldHold() {
        return state == State.CAN_HOLD || (holdingControl && !isHolding());
    }

    public void setState(State state) {
        this.lastState = this.state;
        this.state = state;
    }

    public State getState() {
        return state;
    }

    public void revertToLastState() {
        if (lastState == State.HOLDING)
            setState(State.CANNOT_HOLD);
        else
            setState(lastState);
    }

    public void setHoldingControl(boolean b) {
        holdingControl = b;
    }

    @Override
    protected @Null Drawable getImageDrawable () {
        if (holdingControl && !isHolding())
            return getDrawable(State.CAN_HOLD);
        if (state == null)
            return getDrawable(State.CANNOT_HOLD);
        return getDrawable(state);
    }

    private static Drawable loadDrawable(String path) {
        return new TextureRegionDrawable(new TextureRegion(ImageUtils.getTexture(path)));
    }

    private static Drawable getDrawable(State state) {
        switch (state) {
            case HOLDING:
                if (closed == null)
                    closed = loadDrawable("icons/hand_closed.png");
                return closed;

            case CAN_HOLD:
                if (open == null)
                    open = loadDrawable("icons/hand_open.png");
                return open;

            default:
                if (openGrey == null)
                    openGrey = loadDrawable("icons/hand_open_grey.png");
                return openGrey;
        }
    }

    public static void dispose() {
        if (openGrey != null)
            ((TextureRegionDrawable) openGrey).getRegion().getTexture().dispose();
        if (open != null)
            ((TextureRegionDrawable) open).getRegion().getTexture().dispose();
        if (closed != null)
            ((TextureRegionDrawable) closed).getRegion().getTexture().dispose();
        openGrey = null;
        open = null;
        closed = null;
    }
}
