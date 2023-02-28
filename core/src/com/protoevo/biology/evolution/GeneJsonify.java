package com.protoevo.biology.evolution;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.protoevo.biology.nn.NetworkGenome;
import com.protoevo.utils.FileIO;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class GeneJsonify {

    public static class GeneJson implements Serializable {
        public static long serialVersionUID = 1L;

        public long id;
        public String name;
    }

    public static class GenomeJson implements Serializable {
        public static long serialVersionUID = 1L;

        public List<GeneJson> genes;
        public List<GeneJson> regulators;
    }

//    public static List<GeneJson> getRegulatorsJson(GeneExpressionFunction fn) {
//            List<GeneJson> json = new ArrayList<>();
//        for (GeneExpressionFunction.RegulationNode node : fn.getGeneRegulators().values()) {
//            GeneJson gj = new GeneJson();
//            gj.id = node.getID();
//            gj.name = node.getName();
//            json.add(gj);
//        }
//        return json;
//    }

    public static void toJson(GeneExpressionFunction fn, String outPath) {
        FileIO.writeJson(fn.getGRNGenome(), outPath);
    }
}
