FROM postgres:17.3-alpine

COPY docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh

ENTRYPOINT [ "bash", "-c", "exec docker-entrypoint.sh postgres" ]

LABEL authors="djordje"