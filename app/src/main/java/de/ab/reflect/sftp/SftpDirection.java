package de.ab.reflect.sftp;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(value = {ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD, ElementType.LOCAL_VARIABLE})
public @interface SftpDirection {
}