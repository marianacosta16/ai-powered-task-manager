#!/bin/sh
# Runs before nginx starts. Without a certificate in /etc/nginx/certs, makes a self-signed one for localhost
# (the browser will warn about it). To use a real one, mount tls.crt and tls.key in that folder.
set -eu
dir=/etc/nginx/certs
if [ -f "$dir/tls.crt" ] && [ -f "$dir/tls.key" ]; then
  echo "Using the certificate found in $dir"
  exit 0
fi
mkdir -p "$dir"
openssl req -x509 -newkey rsa:2048 -nodes -days 365 -subj "/CN=localhost" \
  -addext "subjectAltName=DNS:localhost,IP:127.0.0.1" \
  -keyout "$dir/tls.key" -out "$dir/tls.crt" 2>/dev/null
echo "Made a self-signed certificate for localhost in $dir"
