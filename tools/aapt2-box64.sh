#!/bin/sh
# El aapt2 de AGP es x86-64 y en este VPS (aarch64) el binfmt activo es
# qemu-x86_64, que no encuentra ld-linux-x86-64. box64 sí lo ejecuta.
exec box64 /home/ubuntu/.local/share/aapt2/aapt2 "$@"
