FROM gitpod/workspace-full

USER gitpod

RUN bash -c ". /home/gitpod/.sdkman/bin/sdkman-init.sh && \
    sdk install java 17.0.4-tem && \
    sdk default java 17.0.4-tem"
    
RUN brew install kotlin
