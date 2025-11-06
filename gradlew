#!/bin/sh
APP_HOME=$( cd "${APP_HOME:-./}" && pwd -P ) || exit
APP_NAME="Gradle"
APP_BASE_NAME=${0##*/}
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar
exec java "${JAVA_OPTS:-$DEFAULT_JVM_OPTS}" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
