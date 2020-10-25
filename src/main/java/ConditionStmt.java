import com.intellij.psi.PsiElement;

public class ConditionStmt {

    PsiElement element;
    String condition, functionBelong;
    int lineNumber;

    public ConditionStmt(PsiElement element, String condition, int lineNumber, String functionBelong) {
        this.element = element;
        this.condition = condition;
        this.lineNumber = lineNumber;
        this.functionBelong = functionBelong;
    }

    public PsiElement getElement() {
        return this.element;
    }

    public String getCondition() {
        return this.condition;
    }

    public int getLineNumber() {
        return this.lineNumber;
    }

    public String getFunctionBelong() {
        return this.functionBelong;
    }

    @Override
    public String toString() {
        return condition + " " + lineNumber + " " + functionBelong;
    }
}
