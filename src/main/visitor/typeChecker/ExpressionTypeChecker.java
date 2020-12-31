package main.visitor.typeChecker;

import main.ast.nodes.expression.*;
import main.ast.nodes.expression.values.ListValue;
import main.ast.nodes.expression.values.NullValue;
import main.ast.nodes.expression.values.primitive.BoolValue;
import main.ast.nodes.expression.values.primitive.IntValue;
import main.ast.nodes.expression.values.primitive.StringValue;
import main.ast.types.NullType;
import main.ast.types.Type;
import main.ast.types.list.ListNameType;
import main.ast.types.list.ListType;
import main.ast.types.single.BoolType;
import main.ast.types.single.ClassType;
import main.ast.types.single.IntType;
import main.ast.types.single.StringType;
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
        //TODO
        try {
            MethodSymbolTableItem methodSTI = (MethodSymbolTableItem) SymbolTable.root
                    .getItem(MethodSymbolTableItem.START_KEY + methodCall.getInstance()., true);
        } catch (ItemNotFoundException ignored) {/*todo*/ }
        return null;
    }

    @Override
    public Type visit(ListAccessByIndex listAccessByIndex) {
        //TODO

        return null;
    }

    @Override
    public Type visit(MethodCall methodCall) {
        //TODO
        try {
            MethodSymbolTableItem methodSTI = (MethodSymbolTableItem) SymbolTable.root
                    .getItem(MethodSymbolTableItem.START_KEY + methodCall.getInstance()., true);
        } catch (ItemNotFoundException ignored) {/*todo*/ }
        return null;
    }

    @Override
    public Type visit(NewClassInstance newClassInstance) {
        //TODO
        return newClassInstance.getClassType();
    }

    @Override
    public Type visit(ThisClass thisClass) {
        //TODO this.
        return ;
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
