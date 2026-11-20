#!/usr/bin/env python3
"""Runs one command on a Minecraft server over RCON and prints the reply.

Usage: rcon.py <host> <port> <password> <command...>
Exits with 1 if the server can't be reached or rejects the password.
"""
import socket
import struct
import sys

LOGIN = 3
COMMAND = 2


def send(sock, request_id, kind, body):
    payload = struct.pack("<ii", request_id, kind) + body.encode("utf-8") + b"\x00\x00"
    sock.sendall(struct.pack("<i", len(payload)) + payload)


def receive_exact(sock, size):
    data = b""
    while len(data) < size:
        chunk = sock.recv(size - len(data))
        if not chunk:
            raise ConnectionError("connection closed by the server")
        data += chunk
    return data


def receive(sock):
    (length,) = struct.unpack("<i", receive_exact(sock, 4))
    data = receive_exact(sock, length)
    request_id, _kind = struct.unpack("<ii", data[:8])
    return request_id, data[8:-2].decode("utf-8", "replace")


def main():
    if len(sys.argv) < 5:
        print(__doc__.strip(), file=sys.stderr)
        return 2
    host, port, password = sys.argv[1], int(sys.argv[2]), sys.argv[3]
    command = " ".join(sys.argv[4:])
    try:
        with socket.create_connection((host, port), timeout=20) as sock:
            send(sock, 1, LOGIN, password)
            request_id, _ = receive(sock)
            if request_id == -1:
                print("RCON login was rejected", file=sys.stderr)
                return 1
            send(sock, 2, COMMAND, command)
            _, reply = receive(sock)
            print(reply)
            return 0
    except (OSError, ConnectionError) as error:
        print(f"RCON failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
