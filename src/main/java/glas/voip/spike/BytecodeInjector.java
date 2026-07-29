package glas.voip.spike;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Inserts a call to a static "patch" method right before a specific, precisely-located pair of
 * adjacent field reads (GETSTATIC field1 immediately followed by GETSTATIC field2, with no
 * instruction between them) inside a specific method.
 * <p>
 * Matching on an adjacent pair rather than a single field name matters: the real target
 * ({@code VoiceManager.UpdateVMClient()}) reads {@code maxDistance} twice -- once as part of
 * the exact {@code maxDistance}-then-{@code minDistance} pair that feeds the volume
 * computation this project needs to intercept, and once completely separately a few lines
 * later as a standalone distance check. Matching on the single field name alone would have
 * injected at both sites; matching on the adjacent pair matches only the first (confirmed
 * against the real game's disassembled bytecode during this project's research).
 * <p>
 * Uses ASM's tree API (`ClassNode`/`MethodNode`/`InsnList`) rather than the streaming
 * `ClassVisitor`/`MethodVisitor` API, since detecting "this instruction is followed by that
 * specific other instruction" needs one-instruction lookahead, which the tree API's full
 * in-memory instruction list gives directly -- the streaming API would need fragile manual
 * buffering across every possible instruction-visit method to achieve the same thing.
 * <p>
 * Only the field/method names are parameterized (letting this be tested against a small
 * synthetic fixture class instead of needing real, non-redistributable game bytecode); the
 * injected instruction sequence itself (load local variable slot 8, call
 * {@code IsoPlayer.getOnlineID()}, pass the result to the configured patch method) is fixed,
 * matching the one real call site this project targets -- this class is not a general-purpose
 * bytecode-injection framework.
 */
public class BytecodeInjector {

    private final String targetMethodName;
    private final String firstTriggerFieldOwner;
    private final String firstTriggerFieldName;
    private final String secondTriggerFieldOwner;
    private final String secondTriggerFieldName;
    private final String patchMethodOwner;
    private final String patchMethodName;
    private final String patchMethodDescriptor;

    public BytecodeInjector(String targetMethodName,
                             String firstTriggerFieldOwner, String firstTriggerFieldName,
                             String secondTriggerFieldOwner, String secondTriggerFieldName,
                             String patchMethodOwner, String patchMethodName, String patchMethodDescriptor) {
        this.targetMethodName = targetMethodName;
        this.firstTriggerFieldOwner = firstTriggerFieldOwner;
        this.firstTriggerFieldName = firstTriggerFieldName;
        this.secondTriggerFieldOwner = secondTriggerFieldOwner;
        this.secondTriggerFieldName = secondTriggerFieldName;
        this.patchMethodOwner = patchMethodOwner;
        this.patchMethodName = patchMethodName;
        this.patchMethodDescriptor = patchMethodDescriptor;
    }

    public byte[] inject(byte[] classBytes) {
        ClassNode classNode = new ClassNode();
        new ClassReader(classBytes).accept(classNode, 0);

        int injectionCount = 0;
        for (MethodNode methodNode : classNode.methods) {
            if (targetMethodName.equals(methodNode.name)) {
                injectionCount += injectIntoMethod(methodNode);
            }
        }

        if (injectionCount == 0) {
            // Fail loudly rather than silently returning an unmodified class: a future game
            // update could shift line numbers, rename fields, or otherwise break the match,
            // and a silent no-op here would be indistinguishable from "everything is fine" --
            // exactly the failure mode this class exists to avoid.
            throw new IllegalStateException("No matching " + firstTriggerFieldName + "/"
                    + secondTriggerFieldName + " field pair found in " + targetMethodName
                    + "() -- injection target may have changed");
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        classNode.accept(writer);
        return writer.toByteArray();
    }

    private int injectIntoMethod(MethodNode methodNode) {
        InsnList instructions = methodNode.instructions;
        int injectionCount = 0;

        for (AbstractInsnNode insn : instructions.toArray()) {
            if (!isMatchingGetStatic(insn, firstTriggerFieldOwner, firstTriggerFieldName)) {
                continue;
            }

            AbstractInsnNode next = nextRealInstruction(insn);
            if (!isMatchingGetStatic(next, secondTriggerFieldOwner, secondTriggerFieldName)) {
                continue;
            }

            InsnList injected = new InsnList();
            injected.add(new VarInsnNode(Opcodes.ALOAD, 8));
            injected.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "zombie/characters/IsoPlayer", "getOnlineID", "()S", false));
            injected.add(new MethodInsnNode(Opcodes.INVOKESTATIC, patchMethodOwner, patchMethodName, patchMethodDescriptor, false));
            instructions.insertBefore(insn, injected);
            injectionCount++;
        }

        return injectionCount;
    }

    /**
     * Skips pseudo-instructions (labels, line numbers, frames -- anything with opcode -1) that
     * ASM's tree API may place between two real instructions, so "immediately followed by"
     * matches on actual bytecode adjacency rather than on incidental debug-info placement that
     * could shift with a future recompile.
     */
    private static AbstractInsnNode nextRealInstruction(AbstractInsnNode insn) {
        AbstractInsnNode next = insn.getNext();
        while (next != null && next.getOpcode() < 0) {
            next = next.getNext();
        }
        return next;
    }

    private static boolean isMatchingGetStatic(AbstractInsnNode insn, String owner, String fieldName) {
        return insn instanceof FieldInsnNode fieldInsn
                && fieldInsn.getOpcode() == Opcodes.GETSTATIC
                && owner.equals(fieldInsn.owner)
                && fieldName.equals(fieldInsn.name);
    }
}
