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
import main.ast.types.NoType;
import main.ast.types.list.ListNameType;
import main.ast.types.list.ListType;
import main.ast.types.single.BoolType;
import main.ast.types.single.IntType;
import main.ast.types.single.StringType;
import main.compileErrorException.typeErrors.*;
import main.symbolTable.SymbolTable;
import main.symbolTable.exceptions.ItemNotFoundException;
import main.symbolTable.items.ClassSymbolTableItem;
import main.symbolTable.items.LocalVariableSymbolTableItem;
import main.symbolTable.items.MethodSymbolTableItem;
import main.symbolTable.utils.graph.Graph;
import main.visitor.Visitor;
import main.ast.types.Type;

import java.util.Collection;

public class TypeChecker extends Visitor<Void> {
    private final Graph<String> classHierarchy;
    private final ExpressionTypeChecker expressionTypeChecker;
    public static ClassDeclaration currentClass = null;
    public static int loopDepthCount = 0;

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
        //TODO: check to see if it works properly
        //error number 11
        varDeclaration.accept(this);
        Type varType = varDeclaration.getType();
        if(varType instanceof ListType)
        {

        }
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
        if(loopDepthCount <= 0)
        {
            ContinueBreakNotInLoop exception = new ContinueBreakNotInLoop(breakStmt.getLine(), 0);
            breakStmt.addError(exception);
        }
        return null;
    }

    @Override
    public Void visit(ContinueStmt continueStmt) {
        if(loopDepthCount <= 0)
        {
            ContinueBreakNotInLoop exception = new ContinueBreakNotInLoop(continueStmt.getLine(), 1);
            continueStmt.addError(exception);
        }
        return null;
    }

    @Override
    public Void visit(ForeachStmt foreachStmt) {
        loopDepthCount += 1;
        //TODO: is this necessary?
        //foreachStmt.getVariable().accept(this);
        Type returnedType = foreachStmt.getList().accept(this.expressionTypeChecker);

        //error number 19
        if(!(returnedType instanceof ListType || returnedType instanceof NoType))
        {
            ForeachCantIterateNoneList exception = new ForeachCantIterateNoneList(foreachStmt.getLine());
            foreachStmt.addError(exception);
        }
        else if(!(returnedType instanceof NoType))
        {
            //error number 20
            Type tempType = null;
            boolean typeSet = false;
            for(ListNameType type:((ListType) returnedType).getElementsTypes())
            {
                if(!(type.getType() instanceof NoType) && !(typeSet))
                {
                    tempType = type.getType();
                    typeSet = true;
                }
                else
                {
                    if(!(type.getType() == tempType))
                    {
                        ForeachListElementsNotSameType exception = new ForeachListElementsNotSameType(foreachStmt.getLine());
                        foreachStmt.addError(exception);

                    }
                }
            }

            //error number 21
            //TODO: its probably super buggy, heavy check it
            try
            {
                Type baseType =  ((ListType) returnedType).getElementsTypes().get(0).getType();
                if (!(((LocalVariableSymbolTableItem)
                        (SymbolTable.root.getItem(foreachStmt.getVariable().toString(), false))).getType() == baseType))
                {
                    ForeachVarNotMatchList exception = new ForeachVarNotMatchList(foreachStmt);
                    foreachStmt.addError(exception);
                }
            }
            catch (ItemNotFoundException ex)
            { //TODO: do something
            }
        }


        foreachStmt.getBody().accept(this);
        loopDepthCount -= 1;
        return null;
    }


    @Override
    public Void visit(ForStmt forStmt) {
        loopDepthCount +=1;
        forStmt.getInitialize().accept(this);
        Type conditionType = forStmt.getCondition().accept(this.expressionTypeChecker);
        if (!(conditionType instanceof BoolType)) { //Error 5
            ConditionNotBool exception = new ConditionNotBool(forStmt.getLine());
            forStmt.addError(exception);
        }
        forStmt.getUpdate().accept(this);

        forStmt.getBody().accept(this);
        loopDepthCount -=1;
        return null;
    }

}
