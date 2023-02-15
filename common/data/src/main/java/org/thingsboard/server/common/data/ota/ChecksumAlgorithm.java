package org.thingsboard.server.common.data.ota;

public enum ChecksumAlgorithm {
    MD5,
    SHA256,
    SHA384,
    SHA512,
    CRC32,
    MURMUR3_32,
    MURMUR3_128
}
