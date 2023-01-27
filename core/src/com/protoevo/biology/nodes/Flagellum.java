package com.protoevo.biology.nodes;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.protoevo.biology.Cell;
import com.protoevo.core.settings.ProtozoaSettings;
import com.protoevo.core.settings.SimulationSettings;

public class Flagellum extends NodeAttachment {

    private final Vector2 thrustVector = new Vector2();
    private float torque;

    public Flagellum(SurfaceNode node) {
        super(node);
    }


    public float getKineticEnergyRequired(Vector2 thrustVector, float torque) {

        Cell cell = node.getCell();
        cell.getBody().getAngularVelocity();

        float speed = cell.getSpeed();
        float mass = cell.getMass();

        float linear_ke = .5f * mass * (speed*speed - thrustVector.len2() / (mass * mass));

//        // http://labman.phys.utk.edu/phys221core/modules/m6/energy_and_angular_momentum.html
//        float r = cell.getRadius();
//        float omega = cell.getBody().getAngularVelocity();
//        float angular_ke = 0.75f * mass * r*r * (omega*omega - torque*torque / (mass * r * r));

        return linear_ke;
    }

    @Override
    public void update(float delta, float[] input, float[] output) {
        // semantics of the flagella attachment:
        // input[0] controls generated thrust
        // input[1] controls generated torque

        Cell cell = node.getCell();
        // smaller flagella generate less thrust and torque
        float sizePenalty = cell.getRadius() / SimulationSettings.maxParticleRadius;

        float thrust = MathUtils.clamp(input[0], -1f, 1f);
        torque = MathUtils.clamp(input[1], -1f, 1f);

        thrustVector.set(node.getRelativePos()).scl(-1).nor();
        thrustVector.setLength(sizePenalty * thrust * ProtozoaSettings.maxProtozoaThrust);

        torque *= sizePenalty * ProtozoaSettings.maxProtozoaTorque;

		float work = getKineticEnergyRequired(thrustVector, torque);
		if (cell.enoughEnergyAvailable(work)) {
			cell.useEnergy(work);
			cell.applyImpulse(thrustVector);
            cell.applyTorque(torque);
		}
        else if (cell.getEnergyAvailable() > 0) {
            thrustVector.scl(cell.getEnergyAvailable() / work);
            torque = torque * cell.getEnergyAvailable() / work;
            cell.applyImpulse(thrustVector);
            cell.applyTorque(torque);
        }
    }

    @Override
    public String getName() {
        return "Flagellum";
    }

    @Override
    public String getInputMeaning(int index) {
        if (index == 0)
            return "Thrust";
        if (index == 1)
            return "Torque";
        return null;   // no other inputs
    }

    @Override
    public String getOutputMeaning(int index) {
        return null;  // no attachment output
    }

    public Vector2 getThrustVector() {
        return thrustVector;
    }

    public float getTorque() {
        return torque;
    }
}
