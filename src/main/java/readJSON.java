//import com.alibaba.fastjson.JSON;
//import com.alibaba.fastjson.JSONArray;
//import com.alibaba.fastjson.JSONObject;
//
//import java.io.*;
//import java.util.ArrayList;
//
//public class readJSON {
//
//    public static String readJsonFile(String fileName) {
//        String jsonStr = "";
//        try {
//            File jsonFile = new File(fileName);
//            FileReader fileReader = new FileReader(jsonFile);
//            Reader reader = new InputStreamReader(new FileInputStream(jsonFile),"utf-8");
//            int ch = 0;
//            StringBuffer sb = new StringBuffer();
//            while ((ch = reader.read()) != -1) {
//                sb.append((char) ch);
//            }
//            fileReader.close();
//            reader.close();
//            jsonStr = sb.toString();
//            return jsonStr;
//        } catch (IOException e) {
//            e.printStackTrace();
//            return null;
//        }
//    }
////
////    public static void read_MethodNode() {
////        String path = "/Users/xinyuan/IdeaProjects/my_gradle_plugin/src/main/java/KGdata/modified_methodNode.json";
////        String s = readJsonFile(path);
////        JSONObject jobj = JSON.parseObject(s);
////        JSONArray nodes = jobj.getJSONArray("methodNode");
////        //构建JSONArray数组
////        for (int i = 0 ; i < nodes.size();i++){
////            JSONObject key = (JSONObject)nodes.get(i);
////            String node_name = (String)key.get("node_name");
////            String type = (String)key.get("type");
////            int idx = ((Integer)key.get("idx"));
////            String desc=((String)key.get("Desc"));
////            System.out.println(node_name);
////            System.out.println(type);
////            System.out.println(idx);
////            System.out.println(desc);
////            System.out.println();
////        }
////    }
////
////    public static void read_classNode() {
////        String path = "/Users/xinyuan/IdeaProjects/my_gradle_plugin/src/main/java/KGdata/modified_classNode.json";
////        String s = readJsonFile(path);
////        JSONObject jobj = JSON.parseObject(s);
////        JSONArray nodes = jobj.getJSONArray("classNode");
////        //构建JSONArray数组
////        for (int i = 0 ; i < nodes.size();i++){
////            JSONObject key = (JSONObject)nodes.get(i);
////            String node_name = (String)key.get("node_name");
////            String type = (String)key.get("type");
////            int idx = ((Integer)key.get("idx"));
////            String desc=((String)key.get("Desc"));
////            System.out.println(node_name);
////            System.out.println(type);
////            System.out.println(idx);
////            System.out.println(desc);
////            System.out.println();
////        }
////    }
////
////    public static void read_constructorNode() {
////        String path = "/Users/xinyuan/IdeaProjects/my_gradle_plugin/src/main/java/KGdata/modified_constructorNode.json";
////        String s = readJsonFile(path);
////        JSONObject jobj = JSON.parseObject(s);
////        JSONArray nodes = jobj.getJSONArray("constructorNode");
////        //构建JSONArray数组
////        for (int i = 0 ; i < nodes.size();i++){
////            JSONObject key = (JSONObject)nodes.get(i);
////            String node_name = (String)key.get("node_name");
////            String type = (String)key.get("type");
////            int idx = ((Integer)key.get("idx"));
////            String desc=((String)key.get("Desc"));
////            System.out.println(node_name);
////            System.out.println(type);
////            System.out.println(idx);
////            System.out.println(desc);
////            System.out.println();
////        }
////    }
////
////    public static void read_fieldNode() {
////        String path = "/Users/xinyuan/IdeaProjects/my_gradle_plugin/src/main/java/KGdata/modified_fieldNode.json";
////        String s = readJsonFile(path);
////        JSONObject jobj = JSON.parseObject(s);
////        JSONArray nodes = jobj.getJSONArray("fieldNode");
////        //构建JSONArray数组
////        for (int i = 0 ; i < nodes.size();i++){
////            JSONObject key = (JSONObject)nodes.get(i);
////            String node_name = (String)key.get("node_name");
////            String type = (String)key.get("type");
////            int idx = ((Integer)key.get("idx"));
////            String desc=((String)key.get("Desc"));
////            System.out.println(node_name);
////            System.out.println(type);
////            System.out.println(idx);
////            System.out.println(desc);
////            System.out.println();
////        }
////    }
////
////    public static void read_node_relation() {
////        String path = "/Users/xinyuan/IdeaProjects/my_gradle_plugin/src/main/java/KGdata/modified_node_relation.json";
////        String s = readJsonFile(path);
////        JSONObject jobj = JSON.parseObject(s);
////        JSONArray nodes = jobj.getJSONArray("node_relation");
////        //构建JSONArray数组
////        for (int i = 0 ; i < nodes.size();i++){
////            JSONObject key = (JSONObject)nodes.get(i);
////            int start_id = (Integer) key.get("start_id");
////            int end_id = (Integer)key.get("end_id");
////            String relation =((String)key.get("relation"));
////            System.out.println(start_id);
////            System.out.println(end_id);
////            System.out.println(relation);
////            System.out.println();
////        }
////    }
////
////    public static void read_parametersNode() {
////        String path = "/Users/xinyuan/IdeaProjects/my_gradle_plugin/src/main/java/KGdata/modified_parametersNode.json";
////        String s = readJsonFile(path);
////        JSONObject jobj = JSON.parseObject(s);
////        JSONArray nodes = jobj.getJSONArray("parametersNode");
////        //构建JSONArray数组
////        for (int i = 0 ; i < nodes.size();i++){
////            JSONObject key = (JSONObject)nodes.get(i);
////            int idx = ((Integer)key.get("idx"));
////            String node_name = (String)key.get("node_name");
////            String desc=((String)key.get("Dec"));
////            System.out.println(idx);
////            System.out.println(node_name);
////            System.out.println(desc);
////            System.out.println();
////        }
////    }
//
//    public static ArrayList<APIConstraint> checkConstraint(String target) {
//        String path = "/Users/xinyuan/IdeaProjects/my_gradle_plugin/src/main/java/constraint.json";
//        String s = readJsonFile(path);
//        JSONObject jobj = JSON.parseObject(s);
//        JSONArray nodes = jobj.getJSONArray("constraint");
//        //构建JSONArray数组
//        ArrayList<APIConstraint> constrains = new ArrayList<>();
//        for (int i = 0 ; i < nodes.size();i++){
//            JSONObject key = (JSONObject)nodes.get(i);
//            String start = (String)key.get("start");
//            String end = ((String)key.get("end"));
//            if (start.equals(target)) {
//                JSONObject constraint = key.getJSONObject("constraint");
//                String check = (String) constraint.get("check");
//                String violation = (String) constraint.get("Violation");
//                String desc = (String) constraint.get("Desc");
//                APIConstraint apiConstraint = new APIConstraint(start, end, check, violation, desc);
//                constrains.add(apiConstraint);
//            }
//        }
//        return constrains;
//    }
//
////    public static void changeFormat() throws IOException {
////        Path path = Paths.get("/Users/xinyuan/IdeaProjects/my_gradle_plugin/src/main/java/KGdata/modified_parametersNode.json");
////        Charset charset = StandardCharsets.UTF_8;
////
////        String content = new String(Files.readAllBytes(path), charset);
////        content = content.replaceAll("}", "},");
////        Files.write(path, content.getBytes(charset));
////    }
//
//
//}
//
//
