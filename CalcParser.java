import java.util.*;

// a type for arithmetic expressions
interface AExp {
    double eval();                   
    List<AInstr> compile();                  
}

class Num implements AExp {
    protected double val;
    Num(double v) {
        val = v;
    }
    public double eval() {
        return val;
    }
    public List<AInstr> compile() {
         List<AInstr> l = new ArrayList<AInstr>();
         l.add(new Push(val));
         return l;
    }
}

class BinOp implements AExp {
    protected AExp left, right;
    protected Op op;
    BinOp(AExp l, Op o, AExp r) {
        left = l;
        right = r;
        op = o;
        
    }
    public double eval() {
        return op.calculate(left.eval(),right.eval());
    }
    public List<AInstr> compile() {
        List<AInstr> l = new ArrayList<AInstr>();
        l.addAll(left.compile());
        l.addAll(right.compile());
        l.add(new Calculate(op));
        return l;
    }
}

// a representation of four arithmetic operators
enum Op {
    PLUS { public double calculate(double a1, double a2) { return a1 + a2; } },
    MINUS { public double calculate(double a1, double a2) { return a1 - a2; } },
    TIMES { public double calculate(double a1, double a2) { return a1 * a2; } },
    DIVIDE { public double calculate(double a1, double a2) { return a1 / a2; } };

    abstract double calculate(double a1, double a2);
}

// a type for arithmetic instructions
interface AInstr {
    public Stack<Double> oper(Stack<Double> s);
}

class Push implements AInstr {
    protected double val;
    Push(double v) {
        val = v;
    }
    public Stack<Double> oper(Stack<Double> s) {
        s.push(val);
        return s;
    }
}

class Calculate implements AInstr {
    protected Op op;
    Calculate(Op o) {
        op = o;
    }
    public Stack<Double> oper(Stack<Double> s) {
        double first = s.pop();
        double second = s.pop();
        s.push(op.calculate(first,second));
        return s;
    }
}

class Instrs {
    protected List<AInstr> instrs;
    Stack<Double> stack;
    public Instrs(List<AInstr> instrs) { this.instrs = instrs; }

    public double eval() {
        stack = new Stack<Double>();
        for (AInstr a: instrs) {
            stack = a.oper(stack);
        } 
        return stack.pop();
    }
}


class CalcTest {
    public static void main(String[] args) {
 
    AExp aexp =
        new BinOp(new BinOp(new Num(1.0), Op.PLUS, new Num(2.0)),
           Op.TIMES,
           new Num(3.0));
    System.out.println("aexp evaluates to " + aexp.eval()); // aexp evaluates to 9.0

    List<AInstr> is = new LinkedList<AInstr>();
    is.add(new Push(1.0));
    is.add(new Push(2.0));
    is.add(new Calculate(Op.PLUS));
    is.add(new Push(3.0));
    is.add(new Calculate(Op.TIMES));
    Instrs instrs = new Instrs(is);
    System.out.println("instrs evaluates to " + instrs.eval());  // instrs evaluates to 9.0

    System.out.println("aexp converts to " + aexp.compile());

    }
}
