package org.echoiot.server.common.data.util;


import java.lang.annotation.Annotation;

@SuppressWarnings("unchecked")
public class ReflectionUtils {

    private ReflectionUtils() {}

    public static <T> T getAnnotationProperty(String targetType, String annotationType, String property) throws Exception {
        Class<Annotation> annotationClass = (Class<Annotation>) Class.forName(annotationType);
        Annotation annotation = Class.forName(targetType).getAnnotation(annotationClass);
        return (T) annotationClass.getDeclaredMethod(property).invoke(annotation);
    }

}
