package com.github.cleverelephant.prototype.materialization;

import java.lang.invoke.TypeDescriptor;

import static org.objectweb.asm.Opcodes.*;

public class Util
{
    private Util()
    {
        throw new UnsupportedOperationException();
    }

    public static int loadOpcode(String descriptor)
    {
        return switch (descriptor) {
            case "Z", "B", "C", "S", "I" -> ILOAD;
            case "F" -> FLOAD;
            case "D" -> DLOAD;
            case "J" -> LLOAD;
            default -> {
                if (!descriptor.startsWith("L") && !descriptor.startsWith("["))
                    throw new IllegalArgumentException("invalid descriptor " + descriptor);
                yield ALOAD;
            }
        };
    }

    public static int returnOpcode(String descriptor)
    {
        return switch (descriptor) {
            case "Z", "B", "C", "S", "I" -> IRETURN;
            case "F" -> FRETURN;
            case "D" -> DRETURN;
            case "J" -> LRETURN;
            default -> {
                if (!descriptor.startsWith("L") && !descriptor.startsWith("["))
                    throw new IllegalArgumentException("invalid descriptor " + descriptor);
                yield ARETURN;
            }
        };
    }

    public static String methodDescriptor(TypeDescriptor returnType, TypeDescriptor... paramTypes)
    {
        StringBuilder methodDescriptor = new StringBuilder("(");
        for (TypeDescriptor type : paramTypes)
            methodDescriptor.append(type.descriptorString());
        methodDescriptor.append(")").append(returnType.descriptorString());
        return methodDescriptor.toString();
    }

    public static String internalName(String name)
    {
        return name.replace('.', '/');
    }
}
