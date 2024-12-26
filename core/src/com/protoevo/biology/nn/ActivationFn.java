package com.protoevo.biology.nn;

import com.badlogic.gdx.math.MathUtils;
import com.protoevo.maths.Functions;

import java.io.Serializable;
import java.util.function.Function;

public abstract class ActivationFn implements Serializable, Function<Float, Float> {
    public static final long serialVersionUID = 1L;

    public static class SigmoidFn extends ActivationFn {

        public SigmoidFn() {
            this.name = "Sigmoid";
        }

        @Override
        public Float apply(Float z) {
            return Functions.sigmoid(z);
        }
    }

    public static class LinearFn extends ActivationFn {
        public LinearFn() {
            this.name = "Linear";
        }

        @Override
        public Float apply(Float z) {
            return z;
        }
    }

    public static class TanhFn extends ActivationFn {
        public TanhFn() {
            this.name = "Tanh";
        }

        @Override
        public Float apply(Float z) {
            return Functions.tanh(z);
        }
    }

    public static class StepFn extends ActivationFn {
        public StepFn() {
            this.name = "Step";
        }

        @Override
        public Float apply(Float z) {
            return Functions.step(z);
        }
    }

    public static class ReLuFn extends ActivationFn {
        public ReLuFn() {
            this.name = "ReLu";
        }

        @Override
        public Float apply(Float z) {
            return Functions.relu(z);
        }
    }

    public static class SinFn extends ActivationFn {
        public SinFn() {
            this.name = "Sin";
        }

        @Override
        public Float apply(Float z) {
            return MathUtils.sin(z);
        }
    }

    public static class GaussianFn extends ActivationFn {
        public GaussianFn() {
            this.name = "Gaussian";
        }

        @Override
        public Float apply(Float z) {
            return Functions.gaussian(z);
        }
    }

//    public static final ActivationFn SIGMOID = new ActivationFn(Functions::sigmoid, "Sigmoid");
//    public static final ActivationFn LINEAR = new ActivationFn(Functions::linear, "Linear");
//    public static final ActivationFn TANH = new ActivationFn(Functions::tanh, "Tanh");
//    public static final ActivationFn STEP = new ActivationFn(Functions::step, "Step");
//    public static final ActivationFn RELU = new ActivationFn(Functions::relu, "ReLu");
//    public static final ActivationFn SIN = new ActivationFn(MathUtils::sin, "Sin");
//    public static final ActivationFn GAUSSIAN = new ActivationFn(Functions::gaussian, "Gaussian");

    public static final ActivationFn SIGMOID = new ActivationFn.SigmoidFn();
    public static final ActivationFn LINEAR = new ActivationFn.LinearFn();
    public static final ActivationFn TANH = new ActivationFn.TanhFn();
    public static final ActivationFn STEP = new ActivationFn.StepFn();
    public static final ActivationFn RELU = new ActivationFn.ReLuFn();
    public static final ActivationFn SIN = new ActivationFn.SinFn();
    public static final ActivationFn GAUSSIAN = new ActivationFn.GaussianFn();

    public static final ActivationFn[] hiddenActivationFunctions = new ActivationFn[]{
            SIGMOID, LINEAR, TANH, STEP, RELU, SIN, GAUSSIAN
    };

    public static class OutputActivationFn extends ActivationFn {
        private float min, max;

        public OutputActivationFn() {}

        public OutputActivationFn(String name, float min, float max) {
            this.name = name;
            this.min = min;
            this.max = max;
        }

        @Override
        public Float apply(Float z) {
            return Functions.cyclicalLinearRemap(z, -1, 1, min, max);
        }
    }

//    public static ActivationFn getOutputMapper(float min, float max) {
//        return new ActivationFn(
//                z -> Functions.cyclicalLinearRemap(z, -1, 1, min, max),
//                String.format("CyclicalLinearRemap[[%.3f, %.3f] -> [-1, 1]]", min, max)
//        );
//    }

    public static ActivationFn getOutputMapper(float min, float max) {
        String name = String.format("CyclicalLinearRemap[[%.3f, %.3f] -> [-1, 1]]", min, max);
        return new OutputActivationFn(name, min, max);
    }

//    public static ActivationFn getInputMapper(float min, float max) {
//        return new ActivationFn(
//                z -> Functions.cyclicalLinearRemap(z, min, max, -1, 1),
//                String.format("CyclicalLinearRemap[[-1, 1] -> [%.3f, %.3f]]", min, max)
//        );
//    }

    public static class BooleanInputFn extends ActivationFn {
        @Override
        public Float apply(Float z) {
            return z > 0 ? 1f : -1f;
        }
    }

    public static ActivationFn getBooleanInputMapper() {
//        return new ActivationFn(z -> z > 0 ? 1f : -1f);
        return new BooleanInputFn();
    }

//    @JsonIgnore
//    private final SerializableFunction<Float, Float> function;
    protected String name;

//    public ActivationFn(SerializableFunction<Float, Float> function) {
//        this.function = function;
//    }
//
//    public ActivationFn(SerializableFunction<Float, Float> function, String name) {
//        this.function = function;
//        this.name = name;
//    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ActivationFn)
            return name.equals(((ActivationFn) obj).name);
        return false;
    }

    public static ActivationFn randomActivation() {
        return hiddenActivationFunctions[(int) (Math.random() * hiddenActivationFunctions.length)];
    }

//    @Override
//    public Float apply(Float z) {
//        return function.apply(z);
//    }

    public abstract Float apply(Float z);
}
