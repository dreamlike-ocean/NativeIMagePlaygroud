package io.github.dreamlike;

import io.netty.channel.socket.nio.NioDatagramChannel;
import kotlin.Pair;
import kotlin.reflect.jvm.internal.ReflectionFactoryImpl;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeClassInitialization;
import org.graalvm.nativeimage.hosted.RuntimeForeignAccess;
import org.graalvm.nativeimage.hosted.RuntimeReflection;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.ValueLayout;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class NativeImageFeature implements Feature {
    public void duringSetup(Feature.DuringSetupAccess access) {
        RuntimeForeignAccess
                .registerForDowncall(
                        FunctionDescriptor.of(ValueLayout.JAVA_INT),
                        Linker.Option.critical(false)
                );
    }

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        Class<Person> personClass = Person.class;
        registerSingleClass(personClass);
        registerSingleClass(ReflectionFactoryImpl.class);
        registerSingleClass(NioDatagramChannel.class);
        registerSingleClass(Pair.class);
    }

    private void registerSingleClass(Class c) {
        RuntimeReflection.register(c);
        RuntimeReflection.registerAllDeclaredConstructors(c);
        for (Constructor constructor : c.getDeclaredConstructors()) {
            RuntimeReflection.register(constructor);

        }
        for (Field field : c.getDeclaredFields()) {
            RuntimeReflection.register(field);
            RuntimeReflection.registerFieldLookup(c, field.getName());
        }
        for (Method method : c.getMethods()) {
            RuntimeReflection.registerMethodLookup(c, method.getName(), method.getParameterTypes());
            RuntimeReflection.register(method);
        }
    }
}

