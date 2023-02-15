package com.protoevo.biology.organelles;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.protoevo.biology.cells.Cell;
import com.protoevo.settings.ProtozoaSettings;
import com.protoevo.settings.SimulationSettings;

public class Cilia extends OrganelleFunction {

    private final Vector2 thrustVector = new Vector2();
    private float angle;

    public Cilia(Organelle organelle) {
        super(organelle);
    }

    public float getKineticEnergyRequired(Vector2 thrustVector) {
        Cell cell = organelle.getCell();
        cell.getBody().getAngularVelocity();
        float speed = cell.getSpeed();
        float mass = cell.getMass();
        return .5f * mass * (speed*speed - thrustVector.len2() / (mass * mass));
    }

    @Override
    public void update(float delta, float[] input) {
        // semantics of the flagella attachment:
        // input[0] controls generated thrust
        // input[1] controls generated torque

        Cell cell = organelle.getCell();
        // smaller flagella generate less thrust and torque
        float sizePenalty = cell.getRadius() / SimulationSettings.maxParticleRadius;

        float thrust = MathUtils.clamp(input[0], -1f, 1f);
        float turn = MathUtils.clamp(input[1], -1f, 1f);
        angle += ProtozoaSettings.maxCiliaTurn * turn * delta;

        float l = sizePenalty * thrust * ProtozoaSettings.maxProtozoaThrust;
        thrustVector.set(l * MathUtils.cos(angle), l * MathUtils.sin(angle));

        float work = getKineticEnergyRequired(thrustVector);
        if (cell.enoughEnergyAvailable(work)) {
            cell.depleteEnergy(work);
            cell.applyImpulse(thrustVector);
        }
        else if (cell.getEnergyAvailable() > 0) {
            thrustVector.scl(cell.getEnergyAvailable() / work);
            cell.applyImpulse(thrustVector);
        }
    }

    @Override
    public String getName() {
        return "Cilia";
    }

    @Override
    public String getInputMeaning(int idx) {
        if (idx == 0)
            return "Thrust";
        if (idx == 1)
            return "Turn";
        return null;
    }
}
