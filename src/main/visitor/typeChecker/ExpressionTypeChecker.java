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
import main.compileErrorException.typeErrors.*;
import main.symbolTable.SymbolTable;
import main.symbolTable.exceptions.ItemNotFoundException;
import main.symbolTable.items.ClassSymbolTableItem;
import main.symbolTable.items.FieldSymbolTableItem;
import main.symbolTable.items.LocalVariableSymbolTableItem;
import main.symbolTable.items.MethodSymbolTableItem;
import main.symbolTable.utils.graph.Graph;
import main.visitor.Visitor;


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
        Type instanceType = listAccessByIndex.getInstance().accept(this);
        Type indexType = listAccessByIndex.getIndex().accept(this);
        if ((instanceType instanceof NoType) || (indexType instanceof NoType)) {
            return new NoType();
        }
        boolean foundIndexTypeError = false;
        if (!(indexType instanceof IntType)) { // error number 23
            ListIndexNotInt exception = new ListIndexNotInt(listAccessByIndex.getLine());
            listAccessByIndex.addError(exception);
            foundIndexTypeError = true;
        }
        if (!(instanceType instanceof ListType)) { // error number 22
            ListAccessByIndexOnNoneList exception = new ListAccessByIndexOnNoneList(listAccessByIndex.getLine());
            listAccessByIndex.addError(exception);
            return new NoType();
        }
        ListType instanceListType = (ListType) instanceType;

        boolean areAllElementsSameType = true;
        for (ListNameType elementType : instanceListType.getElementsTypes()) {
            if (!elementType.getType().equals(instanceListType.getElementsTypes().get(0).getType())) {
                areAllElementsSameType = false;
                break;
            }
        }
        if (!(listAccessByIndex.getIndex() instanceof IntValue) && !areAllElementsSameType) { // error number 12
            CantUseExprAsIndexOfMultiTypeList exception = new CantUseExprAsIndexOfMultiTypeList(listAccessByIndex.getLine());
            listAccessByIndex.addError(exception);
            return new NoType();
        }
        else {
            if (foundIndexTypeError) {
                return new NoType();
            }
        }
        if ((listAccessByIndex.getIndex() instanceof IntValue) && areAllElementsSameType) {
            IntValue indexIntValue = (IntValue) listAccessByIndex.getIndex();
            if (indexIntValue.getConstant() < instanceListType.getElementsTypes().size()) {
                return instanceListType.getElementsTypes().get(indexIntValue.getConstant()).getType();
            }
            else {
                return instanceListType.getElementsTypes().get(0).getType();
            }
        }
        else {
            return instanceListType.getElementsTypes().get(0).getType();
        }
    }

    @Override
    public Type visit(MethodCall methodCall) {
        Type instanceType = methodCall.getInstance().accept(this);
        if (instanceType instanceof NoType) {
            return new NoType();
        }
        if (!(instanceType instanceof FptrType)) { // error number 8
            CallOnNoneFptrType exception = new CallOnNoneFptrType(methodCall.getLine());
            methodCall.addError(exception);
            return new NoType();
        }
        FptrType instanceFptrType = (FptrType) instanceType;
        // error number 15
        if (methodCall.getArgs().size() != instanceFptrType.getArgumentsTypes().size()) {
            MethodCallNotMatchDefinition exception = new MethodCallNotMatchDefinition(methodCall.getLine());
            methodCall.addError(exception);
            return new NoType();
        }
        else {
            for (int i = 0; i < methodCall.getArgs().size(); i++) {
                Type callingArgType = methodCall.getArgs().get(i).accept(this);
                if (!this.isFirstTypeSubtypeOf(callingArgType, instanceFptrType.getArgumentsTypes().get(i))) { //?
                    MethodCallNotMatchDefinition exception = new MethodCallNotMatchDefinition(methodCall.getLine());
                    methodCall.addError(exception);
                    return new NoType();
                }
            }
        }
        return instanceFptrType.getReturnType();
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
            return new NoType();
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

    public boolean isFirstTypeSubtypeOf(Type first, Type second) {
        if (first instanceof NoType) {
            return true;
        }
        else if (((first instanceof BoolType) && (second instanceof BoolType))
                || ((first instanceof IntType) && (second instanceof IntType))
                || ((first instanceof StringType) && (second instanceof StringType))) {
            return true;
        }
        else if ((first instanceof FptrType) && (second instanceof FptrType)) {
            FptrType firstFptr = (FptrType) first;
            FptrType secondFptr = (FptrType) second;
            if (this.isFirstTypeSubtypeOf(firstFptr.getReturnType(), secondFptr.getReturnType())) {
                if (firstFptr.getArgumentsTypes().size() != secondFptr.getArgumentsTypes().size()) {
                    return false;
                }
                for (int i = 0; i < firstFptr.getArgumentsTypes().size(); i++) {
                    if (!this.isFirstTypeSubtypeOf(secondFptr.getArgumentsTypes().get(i),
                            firstFptr.getArgumentsTypes().get(i))) {
                        return false;
                    }
                }
                return true;
            }
        }
        else if ((first instanceof ListType) && (second instanceof ListType)) {
            ListType firstList = (ListType) first;
            ListType secondList = (ListType) second;
            if (firstList.getElementsTypes().size() != secondList.getElementsTypes().size()) {
                return false;
            }
            for (int i = 0; i < firstList.getElementsTypes().size(); i++) {
                if (!this.isFirstTypeSubtypeOf(firstList.getElementsTypes().get(i).getType(),
                        secondList.getElementsTypes().get(i).getType())) {
                    return false;
                }
            }
            return true;
        }
        else if ((first instanceof ClassType) && (second instanceof ClassType)) {
            ClassType firstClass = (ClassType) first;
            ClassType secondClass = (ClassType) second;
            if (classHierarchy.isSecondNodeAncestorOf(
                    firstClass.getClassName().getName(),
                    secondClass.getClassName().getName())) {
                return true;
            }
            if (firstClass.getClassName().getName().equals(secondClass.getClassName().getName())) {
                return true;
            }
        }
        else if ((first instanceof NullType) && ((second instanceof ClassType) || (second instanceof FptrType))) {
            return true;
        }
        return false;
    }
}
