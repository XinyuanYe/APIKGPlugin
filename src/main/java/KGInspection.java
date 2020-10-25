import ASTtriplet.ASTtriplet;
import com.intellij.codeInspection.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class KGInspection extends AbstractBaseJavaLocalInspectionTool {
    private static final Logger LOG = Logger.getInstance(KGInspection.class);
    private final CriQuickFix myQuickFix = new CriQuickFix();

    // Defines the text of the quick fix intention
    public static final String QUICK_FIX_NAME = "SDK: This is a warning! Use KG to fix this!";

    // fixed condition
    static final String METHOD_DEFINITION = "MethodDefinition";
    static final String METHOD_BODY = "MethodBody";
    static final String DECLARATION_STMT = "DeclarationStatement";
    static final String NEW_EXPRESSION = "NewExpression";
    static final String RETURN_STMT = "ReturnStatement";
    static final String METHODCALL_EXPRESSION = "MethodCallExpression";
    static final String EXPRESSION_STMT = "ExpressionStatement";
    static final String IF_STMT = "IfStatement";
    static final String THEN_BRANCH = "ThenBranch";
    static final String ELSE_SECTION = "ElseSection";
    static final String BINARY_EXPRESSION = "BinaryExpression";
    static final String LITERAL_EXPRESSION = "LiteralExpression";
    static final String TRY_STATEMENT = "TryStatement";
    static final String CATCH_SECTION = "CatchSection";
    static final String REFERENCE_EXP = "ReferenceExpression";
    static final String REFERENCE_ELEMENT = "ReferenceElement";
    static final String ASSIGNMENT_EXP = "AssignmentExpression";
    static final String ARRAY_INITIALIZER_EXP = "ArrayInitializerExpression";
    static final String POST_FIX_EXP = "PostFixExpression";
    static final String FOR_STATEMENT = "ForStatement";
    static final String FOR_BODY = "ForBody";
    static final String WHILE_STATEMENT = "WhileStatement";
    static final String WHILE_BODY = "WhileBody";
    static final String WHILE_CONDITION = "WhileCondition";


    // triplet counting
    static int id = 0;
    static int lowest_root_id = 0;
    static int next_available_id = 0;

    static int try_lowest_rootID = 0;
    static int try_next_available_id = 0;

    static int if_next_available_id;

    static int rootID = -1;

//    static PsiReferenceExpressionStatement potential_bad_line = null;
    static PsiExpressionStatement potential_bad_line;
    static PsiReferenceExpression jpanel_add_bad_line;
    static PsiReferenceExpression jpanel_remove_bad_line;
    static PsiReferenceExpression iterator_next_bad_line;
    static PsiReferenceExpression iterator_remove_bad_line;
    static PsiReferenceExpression charAt_bad_line;
    static PsiReferenceExpression char_At_indexOf_bad_line;
    static PsiReferenceExpression char_At_substring_indexOf_bad_line;
    static PsiReferenceExpression char_At_substring_larger_bad_line;
    static PsiReferenceExpression char_At_substring_negative_bad_line;
    static PsiReferenceExpression char_At_substring_negative_complex_bad_line;


    static PsiDeclarationStatement filereader_exist_bad_line;
    static PsiDeclarationStatement filereader_directory_badline;
    static PsiDeclarationStatement potential_filereader_try_bad_line;
    static PsiDeclarationStatement filereader_try_bad_line;


    static PsiReferenceExpression read_bad_line;



    public static final String NEW_FILE_WARNING_1 = "1）Missing state checking: file.isDirectory() = true;\nif violated, " +
            "throws FileNotFoundException;";

    public static final String NEW_FILE_WARNING_2 = "2) Missing exception handling: FileNotFoundException \n" +
            "[trigger - the named file for some reason cannot be opened]";
    boolean show_New_File_Warning = false;

    public static final String READ_WARNING = "Missing exception handling: IOException \n [trigger - an IO error occurs]";
    boolean show_Read_Warning = false;

    public static final String JPANEL_WARNING_ADD = "Missing call: validate() should be called after add()";
    public static final String JPANEL_WARNING_REMOVE = "Missing call: validate() should be called after remove()";
    public static final String ITERATORPATTERN_WARNING_HASNEXT = "Missing state checking: hasNext() should be true; \n [if violated, throw NoSuchElementException]";
    public static final String ITERATORPATTERN_WARNING_REMOVE = "Missing call: next() should precede remove(); ";
    public static final String INDEXOF_CONSTRAINT_WARNING = "Return -1;\n [trigger - the character does not occur]";
    public static final String SUBSTRING_INDEXOF_WARNING = "Missing state checking: indexOf should not be -1;  \n [if violated, throw IndexOutOfBoundsException]";
    public static final String SUBSTRING_LARGER_WARNING = "Missing state checking: index should not be larger than the length of this String object;  \n [if violated, throw IndexOutOfBoundsException]";
    public static final String SUBSTRING_NEGATIVE_WARNING = "Missing state checking: index should not be negative; \n [if violated, throw IndexOutOfBoundsException]";
    public static final String FILE_CHECK_WARNING = "Missing state checking: exists() should be true; \n [if violated, throw FileNotFoundException]";
    public static final String DIRECTORY_CHECK_WARNING = "Missing state checking: isDirectory should not be true; \n [if violated, throw FileNotFoundException]";
    public static final String FILEREADER_TRY_WARNING = "Missing exception handling: FileNotFoundException; \n [trigger - the named file for some reason cannot be opened";
    public static final String READ_TRY_WARNING = "Missing exception handling: IOException; \n [trigger - an IO error occurs]";

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

                // GET method condition
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


                // Triplet format: <{Name:condition, Return_Type:void, parameter:[], parameter_Type:[]},
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

                    ASTtriplet body_astTriplet = new ASTtriplet(id++);

                    lowest_root_id = id;
                    next_available_id = id + bodyStatementCount;

                    // list that stores the related triplets
                    ArrayList<String> related_triplets = new ArrayList<>();

                    for (int i = 0; i < bodyStatementCount; i++) {
                        related_triplets.add("triplet_" + (id + i));
                    }

                    // record body triplet
                    body_astTriplet.first_entity.add("Related_triplets: " + related_triplets.toString());
                    body_astTriplet.second_entity.add("End_entity: UNK");
                    body_astTriplet.third_entity.add("Relation: " + METHOD_BODY);
                    astTriplets.add(body_astTriplet);

                    // iterate the body statement
                    PsiStatement[] statements = method.getBody().getStatements();
                    for (int i = 0; i < statements.length; i++) {
                        PsiStatement statement = statements[i];
                        // IMPORTANT: always keep track the root ID
                        rootID = lowest_root_id + i;
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
                String case_name = getCase(astTriplets);
                System.out.println("case is " + case_name);

                boolean thereIsAProblem = checkConstraint(astTriplets, case_name);
                System.out.println("there is a problem " + thereIsAProblem);

                // this will register a problem, and display warning (perhaps suggests quickFix)
                // the boolean variable should be set to true if a problem is detected, it should always be false otherwise
                // change the variable to true if want to display the warning for testing
//                boolean thereIsAProblem = false;
                if (thereIsAProblem) {
                    if (case_name.equals("jpanelWarning")) {
                        ArrayList<String> jpanel_cases = checkJPanel(astTriplets, case_name);
                        String problems = "";
                        for (String c : jpanel_cases) {
                            problems += c + " ";
                        }
                        System.out.println("problem(s) is " + problems);
                        if (!jpanel_cases.isEmpty()) {
                            for (String p : jpanel_cases) {
                                if (p.equals("add")) {
                                    holder.registerProblem(jpanel_add_bad_line, JPANEL_WARNING_ADD, myQuickFix);
                                }
                                else if (p.equals("remove")) {
                                    holder.registerProblem(jpanel_remove_bad_line, JPANEL_WARNING_REMOVE, myQuickFix);
                                }
                            }
                        }
                    }
                    else if (case_name.equals("iteratorWarning")) {
                        ArrayList<String> iterator_cases = checkiteratorPattern(astTriplets, case_name);
                        String problems = "";
                        for (String c : iterator_cases) {
                            problems += c + " ";
                        }
                        System.out.println("problem(s) is " + problems);
                        if (!problems.isEmpty()) {
                            for (String p : iterator_cases) {
                                if (p.equals("hasnext")) {
                                    holder.registerProblem(iterator_next_bad_line, ITERATORPATTERN_WARNING_HASNEXT, myQuickFix);
                                }
                                else if (p.equals("remove")) {
                                    holder.registerProblem(iterator_remove_bad_line, ITERATORPATTERN_WARNING_REMOVE, myQuickFix);

                                }
                            }
                        }
                    }
                    else if (case_name.equals("stringWarning")) {
                        ArrayList<String> charAt_cases = checkcharAt(astTriplets, case_name);
                        String problems = "";
                        for (String c : charAt_cases) {
                            problems += c + " ";
                        }
                        System.out.println("problem(s) is " + problems);
                        if (!problems.isEmpty()) {
                            for (String p : charAt_cases) {
                                if (p.equals("indexOf_constraint")) {
                                    holder.registerProblem(char_At_indexOf_bad_line, INDEXOF_CONSTRAINT_WARNING, myQuickFix);
                                }
                                else if (p.equals("substring_negative")) {
                                    holder.registerProblem(char_At_substring_negative_bad_line, SUBSTRING_NEGATIVE_WARNING, myQuickFix);
                                }
                                else if (p.equals("substring_indexOf_negative")) {
                                    holder.registerProblem(char_At_substring_indexOf_bad_line, SUBSTRING_INDEXOF_WARNING, myQuickFix);
                                }
                                else if (p.equals("substring_larger")) {
                                    holder.registerProblem(char_At_substring_larger_bad_line, SUBSTRING_LARGER_WARNING, myQuickFix);
                                }
                                else if (p.equals("substring_negative_complex")) {
                                    holder.registerProblem(char_At_substring_negative_complex_bad_line, SUBSTRING_NEGATIVE_WARNING, myQuickFix);

                                }
                            }

                        }
                    }
                    else if (case_name.equals("filereaderWarning")) {
                        ArrayList<String> cap_cases = checkCapability(astTriplets, case_name);
                        String problems = "";
                        for (String c : cap_cases) {
                            problems += c + " ";
                        }
                        System.out.println("problem(s) is " + problems);
                        if (!problems.isEmpty()) {
                            for (String p : cap_cases) {
                                if (p.equals("file")) {
                                    holder.registerProblem(filereader_exist_bad_line, FILE_CHECK_WARNING, myQuickFix);
                                }
                                else if (p.equals("directory")) {
                                    holder.registerProblem(filereader_directory_badline, DIRECTORY_CHECK_WARNING, myQuickFix);
                                }
                                else if (p.equals("filereader_try")) {
                                    holder.registerProblem(filereader_try_bad_line, FILEREADER_TRY_WARNING , myQuickFix);
                                }
                                else if (p.equals("read_try")) {
                                    holder.registerProblem(read_bad_line, READ_TRY_WARNING, myQuickFix);
                                }
                            }
                        }
                    }
                    else if (case_name.equals("IGNROE THIS")) {
                        String filetry_case = checkfiletry(astTriplets, case_name);
                        if (filetry_case.equals("file")) {
                            holder.registerProblem(filereader_exist_bad_line, FILE_CHECK_WARNING, myQuickFix);
                        } else if (filetry_case.equals("directory")) {
                            holder.registerProblem(filereader_directory_badline, DIRECTORY_CHECK_WARNING, myQuickFix);
                        } else if (filetry_case.equals("filereader_try")) {
                            holder.registerProblem(filereader_try_bad_line, FILEREADER_TRY_WARNING, myQuickFix);
                        } else if (filetry_case.equals("read_try")) {
                            holder.registerProblem(read_bad_line, READ_TRY_WARNING, myQuickFix);
                        }
                    }
                }

            }

        };

    }

    // this function will be ONLY used once in the iterate BODY statement part
    // so the isRoot boolean must be set to true to ensure correct id being recorded
    private static void whatStatement(ArrayList<ASTtriplet> astTriplets, PsiStatement statement) {
        // check if the given statement is an Declaration statement
        if (statement instanceof PsiDeclarationStatement) {

            PsiDeclarationStatement declarationStatement = (PsiDeclarationStatement) statement;
            PsiElement[] declaredElements = declarationStatement.getDeclaredElements();

            potential_filereader_try_bad_line = declarationStatement;

            for (int i=0; i<declaredElements.length; i++) {
                PsiElement declaredElement = declaredElements[i];
                PsiElement[] children = declaredElement.getChildren();
                whatElement(astTriplets, children, DECLARATION_STMT, true);
            }
        }
        else if (statement instanceof PsiReturnStatement) {
            PsiReturnStatement returnStatement = (PsiReturnStatement) statement;
            PsiElement[] children = returnStatement.getChildren();
            whatElement(astTriplets, children, RETURN_STMT, true);
        }
        else if (statement instanceof PsiExpressionStatement) {
            PsiExpressionStatement psiExpressionStatement = (PsiExpressionStatement) statement;
            PsiElement[] expressions = psiExpressionStatement.getChildren();
            whatElement(astTriplets, expressions, EXPRESSION_STMT, true);

            potential_bad_line = psiExpressionStatement;
        }
        else if (statement instanceof PsiIfStatement) {
            // add if statement relation
            PsiIfStatement psiIfStatement = (PsiIfStatement) statement;

            // list that stores the related triplets
            ArrayList<String> related_triplets = new ArrayList<>();

            for (int i = 0; i < 1; i++) {
                related_triplets.add("triplet_" + (next_available_id + i));
            }

            if_next_available_id = next_available_id + 1;

            ASTtriplet astTriplet;
            astTriplet = new ASTtriplet(rootID++);
            astTriplet.first_entity.add("Related_triplets: " + related_triplets.toString());
            astTriplet.second_entity.add("End_entity: " + ELSE_SECTION);
            astTriplet.third_entity.add("Relation: " + IF_STMT);
            astTriplets.add(astTriplet);

            PsiExpression ifConditionStatement = psiIfStatement.getCondition();
            if (ifConditionStatement instanceof PsiBinaryExpression) {
                PsiBinaryExpression psiBinaryExpression = (PsiBinaryExpression) ifConditionStatement;

                String LOperand = psiBinaryExpression.getLOperand().getText();
                String LOperand_type = psiBinaryExpression.getLOperand().getType().getPresentableText();
                String operation = psiBinaryExpression.getOperationSign().getText();
                String ROperand = psiBinaryExpression.getROperand().getText();
                String ROperand_type = psiBinaryExpression.getROperand().getType().getPresentableText();

                astTriplet = new ASTtriplet(rootID++);
                astTriplet.first_entity.add("Related_triplets: " + "triplet_" + if_next_available_id);
                astTriplet.second_entity.add("LOperand: " + LOperand);
                astTriplet.second_entity.add("Operation: " + operation);
                astTriplet.second_entity.add("ROperand: " + ROperand);
                astTriplet.third_entity.add("Relation: " + BINARY_EXPRESSION);
                astTriplets.add(astTriplet);

                PsiExpression psiBinaryExpressionLOperand = psiBinaryExpression.getLOperand();
                if (psiBinaryExpressionLOperand instanceof PsiMethodCallExpression) {
                    PsiMethodCallExpression psiMethodCallExpression = (PsiMethodCallExpression) psiBinaryExpressionLOperand;
                    PsiReferenceExpression psiReferenceExpression = psiMethodCallExpression.getMethodExpression();
                    String referenceExp = psiReferenceExpression.getCanonicalText();
                    PsiExpression[] psiExpressionList = psiMethodCallExpression.getArgumentList().getExpressions();
                    getArguments(astTriplets, psiExpressionList, referenceExp, IF_STMT, METHODCALL_EXPRESSION, false);
                }
            }
            else if (ifConditionStatement instanceof PsiMethodCallExpression) {
                PsiMethodCallExpression psiMethodCallExpression = (PsiMethodCallExpression) ifConditionStatement;

                PsiReferenceExpression psiReferenceExpression = psiMethodCallExpression.getMethodExpression();
                String referenceExp = psiReferenceExpression.getCanonicalText();
                PsiExpression[] psiExpressionList = psiMethodCallExpression.getArgumentList().getExpressions();
                astTriplet = new ASTtriplet(rootID++);
                astTriplet.first_entity.add("ReferenceExpression: "+ referenceExp);
                astTriplet.second_entity.add("Argument: " + "[]");
                astTriplet.second_entity.add("Type: " + "[]");
                astTriplet.third_entity.add("Relation: " + "IfCondition");
                astTriplets.add(astTriplet);

            }

            PsiStatement thenBranch = psiIfStatement.getThenBranch();
            if (thenBranch instanceof PsiBlockStatement) {
                PsiBlockStatement psiBlockStatement = (PsiBlockStatement) thenBranch;
                // there is something in then branch
                if (!psiBlockStatement.getCodeBlock().isEmpty()) {
                    int blockStatementCount = psiBlockStatement.getCodeBlock().getStatementCount();
                    // list that stores the related triplets
                    related_triplets = new ArrayList<>();
                    for (int i = 0; i < blockStatementCount; i++) {
                        related_triplets.add("triplet_" + (if_next_available_id + i));
                    }

//                    astTriplet = new ASTtriplet(rootID++);
//                    astTriplet.first_entity.add("Related_triplets: " + "triplet_" + if_next_available_id);
//                    astTriplet.second_entity.add("End_entity: " + "UNK");
//
//                    astTriplet.third_entity.add("Relation: " + BINARY_EXPRESSION);
//                    astTriplets.add(astTriplet);

                    astTriplet = new ASTtriplet(rootID++);
                    astTriplet.first_entity.add("Related_triplets: " + "triplet_" + if_next_available_id);
                    astTriplet.second_entity.add("End_entity: " + "UNK");

                    astTriplet.third_entity.add("Relation: " + "IfBody");
                    astTriplets.add(astTriplet);

                    PsiStatement[] psiStatements = psiBlockStatement.getCodeBlock().getStatements();
                    for (PsiStatement psiStatement : psiStatements) {
//                        if (psiStatement instanceof PsiExpressionStatement) {
//                            PsiExpressionStatement psiExpressionStatement = (PsiExpressionStatement) psiStatement;
//                            PsiExpression psiExpression = psiExpressionStatement.getExpression();
//                            if (psiExpression instanceof PsiMethodCallExpression) {
//                                PsiMethodCallExpression psiMethodCallExpression = (PsiMethodCallExpression) psiExpression;
//                                PsiReferenceExpression psiReferenceExpression = psiMethodCallExpression.getMethodExpression();
//                                String referenceExp = psiReferenceExpression.getCanonicalText();
//                                PsiExpression[] psiExpressionList = psiMethodCallExpression.getArgumentList().getExpressions();
//                                getArguments(astTriplets, psiExpressionList, referenceExp, THEN_BRANCH, METHODCALL_EXPRESSION, false);
//                            }
//                        }
                        if (psiStatement instanceof PsiDeclarationStatement) {
                            whatStatement(astTriplets, psiStatement);
                        }
                        else if (psiStatement instanceof PsiExpressionStatement)
                            whatStatement(astTriplets, psiStatement);
                    }
                }
            }
            PsiStatement elseBranch = psiIfStatement.getElseBranch();
            if (elseBranch instanceof PsiBlockStatement) {
                if (!((PsiBlockStatement) elseBranch).getCodeBlock().isEmpty()) {
                    // TODO
                }
                else {
                    astTriplet = new ASTtriplet(rootID++);
                    astTriplet.first_entity.add("UNK");
                    astTriplet.second_entity.add("UNK");
                    astTriplet.third_entity.add("Relation: " + ELSE_SECTION);
                    astTriplets.add(astTriplet);
                }
            }
        }
        else if (statement instanceof PsiTryStatement) {

            PsiTryStatement psiTryStatement = (PsiTryStatement) statement;
            int statementCount = psiTryStatement.getTryBlock().getStatementCount();

            try_lowest_rootID = lowest_root_id;
            try_next_available_id = next_available_id + statementCount;

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
            whatElement(astTriplets, statements, TRY_STATEMENT, false);

            id = try_next_available_id;

            PsiElement[] catchStmts = psiTryStatement.getChildren();
            whatElement(astTriplets, catchStmts, CATCH_SECTION, false);
        }
        else if (statement instanceof PsiForStatement) {

            PsiForStatement psiForStatement = (PsiForStatement) statement;

            // list that stores the related triplets
            ArrayList<String> related_triplets = new ArrayList<>();

            for (int i = 0; i < 4; i++) {
                related_triplets.add("triplet_" + (next_available_id + i));
            }

            ASTtriplet astTriplet;
            astTriplet = new ASTtriplet(rootID++);
            astTriplet.first_entity.add("Related_triplets: " + related_triplets.toString());
            astTriplet.second_entity.add("End_entity: " + "UNK");
            astTriplet.third_entity.add("Relation: " + FOR_STATEMENT);
            astTriplets.add(astTriplet);

            PsiStatement forStatementInitialization = psiForStatement.getInitialization();

            if (forStatementInitialization instanceof PsiDeclarationStatement) {
                PsiDeclarationStatement psiDeclarationStatement = (PsiDeclarationStatement) forStatementInitialization;
                PsiElement[] declaredElements = psiDeclarationStatement.getDeclaredElements();
                String identifierName = "UNK";
                String type = "UNK";
                String literalExp = "UNK";

                for (PsiElement element : declaredElements) {
                    if (element instanceof PsiTypeElement) {
                        PsiTypeElement psiTypeElement = (PsiTypeElement) element;
                        type = psiTypeElement.getType().getPresentableText();
                    } else if (element instanceof PsiIdentifier) {
                        PsiIdentifier psiIdentifier = (PsiIdentifier) element;
                        identifierName = psiIdentifier.getText();
                    } else if (element instanceof PsiLiteralExpression) {
                        PsiLiteralExpression psiLiteralExpression = (PsiLiteralExpression) element;
                        literalExp = psiLiteralExpression.getText();
                    }
                }
                astTriplet = new ASTtriplet(next_available_id++);
                astTriplet.first_entity.add(LITERAL_EXPRESSION + ": " + literalExp);
                astTriplet.second_entity.add("Argument: " + identifierName);
                astTriplet.second_entity.add("Type: " + type);
                astTriplet.third_entity.add("Relation: " + DECLARATION_STMT);
                astTriplets.add(astTriplet);
            }

            PsiExpression forStatementCondition = psiForStatement.getCondition();
            if (forStatementCondition instanceof PsiBinaryExpression) {
                PsiBinaryExpression psiBinaryExpression = (PsiBinaryExpression) forStatementCondition;
                String expression = psiBinaryExpression.getText();
                String LOperand = psiBinaryExpression.getLOperand().getText();
                String LOperand_type = psiBinaryExpression.getLOperand().getType().getPresentableText();
                String ROperand = psiBinaryExpression.getROperand().getText();
                String ROperand_type = psiBinaryExpression.getROperand().getType().getPresentableText();
                String operation = psiBinaryExpression.getOperationSign().getText();

                astTriplet = new ASTtriplet(next_available_id++);
                astTriplet.first_entity.add(BINARY_EXPRESSION + ": " + expression);
                astTriplet.second_entity.add("LOperand: " + LOperand);
                astTriplet.second_entity.add("Operation: " + operation);
                astTriplet.second_entity.add("ROperand: " + ROperand);
                astTriplet.third_entity.add("Relation: " + BINARY_EXPRESSION);
                astTriplets.add(astTriplet);
            }


            PsiStatement forStatementUpdate = psiForStatement.getUpdate();
            if (forStatementUpdate instanceof PsiExpressionStatement) {
                PsiExpressionStatement psiExpressionStatement = (PsiExpressionStatement) forStatementUpdate;
                PsiExpression expression = psiExpressionStatement.getExpression();
                if (expression instanceof PsiPostfixExpression) {
                    PsiPostfixExpression psiPostfixExpression = (PsiPostfixExpression) expression;
                    String operand = psiPostfixExpression.getOperand().getText();
                    String operand_type = psiPostfixExpression.getOperand().getType().getPresentableText();
                    String operation = psiPostfixExpression.getOperationSign().getText();
                    astTriplet = new ASTtriplet(next_available_id++);
                    astTriplet.first_entity.add(POST_FIX_EXP + ": " + psiPostfixExpression.getText());
                    astTriplet.second_entity.add("Lperand: " + operand);
                    astTriplet.second_entity.add("Operation: " + operation);
                    astTriplet.third_entity.add("Relation: " + POST_FIX_EXP);
                    astTriplets.add(astTriplet);
                }
            }

            // this will be block statement
            PsiStatement blockStatement = psiForStatement.getBody();
            PsiElement[] codeBlock = blockStatement.getChildren();
            for (PsiElement element : codeBlock) {
                if (element instanceof PsiCodeBlock) {
                    PsiCodeBlock forBody = (PsiCodeBlock) element;
                    int forBodyCount = forBody.getStatementCount();

                    // list that stores the related triplets
                    related_triplets = new ArrayList<>();

                    for (int i = 0; i < forBodyCount; i++) {
                        related_triplets.add("triplet_" + (next_available_id + i + 1));
                    }

                    astTriplet = new ASTtriplet(next_available_id++);
                    astTriplet.first_entity.add("Related_triplets: " + related_triplets.toString());
                    astTriplet.second_entity.add("End_entity: " + "UNK");
                    astTriplet.third_entity.add("Relation: " + FOR_BODY);
                    astTriplets.add(astTriplet);

                    PsiStatement[] statements = forBody.getStatements();
                    for (PsiStatement psiStatement : statements) {
                        if (psiStatement instanceof PsiExpressionStatement) {
                            PsiExpressionStatement psiExpressionStatement = (PsiExpressionStatement) psiStatement;
                            PsiExpression psiExpression = psiExpressionStatement.getExpression();
                            if (psiExpression instanceof PsiMethodCallExpression) {
                                PsiMethodCallExpression psiMethodCallExpression = (PsiMethodCallExpression) psiExpression;
                                PsiReferenceExpression psiReferenceExpression = psiMethodCallExpression.getMethodExpression();
                                String referenceExp = psiReferenceExpression.getCanonicalText();
                                PsiExpression[] psiExpressionList = psiMethodCallExpression.getArgumentList().getExpressions();
                                getArguments(astTriplets, psiExpressionList, referenceExp, FOR_STATEMENT, METHODCALL_EXPRESSION, false);
                            }
                        }
                    }
                }
            }
        }
        else if (statement instanceof PsiWhileStatement) {

            PsiWhileStatement psiWhileStatement = (PsiWhileStatement) statement;

            // list that stores the related triplets
            ArrayList<String> related_triplets = new ArrayList<>();

            for (int i = 0; i < 2; i++) {
                related_triplets.add("triplet_" + (next_available_id + i));
            }

            ASTtriplet astTriplet;
            astTriplet = new ASTtriplet(rootID++);
            astTriplet.first_entity.add("Related_triplets: " + related_triplets.toString());
            astTriplet.second_entity.add("End_entity: " + "UNK");
            astTriplet.third_entity.add("Relation: " + WHILE_STATEMENT);
            astTriplets.add(astTriplet);

            // get while conditions
            PsiExpression whileCondition = psiWhileStatement.getCondition();
            String condition = whileCondition.getText();

            astTriplet = new ASTtriplet(next_available_id++);
            astTriplet.first_entity.add(METHODCALL_EXPRESSION + ": " + condition);
            astTriplet.second_entity.add("Argument: " + "UNK");
            astTriplet.second_entity.add("Type: " + "UNK");
            astTriplet.third_entity.add("Relation: " + WHILE_CONDITION);
            astTriplets.add(astTriplet);

            // this will be block statement
            PsiStatement blockStatement = psiWhileStatement.getBody();
            PsiElement[] codeBlock = blockStatement.getChildren();
            for (PsiElement element : codeBlock) {
                if (element instanceof PsiCodeBlock) {
                    PsiCodeBlock whileBody = (PsiCodeBlock) element;
                    int forBodyCount = whileBody.getStatementCount();

                    // list that stores the related triplets
                    related_triplets = new ArrayList<>();

                    for (int i = 0; i < forBodyCount; i++) {
                        related_triplets.add("triplet_" + (next_available_id + i + 1));
                    }

                    astTriplet = new ASTtriplet(next_available_id++);
                    astTriplet.first_entity.add("Related_triplets: " + related_triplets.toString());
                    astTriplet.second_entity.add("End_entity: " + "UNK");
                    astTriplet.third_entity.add("Relation: " + WHILE_BODY);
                    astTriplets.add(astTriplet);

                    PsiStatement[] statements = whileBody.getStatements();
                    for (PsiStatement psiStatement : statements) {
                        if (psiStatement instanceof PsiExpressionStatement) {
                            PsiExpressionStatement psiExpressionStatement = (PsiExpressionStatement) psiStatement;
                            PsiExpression psiExpression = psiExpressionStatement.getExpression();
                            potential_bad_line = psiExpressionStatement;
                            if (psiExpression instanceof PsiMethodCallExpression) {
                                PsiMethodCallExpression psiMethodCallExpression = (PsiMethodCallExpression) psiExpression;
                                PsiReferenceExpression psiReferenceExpression = psiMethodCallExpression.getMethodExpression();
                                String referenceExp = psiReferenceExpression.getCanonicalText();
                                PsiExpression[] psiExpressionList = psiMethodCallExpression.getArgumentList().getExpressions();
                                getArguments(astTriplets, psiExpressionList, referenceExp, WHILE_STATEMENT, METHODCALL_EXPRESSION, false);
                            }
                        }
                    }
                }
            }
        }
    }

    private static void whatElement(ArrayList<ASTtriplet> astTriplets, PsiElement[] psiElements, String relationType, boolean isRoot) {

        String psiType = "UNK";
        String psiIdentName = "UNK";
        String referenceElement_name = "UNK";
        String referenceExp = "UNK";

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

                potential_filereader_try_bad_line = declarationStatement;

                // normally this would be in a try block
                for (int i=0; i<declaredElements.length; i++) {
                    PsiElement declaredElement = declaredElements[i];
                    PsiElement[] children = declaredElement.getChildren();
                    whatElement(astTriplets, children, TRY_STATEMENT, isRoot);
                }
            }
            // EXPRESSION STATEMENT
            else if (psiElement instanceof PsiExpressionStatement) {
                if (relationType.equals(TRY_STATEMENT)) {
                    PsiExpressionStatement psiExpressionStatement = (PsiExpressionStatement) psiElement;
                    PsiElement[] expressions = psiExpressionStatement.getChildren();
                    whatElement(astTriplets, expressions, TRY_STATEMENT, isRoot);
                }
                else if (relationType.equals(CATCH_SECTION)) {
                    PsiExpressionStatement psiExpressionStatement = (PsiExpressionStatement) psiElement;
                    PsiElement[] expressions = psiExpressionStatement.getChildren();
                    whatElement(astTriplets, expressions, CATCH_SECTION, isRoot);
                }
            }
            // REFERENCE EXPRESSION
            else if (psiElement instanceof PsiReferenceExpression) {
                PsiReferenceExpression psiReferenceExpression = (PsiReferenceExpression) psiElement;
                referenceExp = psiReferenceExpression.getCanonicalText();
                classType = "reference_expression";
                if (referenceExp.equals("panel.add")) {
//                    potential_bad_line = psiReferenceExpression;

//                    jpanel_bad_line = potential_bad_line;
                    jpanel_add_bad_line = psiReferenceExpression;
                }
                else if (referenceExp.equals("panel.remove")) {
                    jpanel_remove_bad_line = psiReferenceExpression;

                }
                else if (referenceExp.equals("string.substring")) {
                    char_At_substring_indexOf_bad_line = psiReferenceExpression;
                    char_At_substring_larger_bad_line = psiReferenceExpression;
                    char_At_substring_negative_bad_line = psiReferenceExpression;
                }
                else if (referenceExp.equals("string.indexOf")) {
                    char_At_indexOf_bad_line = psiReferenceExpression;
                }
                else if (referenceExp.equals("iterator.next")) {
                    iterator_next_bad_line = psiReferenceExpression;
                }
                else if (referenceExp.equals("iterator.remove")) {
                    iterator_remove_bad_line = psiReferenceExpression;
                }
                else if (referenceExp.equals("reader.read")) {
                    read_bad_line = psiReferenceExpression;
                }

            }
            // CODE REFERENCE
            else if (psiElement instanceof PsiJavaCodeReferenceElement) {
                PsiJavaCodeReferenceElement psiJavaCodeReferenceElement = (PsiJavaCodeReferenceElement) psiElement;
                referenceElement_name = psiJavaCodeReferenceElement.getText();
                classType = "reference_element";
                if (referenceElement_name.equals("FileReader")) {
                    filereader_try_bad_line = potential_filereader_try_bad_line;
                    filereader_directory_badline = potential_filereader_try_bad_line;
                    filereader_exist_bad_line = potential_filereader_try_bad_line;
                }

            }
            // all the arguments are in EXPRESSION LIST
            else if (psiElement instanceof PsiExpressionList) {
                PsiExpressionList psiExpressionList = (PsiExpressionList) psiElement;
                PsiExpression[] psiExpressions = psiExpressionList.getExpressions();
                if (relationType.equals(METHODCALL_EXPRESSION)) {
                    getArguments(astTriplets, psiExpressions, referenceExp, relationType, classType, isRoot);
                }
                else if (relationType.equals(TRY_STATEMENT)) {
                    if (classType.equals("reference_element")) {
                        getArguments(astTriplets, psiExpressions, referenceElement_name, relationType, classType, isRoot);
                    }
                    else {
                        getArguments(astTriplets, psiExpressions, referenceExp, relationType, classType, isRoot);

                    }
                }
                else if (relationType.equals(CATCH_SECTION)) {
                    getArguments(astTriplets, psiExpressions, referenceExp, relationType, classType, isRoot);
                }
                else if (relationType.equals(NEW_EXPRESSION)) {
                    if (classType.equals("reference_element")) {
                        getArguments(astTriplets, psiExpressions, referenceElement_name, relationType, classType, isRoot);
                    }
                    else {
                        getArguments(astTriplets, psiExpressions, referenceExp, relationType, classType, isRoot);
                    }
                }
            }
            // METHOD CALL EXPRESSION
            else if (psiElement instanceof PsiMethodCallExpression) {
                PsiMethodCallExpression psiMethodCallExpression = (PsiMethodCallExpression) psiElement;

                if (relationType.equals(RETURN_STMT)) {
                    ASTtriplet astTriplet;
                    if (isRoot) {
                        astTriplet = new ASTtriplet(rootID++);
                        astTriplet.first_entity.add("Related_triplet: " + "triplet_" + (next_available_id));
                        astTriplet.second_entity.add("End_entity: " + "UNK");
                        astTriplet.third_entity.add("Relation: " + RETURN_STMT);
                        astTriplets.add(astTriplet);
                        isRoot = false;
                    }
                    else {
                        astTriplet = new ASTtriplet(next_available_id++);
                        astTriplet.first_entity.add("Type: " + psiType);
                        astTriplet.first_entity.add("Name: " + psiIdentName);
                        astTriplet.second_entity.add("Related_triplets: " + "triplet_" + (next_available_id + 1));
                        astTriplet.third_entity.add("Relation: " + DECLARATION_STMT);
                        astTriplets.add(astTriplet);
                    }

                    PsiElement[] children = psiMethodCallExpression.getChildren();

                    whatElement(astTriplets, children, METHODCALL_EXPRESSION, isRoot);
                } else if (relationType.equals(EXPRESSION_STMT)) {
                    PsiElement[] children = psiMethodCallExpression.getChildren();
                    whatElement(astTriplets, children, METHODCALL_EXPRESSION, isRoot);


                }
                else if (relationType.equals(TRY_STATEMENT)) {
                    PsiElement[] children = psiMethodCallExpression.getChildren();
                    whatElement(astTriplets, children, TRY_STATEMENT, isRoot);
                }
                else if (relationType.equals(CATCH_SECTION)) {
                    PsiElement[] children = psiMethodCallExpression.getChildren();
                    whatElement(astTriplets, children, CATCH_SECTION, isRoot);
                }
                else if (relationType.equals(DECLARATION_STMT)) {
                    ASTtriplet astTriplet;
                    if (isRoot) {
                        astTriplet = new ASTtriplet(rootID++);
                        astTriplet.first_entity.add("Type: " + psiType);
                        astTriplet.first_entity.add("Name: " + psiIdentName);
                        astTriplet.second_entity.add("Related_triplets: " + "triplet_" + (next_available_id));
                        astTriplet.third_entity.add("Relation: " + DECLARATION_STMT);
                        astTriplets.add(astTriplet);
                        isRoot = false;
                    }
                    else {
                        astTriplet = new ASTtriplet(next_available_id++);
                        astTriplet.first_entity.add("Type: " + psiType);
                        astTriplet.first_entity.add("Name: " + psiIdentName);
                        astTriplet.second_entity.add("Related_triplets: " + "triplet_" + (id + 1));
                        astTriplet.third_entity.add("Relation: " + DECLARATION_STMT);
                        astTriplets.add(astTriplet);
                    }

                    PsiElement[] newExpressionChildren = psiMethodCallExpression.getChildren();

                    whatElement(astTriplets, newExpressionChildren, METHODCALL_EXPRESSION, isRoot);
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
                    ASTtriplet astTriplet;
                    if (isRoot) {
                        astTriplet = new ASTtriplet(rootID++);
                        astTriplet.first_entity.add("Type: " + psiType);
                        astTriplet.first_entity.add("Name: " + psiIdentName);
                        astTriplet.second_entity.add("Related_triplets: " + "triplet_" + (next_available_id));
                        astTriplet.third_entity.add("Relation: " + DECLARATION_STMT);
                        astTriplets.add(astTriplet);
                        isRoot = false;
                    }
                    else {
                        astTriplet = new ASTtriplet(next_available_id++);
                        astTriplet.first_entity.add("Type: " + psiType);
                        astTriplet.first_entity.add("Name: " + psiIdentName);
                        astTriplet.second_entity.add("Related_triplets: " + "triplet_" + (next_available_id + 1));
                        astTriplet.third_entity.add("Relation: " + DECLARATION_STMT);
                        astTriplets.add(astTriplet);
                    }

                    PsiElement[] newExpressionChildren = psiNewExpression.getChildren();

                    whatElement(astTriplets, newExpressionChildren, NEW_EXPRESSION, isRoot);
                }
                else if (relationType.equals(TRY_STATEMENT)) {
                    ASTtriplet astTriplet;
                    if (isRoot) {
                        astTriplet = new ASTtriplet(try_lowest_rootID);
                        astTriplet.first_entity.add("Type: " + psiType);
                        astTriplet.first_entity.add("Name: " + psiIdentName);
                        astTriplet.second_entity.add("Related_triplets: " + "triplet_" + (try_next_available_id));
                        astTriplet.third_entity.add("Relation: " + DECLARATION_STMT);
                        astTriplets.add(astTriplet);
                        isRoot = false;
                    }
                    else {
                        astTriplet = new ASTtriplet(id++);
                        astTriplet.first_entity.add("Type: " + psiType);
                        astTriplet.first_entity.add("Name: " + psiIdentName);
                        astTriplet.second_entity.add("Related_triplets: " + "triplet_" + (try_next_available_id));
                        astTriplet.third_entity.add("Relation: " + DECLARATION_STMT);
                        astTriplets.add(astTriplet);
                    }

                    PsiElement[] newExpressionChildren = psiNewExpression.getChildren();

                    whatElement(astTriplets, newExpressionChildren, TRY_STATEMENT, isRoot);
                }
                else if (relationType.equals(ASSIGNMENT_EXP)) {
                    ASTtriplet astTriplet;
                    if (isRoot) {
                        astTriplet = new ASTtriplet(rootID++);
                        astTriplet.first_entity.add(REFERENCE_EXP + ": "+ referenceExp);
                        astTriplet.second_entity.add("Related_triplets: " + "triplet_" + (next_available_id));
                        astTriplet.third_entity.add("Relation: " + ASSIGNMENT_EXP);
                        astTriplets.add(astTriplet);
                        isRoot = false;
                    }
                    else {
                        astTriplet = new ASTtriplet(next_available_id++);
                        astTriplet.first_entity.add("Type: " + psiType);
                        astTriplet.first_entity.add("Name: " + psiIdentName);
                        astTriplet.second_entity.add("Related_triplets: " + "triplet_" + (next_available_id));
                        astTriplet.third_entity.add("Relation: " + ASSIGNMENT_EXP);
                        astTriplets.add(astTriplet);
                    }

                    PsiElement[] newExpressionChildren = psiNewExpression.getChildren();
                    whatElement(astTriplets, newExpressionChildren, NEW_EXPRESSION, isRoot);
                }
            }
            // ASSIGNMENT EXPRESSION
            else if (psiElement instanceof PsiAssignmentExpression) {
                PsiAssignmentExpression psiAssignmentExpression = (PsiAssignmentExpression) psiElement;
                PsiElement[] children = psiAssignmentExpression.getChildren();
                whatElement(astTriplets, children, ASSIGNMENT_EXP, isRoot);
            }
            else if (psiElement instanceof PsiArrayInitializerExpression) {
                PsiArrayInitializerExpression psiArrayInitializerExpression = (PsiArrayInitializerExpression) psiElement;
                PsiExpression[] psiExpressions = psiArrayInitializerExpression.getInitializers();

                ArrayList<String> arrayInitializer = new ArrayList<>();
                ArrayList<String> arrayInitializerType = new ArrayList<>();

                getArrayInitializer(arrayInitializer, arrayInitializerType, psiExpressions);

                String arrayInitializerExp = psiArrayInitializerExpression.getText();

                ASTtriplet astTriplet;
                if (isRoot) {
                    astTriplet = new ASTtriplet(rootID++);
                    astTriplet.first_entity.add(ARRAY_INITIALIZER_EXP + ": "+ arrayInitializerExp);
                    astTriplet.second_entity.add("Arguments :" + arrayInitializer.toString());
                    astTriplet.second_entity.add("Types :" + arrayInitializerType.toString());
                    astTriplet.third_entity.add("Relation: " + ARRAY_INITIALIZER_EXP);
                    astTriplets.add(astTriplet);
                    isRoot = false;
                }
                else {
                    astTriplet = new ASTtriplet(next_available_id++);
                    astTriplet.first_entity.add(ARRAY_INITIALIZER_EXP + ": "+ arrayInitializerExp);
                    astTriplet.second_entity.add("Arguments :" + arrayInitializer.toString());
                    astTriplet.second_entity.add("Types :" + arrayInitializerType.toString());
                    astTriplet.third_entity.add("Relation: " + ARRAY_INITIALIZER_EXP);
                    astTriplets.add(astTriplet);
                }

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
                whatElement(astTriplets, children, CATCH_SECTION, isRoot);

            }
            else if (psiElement instanceof PsiKeyword) {
                PsiKeyword psiKeyword = (PsiKeyword) psiElement;
                String keyword = psiKeyword.getText().toLowerCase();
                if (keyword.equals("else")) {
                    elseSection = true;
                }
            }
            else if (psiElement instanceof PsiIfStatement) {
                // add if statement relation
                PsiIfStatement psiIfStatement = (PsiIfStatement) psiElement;

                // list that stores the related triplets
                ArrayList<String> related_triplets = new ArrayList<>();

                for (int i = 0; i < 1; i++) {
                    related_triplets.add("triplet_" + (next_available_id + i));
                }

                if_next_available_id = next_available_id + 1;

                ASTtriplet astTriplet;
                astTriplet = new ASTtriplet(rootID++);
                astTriplet.first_entity.add("Related_triplets: " + related_triplets.toString());
                astTriplet.second_entity.add("End_entity: " + ELSE_SECTION);
                astTriplet.third_entity.add("Relation: " + IF_STMT);
                astTriplets.add(astTriplet);

                PsiExpression ifConditionStatement = psiIfStatement.getCondition();
                if (ifConditionStatement instanceof PsiBinaryExpression) {
                    PsiBinaryExpression psiBinaryExpression = (PsiBinaryExpression) ifConditionStatement;

                    String LOperand = psiBinaryExpression.getLOperand().getText();
                    String LOperand_type = psiBinaryExpression.getLOperand().getType().getPresentableText();
                    String operation = psiBinaryExpression.getOperationSign().getText();
                    String ROperand = psiBinaryExpression.getROperand().getText();
                    String ROperand_type = psiBinaryExpression.getROperand().getType().getPresentableText();

                    astTriplet = new ASTtriplet(rootID++);
                    astTriplet.first_entity.add("Related_triplets: " + "triplet_" + if_next_available_id);
                    astTriplet.second_entity.add("LOperand: " + LOperand);
                    astTriplet.second_entity.add("Operation: " + operation);
                    astTriplet.second_entity.add("ROperand: " + ROperand);
                    astTriplet.third_entity.add("Relation: " + BINARY_EXPRESSION);
                    astTriplets.add(astTriplet);

                    PsiExpression psiBinaryExpressionLOperand = psiBinaryExpression.getLOperand();
                    if (psiBinaryExpressionLOperand instanceof PsiMethodCallExpression) {
                        PsiMethodCallExpression psiMethodCallExpression = (PsiMethodCallExpression) psiBinaryExpressionLOperand;
                        PsiReferenceExpression psiReferenceExpression = psiMethodCallExpression.getMethodExpression();
                        String referenceExpressionCanonicalText = psiReferenceExpression.getCanonicalText();
                        PsiExpression[] psiExpressionList = psiMethodCallExpression.getArgumentList().getExpressions();
                        getArguments(astTriplets, psiExpressionList, referenceExpressionCanonicalText, IF_STMT, METHODCALL_EXPRESSION, false);
                    }
                }
                else if (ifConditionStatement instanceof PsiMethodCallExpression) {
                    PsiMethodCallExpression psiMethodCallExpression = (PsiMethodCallExpression) ifConditionStatement;

                    PsiReferenceExpression psiReferenceExpression = psiMethodCallExpression.getMethodExpression();
                    String referenceExpressionCanonicalText = psiReferenceExpression.getCanonicalText();
                    PsiExpression[] psiExpressionList = psiMethodCallExpression.getArgumentList().getExpressions();
                    astTriplet = new ASTtriplet(rootID++);
                    astTriplet.first_entity.add("ReferenceExpression: "+ referenceExpressionCanonicalText);
                    astTriplet.second_entity.add("Argument: " + "[]");
                    astTriplet.second_entity.add("Type: " + "[]");
                    astTriplet.third_entity.add("Relation: " + "IfCondition");
                    astTriplets.add(astTriplet);

                    PsiStatement thenBranch = psiIfStatement.getThenBranch();
                    if (thenBranch instanceof PsiBlockStatement) {
                        PsiBlockStatement psiBlockStatement = (PsiBlockStatement) thenBranch;
                        // there is something in then branch
                        if (!psiBlockStatement.getCodeBlock().isEmpty()) {
                            int blockStatementCount = psiBlockStatement.getCodeBlock().getStatementCount();
                            // list that stores the related triplets
                            related_triplets = new ArrayList<>();
                            for (int i = 0; i < blockStatementCount; i++) {
                                related_triplets.add("triplet_" + (if_next_available_id + i));
                            }

                            astTriplet = new ASTtriplet(rootID++);
                            astTriplet.first_entity.add("Related_triplets: " + "triplet_" + if_next_available_id);
                            astTriplet.second_entity.add("End_entity: " + "UNK");

                            astTriplet.third_entity.add("Relation: " + "IfBody");
                            astTriplets.add(astTriplet);

                            PsiStatement[] psiStatements = psiBlockStatement.getCodeBlock().getStatements();
                            for (PsiStatement psiStatement : psiStatements) {
                                if (psiStatement instanceof PsiDeclarationStatement) {
                                    whatStatement(astTriplets, psiStatement);
                                } else if (psiStatement instanceof PsiExpressionStatement)
                                    whatStatement(astTriplets, psiStatement);
                            }
                        }
                    }
                }
            }
        }
    }


    // GET ARGUMENTS
    private static void getArguments(ArrayList<ASTtriplet> astTriplets, PsiExpression[] psiExpressions, String first_Entity, String relationType, String classType, boolean isRoot) {
        // no arguments
        if (psiExpressions.length == 0) {
            // assume it always follow a methodcallexpression
            if (relationType.equals(TRY_STATEMENT)) {
                ASTtriplet astTriplet = new ASTtriplet(id++);
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
            else if (relationType.equals(NEW_EXPRESSION)) {
                ASTtriplet astTriplet = new ASTtriplet(next_available_id++);
                if (classType.equals("reference_expression")) {
                    astTriplet.first_entity.add(REFERENCE_EXP + ": " + first_Entity);
                }
                else if (classType.equals("reference_element"))
                    astTriplet.first_entity.add(REFERENCE_ELEMENT + ": " + first_Entity);
                    astTriplet.second_entity.add("Arguments: " + "UNK");
                    astTriplet.second_entity.add("Types: " + "UNK");
                    astTriplet.third_entity.add("Relation: " + NEW_EXPRESSION);
                    astTriplets.add(astTriplet);
            }
            else if (relationType.equals(METHODCALL_EXPRESSION)) {
                ASTtriplet astTriplet;
                if (isRoot) {
                    astTriplet = new ASTtriplet(rootID++);

                }
                else {
                    astTriplet = new ASTtriplet(next_available_id++);

                }
                if (classType.equals("reference_expression")) {
                    astTriplet.first_entity.add(REFERENCE_EXP + ": " + first_Entity);
                }
                astTriplet.second_entity.add("Arguments: " + "UNK");
                astTriplet.second_entity.add("Types: " + "UNK");
                astTriplet.third_entity.add("Relation: " + METHODCALL_EXPRESSION);
                astTriplets.add(astTriplet);
            }
            else if (relationType.equals(WHILE_STATEMENT)) {
                ASTtriplet astTriplet;
                if (isRoot) {
                    astTriplet = new ASTtriplet(rootID++);

                }
                else {
                    astTriplet = new ASTtriplet(next_available_id++);

                }

                astTriplet.first_entity.add(REFERENCE_EXP + ": " + first_Entity);
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
                astTriplet = new ASTtriplet(try_next_available_id++);
            } else if (relationType.equals(IF_STMT) || relationType.equals(THEN_BRANCH)) {
                astTriplet = new ASTtriplet(if_next_available_id++);
            } else {
                if (isRoot) {
                    astTriplet = new ASTtriplet(rootID++);
                    isRoot = false;
                } else {
                    astTriplet = new ASTtriplet(next_available_id++);
                }
            }

            ArrayList<String> argumentsList = new ArrayList<>();
            ArrayList<String> argumentsTypeList = new ArrayList<>();

            boolean stop = false;

            for (int i = 0; i < psiExpressions.length; i++) {
                PsiExpression psiExpression = psiExpressions[i];
                if (psiExpression instanceof PsiMethodCallExpression) {
                    if (classType.equals("reference_expression")) {
                        astTriplet.first_entity.add(REFERENCE_EXP + ": " + first_Entity);
                    }
                    astTriplet.second_entity.add("Arguments: " + "triplet_" + (next_available_id));
                    astTriplet.third_entity.add("Relation: " + METHODCALL_EXPRESSION);
                    astTriplets.add(astTriplet);
                    char_At_substring_negative_complex_bad_line = char_At_substring_negative_bad_line;

                    PsiElement[] children = ((PsiMethodCallExpression) psiExpression).getChildren();
                    whatElement(astTriplets, children, METHODCALL_EXPRESSION, isRoot);
                    stop = true;
                } else if (psiExpression instanceof PsiLiteralExpression) {
                    String argument = ((PsiLiteralExpression) psiExpression).getText();
                    String type = ((PsiLiteralExpression) psiExpression).getType().getPresentableText();

                    argumentsList.add(argument);
                    argumentsTypeList.add(type);
                } else if (psiExpression instanceof PsiReferenceExpression) {
                    String argument = ((PsiReferenceExpression) psiExpression).getText();
                    String type = ((PsiReferenceExpression) psiExpression).getType().getPresentableText();

                    argumentsList.add(argument);
                    argumentsTypeList.add(type);
                } else if (psiExpression instanceof PsiArrayAccessExpression) {
                    String argument = ((PsiArrayAccessExpression) psiExpression).getText();
                    String type = ((PsiArrayAccessExpression) psiExpression).getType().getPresentableText();
                    argumentsList.add(argument);
                    argumentsTypeList.add(type);
                } else if (psiExpression instanceof PsiNewExpression) {
                    String argument = ((PsiNewExpression) psiExpression).getText();
                    String type = ((PsiNewExpression) psiExpression).getType().getPresentableText();
                    argumentsList.add(argument);
                    argumentsTypeList.add(type);
                } else if (psiExpression instanceof PsiPrefixExpression) {
                    String argument = ((PsiPrefixExpression) psiExpression).getText();
                    String type = ((PsiPrefixExpression) psiExpression).getType().getPresentableText();
                    argumentsList.add(argument);
                    argumentsTypeList.add(type);
                }
            }
            if (classType.equals("reference_element")) {
                astTriplet.first_entity.add(REFERENCE_ELEMENT + ": " + first_Entity);
            } else if (classType.equals("reference_expression")) {
                astTriplet.first_entity.add(REFERENCE_EXP + ": " + first_Entity);
            }

            astTriplet.second_entity.add("Arguments: " + argumentsList.toString());
            astTriplet.second_entity.add("Types: " + argumentsTypeList.toString());

            if (relationType.equals(METHODCALL_EXPRESSION)) {
                astTriplet.third_entity.add("Relation: " + METHODCALL_EXPRESSION);

            } else if (relationType.equals(FOR_STATEMENT)) {
                astTriplet.first_entity.add(REFERENCE_EXP + ": " + first_Entity);
                astTriplet.third_entity.add("Relation: " + classType);
            } else if (relationType.equals(IF_STMT) || relationType.equals(THEN_BRANCH)) {
                astTriplet.first_entity.add(classType + ": " + first_Entity);
                astTriplet.third_entity.add("Relation: " + classType);
            } else {
                astTriplet.third_entity.add("Relation: " + NEW_EXPRESSION);
            }
            if (!stop) {
                astTriplets.add(astTriplet);
            }
        }
    }

    private static void getArrayInitializer(ArrayList<String> arrayInitializer, ArrayList<String> arrayInitializerType, PsiExpression[] psiExpressions) {
        for (PsiExpression psiExpression : psiExpressions) {
            if (psiExpression instanceof PsiLiteralExpression) {
                PsiLiteralExpression psiLiteralExpression = (PsiLiteralExpression) psiExpression;
                String name = psiLiteralExpression.getText();
                String type = psiLiteralExpression.getType().getPresentableText();
                arrayInitializer.add(name);
                arrayInitializerType.add(type);
            }
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

    private static boolean checkConstraint(ArrayList<ASTtriplet> astTriplets, String case_name) {
        String mark = "validate";
        if (case_name.equals("jpanelWarning")) {
            int test_id = 0;
            boolean test = false;
            for (ASTtriplet t : astTriplets) {
                int triplet_id = t.getID();
                if (t.third_entity.get(0).equals("Relation: MethodCallExpression")) {
                    if (t.first_entity.get(0).equals("ReferenceExpression: panel.add") | t.first_entity.get(0).equals("ReferenceExpression: panel.remove")) {
                        test_id = triplet_id;
                        mark = "add/remove";
                        for (ASTtriplet t1 : astTriplets) {
                            int id = t1.getID();
                            if (id > test_id) {
                                if (t1.first_entity.get(0).equals("ReferenceExpression: panel.validate")) {
                                    mark = "validate";
                                }
                            }
                        }
                    }
                }
            }
            if (mark.equals("validate")) {
                return false;
            }
            return true;
        }
        else if (case_name.equals("iteratorWarning")) {
            int test_id = 0;
            boolean next_test = false;
            boolean remove_test = false;
            int total_len = astTriplets.size();
            for (ASTtriplet t : astTriplets) {
                int triplet_id = t.getID();
                if (t.third_entity.get(0).equals("Relation: MethodCallExpression")) {
                    if (t.first_entity.get(0).equals("ReferenceExpression: iterator.next")) {
                        next_test = true;
                        test_id = triplet_id;
                    }
                }
                if (t.third_entity.get(0).equals("Relation: MethodCallExpression")) {
                    if (t.first_entity.get(0).equals("ReferenceExpression: iterator.remove")) {
                        remove_test = true;
                        test_id = triplet_id;
                    }
                }
            }
            if (next_test) {
                for (ASTtriplet t : astTriplets) {
                    int triplet_id = t.getID();
                    if (triplet_id < test_id) {
                        if (t.first_entity.get(0).equals("MethodCallExpression: iterator.hasNext()") | t.first_entity.get(0).equals("ReferenceExpression: iterator.hasNext")) {
                            return false;
                        }
                    }
                }
            }
            else if (remove_test) {
                for (ASTtriplet t: astTriplets) {
                    int triplet_id = t.getID();
                    if (triplet_id < test_id) {
                        if (t.first_entity.get(0).equals("ReferenceExpression: iterator.next")) {
                            return false;
                        }
                    }
                }
            }
            if (!remove_test && !next_test) {
                return false;
            }
            return true;
        }
        else if (case_name.equals("stringWarning")) {
            int test_id = 0;
            boolean test = false;
            String literalExpression = "";
            int idxOf_value = -1;
            for (ASTtriplet t : astTriplets) {
                int triplet_id = t.getID();
                if (t.third_entity.get(0).equals("Relation: DeclarationStatement")) {
                    String type = t.first_entity.get(0).split(",", 2)[0];
                    literalExpression = t.second_entity.get(0).split(":", 2)[1];
                    literalExpression = literalExpression.replace(" ", "");
                    literalExpression = literalExpression.replace("\"", "");
                }
                if (t.third_entity.get(0).equals("Relation: MethodCallExpression")) {
                    if (t.first_entity.get(0).equals("ReferenceExpression: string.indexOf")) {
                        String argument = t.second_entity.get(0).split(",", 2)[0];
                        argument = argument.split(":", 2)[1];
                        argument = argument.replace(" ", "");
                        argument = argument.replace("[", "");
                        argument = argument.replace("]", "");
                        argument = argument.replace("\'", "");
                        if (!literalExpression.contains(argument)) {
                            return true;

                        } else {
                            idxOf_value = literalExpression.indexOf(argument);
                        }
                    }
                }

                if (t.third_entity.get(0).split(",", 2)[0].equals("Relation: MethodCallExpression")) {
                    if (t.first_entity.get(0).split(",", 2)[0].equals("ReferenceExpression: string.substring")) {
                        String substring_arg = t.second_entity.get(0);
                        substring_arg = substring_arg.split(":", 2)[1];
                        String substring_type = t.second_entity.get(1);
                        substring_type = substring_type.replace(" ", "");
                        if (substring_type.equals("Types:[int]")) {
                            substring_arg = substring_arg.replace(" ", "");
                            substring_arg = substring_arg.replace("[", "");
                            substring_arg = substring_arg.replace("]", "");
                            int substring_arg_value = Integer.parseInt(substring_arg);
                            if (substring_arg_value <= 0) {
                                return true;
                            } else if (substring_arg_value >= literalExpression.length()) {
                                return true;
                            }
                        }
                        else if (substring_type.equals("Arguments:[]")) {
                            continue;
                        }
                        else if (substring_arg.contains("triplet")){
                            substring_arg = substring_arg.split("_",2)[1];
                            int related_triplet_id = Integer.parseInt(substring_arg);
                            for (ASTtriplet related_t : astTriplets) {
                                if (related_t.getID() == related_triplet_id && related_t.first_entity.equals("ReferenceExpression: string.indexOf")) {
                                    String argument = t.second_entity.get(0).split(",", 2)[0];
                                    argument = argument.split(":", 2)[1];
                                    argument = argument.replace(" ", "");
                                    argument = argument.replace("[", "");
                                    argument = argument.replace("]", "");
                                    argument = argument.replace("\'", "");
                                    if (!literalExpression.contains(argument)) {
                                        return true;
                                    }
                                }
                            }
                        }
                        else {
                            if (idxOf_value < 0) {
                                return true;
                            } else if (idxOf_value >= literalExpression.length()) {
                                return true;
                            }
                        }
                    }
                }
            }
            return false;
        }
        else if (case_name.equals("filereaderWarning")) {
            boolean fileExist = false;
            boolean directoryExist = false;
            boolean tryExist = false;
            for (ASTtriplet t : astTriplets) {
                if (t.first_entity.get(0).equals("ReferenceExpression: file.exists")) {
                    fileExist = true;
                }
                if (t.first_entity.get(0).equals("ReferenceExpression: file.isDirectory")) {
                    directoryExist = true;
                }
                if (t.third_entity.get(0).equals("Relation: TryStatement")) {
                    tryExist = true;
                }
                if (t.first_entity.get(0).equals("ReferenceElement: FileReader")) {
                    if (!tryExist) {
                        return true;
                    }
                    if (!fileExist) {
                        return true;
                    }
                    if (!directoryExist) {
                        return true;
                    }
                }
                if (t.first_entity.get(0).equals("ReferenceExpression: reader.read")) {
                    if (!tryExist) {
                        return true;
                    }
                }

            }
            return false;
        }
        return false;
    }

    private static String checkfiletry(ArrayList<ASTtriplet> astTriplets, String case_name) {
        if (case_name.equals("fileReaderTryCatch")) {
            boolean fileExist = false;
            boolean directoryExist = false;
            boolean tryExist = false;
            for (ASTtriplet t : astTriplets) {
                if (t.first_entity.get(0).equals("ReferenceExpression: file.exists")) {
                    fileExist = true;
                }
                if (t.first_entity.get(0).equals("ReferenceExpression: file.isDirectory")) {
                    directoryExist = true;
                }
                if (t.third_entity.get(0).equals("Relation: TryStatement")){
                    tryExist = true;
                }
                if (t.first_entity.get(0).equals("ReferenceElement: FileReader")) {
                    if (!tryExist) {
                        return "filereader_try";
                    }
                    if (!fileExist) {
                        return "file";
                    }
                    if (!directoryExist) {
                        return "directory";
                    }

                }
                if (t.first_entity.get(0).equals("ReferenceExpression: reader.read")) {
                    if (!tryExist) {
                        return "read_try";
                    }
                }
            }
        }
        return "end";
    }

    private static ArrayList<String> checkCapability(ArrayList<ASTtriplet> astTriplets, String case_name) {
        ArrayList<String> problems = new ArrayList<>();
        if (case_name.equals("filereaderWarning")) {
            boolean fileExist = false;
            boolean directoryExist = false;
            boolean tryExist = false;
            for (ASTtriplet t : astTriplets) {
                if (t.first_entity.get(0).equals("ReferenceExpression: file.exists")) {
                    fileExist = true;
                }
                if (t.first_entity.get(0).equals("ReferenceExpression: file.isDirectory")) {
                    directoryExist = true;
                }
                if (t.third_entity.get(0).equals("Relation: TryStatement")){
                    tryExist = true;
                }
                if (t.first_entity.get(0).equals("ReferenceElement: FileReader")) {
                    if (!tryExist) {
                        problems.add("filereader_try");
                    }
                    if (!fileExist) {
                        problems.add("file");
                    }
                    if (!directoryExist) {
                        problems.add("directory");
                    }

                }
                if (t.first_entity.get(0).equals("ReferenceExpression: reader.read")) {
                    if (!tryExist) {
                        problems.add("read_try");
                    }
                }
            }
        }
        return problems;
    }

    private static ArrayList<String> checkJPanel(ArrayList<ASTtriplet> astTriplets, String case_name) {
        ArrayList<String> marks = new ArrayList<>();
        if (case_name.equals("jpanelWarning")) {
            int test_id = 0;
            boolean test = false;
            for (ASTtriplet t : astTriplets) {
                int triplet_id = t.getID();
                if (t.third_entity.get(0).equals("Relation: MethodCallExpression")) {
                    if (t.first_entity.get(0).equals("ReferenceExpression: panel.add")) {
                        marks.add("add");
                    }
                    else if (t.first_entity.get(0).equals("ReferenceExpression: panel.remove")) {
                        marks.add("remove");
                    }
                    test_id = triplet_id;
                    for (ASTtriplet t1 : astTriplets) {
                        int id = t1.getID();
                        if (id > test_id) {
                            if (t1.first_entity.get(0).equals("ReferenceExpression: panel.validate")) {
                                marks.clear();
                            }
                        }
                    }
                }
            }
        }
        return marks;
    }

    private static ArrayList<String> checkcharAt(ArrayList<ASTtriplet> astTriplets, String case_name) {
        ArrayList<String> problems = new ArrayList<>();
        if (case_name.equals("stringWarning")) {
            int test_id = 0;
            boolean test = false;
            String s = "";
            int idxOf_value = -1;
            for (ASTtriplet t : astTriplets) {
                int triplet_id = t.getID();
                if (t.third_entity.get(0).equals("Relation: DeclarationStatement") && t.second_entity.get(0).split(":",2)[0].equals("LiteralExpression")) {
                    String type = t.first_entity.get(0).split(",", 2)[0];
                    s = t.second_entity.get(0).split(":", 2)[1];
                    s = s.replace(" ", "");
                    s = s.replace("\"", "");

                }
                if (t.third_entity.get(0).equals("Relation: MethodCallExpression")) {
                    if (t.first_entity.get(0).equals("ReferenceExpression: string.indexOf")) {
                        String argument = t.second_entity.get(0).split(",", 2)[0];
                        argument = argument.split(":", 2)[1];
                        argument = argument.replace(" ", "");
                        argument = argument.replace("[", "");
                        argument = argument.replace("]", "");
                        argument = argument.replace("\'", "");

                        if (!s.contains(argument)) {
                            problems.add("indexOf_constraint");
                        } else {
                            idxOf_value = s.indexOf(argument);
                        }

                    }
                }

                if (t.third_entity.get(0).split(",", 2)[0].equals("Relation: MethodCallExpression")) {
                    if (t.first_entity.get(0).split(",", 2)[0].equals("ReferenceExpression: string.substring")) {
                        String substring_arg = t.second_entity.get(0);
                        substring_arg = substring_arg.split(":", 2)[1];
                        String substring_type = t.second_entity.get(1);
                        substring_type = substring_type.replace(" ", "");
                        if (substring_type.equals("Types:[int]")) {
                            substring_arg = substring_arg.replace(" ", "");
                            substring_arg = substring_arg.replace("[", "");
                            substring_arg = substring_arg.replace("]", "");
                            int substring_arg_value = Integer.parseInt(substring_arg);
                            if (substring_arg_value <= 0) {
                                problems.add("substring_negative");
                            } else if (substring_arg_value >= s.length()) {
                                problems.add("substring_larger");
                            }
                        }
                        else if (substring_arg.contains("triplet")) {
                            substring_arg = substring_arg.split("_", 2)[1];
                            int related_triplet_id = Integer.parseInt(substring_arg);
                            for (ASTtriplet related_t : astTriplets) {
                                if (related_t.getID() == related_triplet_id && related_t.first_entity.get(0).equals("ReferenceExpression: string.indexOf")) {
                                    String argument = related_t.second_entity.get(0).split(",", 2)[0];
                                    argument = argument.split(":", 2)[1];
                                    argument = argument.replace(" ", "");
                                    argument = argument.replace("[", "");
                                    argument = argument.replace("]", "");
                                    argument = argument.replace("\'", "");
                                    if (!s.contains(argument)) {
                                        problems.add("substring_negative_complex");
                                    }
                                }
                            }
                        }
                        else if (substring_type.equals("Arguments:[]")) {
                            continue;
                        }

                        else {
                            if (idxOf_value < 0) {
                                problems.add("substring_indexOf_negative");
                            }
                            else if (idxOf_value >= s.length()) {
                                problems.add("substring_larger");
                            }
                        }
                    }
                }
            }
        }
        return problems;
    }

    private static ArrayList<String> checkiteratorPattern(ArrayList<ASTtriplet> astTriplets, String case_name) {
        ArrayList<String> problems = new ArrayList<>();
        boolean hasnext_b = false;
        boolean remove_b = false;
        boolean next_test = false;
        boolean remove_test = false;
        if (case_name.equals("iteratorWarning")) {
            int total_len = astTriplets.size();
            int test_id = 0;
            for (ASTtriplet t : astTriplets) {
                int triplet_id = t.getID();
                if (t.third_entity.get(0).equals("Relation: MethodCallExpression")) {
                    if (t.first_entity.get(0).equals("ReferenceExpression: iterator.next")) {
                        next_test = true;
                        test_id = triplet_id;
                    }
                }
                if (t.third_entity.get(0).equals("Relation: MethodCallExpression")) {
                    if (t.first_entity.get(0).equals("ReferenceExpression: iterator.remove")) {
                        remove_test = true;
                        test_id = triplet_id;
                    }
                }
            }
            if (next_test) {
                for (ASTtriplet t : astTriplets) {
                    int triplet_id = t.getID();
                    if (triplet_id < test_id) {
                        if (t.first_entity.get(0).equals("MethodCallExpression: iterator.hasNext()") | t.first_entity.get(0).equals("ReferenceExpression: iterator.hasNext")) {
                            hasnext_b = true;
                        }
                    }
                }
            }
            if (remove_test) {
                for (ASTtriplet t : astTriplets) {
                    int triplet_id = t.getID();
                    if (triplet_id < test_id) {
                        if (t.first_entity.get(0).equals("ReferenceExpression: iterator.next")) {
                            remove_b = true;
                        }
                    }
                }
            }
        }
        if (!hasnext_b && next_test) {
            problems.add("hasnext");
        }
        if (!remove_b) {
            problems.add("remove");
        }
        return problems;
    }


    private static String getCase(ArrayList<ASTtriplet> astTriplets) {
        for (ASTtriplet t : astTriplets) {
            if (t.getID() == 0) {
                String first_entity = t.first_entity.get(0);
                String name = first_entity.split(",", 2)[0];
                String case_name = name.split(":", 2)[1];
                case_name = case_name.replace(" ", "");
                return case_name;
            }
        }
        return "error";
    }

}




