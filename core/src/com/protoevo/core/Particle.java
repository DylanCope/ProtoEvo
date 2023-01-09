package com.protoevo.core;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import com.protoevo.core.settings.Settings;
import com.protoevo.env.Environment;
import com.protoevo.env.Rock;
import com.protoevo.utils.Geometry;

import java.util.*;


public class Particle implements Collidable {

    private final Vector2[] boundingBox = new Vector2[2];
    private Environment environment;
    private Body body;
    private Fixture dynamicsFixture, sensorFixture;
    private boolean dead;
    private float radius = 1f;
    private final Vector2 pos = new Vector2(0, 0);
    private final TreeMap<String, Float> stats = new TreeMap<>();
    private final Queue<Object> contactObjects = new LinkedList<>();
    private final Queue<Object> interactionQueue = new LinkedList<>();

    public Particle() {}


    public Environment getEnv() {
        return environment;
    }

    public void setEnv(Environment environment) {
        this.environment = environment;
        environment.ensureAddedToEnvironment(this);
        createBody();
    }

    public void update(float delta) {
        dynamicsFixture.getShape().setRadius(radius);
        if (doesInteract())
            sensorFixture.getShape().setRadius(getInteractionRange());
    }

    public void createBody() {
        if (body != null)
            return;

        CircleShape circle = new CircleShape();
        circle.setRadius(radius);

        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;

        // Body consists of a dynamics fixture and a sensor fixture
        // The dynamics fixture is used for collisions and the sensor fixture is used for
        // detecting when the particle in interaction range with other objects
        body = environment.getWorld().createBody(bodyDef);

        // Create the dynamics fixture and attach it to the body

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = circle;
        fixtureDef.density = 1f;  // will be updated later
        fixtureDef.friction = 0.8f;
        fixtureDef.restitution = 0.6f;

//        if (getSensorCategory() != 0x0000)
//            fixtureDef.filter.categoryBits = getSensorCategory();

        dynamicsFixture = body.createFixture(fixtureDef);
        dynamicsFixture.setUserData(this);
        body.setUserData(this);
        body.setLinearDamping(.5f);

        circle.dispose();

        if (doesInteract()) {
            // Create the sensor fixture and attach it to the body
            CircleShape interactionCircle = new CircleShape();

            FixtureDef sensorFixtureDef = new FixtureDef();
            sensorFixtureDef.shape = interactionCircle;
            sensorFixtureDef.isSensor = true;
//        if (getSensorMask() != 0x0000)
//            sensorFixtureDef.filter.maskBits = getSensorMask();

            sensorFixture = body.createFixture(sensorFixtureDef);
            sensorFixture.setUserData(this);
            sensorFixture.setFilterData(new Filter());

            interactionCircle.dispose();
        }
    }

    /**
     * The category used for the dynamic fixture to determine
     * which sensors will interact with it.
     * @return the category bits
     */
    public short getSensorCategory() {
        return 0x0000;
    }

    /**
     * The mask used for the sensor fixture to determine
     * which dynamic fixtures will interact with it.
     * @return the mask bits
     */
    public short getSensorMask() {
        return 0x0000;
    }

    public Body getBody() {
        return body;
    }

    public void applyForce(Vector2 force) {
        body.applyForceToCenter(force, true);
    }

    public void applyImpulse(Vector2 impulse) {
        body.applyLinearImpulse(impulse, body.getWorldCenter(), true);
    }

    public float getInteractionRange() {
        return radius * 2;
    }

    public void queueInteraction(Object object) {
        interactionQueue.add(object);
    }

    public Collection<Object> getInteractionQueue() {
        return interactionQueue;
    }

    public boolean doesInteract() { return true; }
    public void interact(List<Object> interactions) {}

    public void onCollision(Particle other) {
        contactObjects.add(other);
    }

    public void onCollision(Rock rock) {
        contactObjects.add(rock);
    }

    public void reset() {
        contactObjects.clear();
    }

    public Collection<Object> getContactObjects() {
        return contactObjects;
    }

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = radius;
        if (dynamicsFixture != null) {
            dynamicsFixture.getShape().getRadius();
        }
    }

    public boolean isPointInside(Vector2 point) {
        float r = getRadius();
        return point.dst2(getPos()) < r*r;
    }

    public void setPos(Vector2 pos) {
        this.pos.set(pos);
        if (body != null) {
            body.setTransform(pos, 0);
        }
    }

    public Vector2 getPos() {
        return body.getPosition();
    }

    public Vector2 getVel() {
        return body.getLinearVelocity();
    }

    public float getSpeed() {
        return getVel().len();
    }

    public float getMass() {
        return getMass(getRadius());
    }

    public float getMass(float r) {
        return getMass(r, 0);
    }

    public float getMass(float r, float extraMass) {
        return Geometry.getCircleArea(r) * getMassDensity() + extraMass;
    }

    public float getMassDensity() {
        return 1f;
    }

    @Override
    public boolean pointInside(Vector2 p) {
        return Geometry.isPointInsideCircle(getPos(), getRadius(), p);
    }

    @Override
    public boolean rayIntersects(Vector2 start, Vector2 end) {
        return false;
    }

    @Override
    public Vector2[] rayCollisions(Vector2 start, Vector2 end) {
        return new Vector2[0];
    }

    @Override
    public Color getColor() {
        return Color.WHITE;
    }

    @Override
    public Vector2[] getBoundingBox() {
        float x = getPos().x;
        float y = getPos().y;
        float r = getRadius();
        boundingBox[0] = new Vector2(x - r, y - r);
        boundingBox[1] = new Vector2(x + r, y + r);
        return boundingBox;
    }

    public boolean isCollidingWith(Rock rock) {
        Vector2[][] edges = rock.getEdges();
        float r = getRadius();
        Vector2 pos = getPos();

        if (rock.pointInside(pos))
            return true;

        for (Vector2[] edge : edges) {
            if (Geometry.doesLineIntersectCircle(edge, pos, r))
                return true;
        }
        return false;
    }

    public boolean isCollidingWith(Particle other)
    {
        if (other == this)
            return false;
        float r = getRadius() + other.getRadius();
        return other.getPos().dst2(getPos()) < r*r;
    }

    public Map<String, Float> getStats() {
        stats.clear();
        stats.put("Size", Settings.statsDistanceScalar * getRadius());
        stats.put("Speed", Settings.statsDistanceScalar * getSpeed());
        stats.put("Total Mass", Settings.statsMassScalar * getMass());
        stats.put("Mass Density", Settings.statsMassScalar * getMassDensity());
        return stats;
    }

    public boolean isDead() {
        return dead;
    }

    public void kill() {
        dead = true;
    }

    public void dispose() {
        kill();
        environment.getWorld().destroyBody(body);
    }

    public String getPrettyName() {
        return "Particle";
    }

    public Map<String, Float> getDebugStats() {
        Map<String, Float> stats = new TreeMap<>();
        stats.put("Position X", Settings.statsDistanceScalar * getPos().x);
        stats.put("Position Y", Settings.statsDistanceScalar * getPos().y);
        stats.put("Inertia", body.getInertia());
        stats.put("Num Joints", (float) body.getJointList().size);
        stats.put("Num Fixtures", (float) body.getFixtureList().size);
        stats.put("Num Contacts", (float) contactObjects.size());
        stats.put("Num Interactions", (float) interactionQueue.size());
        stats.put("Is Dead", dead ? 1f : 0f);
        stats.put("Is Sleeping", body.isAwake() ? 0f : 1f);
        return stats;
    }

    public Array<JointEdge> getJoints() {
        return body.getJointList();
    }
}
