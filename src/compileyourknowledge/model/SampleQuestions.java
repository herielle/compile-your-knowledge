package compileyourknowledge.model;


public final class SampleQuestions {

    private SampleQuestions() {
    }

    public static Question[] theoretical() {
        String t = "Procedural";
        return new Question[]{
            new Question(t, 1,
                "Which statement best describes the procedural programming paradigm?",
                new String[]{
                    "Programs are organized as a sequence of procedures (routines) that operate on data.",
                    "Programs are built from objects that bundle data with methods.",
                    "Programs are expressions built from pure mathematical functions.",
                    "Programs are sets of facts and rules answered by queries."},
                0, "Think of C: functions called one after another."),
            new Question(t, 1,
                "In procedural programming, what is the main purpose of a function (procedure)?",
                new String[]{
                    "To store an object's state",
                    "To group reusable steps under one name that can be called many times",
                    "To declare facts for a logic engine",
                    "To react to a user event automatically"},
                1, "Reuse and decomposition of a big task."),
            new Question(t, 2,
                "Read the following pseudocode carefully.\n\n"
                + "    procedure main()\n"
                + "        total <- 0\n"
                + "        for i <- 1 to 5 do\n"
                + "            total <- addSquare(total, i)\n"
                + "        end for\n"
                + "        print(total)\n"
                + "    end procedure\n\n"
                + "    function addSquare(sum, n)\n"
                + "        return sum + (n * n)\n"
                + "    end function\n\n"
                + "What does main() print?",
                new String[]{"15", "55", "30", "225"},
                1, "1 + 4 + 9 + 16 + 25."),
            new Question(t, 2,
                "Which of the following is a common drawback of purely procedural design in large programs?",
                new String[]{
                    "It cannot use loops",
                    "Data is often shared globally, so changes in one procedure can unexpectedly affect others",
                    "It cannot be compiled",
                    "It forbids the use of functions"},
                1, "Think about global variables."),
            new Question(t, 3,
                "A program is structured as shown below. Which paradigm characteristic does this layout MOST clearly show?\n\n"
                + "    main\n"
                + "     |-- readInput()\n"
                + "     |-- validate()\n"
                + "     |      |-- checkRange()\n"
                + "     |      '-- checkFormat()\n"
                + "     |-- compute()\n"
                + "     |      |-- normalize()\n"
                + "     |      |-- applyRules()\n"
                + "     |      '-- aggregate()\n"
                + "     '-- printReport()\n"
                + "            |-- formatHeader()\n"
                + "            |-- formatRows()\n"
                + "            '-- formatFooter()\n\n"
                + "Each box is a routine that calls smaller routines to finish its job.",
                new String[]{
                    "Top-down stepwise refinement into smaller procedures",
                    "Inheritance and polymorphism",
                    "Lazy evaluation of expressions",
                    "Backtracking over a knowledge base"},
                0, "A big task broken into smaller named steps, from the top."),
        };
    }

    public static Question[] practical() {
        String t = "C";
        return new Question[]{
            new Question(t, 1,
                "Which C function is used to print formatted text to the console?",
                new String[]{"echo()", "printf()", "System.out.println()", "write_line()"},
                1, "It is declared in <stdio.h>."),
            new Question(t, 1,
                "What is the correct way to declare an integer variable named count in C?",
                new String[]{"integer count;", "count : int;", "int count;", "var count = int;"},
                2, "The type comes first."),
            new Question(t, 2,
                "What is the output of the following C program?\n\n"
                + "    #include <stdio.h>\n\n"
                + "    int mystery(int n) {\n"
                + "        if (n <= 1) {\n"
                + "            return 1;\n"
                + "        }\n"
                + "        return n * mystery(n - 1);\n"
                + "    }\n\n"
                + "    int main(void) {\n"
                + "        int result = mystery(5);\n"
                + "        printf(\"%d\\n\", result);\n"
                + "        return 0;\n"
                + "    }",
                new String[]{"15", "24", "120", "720"},
                2, "This is a factorial."),
            new Question(t, 3,
                "What does the following C program print?\n\n"
                + "    #include <stdio.h>\n\n"
                + "    void swap(int a, int b) {\n"
                + "        int temp = a;\n"
                + "        a = b;\n"
                + "        b = temp;\n"
                + "    }\n\n"
                + "    void swapPtr(int *a, int *b) {\n"
                + "        int temp = *a;\n"
                + "        *a = *b;\n"
                + "        *b = temp;\n"
                + "    }\n\n"
                + "    int main(void) {\n"
                + "        int x = 3, y = 7;\n"
                + "        swap(x, y);\n"
                + "        printf(\"%d %d\\n\", x, y);\n"
                + "        swapPtr(&x, &y);\n"
                + "        printf(\"%d %d\\n\", x, y);\n"
                + "        return 0;\n"
                + "    }",
                new String[]{
                    "7 3, then 3 7",
                    "3 7, then 7 3",
                    "3 7, then 3 7",
                    "7 3, then 7 3"},
                1, "C passes arguments by value, so the first swap changes only copies."),
            new Question(t, 3,
                "Which line in this C code is the cause of a compile error?\n\n"
                + "    1  #include <stdio.h>\n"
                + "    2\n"
                + "    3  int main(void) {\n"
                + "    4      int values[3] = {1, 2, 3};\n"
                + "    5      int sum = 0;\n"
                + "    6      for (int i = 0; i < 3; i++) {\n"
                + "    7          sum += values[i]\n"
                + "    8      }\n"
                + "    9      printf(\"%d\\n\", sum);\n"
                + "    10     return 0;\n"
                + "    11 }",
                new String[]{"Line 4", "Line 6", "Line 7", "Line 9"},
                2, "Look for a missing terminator."),
        };
    }
}
