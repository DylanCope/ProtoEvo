package com.protoevo.physics.box2d;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.protoevo.biology.CauseOfDeath;
import com.protoevo.core.Statistics;
import com.protoevo.env.Environment;
import com.protoevo.env.Rock;
import com.protoevo.physics.Collision;
import com.protoevo.physics.FixtureCategories;
import com.protoevo.physics.Joining;
import com.protoevo.physics.Particle;
import com.protoevo.maths.Geometry;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Box2DParticle extends Particle implements Serializable {

    public static long serialVersionUID = 1L;
    protected final long id = UUID.randomUUID().getMostSignificantBits();

    private final Vector2[] boundingBox = new Vector2[]{new Vector2(), new Vector2()};

    private Object userData;
    private final Map<Long, Long> joiningIds = new ConcurrentHashMap<>();  // maps cell id to joining id
    private transient Body body;
    private transient Fixture dynamicsFixture, sensorFixture;
    private boolean dead = false, disposed = false;
    private double radius = Environment.settings.minParticleRadius.get() * (1 + 2 * Math.random());
    private float interactionRadius = 0f;
    private final Vector2 pos = new Vector2(0, 0);
    private final Vector2 impulseToApply = new Vector2(0, 0);
    private final Vector2 forceToApply = new Vector2(0, 0);
    private final Vector2 vel = new Vector2(0, 0);
    protected final Vector2 tmp = new Vector2(0, 0);
    private float angle, torqueToApply = 0;
    @JsonIgnore
    private final Statistics stats = new Statistics();
    private final Collection<Collision> contacts = new ConcurrentLinkedQueue<>();
    private final Collection<Object> interactionObjects = new ConcurrentLinkedQueue<>();
    private CauseOfDeath causeOfDeath = null;
    private boolean requestedDestroyBody = false;

    public Box2DParticle(Box2DPhysics physics) {
        super(physics);
        createBody();
    }

    public Object getUserData() {
        return userData;
    }

    public <T> T getUserData(Class<T> type) {
        if (userData == null)
            throw new NullPointerException("User data is null");
        if (!type.isInstance(userData))
            throw new ClassCastException("User data is not of type " + type.getName());
        return type.cast(userData);
    }

    @Override
    public void setUserData(Object userData) {
        this.userData = userData;
    }

    public Optional<Joining> getJoining(long joiningID) {
        return physics.getJointsManager().getJoining(joiningID);
    }

    public void update(float delta) {
        interactionObjects.removeIf(o -> (o instanceof Box2DParticle) && ((Box2DParticle) o).isDead());
        contacts.removeIf(c -> (getOther(c) instanceof Box2DParticle) && ((Box2DParticle) getOther(c)).isDead());
    }

    public void requestJointRemoval(Joining joining) {
        physics.getJointsManager().requestJointRemoval(joining);
    }

    public int getNumAttachedParticles() {
        return joiningIds.size();
    }

    public float getDampeningFactor() {
        if (getNumAttachedParticles() == 0 || getVel().len2() < 1e-12f)
            return 1f;

        float k = 0;
        float speed = getSpeed();
        for (long joiningId : joiningIds.values()) {
            Optional<Particle> maybeOther = physics.getJointsManager()
                    .getJoining(joiningId).flatMap(joining -> joining.getOther(this));
            if (maybeOther.isPresent()) {
                Particle otherParticle = maybeOther.get();
                tmp.set(otherParticle.getPos()).sub(getPos()).nor();
                k += tmp.dot(getVel()) / speed;
            }
            if (k >= 1)
                return 0;
        }

        return MathUtils.clamp(1 - k, 0, 1);
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
        body = ((Box2DPhysics) physics).getWorld().createBody(bodyDef);

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
    }

    @Override
    public void setCanInteractAtRange() {
        super.setCanInteractAtRange();
        createInteractionFixture();
    }

    private void createInteractionFixture() {
        if (body == null)
            return;
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

    @Override
    public void rebuildTransientFields() {
        createBody();
        if (canInteractAtRange())
            createInteractionFixture();
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

    public Vector2 getForce() {
        return forceToApply;
    }

    public Vector2 getImpulse() {
        return impulseToApply;
    }

    public float getTorque() {
        return torqueToApply;
    }

    public void setRangedInteractionRadius(float radius) {
        interactionRadius = radius;
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

    public void interact(List<Object> interactions) {}

    public void onCollision(Collision collision, Box2DParticle other) {

        if (other.isPointInside(getPos())) {
            kill(CauseOfDeath.SUFFOCATION);
            return;
        }

        if (getOther(collision) != null)
            contacts.add(collision);
    }

    public void onCollision(Collision collision, Rock rock) {

        if (rock.pointInside(getPos())) {
            kill(CauseOfDeath.SUFFOCATION);
            return;
        }

        if (getOther(collision) != null)
            contacts.add(collision);
    }

    public void endContact(Object object) {
        contacts.removeIf(c -> getOther(c).equals(object));
    }

    public Object getOther(Collision collision) {
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
            if (rangedInteractionsEnabled && interactionRadius > getRadius())
                sensorFixture.getShape().setRadius(interactionRadius);
        }

        torqueToApply = 0;
        impulseToApply.set(0, 0);

        contacts.removeIf(this::removeCollision);
    }

    private boolean removeCollision(Collision collision) {
        if (getOther(collision) == null)
            return true;
        if (getOther(collision) instanceof Box2DParticle) {
            Box2DParticle other = (Box2DParticle) getOther(collision);
            float rr = getRadius() + other.getRadius();
            return other.isDead() || other.getPos().dst2(getPos()) > rr*rr;
        }
        return true;
    }

    public Collection<Collision> getContacts() {
        return contacts;
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

    @Override
    public void setAngle(float angle) {
        this.angle = angle;
        if (body != null)
            body.setTransform(body.getPosition(), angle);
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

    public float getMass() {
        return (float) getMassIfRadius(getRadius());
    }

    public double getMassIfRadius(double r) {
        return Geometry.getCircleArea(r) * getMassDensity();
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

    @Override
    public void setVel(float vx, float vy) {
        body.setLinearVelocity(vx, vy);
    }

    @Override
    public void setVel(Vector2 vel) {
        body.setLinearVelocity(vel);
    }

    @Override
    public void setAngularVel(float angularVel) {
        body.setAngularVelocity(angularVel);
    }

    public void destroyBody() {
        if (body != null) {
            ((Box2DPhysics) physics).getWorld().destroyBody(body);
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
        stats.put("Dampening Factor", getDampeningFactor());
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

    public Map<Long, Long> getJoiningIds() {
        return joiningIds;
    }
}
