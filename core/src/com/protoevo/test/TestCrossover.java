package com.protoevo.test;

import com.protoevo.biology.cells.Protozoan;
import com.protoevo.biology.evolution.Evolvable;

public class TestCrossover {

    public static void main(String[] args) {
        Protozoan p1 = Evolvable.createNew(Protozoan.class);
        Protozoan p2 = Evolvable.createNew(Protozoan.class);

        Protozoan child = Evolvable.createChild(
                Protozoan.class, p1.getGeneExpressionFunction(), p2.getGeneExpressionFunction());

        System.out.println(child);
    }
}
