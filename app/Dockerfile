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

FROM public.ecr.aws/docker/library/amazoncorretto:11-alpine-jdk AS build
RUN ["/usr/lib/jvm/default-jvm/bin/jlink", "--compress=2", "--no-man-pages", "--module-path", "/usr/lib/jvm/default-jvm/jmods", "--add-modules", "java.base,java.logging,java.xml,jdk.unsupported,java.sql,java.sql.rowset,java.naming,java.desktop,java.management,java.security.jgss,java.instrument,java.net.http", "--output", "/jdk-mini"]

FROM public.ecr.aws/docker/library/alpine:latest
COPY --from=build /jdk-mini /opt/jdk/
ENV PATH=$PATH:/opt/jdk/bin
COPY ./target/SaaSFactoryPgRLS.war /usr/src/app/app.war
ENTRYPOINT ["java", "-jar", "/usr/src/app/app.war"]
EXPOSE 8080/tcp
