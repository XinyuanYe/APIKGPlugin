import com.intellij.codeInspection.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.siyeh.ig.psiutils.ExpressionUtils.isNullLiteral;

public class APIChainInspection extends AbstractBaseJavaLocalInspectionTool {

    private static final Logger LOG = Logger.getInstance("#com.intellij.codeInspection.APIChainInspection");
    private final CriQuickFix myQuickFix = new CriQuickFix();

    // Defines the text of the quick fix intention
    public static final String QUICK_FIX_NAME = "SDK: " + InspectionsBundle.message("inspection.comparing.references.use.quickfix");

    private HashMap<String, String> ast_info = new HashMap<>();
    private HashMap<String, String> reference_info = new HashMap<>();
    private static String fileName;

    /**
     * This method is overridden to provide a custom visitor
     * that inspects expressions with relational operators '==' and '!='
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

            public void visitMethod(PsiMethod psiMethod) {
                super.visitMethod(psiMethod);
                fileName = psiMethod.getContainingFile().getName();
            }


            /**
             * Evaluate reference expression for possible API chain.
             *
             * @param psiReferenceExpression The expression to be evaluated.
             */
            @Override
            public void visitReferenceExpression(PsiReferenceExpression psiReferenceExpression) {
                super.visitReferenceExpression(psiReferenceExpression);

                String reference = psiReferenceExpression.getReferenceName();

                PsiElement local_variable = psiReferenceExpression.getParent();
                if (local_variable instanceof PsiLocalVariable) {
                    String variable = local_variable.toString().split(":")[1];
                    reference_info.put(variable, reference_info.getOrDefault(reference, reference));
                }
            }

            /**
             * This string defines the short message shown to a user signaling the inspection
             * found a problem. It reuses a string from the inspections bundle.
             */
            @NonNls
            private final String DESCRIPTION_TEMPLATE = "SDK " + InspectionsBundle.message("inspection.comparing.references.problem.descriptor");

            /**
             * Evaluate method call expression for possible API chain.
             *
             * @param psiMethodCallExpression The expression to be evaluated.
             */
            @Override
            public void visitMethodCallExpression(PsiMethodCallExpression psiMethodCallExpression) {
                super.visitMethodCallExpression(psiMethodCallExpression);

                PsiReferenceExpression referenceExpression = psiMethodCallExpression.getMethodExpression();

                String methodName = referenceExpression.getCanonicalText();

                // we do not consider println
                if (!methodName.equals("System.out.println")) {

                    // Step 1: record AST INFORMATION
                    // get the method and its parent
                    // store them in a hashmap
                    String text = psiMethodCallExpression.getText();
                    String parent = psiMethodCallExpression.getParent().toString();
                    parent = parent.split(" ")[0];

                    ast_info.put(text, parent);

                    // Step 2: record potential API chain
                    PsiExpressionList argument_list = psiMethodCallExpression.getArgumentList();

                    if (!argument_list.isEmpty()) {
                        PsiExpression[] argument_expressions = argument_list.getExpressions();
                        for (PsiExpression argument : argument_expressions) {
                            // confirmed case of API chain
                            if (argument instanceof PsiMethodCallExpression) {
                                holder.registerProblem(psiMethodCallExpression.getMethodExpression(), "API call involve " + argument.getText(), myQuickFix);
                                int offset = psiMethodCallExpression.getTextOffset();
                                int lineNumber = StringUtil.offsetToLineNumber(psiMethodCallExpression.getContainingFile().getText(), offset) + 1;
                                generateReport(psiMethodCallExpression.getText() + " -> " + argument.getText() + " in line " + lineNumber);

                            } else if (argument instanceof PsiReferenceExpression) {
                                String reference = ((PsiReferenceExpression) argument).getCanonicalText();

                                if (reference_info.containsKey(reference)) {
                                    reference = reference_info.get(reference);
                                }

                                for (Map.Entry<String, String> entry : ast_info.entrySet()) {
                                    String key = entry.getKey();
                                    String value = entry.getValue();
                                    String variable = (value.contains(":") && (value.charAt(value.length() - 1) != ':')) ? value.split(":")[1] : value;
                                    // found a reference API chain
                                    if (variable.equals(reference)) {
                                        holder.registerProblem(psiMethodCallExpression.getMethodExpression(), "API call involve " + key, myQuickFix);
                                        int offset = psiMethodCallExpression.getTextOffset();
                                        int lineNumber = StringUtil.offsetToLineNumber(psiMethodCallExpression.getContainingFile().getText(), offset) + 1;
                                        generateReport(psiMethodCallExpression.getText() + " -> " + key + " in line " + lineNumber);
                                    }
                                }


                            }
                        }
                    }
                }

            }

        };
    }

    private static void generateReport(String APIChain) {
        if (fileName == null) {
            return;
        }
        try {
            String output_file = "/Users/xinyuan/IdeaProjects/my_gradle_plugin/src/main/java/Report/" + fileName + ".txt";
            File f = new File(output_file);

            PrintWriter out = null;
            if ( f.exists() && !f.isDirectory() ) {
                out = new PrintWriter(new FileOutputStream(new File(output_file), true));
            }
            else {
                out = new PrintWriter(output_file);
            }
            APIChain = APIChain + System.lineSeparator();
            out.append(APIChain);
            out.close();
            String timeStamp = new SimpleDateFormat("yyyy.MM.dd. HH:mm:ss").format(new Date());
            System.out.println("Successfully wrote to " + fileName + ".txt" + " at " + timeStamp);
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
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
}

