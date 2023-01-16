package com.protoevo.biology.nodes;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.protoevo.biology.Cell;
import com.protoevo.core.settings.ProtozoaSettings;
import com.protoevo.core.settings.SimulationSettings;

public class FlagellumAttachment extends NodeAttachment {

    private final Vector2 thrustVector = new Vector2();

    public FlagellumAttachment(SurfaceNode node) {
        super(node);
    }

    @Override
    public void update(float delta) {
        thrustVector.set(node.getRelativePos()).scl(-1).nor();
    }

    @Override
    public void handleIO(float[] input, float[] output) {
        // semantics of the flagella attachment:
        // input[0] controls generated thrust
        // input[1] controls generated torque

//        float thrust = MathUtils.clamp(input[0], -1f, 1f);
//        float torque = MathUtils.clamp(input[1], -1f, 1f);

        float thrust = MathUtils.clamp(1, -1f, 1f);
        float torque = MathUtils.clamp(1, -1f, 1f);

        Cell cell = node.getCell();
        float sizePenalty = cell.getRadius() / SimulationSettings.maxParticleRadius; // smaller flagella generate less impulse
        thrustVector.setLength(sizePenalty * thrust * ProtozoaSettings.maxProtozoaThrust);
        cell.applyImpulse(thrustVector);
        cell.applyTorque(torque * ProtozoaSettings.maxProtozoaTorque);
    }

    public Vector2 getThrustVector() {
        return thrustVector;
    }

    @Override
    public float energyUsage() {
        return 0;
    }

}
