import com.intellij.psi.PsiElement;

public class MethodCallExp {

    PsiElement element;
    String name, functionBelong;
    int lineNumber;

    public MethodCallExp(PsiElement element, String methodName, int lineNumber, String functionBelong) {
        this.element = element;
        this.name = methodName;
        this.lineNumber = lineNumber;
        this.functionBelong = functionBelong;
    }

    public PsiElement getElement() {
        return this.element;
    }

    public String getName() {
        return this.name;
    }

    public int getLineNumber() {
        return this.lineNumber;
    }

    public String getFunctionBelong() {
        return this.functionBelong;
    }

    @Override
    public String toString() {
        return name + " " + lineNumber + " " + functionBelong;
    }

}
