// "Create Parameter 'popop'" "true"
class Calculator {
    public void printError() {
        int i = <caret>popop;
    }

    {
        printError();
    }
}
