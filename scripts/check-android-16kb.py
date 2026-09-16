#!/usr/bin/env python3
"""Check shipped 64-bit ELF LOAD and GNU_RELRO alignment in an AAR or APK."""

import argparse
import struct
import zipfile


def check_elf(data, name):
    if data[:6] != b"\x7fELF\x02\x01":
        raise ValueError(f"{name}: expected little-endian ELF64")
    phoff = struct.unpack_from("<Q", data, 32)[0]
    phentsize, phnum = struct.unpack_from("<HH", data, 54)
    if phentsize < 56 or phoff + phentsize * phnum > len(data):
        raise ValueError(f"{name}: invalid program headers")
    loads = relros = 0
    errors = []
    for i in range(phnum):
        kind, _, offset, address, _, _, size, alignment = struct.unpack_from(
            "<IIQQQQQQ", data, phoff + i * phentsize
        )
        if kind == 1:
            loads += 1
            if alignment < 16384 or alignment & (alignment - 1) or (address - offset) % 16384:
                errors.append(f"LOAD alignment={alignment:#x}, address={address:#x}, offset={offset:#x}")
        elif kind == 0x6474E552:
            relros += 1
            end = address + size
            print(f"{name}: GNU_RELRO end={end:#x}, remainder={end % 16384:#x}")
            if end % 16384:
                errors.append("GNU_RELRO end is not 16 KB aligned")
    if not loads or not relros:
        errors.append("missing LOAD or GNU_RELRO (do not disable RELRO to pass)")
    if errors:
        raise ValueError(f"{name}: " + "; ".join(errors))
    print(f"PASS {name}: {loads} LOAD segments, {relros} GNU_RELRO")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("archive")
    args = parser.parse_args()
    found = set()
    failures = []
    with zipfile.ZipFile(args.archive) as archive:
        for name in archive.namelist():
            parts = name.split("/")
            if len(parts) != 3 or parts[0] not in ("jni", "lib") or parts[1] not in ("arm64-v8a", "x86_64") or not name.endswith(".so"):
                continue
            found.add(parts[1])
            try:
                check_elf(archive.read(name), name)
            except (ValueError, struct.error) as error:
                failures.append(str(error))
    for abi in ("arm64-v8a", "x86_64"):
        if abi not in found:
            failures.append(f"missing required ABI: {abi}")
    for failure in failures:
        print(f"FAIL {failure}")
    return bool(failures)


if __name__ == "__main__":
    raise SystemExit(main())
