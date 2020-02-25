import KGtree.KGnode;
import com.intellij.codeInspection.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

import static com.siyeh.ig.psiutils.ExpressionUtils.isNullLiteral;

public class KGInspection extends AbstractBaseJavaLocalInspectionTool {
    private static final Logger LOG = Logger.getInstance(KGInspection.class);
    private final CriQuickFix myQuickFix = new CriQuickFix();

    // Defines the text of the quick fix intention
    public static final String QUICK_FIX_NAME = "SDK: This is a warning! Use KG to fix this!";

    // count for precise recording
    static int methodCount = 1;
    static int declarationStatementCount = 1;
    static int expressionStatementCount = 1;
    static int forStatementCount = 1;
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

                // a simple data structure to store the information
                // kgnode
                KGnode kgTree = KGnode.newNode("root", null);
                // first level of KG tree is ROOT - ignore this level
                // second level of KG tree is all the method, this is kgTree.children

//                String methodDeclaration = "Method (" + methodCount + ")";
                String methodDeclaration = "Method";

                // create a KGnode object for this methodDeclaration
                KGnode kg_methodDeclaration = KGnode.newNode(methodDeclaration, method.getName());
                // add to KG tree
                kgTree.children.add(kg_methodDeclaration);
                // get the index of current methodDeclaration, so its corresponding elements
                // can be added correctly in the KG tree
                int kg_method_index = kgTree.children.indexOf(kg_methodDeclaration);

                // get the current method node, and add all its information
                // under this node
                KGnode currentMethod = kgTree.children.get(kg_method_index);

                // NAME, RETURN_TYPE, PARAMETERS, and BODY is one level below current method node
                // i.e. currentMethod.children

                // create a KGnode object for NAME of current method
                KGnode kg_methodName = KGnode.newNode("NAME", null);
                currentMethod.children.add(kg_methodName);
                int kg_methodName_index = currentMethod.children.indexOf(kg_methodName);
                // get method name
                String methodName = method.getName();
                // add method name under the NAME node
                currentMethod.children.get(kg_methodName_index).children.add(KGnode.newNode(methodName, null));

                // create a KGnode object for RETURN_TYPE of current method
                KGnode kg_returnType = KGnode.newNode("RETURN_TYPE", null);
                currentMethod.children.add(kg_returnType);
                int kg_returnType_index = currentMethod.children.indexOf(kg_returnType);
                // get method return type
                String methodType = method.getReturnType().getPresentableText();
                currentMethod.children.get(kg_returnType_index).children.add(KGnode.newNode(methodType, null));

                // create a KGnode object for PARAMETERS of current method
                KGnode kg_methodParameters = KGnode.newNode("PARAMETERS", null);
                currentMethod.children.add(kg_methodParameters);
                int kg_methodParameters_index = currentMethod.children.indexOf(kg_methodParameters);

                //check if method has parameters
                if (method.hasParameters()) {
                    // get parameters types
                    PsiParameter[] parameters = method.getParameterList().getParameters();
                    for (int i = 0; i < parameters.length; i++) {
                        String parameterName = parameters[i].getName();
                        KGnode kg_parameterName = KGnode.newNode(parameterName, null);

                        currentMethod.children.get(kg_methodParameters_index).children.add(kg_parameterName);
                        int kg_parameterName_index = currentMethod.children.get(kg_methodParameters_index).children.indexOf(kg_parameterName);

                        String parameterType = parameters[i].getType().getPresentableText();
                        currentMethod.children.get(kg_methodParameters_index).children.get(kg_parameterName_index).children.add(KGnode.newNode(parameterType, null));
                    }
                }
                else {
                    // add NULL under PARAMETERS node
                    currentMethod.children.get(kg_methodParameters_index).children.add(KGnode.newNode("NULL", null));
                }

                // create a KGnode object for BODY of current method
                KGnode kg_body = KGnode.newNode("BODY", null);
                currentMethod.children.add(kg_body);
                int kg_body_index = currentMethod.children.indexOf(kg_body);

                // get the current method's BODY node, and add all its information
                // under this BODY node
                KGnode currentBody = currentMethod.children.get(kg_body_index);

                // body of the function
                // check if there is a body first, to avoid NULL
                if (!method.getBody().isEmpty()) {
                    PsiStatement[] statements = method.getBody().getStatements();
                    for (int i=0; i<statements.length; i++) {
                        PsiStatement statement = statements[i];
                        whatStatement(currentBody, statement);

                    }

                }
                // increment the count so the naming for next method is correct
                // reset declarationStatementCount for new method
                methodCount += 1;
                declarationStatementCount = 1;
                expressionStatementCount = 1;
                KGnode.LevelOrderTraversal(kgTree);
                holder.registerProblem(method,DESCRIPTION_TEMPLATE,myQuickFix);
            }
        };
    }

    private static void whatStatement(KGnode kgTree, PsiStatement statement) {
        // check if the given statement is an Declaration statement
        if (statement instanceof PsiDeclarationStatement) {

            // create a KGnode object for this declaration statement
            KGnode kg_DeclarationStatement = KGnode.newNode("DeclarationStatement" + "(" + declarationStatementCount + ")", null);
            kgTree.children.add(kg_DeclarationStatement);
            int kg_declarationStatement_index = kgTree.children.indexOf(kg_DeclarationStatement);

            // get the current method's BODY's declaration statement node, and add all its information
            // under this node
            KGnode currentDeclarationStatement = kgTree.children.get(kg_declarationStatement_index);

            PsiDeclarationStatement declarationStatement = (PsiDeclarationStatement) statement;
            PsiElement[] declaredElements = declarationStatement.getDeclaredElements();
            for (int i=0; i<declaredElements.length; i++) {
                PsiElement declaredElement = declaredElements[i];
                PsiElement[] children = declaredElement.getChildren();
                for (int j=0; j<children.length;j++) {
                    PsiElement child = children[j];
                    whatElement(currentDeclarationStatement, child);
                }
                //
            }
            // increment the count so the naming for next declaration statement is correct
            declarationStatementCount += 1;
        }
        else if (statement instanceof PsiExpressionStatement) {
            // create a KGnode object for this expression statement
            KGnode kg_expressionStatement = KGnode.newNode("ExpressionStatement" + "(" + expressionStatementCount + ")", null);
            kgTree.children.add(kg_expressionStatement);
            int kg_expressionStatement_index = kgTree.children.indexOf(kg_expressionStatement);

            // get the current method's BODY's expression statement node, and add all its information
            // under this node
            KGnode currentExpressionStatement = kgTree.children.get(kg_expressionStatement_index);

            PsiExpressionStatement psiExpressionStatement = (PsiExpressionStatement) statement;
            PsiElement[] expressions = psiExpressionStatement.getChildren();
            for (int i=0; i<expressions.length; i++) {
                PsiElement expression = expressions[i];
                whatElement(currentExpressionStatement, expression);
            }
            // increment the count so the naming for next declaration statement is correct
            expressionStatementCount += 1;
        }
        else if (statement instanceof PsiForStatement) {
            // create a KGnode object for this for statement
            KGnode kg_forStatement = KGnode.newNode("ForStatement" + "(" + forStatementCount + ")", null);
            kgTree.children.add(kg_forStatement);
            int kg_forStatement_index = kgTree.children.indexOf(kg_forStatement);

            // get the current method's BODY's for statement node, and add all its information
            // under this node

            KGnode currentForStatement = kgTree.children.get(kg_forStatement_index);

            PsiForStatement psiForStatement = (PsiForStatement) statement;
            PsiElement[] children = psiForStatement.getChildren();
            for (int i=0; i<children.length; i++) {
                PsiElement child = children[i];
                whatElement(currentForStatement, child);
            }


        }
        else if (statement instanceof PsiReturnStatement) {
            // create a KGnode object for this RETURN statement
            KGnode kg_ReturnStatement = KGnode.newNode("ReturnStmt", null);
            kgTree.children.add(kg_ReturnStatement);
            int kg_ReturnStatement_index = kgTree.children.indexOf(kg_ReturnStatement);

            // get the current method's BODY's RETURN statement node, and add all its information
            // under this node
            KGnode currentReturnStatement = kgTree.children.get(kg_ReturnStatement_index);

            PsiReturnStatement returnStatement = (PsiReturnStatement) statement;
            PsiElement[] children = returnStatement.getChildren();
            for (int i=0; i<children.length; i++) {
                PsiElement child = children[i];
                whatElement(currentReturnStatement, child);
            }
        }
    }

    private static void whatElement(KGnode kgTree, PsiElement psiElement) {
        if (psiElement instanceof PsiTypeElement) {
            // create a KGnode object for TYPE
            KGnode kg_type = KGnode.newNode("TYPE", null);
            kgTree.children.add(kg_type);
            int kg_type_index = kgTree.children.indexOf(kg_type);

            PsiTypeElement psiTypeElement = (PsiTypeElement) psiElement;
            String psiType = psiTypeElement.getType().getPresentableText();

            // add under TYPE node
            kgTree.children.get(kg_type_index).children.add(KGnode.newNode(psiType, null));
        }
        else if (psiElement instanceof PsiIdentifier) {
            // create a KGnode object for IDENTIFIER/NAME
            KGnode kg_name = KGnode.newNode("NAME", null);
            kgTree.children.add(kg_name);
            int kg_name_index = kgTree.children.indexOf(kg_name);

            PsiIdentifier psiIdentifier = (PsiIdentifier) psiElement;
            String psiIdentName = psiIdentifier.getText();

            // add under NAME node
            kgTree.children.get(kg_name_index).children.add(KGnode.newNode(psiIdentName, null));
        }
        else if (psiElement instanceof PsiNewExpression) {
            // create a KGnode object for NEW expression
            KGnode kg_newExpression = KGnode.newNode("NewExpression", null);
            kgTree.children.add(kg_newExpression);
            int kg_newExpression_index = kgTree.children.indexOf(kg_newExpression);

            // get the current NEW Expression node, and add all its information
            // under this node
            KGnode currentNewExpression = kgTree.children.get(kg_newExpression_index);

            PsiNewExpression psiNewExpression = (PsiNewExpression) psiElement;
            PsiElement[] newExpressionChildren = psiNewExpression.getChildren();
            for (int i=0; i<newExpressionChildren.length; i++) {
                PsiElement child = newExpressionChildren[i];
                // go further to check what element it is
                whatElement(currentNewExpression, child);
            }
        }
        // based on inspection, all arguments are under PsiExpressionList
        else if (psiElement instanceof PsiExpressionList) {
            // create a Kgnode object for ARGUMENTS
            KGnode kg_arguments = KGnode.newNode("ARGUMENTS", null);
            kgTree.children.add(kg_arguments);
            int kg_arguments_index = kgTree.children.indexOf(kg_arguments);

            // get ARGUMENT NODE
            KGnode kg_currentArguments = kgTree.children.get(kg_arguments_index);

            PsiExpressionList psiExpressionList = (PsiExpressionList) psiElement;
            if (!psiExpressionList.isEmpty()) {
                PsiExpression[] psiExpressions = psiExpressionList.getExpressions();
                getArguments(kg_currentArguments, psiExpressions);
            }
            else {
                kg_currentArguments.children.add(KGnode.newNode("null", null));
            }
        }
        else if (psiElement instanceof PsiMethodCallExpression) {
            // create a Kgnode object for MethodCallExpr
            KGnode kg_methodCallExpression = KGnode.newNode("MethodCallExpr", null);
            kgTree.children.add(kg_methodCallExpression);
            int kg_methodCallExpression_index = kgTree.children.indexOf(kg_methodCallExpression);

            // get the current MethodCallExpression node, and add all its information
            // under this node
            KGnode currentMethodCallExpression = kgTree.children.get(kg_methodCallExpression_index);

            PsiMethodCallExpression psiMethodCallExpression = (PsiMethodCallExpression) psiElement;
            PsiElement[] children = psiMethodCallExpression.getChildren();
            for (int i=0; i<children.length; i++) {
                PsiElement child = children[i];
                whatElement(currentMethodCallExpression, child);
            }
        }
        else if (psiElement instanceof PsiReferenceExpression) {
            // create a Kgnode object for ReferenceExpression
            KGnode kg_referenceExpression = KGnode.newNode("ReferenceExpression", null);
            kgTree.children.add(kg_referenceExpression);
            int kg_referenceExpression_index = kgTree.children.indexOf(kg_referenceExpression);

            // get the current ReferenceExpression node, and add all its information
            // under this node
            KGnode currentReferenceExpression = kgTree.children.get(kg_referenceExpression_index);

            PsiReferenceExpression psiReferenceExpression = (PsiReferenceExpression) psiElement;
            PsiElement[] children = psiReferenceExpression.getChildren();
            for (int i=0; i<children.length; i++) {
                PsiElement child = children[i];
                whatElement(currentReferenceExpression, child);
            }
        }
        else if (psiElement instanceof PsiJavaCodeReferenceElement) {
            // create a KGnode object for Reference Element
            KGnode kg_referenceElement = KGnode.newNode("ReferenceElement",null);
            kgTree.children.add(kg_referenceElement);
            int kg_referenceElement_index = kgTree.children.indexOf(kg_referenceElement);

            PsiJavaCodeReferenceElement referenceElement = (PsiJavaCodeReferenceElement) psiElement;
            String referenceElement_name = referenceElement.getText();

            kgTree.children.get(kg_referenceElement_index).children.add(KGnode.newNode(referenceElement_name,null));
        }
        else if (psiElement instanceof PsiLiteralExpression) {
            // create a KGnode object for Literal Expression
            KGnode kg_literalExpression = KGnode.newNode("LiteralExpression", null);
            kgTree.children.add(kg_literalExpression);
            int kg_literalExpression_index = kgTree.children.indexOf(kg_literalExpression);

            KGnode current_literalExpression = kgTree.children.get(kg_literalExpression_index);

            PsiLiteralExpression psiLiteralExpression = (PsiLiteralExpression) psiElement;
            current_literalExpression.children.add(KGnode.newNode(psiLiteralExpression.getText(), null));
        }
        else if (psiElement instanceof PsiBinaryExpression) {
            // create a KGnode object for Binary Expression
            KGnode kg_binaryExpression = KGnode.newNode("BinaryExpression", null);
            kgTree.children.add(kg_binaryExpression);
            int kg_binaryExpression_index = kgTree.children.indexOf(kg_binaryExpression);

            KGnode current_binaryExpression = kgTree.children.get(kg_binaryExpression_index);

            PsiBinaryExpression psiBinaryExpression = (PsiBinaryExpression) psiElement;
            current_binaryExpression.children.add(KGnode.newNode(psiBinaryExpression.getText(), null));
        }
        else if (psiElement instanceof PsiExpressionStatement) {
            // create a KGnode object for ExpressionStatement
            KGnode kg_expressionStatement = KGnode.newNode("ExpressionStatement", null);
            kgTree.children.add(kg_expressionStatement);
            int kg_expressionStatement_index = kgTree.children.indexOf(kg_expressionStatement);

            KGnode current_expressionStatement = kgTree.children.get(kg_expressionStatement_index);

            PsiExpressionStatement psiExpressionStatement = (PsiExpressionStatement) psiElement;
            PsiElement[] children = psiExpressionStatement.getChildren();
            for (int i=0; i<children.length; i++) {
                PsiElement child = children[i];
                whatElement(current_expressionStatement, child);
            }
        }
        else if (psiElement instanceof PsiAssignmentExpression) {
            // create a KGnode object for Assignment Expression
            KGnode kg_assignmentExpression = KGnode.newNode("AssignmentExpression", null);
            kgTree.children.add(kg_assignmentExpression);
            int kg_assignmentExpression_index = kgTree.children.indexOf(kg_assignmentExpression);

            KGnode current_assignmentExpression = kgTree.children.get(kg_assignmentExpression_index);

            PsiAssignmentExpression psiAssignmentExpression = (PsiAssignmentExpression) psiElement;
            String expression = psiAssignmentExpression.getText();
            current_assignmentExpression.children.add(KGnode.newNode(expression, null));
        }
        else if (psiElement instanceof PsiPrefixExpression) {
            // create a KGnode object for Prefix Expression
            KGnode kg_prefixExpression = KGnode.newNode("PrefixExpression", null);
            kgTree.children.add(kg_prefixExpression);
            int kg_prefixExpression_index = kgTree.children.indexOf(kg_prefixExpression);

            KGnode current_prefixExpression = kgTree.children.get(kg_prefixExpression_index);

            PsiPrefixExpression psiPrefixExpression = (PsiPrefixExpression) psiElement;
            String expression = psiPrefixExpression.getText();
            current_prefixExpression.children.add(KGnode.newNode(expression, null));
        }
        else if (psiElement instanceof PsiBlockStatement) {
            // create a KGnode object for Block statement
            KGnode kg_blockStatement = KGnode.newNode("BlockStatement", null);
            kgTree.children.add(kg_blockStatement);
            int kg_blockStatement_index = kgTree.children.indexOf(kg_blockStatement);

            KGnode current_blockStatement = kgTree.children.get(kg_blockStatement_index);

            PsiBlockStatement psiBlockStatement = (PsiBlockStatement) psiElement;
            PsiCodeBlock psiCodeBlock = psiBlockStatement.getCodeBlock();
            // check if the code block is empty
            if (!psiCodeBlock.isEmpty()) {
                PsiElement[] children = psiCodeBlock.getChildren();
                for (int i=0; i<children.length; i++) {
                    PsiElement child = children[i];
                    whatElement(current_blockStatement, child);
                }
            }
            else {
                current_blockStatement.children.add(KGnode.newNode("null", null));
            }
        }
    }

    private static void getArguments(KGnode kgTree, PsiExpression[] psiExpressions) {
        for (int i=0; i<psiExpressions.length; i++) {
            PsiExpression psiExpression = psiExpressions[i];
            // Arguments are PsiReferenceExpression
            if (psiExpression instanceof PsiReferenceExpression) {
                String argument = ((PsiReferenceExpression) psiExpression).getCanonicalText();
                kgTree.children.add(KGnode.newNode(argument, null));
            }
            // Arguments are Binary Expression, i.e. 1+1, 1-1, 1*1, ...
            else if (psiExpression instanceof PsiBinaryExpression) {
                String argument = ((PsiBinaryExpression) psiExpression).getText();
                kgTree.children.add(KGnode.newNode(argument, null));
            }
            // Arguments are Literal Expression, i.e., 6, 9, 69, ...
            else if (psiExpression instanceof PsiLiteralExpression) {
                String argument = ((PsiLiteralExpression) psiExpression).getText();
                kgTree.children.add(KGnode.newNode(argument, null));
            }
            // Arguments are Method Call Expression, i.e., foo(foo_1())
            else if (psiExpression instanceof PsiMethodCallExpression) {

                // create a Kgnode object for nested method Call Expression
                KGnode kg_methodCallExpression = KGnode.newNode("MethodCallExpr", null);
                kgTree.children.add(kg_methodCallExpression);
                int kg_methodCallExpression_index = kgTree.children.indexOf(kg_methodCallExpression);

                KGnode currentMethodCallExpression = kgTree.children.get(kg_methodCallExpression_index);

                PsiMethodCallExpression psiMethodCallExpression = (PsiMethodCallExpression) psiExpression;
                PsiElement[] children = psiMethodCallExpression.getChildren();
                for (int j=0; j<children.length; j++) {
                    PsiElement child = children[j];
                    whatElement(currentMethodCallExpression, child);
                }
            }
        }
    }

    // check if method has already existed in KG tree
    private static boolean checkDuplicate(KGnode kgTree, String methodName) {
        int treeSize = kgTree.children.size();
        for (int i=0; i<treeSize; i++) {
            if (kgTree.children.get(i).getReference().equals(methodName)) {
                return true;
            }
        }
        return false;
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
