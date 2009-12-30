#!/bin/sh

version=1.2.8

#mvn install

mvn deploy:deploy-file \
    -Dfile=target/utilities-${version}.jar \
    -DpomFile=pom.xml \
    -DrepositoryId=st-thirdparty \
    -Durl=dav:https://maven.st.cs.uni-saarland.de/nexus/content/repositories/thirdparty

mvn deploy:deploy-file \
    -Dfile=target/utilities-${version}-sources.jar \
    -Dclassifier=sources \
    -DpomFile=pom.xml \
    -DrepositoryId=st-thirdparty \
    -Durl=dav:https://maven.st.cs.uni-saarland.de/nexus/content/repositories/thirdparty
mvn deploy:deploy-file \
    -Dfile=target/utilities-${version}-javadoc.jar \
    -Dclassifier=javadoc \
    -DpomFile=pom.xml \
    -DrepositoryId=st-thirdparty \
    -Durl=dav:https://maven.st.cs.uni-saarland.de/nexus/content/repositories/thirdparty

