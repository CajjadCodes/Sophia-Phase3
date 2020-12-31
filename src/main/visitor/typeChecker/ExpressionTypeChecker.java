package main.visitor.typeChecker;

import main.ast.nodes.expression.*;
import main.ast.nodes.expression.values.ListValue;
import main.ast.nodes.expression.values.NullValue;
import main.ast.nodes.expression.values.primitive.BoolValue;
import main.ast.nodes.expression.values.primitive.IntValue;
import main.ast.nodes.expression.values.primitive.StringValue;
import main.ast.types.NoType;
import main.ast.types.NullType;
import main.ast.types.Type;
import main.ast.types.functionPointer.FptrType;
import main.ast.types.list.ListNameType;
import main.ast.types.list.ListType;
import main.ast.types.single.BoolType;
import main.ast.types.single.ClassType;
import main.ast.types.single.IntType;
import main.ast.types.single.StringType;
import main.compileErrorException.typeErrors.CallOnNoneFptrType;
import main.compileErrorException.typeErrors.ClassNotDeclared;
import main.compileErrorException.typeErrors.MethodCallNotMatchDefinition;
import main.compileErrorException.typeErrors.VarNotDeclared;
import main.symbolTable.SymbolTable;
import main.symbolTable.exceptions.ItemNotFoundException;
import main.symbolTable.items.ClassSymbolTableItem;
import main.symbolTable.items.FieldSymbolTableItem;
import main.symbolTable.items.LocalVariableSymbolTableItem;
import main.symbolTable.items.MethodSymbolTableItem;
import main.symbolTable.utils.graph.Graph;
import main.symbolTable.utils.graph.exceptions.GraphDoesNotContainNodeException;
import main.visitor.Visitor;
import main.visitor.nameAnalyzer.NameAnalyzer;
import main.visitor.nameAnalyzer.NameChecker;


public class ExpressionTypeChecker extends Visitor<Type> {
    private final Graph<String> classHierarchy;

    public ExpressionTypeChecker(Graph<String> classHierarchy) {
        this.classHierarchy = classHierarchy;
    }

    @Override
    public Type visit(BinaryExpression binaryExpression) {
        //TODO
        return null;
    }

    @Override
    public Type visit(UnaryExpression unaryExpression) {
        //TODO
        return null;
    }

    @Override
    public Type visit(ObjectOrListMemberAccess objectOrListMemberAccess) {
        //TODO
        return null;
    }

    @Override
    public Type visit(Identifier identifier) {
        //check for variable
        try {
            LocalVariableSymbolTableItem localItem = (LocalVariableSymbolTableItem) SymbolTable.top
                    .getItem(LocalVariableSymbolTableItem.START_KEY + identifier.getName(), true);
                    return localItem.getType();

        } catch (ItemNotFoundException ignored) {}

        //check for field
        try {
            FieldSymbolTableItem fieldItem = (FieldSymbolTableItem) SymbolTable.top
                    .getItem(FieldSymbolTableItem.START_KEY + identifier.getName(), true);
            return fieldItem.getType();

        } catch (ItemNotFoundException ignored) {}

        //check for class
        try {
            ClassSymbolTableItem classItem = (ClassSymbolTableItem) SymbolTable.top
                    .getItem(ClassSymbolTableItem.START_KEY + identifier.getName(), true);
            return new ClassType(classItem.getClassDeclaration().getClassName());

        } catch (ItemNotFoundException ignored) {}

        //check for funcPtr
        try {
            MethodSymbolTableItem methodItem = (MethodSymbolTableItem) SymbolTable.top
                    .getItem(MethodSymbolTableItem.START_KEY + identifier.getName(), true);
            return new FptrType(methodItem.getArgTypes(), methodItem.getReturnType());

        } catch (ItemNotFoundException err) { // error number 1
            VarNotDeclared exception = new VarNotDeclared(identifier.getLine(), identifier.getName());
            identifier.addError(exception);
            return new NoType();
        }
    }

    @Override
    public Type visit(ListAccessByIndex listAccessByIndex) {
        //TODO

        return null;
    }

    @Override
    public Type visit(MethodCall methodCall) {
        Type retType = methodCall.getInstance().accept(this);
        if (!(retType instanceof FptrType)) { // error number 8
            CallOnNoneFptrType exception = new CallOnNoneFptrType(methodCall.getLine());
            methodCall.addError(exception);
            return new NoType(); //?
        }
        FptrType fptrType = (FptrType) retType;
        if (methodCall.getArgs().size() != fptrType.getArgumentsTypes().size()) { // error number 15
            MethodCallNotMatchDefinition exception = new MethodCallNotMatchDefinition(methodCall.getLine());
            methodCall.addError(exception);
        }
        else {
            for (int i = 0; i < methodCall.getArgs().size(); i++) { // error number 15
                Type methodCallArgType = methodCall.getArgs().get(i).accept(this);
                if (!methodCallArgType.equals(fptrType.getArgumentsTypes().get(i))) {
                    MethodCallNotMatchDefinition exception = new MethodCallNotMatchDefinition(methodCall.getLine());
                    methodCall.addError(exception);
                    break;
                }
            }
        }
        return fptrType.getReturnType();
    }

    @Override
    public Type visit(NewClassInstance newClassInstance) {
        try {
            SymbolTable.top
                    .getItem(ClassSymbolTableItem.START_KEY
                            + newClassInstance.getClassType().getClassName().getName(), true);
        } catch (ItemNotFoundException exp) { // error number 2
            ClassNotDeclared exception = new ClassNotDeclared(newClassInstance.getLine(),
                    newClassInstance.getClassType().getClassName().getName());
            newClassInstance.addError(exception);
            return new NoType(); //?
        }
        return newClassInstance.getClassType();
    }

    @Override
    public Type visit(ThisClass thisClass) {
        return new ClassType(TypeChecker.currentClass.getClassName());
    }

    @Override
    public Type visit(ListValue listValue) {
        ListType listType = new ListType();
        for (Expression expression : listValue.getElements()) {
            Type elementType = expression.accept(this);
            listType.addElementType(new ListNameType(elementType));
        }
        return listType;
    }

    @Override
    public Type visit(NullValue nullValue) {
        return new NullType();
    }

    @Override
    public Type visit(IntValue intValue) {
        return new IntType();
    }

    @Override
    public Type visit(BoolValue boolValue) {
        return new BoolType();
    }

    @Override
    public Type visit(StringValue stringValue) {
        return new StringType();
    }
}
