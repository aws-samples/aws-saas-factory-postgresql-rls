# Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
# 
# Permission is hereby granted, free of charge, to any person obtaining a copy of this
# software and associated documentation files (the "Software"), to deal in the Software
# without restriction, including without limitation the rights to use, copy, modify,
# merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
# permit persons to whom the Software is furnished to do so.
# 
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
# INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
# PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
# HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
# OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
# SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

#FROM amazoncorretto:11-alpine-jdk AS build
FROM public.ecr.aws/amazoncorretto/amazoncorretto:11-al2-jdk AS build
RUN ["/usr/lib/jvm/java/bin/jlink", "--compress=2", "--module-path", "/usr/lib/jvm/java/jmods", "--add-modules", "java.base,java.logging,java.xml,jdk.unsupported,java.sql,java.naming,java.desktop,java.management,java.security.jgss,java.instrument", "--output", "/jdk-mini"]

#FROM alpine
FROM public.ecr.aws/micahhausler/alpine:3.14.0
COPY --from=build /jdk-mini /opt/jdk/
ENV PATH=$PATH:/opt/jdk/bin
COPY ./target/SaaSFactoryPgRLS.war /usr/src/app/app.war
ENTRYPOINT ["java", "-jar", "/usr/src/app/app.war"]
EXPOSE 8080/tcp
