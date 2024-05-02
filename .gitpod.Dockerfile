FROM gitpod/workspace-full

USER gitpod

RUN bash -c ". /home/gitpod/.sdkman/bin/sdkman-init.sh && \
    sdk install java 17.0.11-zulu && \
    sdk default java 17.0.11-zulu && \
    sdk install kotlin && \
    sdk default kotlin 1.9.23 && \
    sdk install ki 0.5.2 && \
    sdk default ki 0.5.2 && \
    export PATH=/home/gitpod/.sdkman/candidates/kotlin/current/bin/:/home/gitpod/.sdkman/candidates/ki/current/bin:$PATH"
