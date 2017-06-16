package io.lnk.api.exception.transport;

import java.io.Serializable;
import java.lang.reflect.Field;

import org.apache.commons.lang3.ArrayUtils;

import io.lnk.api.utils.ReflectionUtils;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月31日 上午11:43:58
 */
public class CommandTransportException implements Serializable {
    private static final long serialVersionUID = 4247532888629155074L;
    private static final Field STACKTRACE_ELEMENT_DECLARINGCLASS = ReflectionUtils.findField(StackTraceElement.class, "declaringClass", String.class);
    private String className;
    private String message;
    private StackTrace[] stackTraces;

    static {
        STACKTRACE_ELEMENT_DECLARINGCLASS.setAccessible(true);
    }

    public CommandTransportException() {
        super();
    }

    public CommandTransportException(Throwable e) {
        super();
        this.className = e.getClass().getName();
        this.message = e.getLocalizedMessage();
        StackTraceElement[] stackTraceList = e.getStackTrace();
        if (ArrayUtils.isNotEmpty(stackTraceList)) {
            final int length = stackTraceList.length;
            this.stackTraces = new StackTrace[length];
            for (int i = 0; i < length; i++) {
                StackTraceElement stackTraceElement = stackTraceList[i];
                StackTrace stackTrace = new StackTrace();
                String declaringClass = (String) ReflectionUtils.getField(STACKTRACE_ELEMENT_DECLARINGCLASS, stackTraceElement);
                stackTrace.setDeclaringClass(declaringClass);
                stackTrace.setFileName(stackTraceElement.getFileName());
                stackTrace.setLineNumber(stackTraceElement.getLineNumber());
                stackTrace.setMethodName(stackTraceElement.getMethodName());
                this.stackTraces[i] = stackTrace;
            }
        }
    }

    public StackTraceElement[] buildStackTraceElement() {
        final int length = this.stackTraces.length;
        StackTraceElement[] stackTraceList = new StackTraceElement[length];
        for (int i = 0; i < length; i++) {
            StackTrace stackTrace = this.stackTraces[i];
            stackTraceList[i] = new StackTraceElement(stackTrace.getDeclaringClass(), stackTrace.getMethodName(), stackTrace.getFileName(), stackTrace.getLineNumber());
        }
        return stackTraceList;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public StackTrace[] getStackTraces() {
        return stackTraces;
    }

    public void setStackTraces(StackTrace[] stackTraces) {
        this.stackTraces = stackTraces;
    }
}
