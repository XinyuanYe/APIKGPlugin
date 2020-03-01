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

    // triplet counting
    static int id = 1;
    static int lowest_id = 1;
    static int highest_id = 1;

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
                ArrayList<ASTtriplet> asTtriplets = new ArrayList<ASTtriplet>();

                // GET method name
                String methodName = method.getName();

                // GET method parameter and its type
                // create a list to store parameters of the method
                ArrayList<Map<String, String>> mapArrayList = new ArrayList<>();
                //check if method has parameters
                if (method.hasParameters()) {
                    PsiParameter[] parameters = method.getParameterList().getParameters();
                    for (int i = 0; i < parameters.length; i++) {
                        String parameterName = parameters[i].getName();
                        String parameterType = parameters[i].getType().getPresentableText();
                        Map<String,String> tmp = new HashMap<>();
                        tmp.put("parameter:"+parameterName, "type:"+parameterType);
                        mapArrayList.add(tmp);
                    }
                }
                // no parameters put UNK
                else {
                    Map<String,String> tmp = new HashMap<>();
                    tmp.put("parameter:" + "UNK", "type:" + "UNK");
                    mapArrayList.add(tmp);
                }

                // GET method return type
                String methodType = method.getReturnType().getPresentableText();

                // Triplet format: <{method:methodName}, [{parameter:parameterName, type:parameterType}...], {relation:StatementType}>

                ASTtriplet method_Triplet = new ASTtriplet(id++);
                method_Triplet.first_entity.put("method", methodName);
                method_Triplet.second_entity = mapArrayList;
                method_Triplet.third_entity.put("relation", METHOD_DEFINITION);
                asTtriplets.add(method_Triplet);


                // body of the function
                // check if there is a body first, to avoid NULL
                if (!method.getBody().isEmpty()) {
                    PsiStatement[] statements = method.getBody().getStatements();
                    for (int i=0; i<statements.length; i++) {
                        PsiStatement statement = statements[i];
                        whatStatement(asTtriplets, statement);
                    }

                }
                id = 1;
                for (ASTtriplet t : asTtriplets) {
                    System.out.println(t.toString());
                }
                System.out.println();
            }
        };
    }

    private static void whatStatement(ArrayList<ASTtriplet> asTtriplets, PsiStatement statement) {
        // check if the given statement is an Declaration statement
        if (statement instanceof PsiDeclarationStatement) {

            PsiDeclarationStatement declarationStatement = (PsiDeclarationStatement) statement;
            PsiElement[] declaredElements = declarationStatement.getDeclaredElements();

            for (int i=0; i<declaredElements.length; i++) {
                PsiElement declaredElement = declaredElements[i];
                PsiElement[] children = declaredElement.getChildren();
                whatElement(asTtriplets, children, DECLARATION_STMT);
            }
        }
        else if (statement instanceof PsiReturnStatement) {
            PsiReturnStatement returnStatement = (PsiReturnStatement) statement;
            PsiElement[] children = returnStatement.getChildren();
            whatElement(asTtriplets, children, RETURN_STMT);
        }
        else if (statement instanceof PsiExpressionStatement) {
            PsiExpressionStatement psiExpressionStatement = (PsiExpressionStatement) statement;
            PsiElement[] expressions = psiExpressionStatement.getChildren();
            whatElement(asTtriplets, expressions, EXPRESSION_STMT);

        }
        else if (statement instanceof PsiIfStatement) {
            // add if statement relation
            ASTtriplet asTtriplet = new ASTtriplet(id);
            asTtriplet.first_entity.put("special_entity", "if");
            Map<String, String> tmp = new HashMap<>();
            tmp.put("related_triplet", "triplet_" + (++id));
            asTtriplet.second_entity.add(tmp);
            asTtriplet.third_entity.put("relation", IF_STMT);
            asTtriplets.add(asTtriplet);

            PsiIfStatement psiIfStatement = (PsiIfStatement) statement;
            PsiElement[] children = psiIfStatement.getChildren();
            whatElement(asTtriplets, children, IF_STMT);

        }
        else if (statement instanceof PsiTryStatement) {

            PsiTryStatement psiTryStatement = (PsiTryStatement) statement;

            int statementCount = psiTryStatement.getTryBlock().getStatementCount();

            lowest_id = id;
            highest_id = id + statementCount;

            ASTtriplet asTtriplet = new ASTtriplet(id++);
            asTtriplet.first_entity.put("special_entity", "Try");
            Map<String, String> tmp = new HashMap<>();
            String related_triplet = "";
            // get all the triplets that are related to this Try statement
            // i.e. the content of Try
            for (int i=0; i<statementCount; i++) {
                if (i != statementCount - 1) {
                    related_triplet += "triplet_" + (id + i) + ", ";
                }
                else {
                    related_triplet += "triplet_" + (id + i);

                }
            }
            tmp.put("related_triplet", related_triplet);
            asTtriplet.second_entity.add(tmp);
            asTtriplet.third_entity.put("relation", TRY_STATEMENT);
            asTtriplets.add(asTtriplet);

            PsiElement[] statements = psiTryStatement.getTryBlock().getChildren();
            whatElement(asTtriplets, statements, TRY_STATEMENT);

            id = highest_id;

            PsiElement[] catchStmts = psiTryStatement.getChildren();
            whatElement(asTtriplets, catchStmts, CATCH_SECTION);
        }
    }

    private static void whatElement(ArrayList<ASTtriplet> asTtriplets, PsiElement[] psiElements, String relationType) {

        String psiType = "UNK";
        String psiIdentName = "UNK";
        String referenceElement_name = "UNK";
        String methodCall_name = "UNK";

        Boolean elseSection = false;

        for (PsiElement psiElement : psiElements) {

            if (psiElement instanceof PsiTypeElement) {
                PsiTypeElement psiTypeElement = (PsiTypeElement) psiElement;
                psiType = psiTypeElement.getType().getPresentableText();
            }
            else if (psiElement instanceof PsiIdentifier) {
                PsiIdentifier psiIdentifier = (PsiIdentifier) psiElement;
                psiIdentName = psiIdentifier.getText();

            }
            else if (psiElement instanceof PsiDeclarationStatement) {
                PsiDeclarationStatement declarationStatement = (PsiDeclarationStatement) psiElement;
                PsiElement[] declaredElements = declarationStatement.getDeclaredElements();

                for (int i=0; i<declaredElements.length; i++) {
                    PsiElement declaredElement = declaredElements[i];
                    PsiElement[] children = declaredElement.getChildren();
                    whatElement(asTtriplets, children, TRY_STATEMENT);
                }
            }
            else if (psiElement instanceof PsiExpressionStatement) {
                if (relationType.equals(TRY_STATEMENT)) {
                    PsiExpressionStatement psiExpressionStatement = (PsiExpressionStatement) psiElement;
                    PsiElement[] expressions = psiExpressionStatement.getChildren();
                    whatElement(asTtriplets, expressions, TRY_STATEMENT);
                }
                else if (relationType.equals(CATCH_SECTION)) {
                    PsiExpressionStatement psiExpressionStatement = (PsiExpressionStatement) psiElement;
                    PsiElement[] expressions = psiExpressionStatement.getChildren();
                    whatElement(asTtriplets, expressions, CATCH_SECTION);
                }
                else if (relationType.equals(IF_BLOCK)) {
                    PsiExpressionStatement psiExpressionStatement = (PsiExpressionStatement) psiElement;
                    PsiElement[] expressions = psiExpressionStatement.getChildren();
                    whatElement(asTtriplets, expressions, IF_BLOCK);
                }
                else if (relationType.equals(ELSE_BLOCK)) {
                    PsiExpressionStatement psiExpressionStatement = (PsiExpressionStatement) psiElement;
                    PsiElement[] expressions = psiExpressionStatement.getChildren();
                    whatElement(asTtriplets, expressions, ELSE_BLOCK);
                }
            }
            // the statement has a new expression
            // which this triplet needs to go deeper
            else if (psiElement instanceof PsiNewExpression) {
                if (relationType.equals(TRY_STATEMENT)) {
                    ASTtriplet asTtriplet = new ASTtriplet(id);
                    asTtriplet.first_entity.put(psiType, psiIdentName);
                    Map<String, String> tmp = new HashMap<>();
                    tmp.put("related_triplet", "triplet_" + (++highest_id));
                    asTtriplet.second_entity.add(tmp);
                    asTtriplet.third_entity.put("relation", DECLARATION_STMT);
                    asTtriplets.add(asTtriplet);

                    PsiNewExpression psiNewExpression = (PsiNewExpression) psiElement;
                    PsiElement[] newExpressionChildren = psiNewExpression.getChildren();

                    whatElement(asTtriplets, newExpressionChildren, TRY_STATEMENT);
                }
                else if (relationType.equals(NEW_EXPRESSION)) {
                    ASTtriplet asTtriplet = new ASTtriplet(id);
                    asTtriplet.first_entity.put(psiType, psiIdentName);
                    Map<String, String> tmp = new HashMap<>();
                    tmp.put("related_triplet", "triplet_" + (++id));
                    asTtriplet.second_entity.add(tmp);
                    asTtriplet.third_entity.put("relation", relationType);
                    asTtriplets.add(asTtriplet);

                    PsiNewExpression psiNewExpression = (PsiNewExpression) psiElement;
                    PsiElement[] newExpressionChildren = psiNewExpression.getChildren();

                    whatElement(asTtriplets, newExpressionChildren, NEW_EXPRESSION);
                }
            }
            else if (psiElement instanceof PsiReferenceExpression) {
                PsiReferenceExpression psiReferenceExpression = (PsiReferenceExpression) psiElement;
                methodCall_name = psiReferenceExpression.getCanonicalText();

            }
            else if (psiElement instanceof PsiJavaCodeReferenceElement) {

                PsiJavaCodeReferenceElement referenceElement = (PsiJavaCodeReferenceElement) psiElement;
                referenceElement_name = referenceElement.getText();

            }
            else if (psiElement instanceof PsiExpressionList) {
                PsiExpressionList psiExpressionList = (PsiExpressionList) psiElement;
                PsiExpression[] psiExpressions = psiExpressionList.getExpressions();
                if (relationType.equals(NEW_EXPRESSION)) {
                    getArguments(asTtriplets, psiExpressions, referenceElement_name, relationType);
                }
                else if (relationType.equals(METHODCALL_EXPRESSION)) {
                    getArguments(asTtriplets, psiExpressions, methodCall_name, relationType);
                }
                else if (relationType.equals(TRY_STATEMENT)) {
                    getArguments(asTtriplets, psiExpressions, methodCall_name, TRY_STATEMENT);
                }
                else if (relationType.equals(CATCH_SECTION)) {
                    getArguments(asTtriplets, psiExpressions, methodCall_name, CATCH_SECTION);
                }
                else if (relationType.equals(IF_BLOCK)) {
                    getArguments(asTtriplets, psiExpressions, methodCall_name, IF_BLOCK);
                }
            }
            else if (psiElement instanceof PsiMethodCallExpression) {
                if (relationType.equals(RETURN_STMT)) {
                    ASTtriplet asTtriplet = new ASTtriplet(id);
                    asTtriplet.first_entity.put("special_entity", "return");
                    Map<String, String> tmp = new HashMap<>();
                    tmp.put("related_triplet", "triplet_" + (++id));
                    asTtriplet.second_entity.add(tmp);
                    asTtriplet.third_entity.put("relation", relationType);
                    asTtriplets.add(asTtriplet);

                    PsiMethodCallExpression psiMethodCallExpression = (PsiMethodCallExpression) psiElement;
                    PsiElement[] children = psiMethodCallExpression.getChildren();

                    whatElement(asTtriplets, children, METHODCALL_EXPRESSION);

                }
                else if (relationType.equals(EXPRESSION_STMT)) {

                    PsiMethodCallExpression psiMethodCallExpression = (PsiMethodCallExpression) psiElement;
                    PsiElement[] children = psiMethodCallExpression.getChildren();

                    whatElement(asTtriplets, children, METHODCALL_EXPRESSION);
                }
                else if (relationType.equals(BINARY_EXPRESSION)) {
                    PsiMethodCallExpression psiMethodCallExpression = (PsiMethodCallExpression) psiElement;
                    PsiElement[] children = psiMethodCallExpression.getChildren();

                    whatElement(asTtriplets, children, METHODCALL_EXPRESSION);
                }
                else if (relationType.equals(TRY_STATEMENT)) {
                    PsiMethodCallExpression psiMethodCallExpression = (PsiMethodCallExpression) psiElement;
                    PsiElement[] children = psiMethodCallExpression.getChildren();

                    whatElement(asTtriplets, children, TRY_STATEMENT);
                }
                else if (relationType.equals(CATCH_SECTION)) {
                    PsiMethodCallExpression psiMethodCallExpression = (PsiMethodCallExpression) psiElement;
                    PsiElement[] children = psiMethodCallExpression.getChildren();

                    whatElement(asTtriplets, children, CATCH_SECTION);
                }
                else if (relationType.equals(IF_BLOCK)) {
                    PsiMethodCallExpression psiMethodCallExpression = (PsiMethodCallExpression) psiElement;
                    PsiElement[] children = psiMethodCallExpression.getChildren();

                    whatElement(asTtriplets, children, IF_BLOCK);
                }
            }
            else if (psiElement instanceof PsiBinaryExpression) {
                if (relationType.equals(IF_STMT)) {

                    PsiBinaryExpression psiBinaryExpression = (PsiBinaryExpression) psiElement;
                    String rightOperand = psiBinaryExpression.getROperand().getText();
                    String operation = psiBinaryExpression.getOperationTokenType().toString();
                    ASTtriplet asTtriplet = new ASTtriplet(id);
                    asTtriplet.first_entity.put("related_triplet","triplet_" + (++id));
                    Map<String, String> tmp = new HashMap<>();
                    tmp.put("value_literals", rightOperand);
                    asTtriplet.second_entity.add(tmp);
                    asTtriplet.third_entity.put("relation", BINARY_EXPRESSION + " - " + transformOPToken(operation));
                    asTtriplets.add(asTtriplet);

                    PsiElement[] child = new PsiElement[1];
                    child[0] = psiBinaryExpression.getLOperand();
                    whatElement(asTtriplets, child, BINARY_EXPRESSION);
                }
            }
            else if (psiElement instanceof PsiLiteralExpression) {

                PsiLiteralExpression psiLiteralExpression = (PsiLiteralExpression) psiElement;
                String text = psiLiteralExpression.getText();

                ASTtriplet asTtriplet = new ASTtriplet(id++);
                asTtriplet.first_entity.put(psiType,psiIdentName);
                Map<String, String> tmp = new HashMap<>();
                tmp.put("content", text);
                asTtriplet.second_entity.add(tmp);
                asTtriplet.third_entity.put("relation", relationType);
                asTtriplets.add(asTtriplet);

            }
            else if (psiElement instanceof PsiCatchSection) {
                PsiCatchSection psiCatchSection = (PsiCatchSection) psiElement;
                psiIdentName = psiCatchSection.getParameter().getName();
                psiType = psiCatchSection.getParameter().getType().getPresentableText();
                ASTtriplet asTtriplet = new ASTtriplet(id);
                asTtriplet.first_entity.put(psiType,psiIdentName);
                Map<String, String> tmp = new HashMap<>();
                tmp.put("related_entity", "triplet_" + (++id));
                asTtriplet.second_entity.add(tmp);
                asTtriplet.third_entity.put("relation", relationType);
                asTtriplets.add(asTtriplet);

                PsiElement[] children = psiCatchSection.getCatchBlock().getChildren();
                whatElement(asTtriplets, children, CATCH_SECTION);
            }
            // this is if-else block, needs to be fixed
            else if (psiElement instanceof PsiBlockStatement) {
                if (relationType.equals(IF_STMT)) {
                    // now is IF content
                    if (!elseSection) {
                        PsiBlockStatement psiBlockStatement = (PsiBlockStatement) psiElement;
                        // no content, maybe all is comment or unfinished
                        if (psiBlockStatement.getCodeBlock().isEmpty()) {
                            ASTtriplet asTtriplet = new ASTtriplet(id++);
                            asTtriplet.first_entity.put("UNK","UNK");
                            Map<String, String> tmp = new HashMap<>();
                            tmp.put("UNK", "UNK");
                            asTtriplet.second_entity.add(tmp);
                            asTtriplet.third_entity.put("relation", IF_BLOCK);
                            asTtriplets.add(asTtriplet);
                        }
                        else {
                            PsiElement[] children = psiBlockStatement.getCodeBlock().getChildren();
                            whatElement(asTtriplets, children, IF_BLOCK);
                        }
                    }
                    // now is ELSE CONTENT
                    else {
                        PsiBlockStatement psiBlockStatement = (PsiBlockStatement) psiElement;
                        // no content
                        if (psiBlockStatement.getCodeBlock().isEmpty()) {
                            ASTtriplet asTtriplet = new ASTtriplet(id++);
                            asTtriplet.first_entity.put("UNK","UNK");
                            Map<String, String> tmp = new HashMap<>();
                            tmp.put("UNK", "UNK");
                            asTtriplet.second_entity.add(tmp);
                            asTtriplet.third_entity.put("relation", ELSE_BLOCK);
                            asTtriplets.add(asTtriplet);
                        }
                        else {
                            PsiElement[] children = psiBlockStatement.getCodeBlock().getChildren();
                            whatElement(asTtriplets,children,ELSE_BLOCK);
                        }

                    }
                }
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
    private static void getArguments(ArrayList<ASTtriplet> asTtriplets, PsiExpression[] psiExpressions, String first_Entity, String relationType) {
        if (psiExpressions.length == 0) {
            if (relationType.equals(NEW_EXPRESSION)) {
                ASTtriplet asTtriplet = new ASTtriplet(id++);
                asTtriplet.first_entity.put("class", first_Entity);
                Map<String, String> tmp = new HashMap<>();
                tmp.put("parameter:" + "UNK", "type:" + "UNK");
                asTtriplet.second_entity.add(tmp);
                asTtriplet.third_entity.put("relation", relationType);
                asTtriplets.add(asTtriplet);
            }
            else if (relationType.equals(METHODCALL_EXPRESSION)) {

                ASTtriplet asTtriplet = new ASTtriplet(id++);
                asTtriplet.first_entity.put("method", first_Entity);
                Map<String, String> tmp = new HashMap<>();
                tmp.put("parameter:" + "UNK", "type:" + "UNK");
                asTtriplet.second_entity.add(tmp);
                asTtriplet.third_entity.put("relation", relationType);
                asTtriplets.add(asTtriplet);
            }
            // assume METHODCALL EXPRESSION
            else if (relationType.equals(TRY_STATEMENT)) {
                ASTtriplet asTtriplet = new ASTtriplet(++id);
                asTtriplet.first_entity.put("method", first_Entity);
                Map<String, String> tmp = new HashMap<>();
                tmp.put("parameter:" + "UNK", "type:" + "UNK");
                asTtriplet.second_entity.add(tmp);
                asTtriplet.third_entity.put("relation", METHODCALL_EXPRESSION);
                asTtriplets.add(asTtriplet);
            }
            else if (relationType.equals(CATCH_SECTION)) {
                ASTtriplet asTtriplet = new ASTtriplet(id++);
                asTtriplet.first_entity.put("method", first_Entity);
                Map<String, String> tmp = new HashMap<>();
                tmp.put("parameter:" + "UNK", "type:" + "UNK");
                asTtriplet.second_entity.add(tmp);
                asTtriplet.third_entity.put("relation", METHODCALL_EXPRESSION);
                asTtriplets.add(asTtriplet);
            }
        }
        else {
            // create a triplet first
            ASTtriplet asTtriplet;
            if (relationType.equals(TRY_STATEMENT)) {
                asTtriplet = new ASTtriplet(highest_id++);

            }
            else {
                asTtriplet = new ASTtriplet(id++);
            }
            Map<String, String> tmp = new HashMap<>();
            for (int i = 0; i < psiExpressions.length; i++) {
                PsiExpression psiExpression = psiExpressions[i];
                if (relationType.equals(METHODCALL_EXPRESSION)) {
                    if (psiExpression instanceof PsiReferenceExpression) {
                        String argument = ((PsiReferenceExpression) psiExpression).getCanonicalText();
                        String type = ((PsiReferenceExpression) psiExpression).getType().getPresentableText();
                        tmp.put("parameter:" + argument, "type:" + type);
                        addEntities(asTtriplet, "method", first_Entity, "relation", relationType);

                    } else if (psiExpression instanceof PsiLiteralExpression) {
                        String argument = ((PsiLiteralExpression) psiExpression).getText();
                        String type = ((PsiLiteralExpression) psiExpression).getType().getPresentableText();
                        tmp.put("parameter:" + argument, "type:" + type);
                        addEntities(asTtriplet, "method", first_Entity, "relation", relationType);

                    } else if (psiExpression instanceof PsiNewExpression) {
                        String argument = ((PsiNewExpression) psiExpression).getText();
                        String type = ((PsiNewExpression) psiExpression).getType().getPresentableText();
                        tmp.put("parameter:" + argument, "type:" + type);
                        addEntities(asTtriplet, "method", first_Entity, "relation", relationType);
                    }
                    else if (psiExpression instanceof PsiMethodCallExpression) {
                        String argument = ((PsiMethodCallExpression) psiExpression).getText();
                        String type = ((PsiMethodCallExpression) psiExpression).getType().getPresentableText();
                        tmp.put("parameter:" + argument, "type:" + type);
                        addEntities(asTtriplet, "method", first_Entity, "relation", relationType);
                    }
                } else if (relationType.equals(NEW_EXPRESSION)) {
                    if (psiExpression instanceof PsiReferenceExpression) {
                        String argument = ((PsiReferenceExpression) psiExpression).getCanonicalText();
                        String type = ((PsiReferenceExpression) psiExpression).getType().getPresentableText();
                        tmp.put("parameter:" + argument, "type:" + type);
                        addEntities(asTtriplet, "class", first_Entity, "relation", relationType);
                    }
                }
                else if (relationType.equals(TRY_STATEMENT)) {
                    // assume is NEW expression
                    if (psiExpression instanceof PsiLiteralExpression) {
                        String argument = ((PsiLiteralExpression) psiExpression).getText();
                        String type = ((PsiLiteralExpression) psiExpression).getType().getPresentableText();
                        tmp.put("parameter:" + argument, "type:" + type);
                        addEntities(asTtriplet, "class", first_Entity, "relation", NEW_EXPRESSION);
                    }
                }
//                else if (relationType.equals(IF_BLOCK)) {
//                    String argument = ((PsiLiteralExpression) psiExpression).getText();
//                    String type = ((PsiLiteralExpression) psiExpression).getType().getPresentableText();
//                    tmp.put("parameter:" + argument, "type:" + type);
//                    addEntities(asTtriplet, "class", first_Entity, "relation", IF_BLOCK);
//                }
            }
            asTtriplet.second_entity.add(tmp);
            asTtriplets.add(asTtriplet);
        }
    }

    private static void addEntities(ASTtriplet asTtriplet, String firstEntityKey, String firstEntityValue,
                                    String thirdEntityKey,String thirdEntityValue) {

        if (asTtriplet.first_entity.isEmpty()) {
            asTtriplet.first_entity.put(firstEntityKey, firstEntityValue);
        }

        if (asTtriplet.third_entity.isEmpty()) {
            asTtriplet.third_entity.put(thirdEntityKey, thirdEntityValue);
        }
    }

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
