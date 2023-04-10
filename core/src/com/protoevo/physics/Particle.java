package com.protoevo.physics;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.protoevo.biology.CauseOfDeath;
import com.protoevo.core.Statistics;
import com.protoevo.env.ChemicalSolution;
import com.protoevo.env.Environment;
import com.protoevo.env.JointsManager;
import com.protoevo.env.Rock;
import com.protoevo.utils.Colour;
import com.protoevo.utils.Geometry;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Particle implements Shape, Serializable {
    public static long serialVersionUID = 1L;
    protected final long id = UUID.randomUUID().getMostSignificantBits();

    private final Vector2[] boundingBox = new Vector2[]{new Vector2(), new Vector2()};

    private transient Environment environment;
    private transient Body body;
    private transient Fixture dynamicsFixture, sensorFixture;
    private boolean dead = false, disposed = false;
    private double radius = Environment.settings.minParticleRadius.get() * (1 + 2 * Math.random());
    private final Vector2 pos = new Vector2(0, 0);
    private final Vector2 impulseToApply = new Vector2(0, 0);
    private final Vector2 forceToApply = new Vector2(0, 0);
    private final Vector2 vel = new Vector2(0, 0);
    protected final Vector2 tmp = new Vector2(0, 0);
    private float angle, torqueToApply = 0;
    @JsonIgnore
    private final Statistics stats = new Statistics();
    private final Collection<CollisionHandler.Collision> contacts = new ConcurrentLinkedQueue<>();
    private final Collection<Object> interactionObjects = new ConcurrentLinkedQueue<>();
    private CauseOfDeath causeOfDeath = null;
    private boolean requestedDestroyBody = false;

    public Particle() {}

    public Environment getEnv() {
        return environment;
    }

    public ChemicalSolution getChemicalSolution() {
        if (environment == null)
            return null;
        return environment.getChemicalSolution();
    }

    public JointsManager.Joining getJoining(long joiningID) {
        return environment.getJointsManager().getJoining(joiningID);
    }

    public void addToEnv(Environment environment) {
        Vector2 pos = environment.getRandomPosition(this);
        if (pos == null) {
            kill(CauseOfDeath.FAILED_TO_CONSTRUCT);
            return;
        }
        this.pos.set(pos);
        setEnv(environment);
    }

    public void setEnv(Environment environment) {
        this.environment = environment;
        createBody();
        environment.ensureAddedToEnvironment(this);
    }

    public void update(float delta) {
        interactionObjects.removeIf(o -> (o instanceof Particle) && ((Particle) o).isDead());
        contacts.removeIf(c -> (getOther(c) instanceof Particle) && ((Particle) getOther(c)).isDead());
    }

    public float getDampeningFactor() {
        return 1f;
    }

    public void createBody() {
        if (body != null || disposed)
            return;

        CircleShape circle = new CircleShape();
        circle.setRadius((float) radius);

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
        fixtureDef.friction = 0.9f;
        fixtureDef.restitution = 0.2f;
        fixtureDef.filter.categoryBits = ~FixtureCategories.SENSOR;

        dynamicsFixture = body.createFixture(fixtureDef);
        dynamicsFixture.setUserData(this);

        body.setUserData(this);
        body.setLinearDamping(Environment.settings.env.fluidDragDampening.get());
        body.setAngularDamping(5f);
        body.setSleepingAllowed(true);

        body.setTransform(pos, angle);

        circle.dispose();

        if (canPossiblyInteract()) {
            // Create the sensor fixture and attach it to the body
            CircleShape interactionCircle = new CircleShape();
            interactionCircle.setRadius((float) radius / 2f);

            FixtureDef sensorFixtureDef = new FixtureDef();
            sensorFixtureDef.shape = interactionCircle;
            sensorFixtureDef.isSensor = true;
            sensorFixtureDef.filter.categoryBits = FixtureCategories.SENSOR;
            sensorFixtureDef.filter.maskBits = ~FixtureCategories.SENSOR;

            sensorFixture = body.createFixture(sensorFixtureDef);
            sensorFixture.setUserData(this);
            sensorFixture.setSensor(true);

            interactionCircle.dispose();
        }
    }

    /**
     * The category used for the dynamic fixture to determine
     * which sensors will interact with it.
     * @return the category bits
     */
    public short getSensorCategory() {
        return FixtureCategories.SENSOR;
    }

    /**
     * The mask used for the sensor fixture to determine
     * which dynamic fixtures will interact with it.
     * @return the mask bits
     */
    public short getSensorMask() {
        return ~FixtureCategories.SENSOR;
    }

    public Body getBody() {
        return body;
    }

    public void applyForce(Vector2 force) {
        forceToApply.add(force);
    }

    public void applyImpulse(Vector2 impulse) {
        impulseToApply.add(impulse);
    }

    public float getInteractionRange() {
        return 0;
    }

    public void addInteractingObject(Object object) {
        interactionObjects.add(object);
    }

    public void removeInteractingObject(Object object) {
        interactionObjects.remove(object);
    }

    public Collection<Object> getInteractionQueue() {
        return interactionObjects;
    }

    public boolean canPossiblyInteract() {
        return false;
    }

    public void interact(List<Object> interactions) {}

    public void onCollision(CollisionHandler.Collision collision, Particle other) {
        if (getOther(collision) != null)
            contacts.add(collision);
    }

    public void onCollision(CollisionHandler.Collision collision, Rock rock) {
        if (getOther(collision) != null)
            contacts.add(collision);
    }

    public void endContact(Object object) {
        contacts.removeIf(c -> getOther(c).equals(object));
    }

    public Object getOther(CollisionHandler.Collision collision) {
        return collision.objB;
    }

    public void physicsUpdate() {
        if (body != null) {
            if (forceToApply.len2() > 0)
                body.applyForceToCenter(forceToApply, true);
            if (impulseToApply.len2() > 0)
                body.applyLinearImpulse(impulseToApply, body.getWorldCenter(), true);
            if (torqueToApply != 0)
                body.applyTorque(torqueToApply, true);

            vel.set(body.getLinearVelocity());
            pos.set(body.getPosition());
            angle = body.getAngle();
            body.setLinearDamping(getDampeningFactor() * Environment.settings.env.fluidDragDampening.get());

            if (getSpeed() < getRadius() / 50f) {
                body.setLinearVelocity(0, 0);
                body.setAwake(false);
            }

            dynamicsFixture.getShape().setRadius((float) radius);
            float interactionRange = getInteractionRange();
            if (canPossiblyInteract() && interactionRange > getRadius())
                sensorFixture.getShape().setRadius(interactionRange);
        }

        torqueToApply = 0;
        impulseToApply.set(0, 0);

        contacts.removeIf(this::removeCollision);
    }

    private boolean removeCollision(CollisionHandler.Collision collision) {
        if (getOther(collision) == null)
            return true;
        if (getOther(collision) instanceof Particle) {
            Particle other = (Particle) getOther(collision);
            float rr = getRadius() + other.getRadius();
            return other.isDead() || other.getPos().dst2(getPos()) > rr*rr;
        }
        return true;
    }

    public Collection<CollisionHandler.Collision> getContacts() {
        return contacts;
    }

    public double getRadiusDouble() {
        return radius;
    }

    public float getRadius() {
        return (float) radius;
    }

    public void setRadius(double radius) {
        this.radius = Math.max(Environment.settings.minParticleRadius.get(), radius);
        this.radius = Math.min(Environment.settings.maxParticleRadius.get(), this.radius);
    }

    public boolean isPointInside(Vector2 point) {
        float r = getRadius();
        return point.dst2(getPos()) < r*r;
    }

    public void setPos(Vector2 pos) {
        this.pos.set(pos);
        if (body != null)
            body.setTransform(pos, body.getAngle());
    }

    public float getAngle() {
        return angle;
    }

    @Override
    public Vector2 getPos() {
        return pos;
    }

    public Vector2 getVel() {
        return vel;
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

    public double getMass(double r) {
        return Geometry.getCircleArea(r) * getMassDensity();
    }

    public float getMass(float r, float extraMass) {
        return Geometry.getCircleArea(r) * getMassDensity() + extraMass;
    }

    public float getMassDensity() {
        return Environment.settings.cell.basicParticleMassDensity.get();
    }

    @Override
    public boolean pointInside(Vector2 p) {
        return Geometry.isPointInsideCircle(getPos(), getRadius(), p);
    }

    @Override
    public boolean rayCollisions(Vector2[] ray, Intersection[] intersection) {
        Vector2 start = ray[0], end = ray[1];
        float dirX = end.x - start.x, dirY = end.y - start.y;
        Vector2 p = getPos();
        float r = getRadius();

        float a = start.dst2(end);
        float b = 2 * (dirX*(start.x - p.x) + dirY*(start.y - p.y));
        float c = p.len2() + start.len2() - r*r - 2 * p.dot(start);

        float d = b*b - 4*a*c;
        if (d == 0)
            return false;

        float t1 = (float) ((-b + Math.sqrt(d)) / (2*a));
        float t2 = (float) ((-b - Math.sqrt(d)) / (2*a));

        boolean anyCollisions = false;
        if (0 <= t1 && t1 <= 1) {
            intersection[0].point.set(start).lerp(end, t1);
            intersection[0].didCollide = true;
            anyCollisions = true;
        }
        if (0 <= t2 && t2 <= 1) {
            intersection[1].point.set(start).lerp(end, t2);
            intersection[1].didCollide = true;
            anyCollisions = true;
        }
        return anyCollisions;
    }

    @Override
    public Colour getColour() {
        return Colour.WHITE;
    }

    @Override
    public Vector2[] getBoundingBox() {
        float x = getPos().x;
        float y = getPos().y;
        float r = getRadius();
        boundingBox[0].set(x - r, y - r);
        boundingBox[1].set(x + r, y + r);
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

    public Statistics getStats() {
        stats.clear();
        stats.putDistance("Size", getRadius());
        stats.putSpeed("Speed", getSpeed());
        stats.putMass("Total Mass", getMass());
        return stats;
    }

    public boolean isDead() {
        return dead;
    }

    public void kill(CauseOfDeath causeOfDeath) {
        dead = true;
        if (this.causeOfDeath == null)
            this.causeOfDeath = causeOfDeath;
    }

    public void dispose() {
        if (disposed)
            return;
        disposed = true;
        kill(CauseOfDeath.DISPOSED);
        destroyBody();
    }

    public void destroyBody() {
        if (body != null && environment != null) {
            environment.getWorld().destroyBody(body);
            body = null;
        }
        requestedDestroyBody = false;
    }

    public String getPrettyName() {
        return "Particle";
    }

    public Statistics getDebugStats() {
        Statistics stats = new Statistics();
        stats.putDistance("Position X", getPos().x);
        stats.putDistance("Position Y", getPos().y);
        if (body != null) {
            stats.put("Inertia", body.getInertia());
            stats.putCount("Num Joints", body.getJointList().size);
            stats.putCount("Num Fixtures", body.getFixtureList().size);
            stats.putBoolean("Is Sleeping", body.isAwake());
        }
        stats.putBoolean("Is Dead", dead);
        stats.putCount("Num Contacts", contacts.size());
        stats.putCount("Num Interactions", interactionObjects.size());
        stats.putCount("Local Count", environment.getLocalCount(this));
        stats.putCount("Local Cap", environment.getLocalCapacity(this));
        return stats;
    }

    public Array<JointEdge> getJoints() {
        if (body != null)
            return body.getJointList();
        return null;
    }

    public CauseOfDeath getCauseOfDeath() {
        return causeOfDeath;
    }

    public float getArea() {
        return Geometry.getCircleArea(getRadius());
    }

    public void applyTorque(float v) {
        torqueToApply += v;
    }

    public void requestDestroyBody() {
        this.requestedDestroyBody = true;
    }

    public boolean didRequestDestroyBody() {
        return requestedDestroyBody;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }

    public long getId() {
        return id;
    }
}
