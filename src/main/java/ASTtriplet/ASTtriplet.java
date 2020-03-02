package ASTtriplet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ASTtriplet {

    private String name;
    private int id;

    public ArrayList<String> first_entity;
    public ArrayList<String> second_entity;
    public ArrayList<String> third_entity;

    public ASTtriplet(int id) {
        this.name = "triplet_" + id;
        this.id = id;
        this.first_entity = new ArrayList<>();
        this.second_entity = new ArrayList<>();
        this.third_entity = new ArrayList<>();
    }

    public String getName() {
        return this.name;
    }

    public int getID() {
        return this.id;
    }

    public String toString() {
        String output = "";
        // first entity
        int first_entity_size = first_entity.size();
        output += "{";
        for (int i=0; i<first_entity_size; i++) {
            if (i != first_entity_size - 1) {
                output += first_entity.get(i) + ", ";
            }
            else {
                output += first_entity.get(i);

            }
        }
        output += "}, ";
        // second entity
        int second_entity_size = second_entity.size();
        output += "{";
        for (int i=0; i<second_entity_size; i++) {
            if (i != second_entity_size - 1) {
                output += second_entity.get(i) + ", ";
            }
            else {
                output += second_entity.get(i);

            }
        }
        output += "}, ";
        // third entity
        int third_entity_size = third_entity.size();
        output += "{";
        for (int i=0; i<third_entity_size; i++) {
            if (i != third_entity_size - 1) {
                output += third_entity.get(i) + ", ";
            }
            else {
                output += third_entity.get(i);

            }
        }
        output += "}";
        // final output string
        return name + ": <" + output + ">";
    }


}
