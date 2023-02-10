package com.protoevo.biology.organelles;

import com.badlogic.gdx.math.Vector2;
import com.protoevo.biology.Cell;
import com.protoevo.biology.PlantCell;
import com.protoevo.env.CollisionHandler;
import com.protoevo.env.JointsManager;

public class PlantAdhesionOrganelle extends OrganelleFunction {

    public PlantAdhesionOrganelle(Organelle organelle) {
        super(organelle);
    }

    @Override
    public void update(float delta, float[] input) {
        Cell cell = organelle.getCell();
        for (CollisionHandler.FixtureCollision contact : cell.getContacts()) {
            Object other = cell.getOther(contact);
            if (other instanceof PlantCell && createBindingCondition((Cell) other)) {
                bindTo(contact.point, (PlantCell) other);
            }
        }
    }

    private void bindTo(Vector2 contact, PlantCell otherCell) {
        Cell cell = organelle.getCell();
        float t1 = contact.angleRad(cell.getPos());
        float t2 = contact.angleRad(otherCell.getPos());
        JointsManager.JoinedParticles joining = new JointsManager.JoinedParticles(cell, otherCell, t1, t2);

        JointsManager jointsManager = cell.getEnv().getJointsManager();
        jointsManager.createJoint(joining);

        cell.registerJoining(joining);
        otherCell.registerJoining(joining);
    }

    private boolean createBindingCondition(Cell other) {
        return !other.isDead() && notAlreadyBound(other);
    }

    private boolean notAlreadyBound(Cell otherCell) {
        Cell cell = organelle.getCell();
        return otherCell.notBoundTo(cell) && cell.notBoundTo(otherCell);
    }
}
