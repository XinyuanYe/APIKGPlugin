/*
This class holds the essential information of a API constraint obtained from KG
 */

public class APIConstraint {
    String start, end, check, violation, desc;

    public APIConstraint(String start, String end, String check, String violation, String desc) {
        this.start = start;
        this.end = end;
        this.check = check;
        this.violation = violation;
        this.desc = desc;
    }

    public String getStart() {
        return this.start;
    }

    public String getEnd() {
        return this.end;
    }

    public String getCheck() {
        return this.check;
    }

    public String getViolation() {
        return this.violation;
    }

    public String getDesc() {
        return this.desc;
    }

    @Override
    public String toString() {
        return "start: " + getStart() + " - " + "end: " + getEnd() + ". Check: " + getCheck();
    }
}
