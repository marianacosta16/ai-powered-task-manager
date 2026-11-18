# The Matriz web interface, served by nginx over HTTPS. The same nginx forwards /api to the API, so the browser
# talks to one single address and the session cookie never crosses sites.
# No credentials here. The certificate is made when the container starts (a self-signed one, for local use) unless a
# real one is mounted in /etc/nginx/certs (tls.crt and tls.key).

# ---- build ----
FROM node:24-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
# The address of the API as the browser sees it: the /api of the same address as the page
ARG VITE_API_URL=/api
RUN VITE_API_URL="$VITE_API_URL" npm run build

# ---- run ----
FROM nginx:1.29-alpine
RUN apk add --no-cache openssl
COPY --from=build /app/dist /usr/share/nginx/html
# The official image fills ${API_UPSTREAM} in the templates when it starts
COPY docker/nginx.conf.template /etc/nginx/templates/default.conf.template
COPY --chmod=755 docker/make-certificate.sh /docker-entrypoint.d/40-make-certificate.sh
ENV API_UPSTREAM=http://api:8080 \
    PUBLIC_HTTPS_PORT=443 \
    DNS_RESOLVER=127.0.0.11
EXPOSE 80 443
# The whole chain a visitor depends on: the page is served over HTTPS (the certificate may be self-signed) and /api reaches the
# API through the proxy. A wrong API_UPSTREAM or a stopped API makes this container unhealthy, not only a broken nginx.
HEALTHCHECK --interval=10s --timeout=5s --start-period=10s --retries=6 \
  CMD wget -q --no-check-certificate -O /dev/null https://localhost/api/actuator/health || exit 1
