package ASTtriplet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ASTtriplet {

    private String name;
    private int id;

    public Map<String, String> first_entity;
    public ArrayList<Map<String, String>> second_entity;
    public Map<String, String> third_entity;

    public ASTtriplet(int id) {
        this.name = "triplet_" + id;
        this.id = id;
        this.first_entity = new HashMap<>();
        this.second_entity = new ArrayList<>();
        this.third_entity = new HashMap<>();
    }

    public String getName() {
        return this.name;
    }

    public int getID() {
        return this.id;
    }

    public String toString() {
        String output = "";
        for (String name: first_entity.keySet()){
            String key = name;
            String value =first_entity.get(name);
            output += "{" + key + ":" + value + "}, ";
        }
        String tmp = "[";
        int length = second_entity.size();
        for (int i=0; i<length; i++) {
            String tmp_1 = "";
            for (String name: second_entity.get(i).keySet()) {
                String key = name;
                String value = second_entity.get(i).get(name);
                if (i != length - 1) {
                    if (key.contains(":")) {
                        tmp_1 += "{" + key + ", " + value + "}, ";
                    }
                    else {
                        tmp_1 += "{" + key + ": " + value + "}, ";
                    }
                }
                else {
                    if (key.contains(":")) {
                        tmp_1 += "{" + key + ", " + value + "}], ";
                    }
                    else {
                        tmp_1 += "{" + key + ": " + value + "}], ";
                    }
                }
            }
            tmp += tmp_1;
        }
        output += tmp;

        for (String name: third_entity.keySet()){
            String key = name;
            String value = third_entity.get(name);
            output += "{" + key + ":" + value + "}";
        }

        return name + ": <" + output + ">";
    }


}
