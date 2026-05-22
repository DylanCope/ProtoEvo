package com.protoevo.env;

import com.badlogic.gdx.math.Vector2;

import java.io.Serializable;

/**
 * Smooth 2D current field driving cells around the world. Derived as the
 * curl of a scalar potential ψ, so the resulting flow is divergence-free
 * (cells don't accumulate at "sinks"). Two superimposed travelling waves
 * give a multi-eddy pattern that drifts slowly with sim time, so the
 * flow pattern itself shifts on a long timescale — preventing protozoa
 * from camping permanent dead spots.
 *
 *   ψ(x, y, t) = sin(k₁x + ω₁t) cos(k₁y)
 *              + 0.5 · sin(k₂y + ω₂t) cos(k₂x)
 *
 * Velocity = curl(ψ):
 *   vx = -∂ψ/∂y =  k₁ sin(k₁x + ω₁t) sin(k₁y)
 *                - 0.5 k₂ cos(k₂y + ω₂t) cos(k₂x)
 *   vy =  ∂ψ/∂x =  k₁ cos(k₁x + ω₁t) cos(k₁y)
 *                - 0.5 k₂ sin(k₂y + ω₂t) sin(k₂x)
 *
 * Applied as a Box2D *force* (not impulse) so cells feel drag against the
 * flow and reach terminal velocity rather than being yeeted to infinity.
 */
public class CurrentField implements Serializable {
    public static final long serialVersionUID = 1L;

    private float intensity;
    private float k1, k2;       // spatial frequencies
    private float omega1, omega2; // temporal frequencies

    public CurrentField() {
        this(1.5e-5f, 4f, 0.05f);
    }

    /**
     * @param intensity     overall force scale (per-frame Box2D force)
     * @param spatialScale  characteristic eddy size in world units
     * @param timeRate      how fast the pattern drifts (radians of phase
     *                      per sim-second of advected wave)
     */
    public CurrentField(float intensity, float spatialScale, float timeRate) {
        this.intensity = intensity;
        // Primary eddy size + a half-scale companion at perpendicular
        // orientation. Multi-scale gives the flow more visual interest
        // and prevents the field from locking cells into one circulation.
        this.k1 = 1f / Math.max(spatialScale, 1e-3f);
        this.k2 = 2f * k1;
        this.omega1 = timeRate;
        this.omega2 = 0.7f * timeRate;
    }

    /** Sample the current velocity at (x, y) at sim time t, into `out`. */
    public void sample(float x, float y, float t, Vector2 out) {
        float a1 = k1 * x + omega1 * t;
        float a2 = k2 * y + omega2 * t;

        float vx = (float) (k1 * Math.sin(a1) * Math.sin(k1 * y)
                          - 0.5f * k2 * Math.cos(a2) * Math.cos(k2 * x));
        float vy = (float) (k1 * Math.cos(a1) * Math.cos(k1 * y)
                          - 0.5f * k2 * Math.sin(a2) * Math.sin(k2 * x));

        out.set(vx * intensity, vy * intensity);
    }

    public float getIntensity() { return intensity; }
    public void setIntensity(float v) { intensity = v; }
}
