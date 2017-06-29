package org.korhan.konithor.globals;

import org.slf4j.LoggerFactory

fun <T> loggerFor(clazz: Class<T>) = LoggerFactory.getLogger(clazz)

