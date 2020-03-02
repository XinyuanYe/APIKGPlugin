import ASTtriplet.ASTtriplet;
import com.intellij.codeInspection.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class KGInspection extends AbstractBaseJavaLocalInspectionTool {
    private static final Logger LOG = Logger.getInstance(KGInspection.class);
    private final CriQuickFix myQuickFix = new CriQuickFix();

    // Defines the text of the quick fix intention
    public static final String QUICK_FIX_NAME = "SDK: This is a warning! Use KG to fix this!";

    // fixed name
    static final String METHOD_DEFINITION = "MethodDefinition";
    static final String METHOD_BODY = "MethodBody";
    static final String DECLARATION_STMT = "DeclarationStatement";
    static final String NEW_EXPRESSION = "NewExpression";
    static final String RETURN_STMT = "ReturnStatement";
    static final String METHODCALL_EXPRESSION = "MethodCallExpression";
    static final String EXPRESSION_STMT = "ExpressionStatement";
    static final String IF_STMT = "IfStatement";
    static final String IF_BLOCK = "IfBlock";
    static final String ELSE_BLOCK = "ElseBlock";
    static final String BINARY_EXPRESSION = "BinaryExpression";
    static final String LITERAL_EXPRESSION = "LiteralExpression";
    static final String TRY_STATEMENT = "TryStatement";
    static final String CATCH_SECTION = "CatchSection";
    static final String REFERENCE_EXP = "ReferenceExpression";
    static final String REFERENCE_ELEMENT = "ReferenceElement";
    static final String ASSIGNMENT_EXP = "AssignmentExpression";


    // triplet counting
    static int id = 0;
    static int lowest_id = 0;
    static int highest_id = 0;

    static int try_lowest_id = 0;
    static int try_highest_id = 0;

    /**
     * This method is overridden to provide a custom visitor
     * that inspects AST/PSI Tree from Method
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
        return new JavaElementVisitor() {

            /**
             * This string defines the short message shown to a user signaling the inspection
             * found a problem. It reuses a string from the inspections bundle.
             */
            @NonNls
            private final String DESCRIPTION_TEMPLATE = "SDK inspection using KG!";


            // evaluate from method
            @Override
            public void visitMethod(PsiMethod method) {
                super.visitMethod(method);

                // create a list to store all the triplets in this METHOD
                ArrayList<ASTtriplet> astTriplets = new ArrayList<ASTtriplet>();

                // GET method name
                String methodName = method.getName();

                // GET method return type
                String methodType = method.getReturnType().getPresentableText();

                // GET method parameter and its type
                // create a list to store parameters of the method
                ArrayList<String> parameter_list = new ArrayList<>();
                // create a list to store parameters' types of the method
                ArrayList<String> parameterType_list = new ArrayList<>();

                //check if method has parameters
                if (method.hasParameters()) {
                    PsiParameter[] parameters = method.getParameterList().getParameters();
                    for (int i = 0; i < parameters.length; i++) {
                        String parameterName = parameters[i].getName();
                        String parameterType = parameters[i].getType().getPresentableText();
                        parameter_list.add(parameterName);
                        parameterType_list.add(parameterType);
                    }
                }
                // no parameters put UNK
                else {
                    parameter_list.add("UNK");
                    parameterType_list.add("UNK");
                }


                // Triplet format: <{Name:methodName, Return_Type:void, parameter:[], parameter_Type:[]},
                // {Body: next_triplet},
                // {relation:StatementType}>
                ASTtriplet method_astTriplet = new ASTtriplet(id++);
                // record first entity
                method_astTriplet.first_entity.add("Name: " + methodName);
                method_astTriplet.first_entity.add("Return_Type: " + methodType);
                method_astTriplet.first_entity.add("Parameter(s): " + parameter_list.toString());
                method_astTriplet.first_entity.add("Parameter_Type: " + parameterType_list.toString());
                // record second entity
                method_astTriplet.second_entity.add("Body: " + "triplet_" + (id));
                // record third entity
                method_astTriplet.third_entity.add("Relation: " + METHOD_DEFINITION);
                astTriplets.add(method_astTriplet);

                // body of the function
                // check if there is a body first, to avoid NULL
                if (!method.getBody().isEmpty()) {

                    // get all the related_triplets
                    int bodyStatementCount = method.getBody().getStatementCount();

                    lowest_id = id;
                    highest_id = id + bodyStatementCount;

                    ASTtriplet body_astTriplet = new ASTtriplet(id++);

                    // list that stores the related triplets
                    ArrayList<String> related_triplets = new ArrayList<>();

                    for (int i=0; i<bodyStatementCount; i++) {
                        related_triplets.add("triplet_" + (id + i));
                    }

                    // record body triplet
                    body_astTriplet.first_entity.add("Related_triplets: " + related_triplets.toString());
                    body_astTriplet.second_entity.add("End_entity: UNK");
                    body_astTriplet.third_entity.add("Relation: " + METHOD_BODY);
                    astTriplets.add(body_astTriplet);

                    // iterate the body statement
                    PsiStatement[] statements = method.getBody().getStatements();
                    for (int i=0; i<statements.length; i++) {
                        PsiStatement statement = statements[i];
                        whatStatement(astTriplets, statement);
                    }
                }
                // reset id for next method
                id = 0;
                // print all the triplets of this method
                for (ASTtriplet t : astTriplets) {
                    System.out.println(t.toString());
                }
                System.out.println();
            }
        };
    }

    private static void whatStatement(ArrayList<ASTtriplet> astTriplets, PsiStatement statement) {
        // check if the given statement is an Declaration statement
        if (statement instanceof PsiDeclarationStatement) {

            PsiDeclarationStatement declarationStatement = (PsiDeclarationStatement) statement;
            PsiElement[] declaredElements = declarationStatement.getDeclaredElements();

            for (int i=0; i<declaredElements.length; i++) {
                PsiElement declaredElement = declaredElements[i];
                PsiElement[] children = declaredElement.getChildren();
                whatElement(astTriplets, children, DECLARATION_STMT);
            }
        }
        else if (statement instanceof PsiReturnStatement) {
            PsiReturnStatement returnStatement = (PsiReturnStatement) statement;
            PsiElement[] children = returnStatement.getChildren();
            whatElement(astTriplets, children, RETURN_STMT);
        }
        else if (statement instanceof PsiExpressionStatement) {
            PsiExpressionStatement psiExpressionStatement = (PsiExpressionStatement) statement;
            PsiElement[] expressions = psiExpressionStatement.getChildren();
            whatElement(astTriplets, expressions, EXPRESSION_STMT);

        }
//        else if (statement instanceof PsiIfStatement) {
//            // add if statement relation
//            ASTtriplet asTtriplet = new ASTtriplet(id);
//            asTtriplet.first_entity.put("special_entity", "if");
//            Map<String, String> tmp = new HashMap<>();
//            tmp.put("related_triplet", "triplet_" + (++id));
//            asTtriplet.second_entity.add(tmp);
//            asTtriplet.third_entity.put("relation", IF_STMT);
//            astTriplets.add(asTtriplet);
//
//            PsiIfStatement psiIfStatement = (PsiIfStatement) statement;
//            PsiElement[] children = psiIfStatement.getChildren();
//            whatElement(astTriplets, children, IF_STMT);
//
//        }
        else if (statement instanceof PsiTryStatement) {

            PsiTryStatement psiTryStatement = (PsiTryStatement) statement;
            int statementCount = psiTryStatement.getTryBlock().getStatementCount();

            try_lowest_id = id;
            try_highest_id = id + statementCount;

            ASTtriplet try_astTriplet = new ASTtriplet(id++);

            // list that stores the related triplets
            ArrayList<String> related_triplets = new ArrayList<>();

            for (int i=0; i<statementCount; i++) {
                related_triplets.add("triplet_" + (id + i));
            }

            try_astTriplet.first_entity.add("Related_triplets: " + related_triplets.toString());
            try_astTriplet.second_entity.add("End_entity: " + CATCH_SECTION);
            try_astTriplet.third_entity.add("Relation: " + TRY_STATEMENT);
            astTriplets.add(try_astTriplet);

            // get try block directly
            PsiElement[] statements = psiTryStatement.getTryBlock().getChildren();
            whatElement(astTriplets, statements, TRY_STATEMENT);

            id = try_highest_id;

            PsiElement[] catchStmts = psiTryStatement.getChildren();
            whatElement(astTriplets, catchStmts, CATCH_SECTION);
        }
    }

    private static void whatElement(ArrayList<ASTtriplet> astTriplets, PsiElement[] psiElements, String relationType) {

        String psiType = "UNK";
        String psiIdentName = "UNK";
        String referenceElement_name = "UNK";
        String methodCall_name = "UNK";

        Boolean elseSection = false;
        String classType = null;

        for (PsiElement psiElement : psiElements) {

            // TYPE
            if (psiElement instanceof PsiTypeElement) {
                PsiTypeElement psiTypeElement = (PsiTypeElement) psiElement;
                psiType = psiTypeElement.getType().getPresentableText();
            }
            // NAME
            else if (psiElement instanceof PsiIdentifier) {
                PsiIdentifier psiIdentifier = (PsiIdentifier) psiElement;
                psiIdentName = psiIdentifier.getText();

            }
            // DECLARATION STATEMENT
            else if (psiElement instanceof PsiDeclarationStatement) {
                PsiDeclarationStatement declarationStatement = (PsiDeclarationStatement) psiElement;
                PsiElement[] declaredElements = declarationStatement.getDeclaredElements();

                // normally this would be in a try block
                for (int i=0; i<declaredElements.length; i++) {
                    PsiElement declaredElement = declaredElements[i];
                    PsiElement[] children = declaredElement.getChildren();
                    whatElement(astTriplets, children, TRY_STATEMENT);
                }
            }
            // EXPRESSION STATEMENT
            else if (psiElement instanceof PsiExpressionStatement) {
                if (relationType.equals(TRY_STATEMENT)) {
                    PsiExpressionStatement psiExpressionStatement = (PsiExpressionStatement) psiElement;
                    PsiElement[] expressions = psiExpressionStatement.getChildren();
                    whatElement(astTriplets, expressions, TRY_STATEMENT);
                }
                else if (relationType.equals(CATCH_SECTION)) {
                    PsiExpressionStatement psiExpressionStatement = (PsiExpressionStatement) psiElement;
                    PsiElement[] expressions = psiExpressionStatement.getChildren();
                    whatElement(astTriplets, expressions, CATCH_SECTION);
                }
            }
            // REFERENCE EXPRESSION
            else if (psiElement instanceof PsiReferenceExpression) {
                PsiReferenceExpression psiReferenceExpression = (PsiReferenceExpression) psiElement;
                methodCall_name = psiReferenceExpression.getCanonicalText();
                classType = "reference_expression";

            }
            // CODE REFERENCE
            else if (psiElement instanceof PsiJavaCodeReferenceElement) {

                PsiJavaCodeReferenceElement psiJavaCodeReferenceElement = (PsiJavaCodeReferenceElement) psiElement;
                referenceElement_name = psiJavaCodeReferenceElement.getText();
                classType = "reference_element";

            }
            // all the arguments are in EXPRESSION LIST
            else if (psiElement instanceof PsiExpressionList) {
                PsiExpressionList psiExpressionList = (PsiExpressionList) psiElement;
                PsiExpression[] psiExpressions = psiExpressionList.getExpressions();
                if (relationType.equals(METHODCALL_EXPRESSION)) {
                    getArguments(astTriplets, psiExpressions, methodCall_name, relationType, classType);
                }
                else if (relationType.equals(TRY_STATEMENT)) {
                    if (classType.equals("reference_element")) {
                        getArguments(astTriplets, psiExpressions, referenceElement_name, relationType, classType);
                    }
                    else {
                        getArguments(astTriplets, psiExpressions, methodCall_name, relationType, classType);

                    }
                }
                else if (relationType.equals(CATCH_SECTION)) {
                    getArguments(astTriplets, psiExpressions, methodCall_name, relationType, classType);
                }
            }
            // METHOD CALL EXPRESSION
            else if (psiElement instanceof PsiMethodCallExpression) {
                if (relationType.equals(RETURN_STMT)) {

                } else if (relationType.equals(EXPRESSION_STMT)) {
                    PsiMethodCallExpression psiMethodCallExpression = (PsiMethodCallExpression) psiElement;
                    PsiElement[] children = psiMethodCallExpression.getChildren();
                    whatElement(astTriplets, children, METHODCALL_EXPRESSION);
                }
                else if (relationType.equals(TRY_STATEMENT)) {
                    PsiMethodCallExpression psiMethodCallExpression = (PsiMethodCallExpression) psiElement;
                    PsiElement[] children = psiMethodCallExpression.getChildren();
                    whatElement(astTriplets, children, TRY_STATEMENT);
                }
                else if (relationType.equals(CATCH_SECTION)) {
                    PsiMethodCallExpression psiMethodCallExpression = (PsiMethodCallExpression) psiElement;
                    PsiElement[] children = psiMethodCallExpression.getChildren();
                    whatElement(astTriplets, children, CATCH_SECTION);
                }
            }
            // LITERAL
            else if (psiElement instanceof PsiLiteralExpression) {
                PsiLiteralExpression psiLiteralExpression = (PsiLiteralExpression) psiElement;
                String literalExpressionText = psiLiteralExpression.getText();

                ASTtriplet astTriplet = new ASTtriplet(id++);
                // record entity
                astTriplet.first_entity.add("Type: " + psiType);
                astTriplet.first_entity.add("Name: " + psiIdentName);
                astTriplet.second_entity.add("LiteralExpression: " + literalExpressionText);
                astTriplet.third_entity.add("Relation: " + DECLARATION_STMT);
                astTriplets.add(astTriplet);

            }
            // NEW Expression
            else if (psiElement instanceof PsiNewExpression) {
                PsiNewExpression psiNewExpression = (PsiNewExpression) psiElement;
                if (relationType.equals(DECLARATION_STMT)) {
                    ASTtriplet astTriplet = new ASTtriplet(id);
                    astTriplet.first_entity.add("Type: " + psiType);
                    astTriplet.first_entity.add("Name: " + psiIdentName);

                    astTriplet.second_entity.add("Related_triplets: " + "triplet_" + (++try_highest_id));

                    astTriplet.third_entity.add("Relation: " + DECLARATION_STMT);
                    astTriplets.add(astTriplet);

                    PsiElement[] newExpressionChildren = psiNewExpression.getChildren();

                    whatElement(astTriplets, newExpressionChildren, NEW_EXPRESSION);
                }
                else if (relationType.equals(TRY_STATEMENT)) {
                    ASTtriplet astTriplet = new ASTtriplet(id);
                    astTriplet.first_entity.add("Type: " + psiType);
                    astTriplet.first_entity.add("Name: " + psiIdentName);

                    astTriplet.second_entity.add("Related_triplets: " + "triplet_" + (++try_highest_id));

                    astTriplet.third_entity.add("Relation: " + DECLARATION_STMT);
                    astTriplets.add(astTriplet);

                    PsiElement[] newExpressionChildren = psiNewExpression.getChildren();

                    whatElement(astTriplets, newExpressionChildren, TRY_STATEMENT);
                }
                else if (relationType.equals(ASSIGNMENT_EXP)) {
                    ASTtriplet astTriplet = new ASTtriplet(++highest_id);
                    astTriplet.first_entity.add(REFERENCE_EXP + ": " + methodCall_name);
                    astTriplet.second_entity.add("Related_triplets: " + "triplet_" + (++highest_id));
                    astTriplet.third_entity.add("Relation: " + ASSIGNMENT_EXP);
                    astTriplets.add(astTriplet);

                    PsiElement[] newExpressionChildren = psiNewExpression.getChildren();

                    whatElement(astTriplets, newExpressionChildren, ASSIGNMENT_EXP);
                }
            }
            // ASSIGNMENT EXPRESSION
            else if (psiElement instanceof PsiAssignmentExpression) {
                PsiAssignmentExpression psiAssignmentExpression = (PsiAssignmentExpression) psiElement;
                PsiElement[] children = psiAssignmentExpression.getChildren();
                // may need to change id
                whatElement(astTriplets, children, ASSIGNMENT_EXP);
                // change back id
            }
            // CATCH SECTION
            else if (psiElement instanceof PsiCatchSection) {
                PsiCatchSection psiCatchSection = (PsiCatchSection) psiElement;
                psiIdentName = psiCatchSection.getParameter().getName();
                psiType = psiCatchSection.getParameter().getType().getPresentableText();

                ASTtriplet asTtriplet = new ASTtriplet(id++);
                asTtriplet.first_entity.add("Type: " + psiType);
                asTtriplet.first_entity.add("Name: " + psiIdentName);
                asTtriplet.second_entity.add("Related_triplets: " + "triplet_" + (id));
                asTtriplet.third_entity.add("Relation: " + CATCH_SECTION);
                astTriplets.add(asTtriplet);

                PsiElement[] children = psiCatchSection.getCatchBlock().getChildren();
                whatElement(astTriplets, children, CATCH_SECTION);

            }
            else if (psiElement instanceof PsiKeyword) {
                PsiKeyword psiKeyword = (PsiKeyword) psiElement;
                String keyword = psiKeyword.getText().toLowerCase();
                if (keyword.equals("else")) {
                    elseSection = true;
                }
            }
        }
    }


    // GET ARGUMENTS
    private static void getArguments(ArrayList<ASTtriplet> astTriplets, PsiExpression[] psiExpressions, String first_Entity, String relationType, String classType) {
        // no arguments
        if (psiExpressions.length == 0) {
            // assume it always follow a methodcallexpression
            if (relationType.equals(TRY_STATEMENT)) {
                ASTtriplet astTriplet = new ASTtriplet(++id);
                if (classType.equals("reference_expression")) {
                    astTriplet.first_entity.add(REFERENCE_EXP + ": " + first_Entity);
                }
                astTriplet.second_entity.add("Arguments: " + "UNK");
                astTriplet.second_entity.add("Types: " + "UNK");
                astTriplet.third_entity.add("Relation: " + METHODCALL_EXPRESSION);
                astTriplets.add(astTriplet);
            }
            else if (relationType.equals(CATCH_SECTION)) {
                ASTtriplet astTriplet = new ASTtriplet(id++);
                if (classType.equals("reference_expression")) {
                    astTriplet.first_entity.add(REFERENCE_EXP + ": " + first_Entity);
                }
                astTriplet.second_entity.add("Arguments: " + "UNK");
                astTriplet.second_entity.add("Types: " + "UNK");
                astTriplet.third_entity.add("Relation: " + METHODCALL_EXPRESSION);
                astTriplets.add(astTriplet);
            }
        }
        else {
            // create a triplet first
            ASTtriplet astTriplet;
            if (relationType.equals(TRY_STATEMENT)) {
                astTriplet = new ASTtriplet(try_highest_id++);
            }
            else {
                astTriplet = new ASTtriplet(id++);
            }
            for (int i=0; i<psiExpressions.length; i++) {
                PsiExpression psiExpression = psiExpressions[i];
                if (psiExpression instanceof PsiMethodCallExpression) {
                    if (classType.equals("reference_expression")) {
                        astTriplet.first_entity.add(REFERENCE_EXP + ": " + first_Entity);
                    }
                    astTriplet.second_entity.add("Arguments: " + "triplet_" + (id));
                    astTriplet.third_entity.add("Relation: " + METHODCALL_EXPRESSION);
                    astTriplets.add(astTriplet);

                    PsiElement[] children = ((PsiMethodCallExpression)psiExpression).getChildren();
                    whatElement(astTriplets, children, METHODCALL_EXPRESSION);
                }
                else if (psiExpression instanceof PsiLiteralExpression) {
                    String argument = ((PsiLiteralExpression) psiExpression).getText();
                    String type = ((PsiLiteralExpression) psiExpression).getType().getPresentableText();

                    if (classType.equals("reference_element")) {
                        astTriplet.first_entity.add(REFERENCE_ELEMENT + ": " + first_Entity);
                    }
                    else if (classType.equals("reference_expression")) {
                        astTriplet.first_entity.add(REFERENCE_EXP + ": " + first_Entity);
                    }
                    astTriplet.second_entity.add("Arguments: " + argument);
                    astTriplet.second_entity.add("Types: " + type);
                    astTriplet.third_entity.add("Relation: " + NEW_EXPRESSION);
                    astTriplets.add(astTriplet);
                }

            }

        }
    }

//    private static void addEntities(ASTtriplet asTtriplet, String firstEntityKey, String firstEntityValue,
//                                    String thirdEntityKey,String thirdEntityValue) {
//
//        if (asTtriplet.first_entity.isEmpty()) {
//            asTtriplet.first_entity.put(firstEntityKey, firstEntityValue);
//        }
//
//        if (asTtriplet.third_entity.isEmpty()) {
//            asTtriplet.third_entity.put(thirdEntityKey, thirdEntityValue);
//        }
//    }

    private static String transformOPToken(String operation) {
        if (operation.equals("NE")) {
            return "!=";
        }
        else if (operation.equals("EQEQ")) {
            return "==";
        }
        else if (operation.equals("LT")) {
            return "<";
        }
        else if (operation.equals("LE")) {
            return "<=";
        }
        else if (operation.equals("GT")) {
            return ">";
        }
        else if (operation.equals("GE")) {
            return ">=";
        }
        else {
            return operation;
        }
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
         * @return Quick fix short name.
         */
        @NotNull
        @Override
        public String getName() {
            return QUICK_FIX_NAME;
        }

        /**
         * This method manipulates the PSI tree to replace 'a==b' with 'a.equals(b)
         * or 'a!=b' with '!a.equals(b)'
         *
         * @param project    The project that contains the file being edited.
         * @param descriptor A problem found by this inspection.
         */
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {

        }

        @NotNull
        public String getFamilyName() {
            return getName();
        }
    }
}
