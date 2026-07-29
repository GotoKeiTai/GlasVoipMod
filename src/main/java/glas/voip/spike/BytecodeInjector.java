package glas.voip.spike;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Inserts a call to a static "patch" method right before a specific field read
 * (GETSTATIC) inside a specific method of a specific class. Generic and
 * parameterized on purpose: lets this be tested against a small synthetic
 * fixture class instead of needing real game bytecode, which is never
 * committed to this repo.
 */
public class BytecodeInjector {

    private final String targetClassInternalName;
    private final String targetMethodName;
    private final String triggerFieldOwner;
    private final String triggerFieldName;
    private final String patchMethodOwner;
    private final String patchMethodName;
    private final String patchMethodDescriptor;

    public BytecodeInjector(String targetClassInternalName, String targetMethodName,
                             String triggerFieldOwner, String triggerFieldName,
                             String patchMethodOwner, String patchMethodName, String patchMethodDescriptor) {
        this.targetClassInternalName = targetClassInternalName;
        this.targetMethodName = targetMethodName;
        this.triggerFieldOwner = triggerFieldOwner;
        this.triggerFieldName = triggerFieldName;
        this.patchMethodOwner = patchMethodOwner;
        this.patchMethodName = patchMethodName;
        this.patchMethodDescriptor = patchMethodDescriptor;
    }

    public byte[] inject(byte[] classBytes) {
        ClassReader reader = new ClassReader(classBytes);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

        ClassVisitor classVisitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (!targetMethodName.equals(name)) {
                    return mv;
                }
                return new MethodVisitor(Opcodes.ASM9, mv) {
                    @Override
                    public void visitFieldInsn(int opcode, String owner, String fieldName, String fieldDescriptor) {
                        if (opcode == Opcodes.GETSTATIC && triggerFieldOwner.equals(owner) && triggerFieldName.equals(fieldName)) {
                            super.visitVarInsn(Opcodes.ALOAD, 8);
                            super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "zombie/characters/IsoPlayer", "getOnlineID", "()S", false);
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, patchMethodOwner, patchMethodName, patchMethodDescriptor, false);
                        }
                        super.visitFieldInsn(opcode, owner, fieldName, fieldDescriptor);
                    }
                };
            }
        };

        reader.accept(classVisitor, 0);
        return writer.toByteArray();
    }
}
