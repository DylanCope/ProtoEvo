package com.protoevo.physics.box2d;

import com.badlogic.gdx.physics.box2d.*;
import com.protoevo.env.Rock;
import com.protoevo.physics.Collision;

import java.io.Serializable;

public class Box2DCollisionHandler implements ContactListener, Serializable {
    public static long serialVersionUID = 1L;

    @Override
    public void beginContact(Contact contact) {
        if (contact.getWorldManifold().getPoints().length == 0)
            return;

        Fixture fixtureA = contact.getFixtureA();
        Fixture fixtureB = contact.getFixtureB();
        Body bodyA = fixtureA.getBody();
        Body bodyB = fixtureB.getBody();
        bodyA.setAwake(true);
        bodyB.setAwake(true);

        if (fixtureA.isSensor() && bodyA.getUserData() instanceof Box2DParticle) {
            Box2DParticle particleA = (Box2DParticle) bodyA.getUserData();
            particleA.addInteractingObject(bodyB.getUserData());
        }
        else if (fixtureB.isSensor() && bodyB.getUserData() instanceof Box2DParticle) {
            Box2DParticle particleB = (Box2DParticle) bodyB.getUserData();
            particleB.addInteractingObject(bodyA.getUserData());
        }
        else {
            if (bodyA.getUserData() instanceof Box2DParticle)
                onContact(contact, (Box2DParticle) bodyA.getUserData(), bodyB);

            if (bodyB.getUserData() instanceof Box2DParticle)
                onContact(contact, (Box2DParticle) bodyB.getUserData(), bodyA);
        }
    }

    private void onContact(Contact contact, Box2DParticle particle, Body body) {
        Collision collision = new Collision(
                particle,
                body.getUserData(),
                contact.getWorldManifold().getPoints()[0]);
        if (body.getUserData() instanceof Box2DParticle) {
            Box2DParticle other = (Box2DParticle) body.getUserData();
            particle.onCollision(collision, other);
        } else if (body.getUserData() instanceof Rock) {
            Rock rock = (Rock) body.getUserData();
            particle.onCollision(collision, rock);
        }
    }

    @Override
    public void endContact(Contact contact) {
        Fixture fixtureA = contact.getFixtureA();
        Fixture fixtureB = contact.getFixtureB();
        Body bodyA = fixtureA.getBody();
        Body bodyB = fixtureB.getBody();

        if (bodyA.getUserData() instanceof Box2DParticle)
            ((Box2DParticle) bodyA.getUserData()).endContact(bodyB.getUserData());

        if (bodyB.getUserData() instanceof Box2DParticle)
            ((Box2DParticle) bodyB.getUserData()).endContact(bodyB.getUserData());


        if (fixtureA.isSensor() && bodyA.getUserData() instanceof Box2DParticle) {
            Box2DParticle particleA = (Box2DParticle) bodyA.getUserData();
            particleA.removeInteractingObject(bodyB.getUserData());
        }
        else if (fixtureB.isSensor() && bodyB.getUserData() instanceof Box2DParticle) {
            Box2DParticle particleB = (Box2DParticle) bodyB.getUserData();
            particleB.removeInteractingObject(bodyA.getUserData());
        }
    }

    @Override
    public void preSolve(Contact contact, Manifold oldManifold) {}

    @Override
    public void postSolve(Contact contact, ContactImpulse impulse) {}
}
