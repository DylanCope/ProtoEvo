package com.protoevo.biology.nodes;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.protoevo.biology.cells.Cell;
import com.protoevo.core.Statistics;
import com.protoevo.env.Environment;
import com.protoevo.utils.Utils;


import java.io.Serializable;

public class Flagellum extends NodeAttachment implements Serializable {

    
    private static final long serialVersionUID = 1L;

    private final Vector2 thrustVector = new Vector2(), lastCellPos = new Vector2();
    private float torque;
    private final Statistics stats = new Statistics();

    public Flagellum(SurfaceNode node) {
        super(node);
    }


    public float getKineticEnergyRequired(Vector2 thrustVector, float torque) {

        Cell cell = node.getCell();

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
        float sizePenalty = getConstructionProgress() * cell.getRadius() / Environment.settings.maxParticleRadius.get();

        float thrust = MathUtils.clamp(input[0], -1f, 1f);

        thrustVector.set(node.getRelativePos()).scl(-1).nor();
        thrustVector.setLength(sizePenalty * thrust * Environment.settings.protozoa.maxFlagellumThrust.get());

        if (input.length > 1) {
            torque = getConstructionProgress() * MathUtils.clamp(input[1], -1f, 1f);
            torque *= sizePenalty * Environment.settings.protozoa.maxFlagellumTorque.get();
        }
        else torque = 0;

        float p = cell.generateMovement(thrustVector, torque);
        output[0] = Utils.clampedLinearRemap(p, 0, 1, -1, 1);

        Vector2 currentCellPos = cell.getPos();
        if (lastCellPos.isZero() || cell.getRadius() == 0) {
            lastCellPos.set(currentCellPos);
            if (output.length > 1) {
                output[1] = 0;
                output[2] = 0;
            }
            return;
        }

        if (output.length > 1) {
            output[1] = (currentCellPos.x - lastCellPos.x) / (20f * cell.getRadius());
            output[2] = (currentCellPos.y - lastCellPos.y) / (20f * cell.getRadius());
        }
        lastCellPos.set(currentCellPos);

//        float dx = delta * (currentCellPos.x - lastCellPos.x) / cell.getRadius();
//        float dy = delta * (currentCellPos.y - lastCellPos.y) / cell.getRadius();
//        output[1] = Utils.linearRemap(dx, -delta, delta, -1, 1);
//        output[2] = Utils.linearRemap(dy, -delta, delta, -1, 1);
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
        if (index == 0)
            return "Propulsion Success";
        if (index == 1)
            return "Speed X";
        if (index == 2)
            return "Speed Y";
        return null;  // no attachment output
    }

    public Vector2 getThrustVector() {
        return thrustVector;
    }

    public float getTorque() {
        return torque;
    }

    @Override
    public void addStats(Statistics stats) {
        stats.put("Thrust", thrustVector.len(), Statistics.ComplexUnit.IMPULSE);
        stats.put("Torque", torque, Statistics.ComplexUnit.TORQUE);
    }
}
