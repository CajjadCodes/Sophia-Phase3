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
import main.compileErrorException.typeErrors.ConditionNotBool;
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

    public TypeChecker(Graph<String> classHierarchy) {
        this.classHierarchy = classHierarchy;
        this.expressionTypeChecker = new ExpressionTypeChecker(classHierarchy);
    }

    @Override
    public Void visit(Program program) {
        SymbolTable.top = SymbolTable.root;
        for(ClassDeclaration classDeclaration : program.getClasses()) {
            classDeclaration.accept(this);
        }
        return null;
    }

    @Override
    public Void visit(ClassDeclaration classDeclaration) {
        try {
            ClassSymbolTableItem classSymbolTableItem = (ClassSymbolTableItem) SymbolTable.top
                    .getItem(ClassSymbolTableItem.START_KEY + classDeclaration.getClassName().getName(), false);
            SymbolTable.push(classSymbolTableItem.getClassSymbolTable());

            for(FieldDeclaration fieldDeclaration : classDeclaration.getFields()) {
                fieldDeclaration.accept(this);
            }
            if(classDeclaration.getConstructor() != null) {
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
            MethodSymbolTableItem methodSymbolTableItem = (MethodSymbolTableItem) SymbolTable.top
                    .getItem(MethodSymbolTableItem.START_KEY + methodDeclaration.getMethodName().getName(), false);
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
        // nothing?
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
        if (!(conditionType instanceof BoolType)) {
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
        print.accept(this.expressionTypeChecker);
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
        if (!(conditionType instanceof BoolType)) {
            ConditionNotBool exception = new ConditionNotBool(forStmt.getLine());
            forStmt.addError(exception);
        }
        forStmt.getUpdate().accept(this);
        forStmt.getBody().accept(this);
        return null;
    }

}
