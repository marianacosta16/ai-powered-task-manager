#!/bin/sh
# Runs before nginx starts. Without a certificate in /etc/nginx/certs, makes a self-signed one for localhost
# (the browser will warn about it). To use a real one, mount tls.crt and tls.key in that folder: it is never replaced.
# A self-signed certificate made here is replaced by a new one when it has less than 30 days left, so it never expires.
set -eu
dir=/etc/nginx/certs
marker="$dir/.self-signed"
if [ -f "$dir/tls.crt" ] && [ -f "$dir/tls.key" ]; then
  if [ ! -f "$marker" ] || openssl x509 -checkend 2592000 -noout -in "$dir/tls.crt" >/dev/null; then
    echo "Using the certificate found in $dir"
    exit 0
  fi
  echo "The self-signed certificate is about to expire: making a new one"
fi
mkdir -p "$dir"
openssl req -x509 -newkey rsa:2048 -nodes -days "${CERT_DAYS:-365}" -subj "/CN=localhost" \
  -addext "subjectAltName=DNS:localhost,IP:127.0.0.1" \
  -keyout "$dir/tls.key" -out "$dir/tls.crt"
touch "$marker"
echo "Made a self-signed certificate for localhost in $dir"
