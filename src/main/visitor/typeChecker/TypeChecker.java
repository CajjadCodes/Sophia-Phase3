package main.visitor.typeChecker;

import main.ast.nodes.Program;
import main.ast.nodes.declaration.classDec.ClassDeclaration;
import main.ast.nodes.declaration.classDec.classMembersDec.ConstructorDeclaration;
import main.ast.nodes.declaration.classDec.classMembersDec.FieldDeclaration;
import main.ast.nodes.declaration.classDec.classMembersDec.MethodDeclaration;
import main.ast.nodes.declaration.variableDec.VarDeclaration;
import main.ast.nodes.statement.*;
import main.ast.nodes.statement.loop.BreakStmt;
import main.ast.nodes.statement.loop.ContinueStmt;
import main.ast.nodes.statement.loop.ForStmt;
import main.ast.nodes.statement.loop.ForeachStmt;
import main.ast.types.single.BoolType;
import main.ast.types.single.IntType;
import main.ast.types.single.StringType;
import main.compileErrorException.typeErrors.*;
import main.symbolTable.SymbolTable;
import main.symbolTable.exceptions.ItemNotFoundException;
import main.symbolTable.items.ClassSymbolTableItem;
import main.symbolTable.items.MethodSymbolTableItem;
import main.symbolTable.utils.graph.Graph;
import main.visitor.Visitor;
import main.ast.types.Type;

public class TypeChecker extends Visitor<Void> {
    private final Graph<String> classHierarchy;
    private final ExpressionTypeChecker expressionTypeChecker;
    public static ClassDeclaration currentClass = null;

    public TypeChecker(Graph<String> classHierarchy) {
        this.classHierarchy = classHierarchy;
        this.expressionTypeChecker = new ExpressionTypeChecker(classHierarchy);
    }

    public void checkMainClassErrors(ClassDeclaration mainClassDeclaration)
    {
        // error number 26
        //todo: check if it works properly
        if(mainClassDeclaration.getParentClassName() != null) {
            MainClassCantExtend exception = new MainClassCantExtend(mainClassDeclaration.getLine());
            mainClassDeclaration.addError(exception);
        }
        // error number 28
        if(mainClassDeclaration.getConstructor() == null) {
            NoConstructorInMainClass exception = new NoConstructorInMainClass(mainClassDeclaration);
            mainClassDeclaration.addError(exception);
        }
        // error number 29
        else if (!mainClassDeclaration.getConstructor().getArgs().isEmpty()) {
            MainConstructorCantHaveArgs exception = new MainConstructorCantHaveArgs(mainClassDeclaration.getLine());
            mainClassDeclaration.addError(exception);
        }


    }


    @Override
    public Void visit(Program program) {
        boolean mainFound = false;
        SymbolTable.top = SymbolTable.root;
        for(ClassDeclaration classDeclaration : program.getClasses()) {
            if(classDeclaration.getClassName().toString().equals("Main")) {
                mainFound = true;
                checkMainClassErrors(classDeclaration);
            }
            currentClass = classDeclaration;
            classDeclaration.accept(this);
            currentClass = null;
        }
        if(!mainFound) {
            NoMainClass exception = new NoMainClass();
            program.addError(exception);
        }
        return null;
    }

    @Override
    public Void visit(ClassDeclaration classDeclaration) {
        try {
            ClassSymbolTableItem classSymbolTableItem = (ClassSymbolTableItem) SymbolTable.top
                    .getItem(ClassSymbolTableItem.START_KEY + classDeclaration.getClassName().getName(), false);
            SymbolTable.push(classSymbolTableItem.getClassSymbolTable());

            // error number 27
            if(classDeclaration.getParentClassName().toString().equals("Main")) {
                CannotExtendFromMainClass exception = new CannotExtendFromMainClass(classDeclaration.getLine());
                classDeclaration.addError(exception);
            }

            for(FieldDeclaration fieldDeclaration : classDeclaration.getFields()) {
                fieldDeclaration.accept(this);
            }
            if(classDeclaration.getConstructor() != null) {

                //error number 17
                if(!classDeclaration.getConstructor().getMethodName().toString().equals(classDeclaration.getClassName().toString()))
                {
                    ConstructorNotSameNameAsClass exception = new ConstructorNotSameNameAsClass(classDeclaration.getConstructor().getLine());
                    classDeclaration.getConstructor().addError(exception);
                }

                classDeclaration.getConstructor().accept(this);
            }
            for(MethodDeclaration methodDeclaration : classDeclaration.getMethods()) {
                methodDeclaration.accept(this);
            }
            SymbolTable.pop();
        } catch (ItemNotFoundException ignored) {}
        return null;
    }

    @Override
    public Void visit(ConstructorDeclaration constructorDeclaration) {
        this.visit((MethodDeclaration) constructorDeclaration);
        return null;
    }

    @Override
    public Void visit(MethodDeclaration methodDeclaration) {
        try {
            MethodSymbolTableItem methodSymbolTableItem = (MethodSymbolTableItem) SymbolTable.top.getItem(MethodSymbolTableItem.START_KEY + methodDeclaration.getMethodName().getName(), false);
            SymbolTable.push(methodSymbolTableItem.getMethodSymbolTable());
            for(VarDeclaration varDeclaration : methodDeclaration.getArgs()) {
                varDeclaration.accept(this);
            }
            for(VarDeclaration varDeclaration : methodDeclaration.getLocalVars()) {
                varDeclaration.accept(this);
            }
            for(Statement statement : methodDeclaration.getBody()) {
                statement.accept(this);
            }
            SymbolTable.pop();
        } catch (ItemNotFoundException ignored) {}

        return null;
    }

    @Override
    public Void visit(FieldDeclaration fieldDeclaration) {
        fieldDeclaration.getVarDeclaration().accept(this);
        return null;
    }

    @Override
    public Void visit(VarDeclaration varDeclaration) {
        return null;
    }

    @Override
    public Void visit(AssignmentStmt assignmentStmt) {
        assignmentStmt.getlValue().accept(this.expressionTypeChecker);
        assignmentStmt.getrValue().accept(this.expressionTypeChecker);
        return null;
    }

    @Override
    public Void visit(BlockStmt blockStmt) {
        for(Statement statement : blockStmt.getStatements()) {
            statement.accept(this);
        }
        return null;
    }

    @Override
    public Void visit(ConditionalStmt conditionalStmt) {
        Type conditionType = conditionalStmt.getCondition().accept(this.expressionTypeChecker);
        if (!(conditionType instanceof BoolType)) { //Error 5
            ConditionNotBool exception = new ConditionNotBool(conditionalStmt.getLine());
            conditionalStmt.addError(exception);
        }
        conditionalStmt.getThenBody().accept(this);
        conditionalStmt.getElseBody().accept(this);
        return null;
    }

    @Override
    public Void visit(MethodCallStmt methodCallStmt) {
        methodCallStmt.accept(this.expressionTypeChecker);
        return null;
    }

    @Override
    public Void visit(PrintStmt print) {
        Type exprType = print.accept(this.expressionTypeChecker);
        if (!(exprType instanceof IntType) && !(exprType instanceof BoolType) && !(exprType instanceof StringType)) { //Error 10
            UnsupportedTypeForPrint exception = new UnsupportedTypeForPrint(print.getLine());
            print.addError(exception);
        }
        return null;
    }

    @Override
    public Void visit(ReturnStmt returnStmt) {
        returnStmt.getReturnedExpr().accept(this.expressionTypeChecker);
        return null;
    }

    @Override
    public Void visit(BreakStmt breakStmt) {
        return null;
    }

    @Override
    public Void visit(ContinueStmt continueStmt) {
        return null;
    }

    @Override
    public Void visit(ForeachStmt foreachStmt) {
        foreachStmt.getList().accept(this.expressionTypeChecker);
        foreachStmt.getBody().accept(this);
        return null;
    }

    @Override
    public Void visit(ForStmt forStmt) {
        forStmt.getInitialize().accept(this);
        Type conditionType = forStmt.getCondition().accept(this.expressionTypeChecker);
        if (!(conditionType instanceof BoolType)) { //Error 5
            ConditionNotBool exception = new ConditionNotBool(forStmt.getLine());
            forStmt.addError(exception);
        }
        forStmt.getUpdate().accept(this);

        forStmt.getBody().accept(this);
        return null;
    }

}
