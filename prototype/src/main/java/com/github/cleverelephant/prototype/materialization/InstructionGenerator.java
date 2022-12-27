package com.github.cleverelephant.prototype.materialization;

import org.objectweb.asm.MethodVisitor;

import static org.objectweb.asm.Opcodes.*;

public class InstructionGenerator
{
    private final MethodVisitor visitor;

    private final String internalClassName;
    private final Class<?> returnType;
    private final Class<?>[] paramTypes;

    public InstructionGenerator(
            MethodVisitor visitor, String internalClassName, Class<?> returnType, Class<?>[] paramTypes
    )
    {
        this.visitor = visitor;
        this.internalClassName = internalClassName;
        this.returnType = returnType;
        this.paramTypes = paramTypes;
    }

    public void invokeOwnMethod(String methodName, Class<?> returnType)
    {
        loadSelf();
        visitor.visitMethodInsn(INVOKEVIRTUAL, internalClassName, methodName, Util.methodDescriptor(returnType), false);
    }

    public void setFieldToConst(String fieldName, Class<?> fieldType, Object value)
    {
        loadSelf();
        visitor.visitLdcInsn(value);
        visitor.visitFieldInsn(PUTFIELD, internalClassName, fieldName, fieldType.descriptorString());
    }

    public void returnType(Class<?> type)
    {
        visitor.visitInsn(Util.returnOpcode(type.descriptorString()));
    }

    public void returnVoid()
    {
        visitor.visitInsn(RETURN);
    }

    public void loadSelf()
    {
        visitor.visitVarInsn(ALOAD, 0);
    }

    public void loadParam(int index)
    {
        loadParam(index, index);
    }

    public void loadParam(int index, int localVariableIndex)
    {
        String typeDescriptor = paramTypes[index].descriptorString();
        visitor.visitVarInsn(Util.loadOpcode(typeDescriptor), localVariableIndex + 1);
    }

    public void loadField(String fieldName, Class<?> fieldType)
    {
        loadSelf();
        visitor.visitFieldInsn(GETFIELD, internalClassName, fieldName, fieldType.descriptorString());
    }

    public void paramToField(String fieldName, int paramIndex, int localVariableIndex)
    {
        String typeDescriptor = paramTypes[paramIndex].descriptorString();

        loadSelf();
        loadParam(paramIndex, localVariableIndex);
        visitor.visitFieldInsn(PUTFIELD, internalClassName, fieldName, typeDescriptor);
    }

    public void paramToField(String fieldName, int paramIndex)
    {
        paramToField(fieldName, paramIndex, paramIndex);
    }

    public MethodVisitor visitor()
    {
        return visitor;
    }

}
