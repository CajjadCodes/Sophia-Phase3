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
import main.compileErrorException.typeErrors.MethodCallNotMatchDefinition;
import main.symbolTable.SymbolTable;
import main.symbolTable.exceptions.ItemNotFoundException;
import main.symbolTable.items.ClassSymbolTableItem;
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
        //search for different type of identifiers, like class or func or var
        return new FptrType(/*args, retType*/); //must set the return type and arguments
    }

    @Override
    public Type visit(ListAccessByIndex listAccessByIndex) {
        //TODO

        return null;
    }

    @Override
    public Type visit(MethodCall methodCall) {
        Type retType = methodCall.getInstance().accept(this);
        if (!(retType instanceof FptrType)) { //Error 8
            CallOnNoneFptrType exception = new CallOnNoneFptrType(methodCall.getLine());
            methodCall.addError(exception);
            return new NoType(); //?
        }
        FptrType fptrType = (FptrType) retType;
        if (methodCall.getArgs().size() != fptrType.getArgumentsTypes().size()) { //Error 15
            MethodCallNotMatchDefinition exception = new MethodCallNotMatchDefinition(methodCall.getLine());
            methodCall.addError(exception);
        }
        for (int i = 0; i < methodCall.getArgs().size(); i++) { //Error 15
            Type methodCallArgType = methodCall.getArgs().get(i).accept(this);
            if (!methodCallArgType.equals(fptrType.getArgumentsTypes().get(i))) {
                MethodCallNotMatchDefinition exception = new MethodCallNotMatchDefinition(methodCall.getLine());
                methodCall.addError(exception);
                break;
            }
        }
        return fptrType.getReturnType();
    }

    @Override
    public Type visit(NewClassInstance newClassInstance) {
        return newClassInstance.getClassType();
    }

    @Override
    public Type visit(ThisClass thisClass) {
        return null;
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
