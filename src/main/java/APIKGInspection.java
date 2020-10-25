import com.intellij.codeInspection.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.*;
import java.util.List;

public class APIKGInspection extends AbstractBaseJavaLocalInspectionTool {


    private static final Logger LOG = Logger.getInstance("#com.intellij.codeInspection.APIKGInspection");
    private final CriQuickFix myQuickFix = new CriQuickFix();

    // Defines the text of the quick fix intention
    public static final String QUICK_FIX_NAME = "SDK: " + InspectionsBundle.message("inspection.comparing.references.use.quickfix");

    private HashMap<Integer, MethodCallExp> methodCallExpMap = new HashMap<>();

    /**
     * This method is overridden to provide a custom visitor
     * that inspects methodCall with knowledge graph
     * The visitor must not be recursive and must be thread-safe.
     *
     * @param holder     object for visitor to register problems found.
     * @param isOnTheFly true if inspection was run in non-batch mode
     * @return non-null visitor for this inspection.
     * @see JavaElementVisitor
     */
    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {

        // clear the hashmap before run the visitor so that hashmap is always updated
        methodCallExpMap.clear();
        return new JavaElementVisitor() {

            /**
             * This string defines the short message shown to a user signaling the inspection
             * found a problem. It reuses a string from the inspections bundle.
             */
            @NonNls
            private final String DESCRIPTION_TEMPLATE = "SDK " + InspectionsBundle.message("inspection.comparing.references.problem.descriptor");


            // we do detection in this method
            @Override
            public void visitMethod(PsiMethod psiMethod) {
                super.visitMethod(psiMethod);
                for (MethodCallExp m : methodCallExpMap.values()) {
//                    System.out.println("Checking " + m.toString());
                    detectAPIMisuse(m, holder);
                }
                System.out.println();
            }

            /**
             * Evaluate method call expression for possible API misuse.
             *
             * @param psiMethodCallExpression The expression to be evaluated.
             */
            @Override
            public void visitMethodCallExpression(PsiMethodCallExpression psiMethodCallExpression) {
                super.visitMethodCallExpression(psiMethodCallExpression);

                PsiElement context = psiMethodCallExpression.getContext();

                PsiMethod rootFunction = findRootFunction(context);
                String functionBelong = rootFunction.getName();

                if (context instanceof PsiIfStatement || context instanceof PsiWhileStatement) {
                    return;
                }

                PsiReferenceExpression referenceExpression = psiMethodCallExpression.getMethodExpression();

                String methodName = referenceExpression.getQualifiedName();

                int lineNumber = getLineNumber(psiMethodCallExpression);

                MethodCallExp methodCallExp = new MethodCallExp(psiMethodCallExpression, methodName, lineNumber, functionBelong);
                methodCallExpMap.put(lineNumber, methodCallExp);

            }

            /**
             * Evaluate new expression for possible API misuse.
             *
             * @param psiNewExpression The expression to be evaluated.
             */
            @Override
            public void visitNewExpression(PsiNewExpression psiNewExpression) {
                super.visitNewExpression(psiNewExpression);

                String name = psiNewExpression.getClassReference().getQualifiedName();

                int lineNumber = getLineNumber(psiNewExpression);

                PsiElement context = psiNewExpression.getContext();
                PsiMethod rootFunction = findRootFunction(context);
                String functionBelong = rootFunction.getName();

                MethodCallExp methodCallExp = new MethodCallExp(psiNewExpression, name, lineNumber, functionBelong);
                methodCallExpMap.put(lineNumber, methodCallExp);
            }

        };
    }

    // perform detection, bless us!
    private void detectAPIMisuse(MethodCallExp target, ProblemsHolder holder) {

        PsiElement psiElement = target.getElement();

        String targetName = target.getName();
        // remove qualifier
        targetName = targetName.split("\\.")[targetName.split("\\.").length - 1];

        int target_at_line = target.getLineNumber();

        String functionBelong = target.getFunctionBelong();

        ArrayList<APIConstraint> constraints = checkConstraint(targetName);

        // it has no API constraints, Hooray!
        if (constraints.isEmpty()) {
            return;
        }

        // iterate through the constraints and check if any is violated
        for (APIConstraint constraint : constraints) {
            String start = constraint.getStart();
            String end = constraint.getEnd();
            String check = constraint.getCheck();
            String violation = constraint.getViolation();
            String desc = constraint.getDesc();

            String conditionOperator = conditionChecking(check);

            // check precede call-order i.e. start is our target and end must be called after it
            if (targetName.equals(end) && check.equals("precede")) {

                // TODO iterator.remove != jpanel.remove. Better naming needed to fix this problem
                if (target.getName().contains("panel")) {
                    continue;
                }

                for (MethodCallExp methodCallExp : methodCallExpMap.values()) {
                    if (methodCallExp.getName().split("\\.")[1].equals(start) && methodCallExp.getFunctionBelong().equals(functionBelong
                    )) {
                        int line_number = methodCallExp.getLineNumber();

                        // the precede call-order is maintained
                        if (line_number <= target_at_line) {
                           return;
                        }
                    }
                }
                // the required start method is not present
                generateAPICaveatReport(psiElement, desc, violation, holder);
            }

            // check follow call-order i.e. start is our target and end must be called after it
            if (targetName.equals(start) && check.equals("follow")) {

                // TODO iterator.remove != jpanel.remove. Better naming needed to fix this problem
                if (target.getName().contains("iterator")) {
                    continue;
                }

                for (MethodCallExp methodCallExp : methodCallExpMap.values()) {
                    if (methodCallExp.getName().split("\\.")[1].equals(end) && methodCallExp.getFunctionBelong().equals(functionBelong
                    )) {
                        int line_number = methodCallExp.getLineNumber();

                        // the follow call-order is maintained
                        if (line_number >= target_at_line) {
                            return;
                        }
                    }
                }
                // the required end method is not present
                generateAPICaveatReport(psiElement, desc, violation, holder);
            }

            // check condition-checking i.e. if a value-checking or state-checking is present before start
            if (targetName.equals(start) && conditionOperator != null) {

                String conditionToBeCheck = check.split(conditionOperator)[0];
                String stateToBe = check.split(conditionOperator)[1];

                PsiElement result = checkConditionCheckingPresence(psiElement);

                // no condition-checking present
                if (result == null || result instanceof PsiMethod) {
                    // the required end method is not presented
                    generateAPICaveatReport(psiElement, desc, violation, holder);
                }
                // condition-checking present, check if it is the correct condition checking required
                else {
                    if (result instanceof PsiIfStatement) {
                        PsiIfStatement ifStatement = (PsiIfStatement) result;
                        PsiElement psiCondition = ifStatement.getCondition();
                        if (psiCondition instanceof PsiMethodCallExpression) {
                            String condition = psiCondition.getText();
                            condition = condition.replace("()", "");
                            condition = condition.split("\\.")[condition.split("\\.").length - 1];
                            if (!condition.equals(conditionToBeCheck)) {
                                generateAPICaveatReport(psiElement, desc, violation, holder);
                            }
                        }
                        // check if the correct condition-checking is present and is in the correct state
                        // e.g. required checking file.exists(), !file.exists() is incorrect even though exists is present
                        else if (psiCondition instanceof PsiPrefixExpression) {
                            PsiPrefixExpression psiPrefixExpression = (PsiPrefixExpression) psiCondition;
                            String state = (psiPrefixExpression.getText().charAt(0) == '!') ? "false" : "true";
                            PsiMethodCallExpression psiMethodCallExpression = (PsiMethodCallExpression) psiPrefixExpression.getOperand();
                            String condition = psiMethodCallExpression.getText();
                            condition = condition.replace("()", "");
                            condition = condition.split("\\.")[condition.split("\\.").length - 1];
                            if (!condition.equals(conditionToBeCheck) || !state.equals(stateToBe)) {
                                generateAPICaveatReport(psiElement, desc, violation, holder);
                            }
                        }
                    }
                    else {
                        PsiWhileStatement whileStatement = (PsiWhileStatement) result;
                        PsiElement psiCondition = whileStatement.getCondition();
                        if (psiCondition instanceof PsiMethodCallExpression) {
                            String condition = psiCondition.getText();
                            condition = condition.replace("()", "");
                            condition = condition.split("\\.")[condition.split("\\.").length - 1];
                            if (!condition.equals(conditionToBeCheck)) {
                                generateAPICaveatReport(psiElement, desc, violation, holder);
                            }
                        }
                        // check if the correct condition-checking is present and is in the correct state
                        // e.g. required checking file.exists(), !file.exists() is incorrect even though exists is present
                        else if (psiCondition instanceof PsiPrefixExpression) {
                            PsiPrefixExpression psiPrefixExpression = (PsiPrefixExpression) psiCondition;
                            String state = (psiPrefixExpression.getText().charAt(0) == '!') ? "false" : "true";
                            PsiMethodCallExpression psiMethodCallExpression = (PsiMethodCallExpression) psiPrefixExpression.getOperand();
                            String condition = psiMethodCallExpression.getText();
                            condition = condition.replace("()", "");
                            condition = condition.split("\\.")[condition.split("\\.").length - 1];
                            if (!condition.equals(conditionToBeCheck) || !state.equals(stateToBe)) {
                                generateAPICaveatReport(psiElement, desc, violation, holder);
                            }
                        }
                    }
                }
            }

            // check if try-catch condition
            if (targetName.equals(start) && check.equals("within try-catch")) {
                Boolean withinTryCatch = checkTryCatchPresence(psiElement);
                if (!withinTryCatch) {
                    // it is not within a try-catch
                    generateAPICaveatReport(psiElement, desc, violation, holder);
                }
            }
        }
    }

    // generate API misuse warning report and display it
    private void generateAPICaveatReport(PsiElement psiElement, String desc, String violation, ProblemsHolder holder) {

        // prevents generating duplicate API misuse report
        if (checkIfReportExisted(psiElement, desc, violation, holder)) {
            return;
        }

        if (violation == null) {
            holder.registerProblem(psiElement, desc, myQuickFix);
        }
        else {
            holder.registerProblem(psiElement, desc + ";\n if violated, throws " + violation, myQuickFix);
        }
    }

    // check if a API misuse report has already been generated for the PsiElement to prevent duplicate reports
    private boolean checkIfReportExisted(PsiElement psiElement, String desc, String violation, ProblemsHolder holder) {
        List<ProblemDescriptor> registeredProblems = holder.getResults();
        String description = (violation == null) ? desc : desc + ";\n if violated, throws " + violation;
        if (!registeredProblems.isEmpty()) {
            for (ProblemDescriptor pd : registeredProblems) {
                PsiElement existedPsiElement = pd.getPsiElement();
                String existedDescription = pd.toString();
                if (psiElement.isEquivalentTo(existedPsiElement) && description.equals(existedDescription)) {
                    return true;
                }
            }
        }
        return false;
    }

    // Given a methodcall that needs to be within a TRY-CATCH, check if it is within
    private boolean checkTryCatchPresence(PsiElement psiElement) {
        PsiElement context = psiElement.getContext();
        if (context instanceof PsiTryStatement) {
            return true;
        }
        else if (context == null || (context instanceof PsiMethod)) {
            return false;
        }
        else {
            return checkTryCatchPresence(context);
        }
    }

    // Given a methodcall that needs a condition-checking, check if it is in a IF/WHILE STMT
    private PsiElement checkConditionCheckingPresence(PsiElement psiElement) {
        PsiElement context = psiElement.getContext();
        if ((context instanceof PsiIfStatement) || (context instanceof PsiWhileStatement)) {
            return context;
        }
        else if (context == null || (context instanceof PsiMethod)) {
            return null;
        }
        else {
            return checkConditionCheckingPresence(context);
        }
    }

    // see if the check component of the constraint is a condition-checking and return corresponding operator
    // otherwise, return null which means that it is not a condition checking
    private String conditionChecking(String check) {
        if (check.contains("<=")) {
            return "<=";
        }
        else if (check.contains(">=")) {
            return ">=";
        }
        else if (check.contains("==")) {
            return "==";
        }
        else if (check.contains("<")) {
            return "<";
        }
        else if (check.contains(">")) {
            return ">";
        }
        return null;
    }

    // get the line number of the code in the inspecting Java file
    private int getLineNumber(PsiElement psiElement) {
        int offset = psiElement.getTextOffset();
        return StringUtil.offsetToLineNumber(psiElement.getContainingFile().getText(), offset) + 1;
    }

    // finds the root function of a method so that we know which function does it belongs to
    private PsiMethod findRootFunction(PsiElement psiElement) {
        PsiElement context = psiElement.getContext();
        if (context == null) {
            System.out.println("NULL found: " + psiElement.getText());
        }
        if (context != null && !(context instanceof PsiMethod)) {
            return findRootFunction(context);
        }
        else {
            return (PsiMethod) context;
        }
    }

    // read JSON file
    public static String readJsonFile(String fileName) {
        String jsonStr = "";
        try {
            File jsonFile = new File(fileName);
            FileReader fileReader = new FileReader(jsonFile);
            Reader reader = new InputStreamReader(new FileInputStream(jsonFile),"utf-8");
            int ch = 0;
            StringBuffer sb = new StringBuffer();
            while ((ch = reader.read()) != -1) {
                sb.append((char) ch);
            }
            fileReader.close();
            reader.close();
            jsonStr = sb.toString();
            return jsonStr;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Given a method element, find all the API constraints associated to it
    public static ArrayList<APIConstraint> checkConstraint(String target) {
        String path = "/Users/xinyuan/IdeaProjects/my_gradle_plugin/src/main/java/constraint.json";
        String s = readJsonFile(path);
        JSONObject jobj = JSON.parseObject(s);
        JSONArray nodes = jobj.getJSONArray("constraint");
        //构建JSONArray数组
        ArrayList<APIConstraint> constrains = new ArrayList<>();
        for (int i = 0 ; i < nodes.size();i++){
            JSONObject key = (JSONObject)nodes.get(i);
            String start = (String)key.get("start");
            String end = ((String)key.get("end"));
            if (start.equals(target)) {
                JSONObject constraint = key.getJSONObject("constraint");
                String check = (String) constraint.get("check");
                String violation = (String) constraint.get("Violation");
                String desc = (String) constraint.get("Desc");
                APIConstraint apiConstraint = new APIConstraint(start, end, check, violation, desc);
                constrains.add(apiConstraint);
            }
            if (end != null && end.equals(target)) {
                JSONObject constraint = key.getJSONObject("constraint");
                String check = (String) constraint.get("check");
                String violation = (String) constraint.get("Violation");
                String desc = (String) constraint.get("Desc");
                APIConstraint apiConstraint = new APIConstraint(start, end, check, violation, desc);
                constrains.add(apiConstraint);
            }
        }
        return constrains;
    }

    /**
     * This class provides a solution to inspection problem expressions by manipulating
     * the PSI tree
     */
    private static class CriQuickFix implements LocalQuickFix {

        /**
         * Returns a partially localized string for the quick fix intention.
         * Used by the test code for this plugin.
         *
         * @return Quick fix short condition.
         */
        @NotNull
        @Override
        public String getName() {
            return QUICK_FIX_NAME;
        }

        /**
         * This method manipulates the PSI tree to resolve the problem
         *
         *
         * @param project    The project that contains the file being edited.
         * @param descriptor A problem found by this inspection.
         */
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // TODO
        }

        @NotNull
        public String getFamilyName() {
            return getName();
        }
    }
}


