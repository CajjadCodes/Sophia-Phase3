package main.visitor.typeChecker;

import main.ast.nodes.declaration.variableDec.VarDeclaration;
import main.ast.nodes.expression.*;
import main.ast.nodes.expression.operators.BinaryOperator;
import main.ast.nodes.expression.operators.UnaryOperator;
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
import main.symbolTable.utils.graph.exceptions.GraphDoesNotContainNodeException;
import main.visitor.Visitor;

import java.util.ArrayList;
import java.util.Set;


public class ExpressionTypeChecker extends Visitor<Type> {
    public static int indexingDepthForLvalueViolation = 0;
    private final Graph<String> classHierarchy;


    public ExpressionTypeChecker(Graph<String> classHierarchy) {
        this.classHierarchy = classHierarchy;
    }

    @Override
    public Type visit(BinaryExpression binaryExpression) {
        if (indexingDepthForLvalueViolation <= 0) {
            TypeChecker.isViolatingLvalue = true;
        }
        boolean foundOperandTypeError = false;
        Type firstOperandType = binaryExpression.getFirstOperand().accept(this);
        Type secondOperandType = binaryExpression.getSecondOperand().accept(this);
        if ((firstOperandType instanceof NoType) && (secondOperandType instanceof NoType)) {
            return new NoType();
        }
        if ((binaryExpression.getBinaryOperator() == BinaryOperator.eq)
            || (binaryExpression.getBinaryOperator() == BinaryOperator.neq)) {
            if (firstOperandType instanceof NoType) {
                if (!(secondOperandType instanceof ListType)) {
                    return new BoolType();
                }
                foundOperandTypeError = true;
            }
            else if (secondOperandType instanceof NoType) {
                if (!(firstOperandType instanceof ListType)) {
                    return firstOperandType;
                }
                foundOperandTypeError = true;
            }
            else {
                if ((firstOperandType instanceof NullType) || (secondOperandType instanceof NullType)) {
                    if ((firstOperandType instanceof NullType) && (secondOperandType instanceof NullType)) {
                        return new BoolType();
                    }
                    else if (!(firstOperandType instanceof FptrType) && !(secondOperandType instanceof FptrType)
                        && !(firstOperandType instanceof ClassType) && !(secondOperandType instanceof ClassType)) {
                        foundOperandTypeError = true;
                    }
                    else {
                        return new BoolType();
                    }
                }
                else {
                    if (((firstOperandType instanceof FptrType) && (secondOperandType instanceof FptrType))
                        || ((firstOperandType instanceof BoolType) && (secondOperandType instanceof BoolType))
                        || ((firstOperandType instanceof IntType) && (secondOperandType instanceof IntType))
                        || ((firstOperandType instanceof StringType) && (secondOperandType instanceof StringType))
                        || ((firstOperandType instanceof ClassType) && (secondOperandType instanceof ClassType))) {
                        return new BoolType();
                    }
                    else {
                        foundOperandTypeError = true;
                    }
                }
            }
        }
        else if ((binaryExpression.getBinaryOperator() == BinaryOperator.mult)
            || (binaryExpression.getBinaryOperator() == BinaryOperator.div)
            || (binaryExpression.getBinaryOperator() == BinaryOperator.mod)
            || (binaryExpression.getBinaryOperator() == BinaryOperator.add)
            || (binaryExpression.getBinaryOperator() == BinaryOperator.sub)
            || (binaryExpression.getBinaryOperator() == BinaryOperator.gt)
            || (binaryExpression.getBinaryOperator() == BinaryOperator.lt)) {
            if ((firstOperandType instanceof IntType) && (secondOperandType instanceof IntType)) {
                return new IntType();
            }
            else {
                foundOperandTypeError = true;
            }
        }
        else if ((binaryExpression.getBinaryOperator() == BinaryOperator.and)
            || (binaryExpression.getBinaryOperator() == BinaryOperator.or)) {
            if ((firstOperandType instanceof BoolType) && (secondOperandType instanceof BoolType)) {
                return new BoolType();
            }
            else {
                foundOperandTypeError = true;
            }
        }
        else {
            foundOperandTypeError = true;
        }
        // error number 4
        if (foundOperandTypeError) {
            UnsupportedOperandType exception = new UnsupportedOperandType(binaryExpression.getLine(),
                    binaryExpression.getBinaryOperator().name());
            binaryExpression.addError(exception);
            return new NoType();
        }
        return new NoType();
    }

    @Override
    public Type visit(UnaryExpression unaryExpression) {
        if (indexingDepthForLvalueViolation <= 0) {
            TypeChecker.isViolatingLvalue = true;
        }
        Type operandType = unaryExpression.getOperand().accept(this);
        if (operandType instanceof NoType) {
            return new NoType();
        }
        if (unaryExpression.getOperand() instanceof IntValue) {
            TypeChecker.isViolatingLvalue = true;
            // maybe even return NoType for 3++
        }
        if ((unaryExpression.getOperator() == UnaryOperator.minus)
            || (unaryExpression.getOperator() == UnaryOperator.preinc)
            || (unaryExpression.getOperator() == UnaryOperator.predec)
            || (unaryExpression.getOperator() == UnaryOperator.postinc)
            || (unaryExpression.getOperator() == UnaryOperator.postdec)) {
            if (operandType instanceof IntType) {
                return new IntType();
            }
            else {
                //error number 4
                UnsupportedOperandType exception = new UnsupportedOperandType(unaryExpression.getLine(),
                        unaryExpression.getOperator().name());
                unaryExpression.addError(exception);
            }
        }
        else if (unaryExpression.getOperator() == UnaryOperator.not) {
            if (operandType instanceof BoolType) {
                return new BoolType();
            }
            else {
                //error number 4
                UnsupportedOperandType exception = new UnsupportedOperandType(unaryExpression.getLine(),
                        unaryExpression.getOperator().name());
                unaryExpression.addError(exception);
            }
        }
        return new NoType();
    }

    @Override
    public Type visit(ObjectOrListMemberAccess objectOrListMemberAccess) {
        Type instanceType = objectOrListMemberAccess.getInstance().accept(this);
        if (instanceType instanceof NoType) {
            return new NoType();
        }
        else if (instanceType instanceof ClassType) {
            ClassType instanceClassType = (ClassType) instanceType;
            ClassSymbolTableItem classItem;
            Set<String> classFathers;
            //find class
            try {
                classItem = (ClassSymbolTableItem) SymbolTable.root
                        .getItem(ClassSymbolTableItem.START_KEY + instanceClassType.getClassName().getName(), true);
                classFathers = (Set<String>) classHierarchy.getParentsOfNode(instanceClassType.getClassName().getName());
            } catch (ItemNotFoundException | GraphDoesNotContainNodeException err) {return new NoType();}

            //search in class
            try {
                MethodSymbolTableItem methodItem = (MethodSymbolTableItem) classItem.getClassSymbolTable()
                        .getItem(MethodSymbolTableItem.START_KEY + objectOrListMemberAccess.getMemberName().getName(), true);
                return new FptrType(methodItem.getArgTypes(), methodItem.getReturnType());
            } catch (ItemNotFoundException err) {
                try {
                    FieldSymbolTableItem fieldItem = (FieldSymbolTableItem) classItem.getClassSymbolTable()
                            .getItem(FieldSymbolTableItem.START_KEY + objectOrListMemberAccess.getMemberName().getName(), true);
                    return fieldItem.getType();
                } catch (ItemNotFoundException ignored) {}
            }

            //search in class' ancestors
            for (String fatherName : classFathers) {
                ClassSymbolTableItem fatherClass;
                try {
                    fatherClass = (ClassSymbolTableItem) SymbolTable.root
                            .getItem(ClassSymbolTableItem.START_KEY + fatherName, true);
                } catch (ItemNotFoundException ignored) {break;}
                try {
                    MethodSymbolTableItem methodItem = (MethodSymbolTableItem) fatherClass.getClassSymbolTable()
                            .getItem(MethodSymbolTableItem.START_KEY + objectOrListMemberAccess.getMemberName().getName(), true);
                    return new FptrType(methodItem.getArgTypes(), methodItem.getReturnType());
                } catch (ItemNotFoundException err1) {
                    try {
                        FieldSymbolTableItem fieldItem = (FieldSymbolTableItem)  fatherClass.getClassSymbolTable()
                                .getItem(FieldSymbolTableItem.START_KEY + objectOrListMemberAccess.getMemberName().getName(), true);
                        return fieldItem.getType();
                    } catch (ItemNotFoundException ignored) {}
                }
            }
            // error number 3
            MemberNotAvailableInClass exception = new MemberNotAvailableInClass(objectOrListMemberAccess.getLine(),
                    objectOrListMemberAccess.getMemberName().getName(), instanceClassType.getClassName().getName());
            objectOrListMemberAccess.addError(exception);
            return new NoType();
        }
        else if(instanceType instanceof ListType) {
            ListType instanceListType = (ListType) instanceType;
            for (ListNameType elementType : instanceListType.getElementsTypes()) {
                if (elementType.getName().getName().equals(objectOrListMemberAccess.getMemberName().getName())) {
                    return elementType.getType();
                }
            }
            // error number 24
            ListMemberNotFound exception = new ListMemberNotFound(objectOrListMemberAccess.getLine(),
                    objectOrListMemberAccess.getMemberName().getName());
            objectOrListMemberAccess.addError(exception);
            return new NoType();
        }
        else { // error number 30
            MemberAccessOnNoneObjOrListType exception = new MemberAccessOnNoneObjOrListType(objectOrListMemberAccess.getLine());
            objectOrListMemberAccess.addError(exception);
            return new NoType();
        }
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
            ClassSymbolTableItem classItem = (ClassSymbolTableItem) SymbolTable.root
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
        indexingDepthForLvalueViolation++;
        Type instanceType = listAccessByIndex.getInstance().accept(this);
        Type indexType = listAccessByIndex.getIndex().accept(this);
        indexingDepthForLvalueViolation--;
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
        TypeChecker.isViolatingLvalue = true;
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
        TypeChecker.isViolatingLvalue = true;
        try {
            ClassSymbolTableItem classItem = (ClassSymbolTableItem) SymbolTable.top
                    .getItem(ClassSymbolTableItem.START_KEY + newClassInstance.getClassType().getClassName().getName(), true);
            ArrayList<VarDeclaration> constructorArgs = classItem.getClassDeclaration().getConstructor().getArgs();
            ArrayList<Type> calledArgsTypes = new ArrayList<>();
            for (int i = 0; i < newClassInstance.getArgs().size(); i++) {
                calledArgsTypes.add(newClassInstance.getArgs().get(i).accept(this));
            }
            ArrayList<Type> constructorArgType = new ArrayList<>();
            for (VarDeclaration arg : constructorArgs) {
                constructorArgType.add(arg.getType());
            }
            // error number 16
            if (calledArgsTypes.size() != constructorArgType.size()) {
                ConstructorArgsNotMatchDefinition exception = new ConstructorArgsNotMatchDefinition(newClassInstance);
                newClassInstance.addError(exception);
                return new NoType();
            }
            else {
                for (int i = 0; i < calledArgsTypes.size(); i++) {
                    if (!this.isFirstTypeSubtypeOf(calledArgsTypes.get(i), constructorArgType.get(i))) {
                        ConstructorArgsNotMatchDefinition exception = new ConstructorArgsNotMatchDefinition(newClassInstance);
                        newClassInstance.addError(exception);
                        return new NoType();
                    }
                }
            }
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
        TypeChecker.isViolatingLvalue = true;
        ListType listType = new ListType();
        for (Expression expression : listValue.getElements()) {
            Type elementType = expression.accept(this);
            listType.addElementType(new ListNameType(elementType));
        }
        return listType;
    }

    @Override
    public Type visit(NullValue nullValue) {
        TypeChecker.isViolatingLvalue = true;
        return new NullType();
    }

    @Override
    public Type visit(IntValue intValue) {
        if (indexingDepthForLvalueViolation <= 0) {
            TypeChecker.isViolatingLvalue = true;
        }
        return new IntType();
    }

    @Override
    public Type visit(BoolValue boolValue) {
        TypeChecker.isViolatingLvalue = true;
        return new BoolType();
    }

    @Override
    public Type visit(StringValue stringValue) {
        TypeChecker.isViolatingLvalue = true;
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
