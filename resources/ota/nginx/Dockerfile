# nginx 1.30.7 (stable) fixes the NGINX2Root advisories (CVE-2026-123007, CVE-2026-123633,
# CVE-2026-123634, CVE-2026-121810, CVE-2026-113425) and all earlier 2026 nginx advisories.
# See https://nginx.org/en/security_advisories.html before changing this version.
FROM nginx:1.30.7-alpine

# Upgrade base packages to pick up patched Alpine libraries (e.g. OpenSSL) and avoid caching the index
RUN apk upgrade --no-cache && apk add --no-cache \
    apache2-utils \
    jq \
    openssl \
    bash

CMD ["nginx", "-g", "daemon off;"]
