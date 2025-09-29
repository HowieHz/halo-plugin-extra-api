package top.howiehz.gradle;

import java.util.List;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.tree.AnnotationNode;
import top.howiehz.gradle.utils.AsmConst;

/**
 * ASM 类访问器，用于识别 Spring 组件和 Halo Finder 注解的类
 */
public class FilterComponentClassVisitor extends ClassVisitor {

    private static final String COMPONENT_ANNOTATION_DESC = "Lorg/springframework/stereotype/Component;";
    private static final String SERVICE_ANNOTATION_DESC = "Lorg/springframework/stereotype/Service;";
    private static final String REPOSITORY_ANNOTATION_DESC = "Lorg/springframework/stereotype/Repository;";
    private static final String FINDER_ANNOTATION_DESC = "Lrun/halo/app/theme/finder/Finder;";

    private boolean isComponentClass = false;
    private String className;

    public FilterComponentClassVisitor(int api) {
        super(api);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName,
        String[] interfaces) {
        this.className = name;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        if (COMPONENT_ANNOTATION_DESC.equals(descriptor)
            || SERVICE_ANNOTATION_DESC.equals(descriptor)
            || REPOSITORY_ANNOTATION_DESC.equals(descriptor)
            || FINDER_ANNOTATION_DESC.equals(descriptor)) {
            isComponentClass = true;
        }
        return super.visitAnnotation(descriptor, visible);
    }

    public boolean isComponentClass() {
        return isComponentClass;
    }

    public String getName() {
        return className;
    }
}
