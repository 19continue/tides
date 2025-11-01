package com.tides.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.tides.vo.DemoDataSnapshotVo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Date;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * 演示数据快照文件存储。
 * 快照数据不放 Redis，避免占用抢票链路核心缓存资源。
 */
public final class DemoDataSnapshotFileStore {

    private DemoDataSnapshotFileStore() {
    }

    public static <T> T read(String snapshotDir, String serviceName, Class<T> snapshotClass) {
        Path path = snapshotPath(snapshotDir, serviceName);
        if (!Files.exists(path)) {
            return readClasspath(serviceName, snapshotClass);
        }
        try (InputStream fileInputStream = Files.newInputStream(path);
             GZIPInputStream gzipInputStream = new GZIPInputStream(fileInputStream)) {
            String json = new String(gzipInputStream.readAllBytes(), StandardCharsets.UTF_8);
            return JSON.parseObject(json, snapshotClass);
        } catch (IOException exception) {
            throw new IllegalStateException("读取演示数据快照失败：" + path, exception);
        }
    }

    public static DemoDataSnapshotVo readStatus(String snapshotDir, String serviceName) {
        Path path = metadataPath(snapshotDir, serviceName);
        if (Files.exists(path)) {
            try {
                String json = Files.readString(path, StandardCharsets.UTF_8);
                return JSON.parseObject(json, DemoDataSnapshotVo.class);
            } catch (IOException exception) {
                throw new IllegalStateException("读取演示数据快照元数据失败：" + path, exception);
            }
        }
        String resourcePath = "demo-snapshots/" + metadataFileName(serviceName);
        try (InputStream inputStream = DemoDataSnapshotFileStore.class.getClassLoader()
                .getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                return null;
            }
            String json = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            return JSON.parseObject(json, DemoDataSnapshotVo.class);
        } catch (IOException exception) {
            throw new IllegalStateException("读取内置演示数据快照元数据失败：" + resourcePath, exception);
        }
    }

    public static void writeStatus(String snapshotDir, String serviceName, DemoDataSnapshotVo status) {
        Path path = metadataPath(snapshotDir, serviceName);
        Path tempPath = path.resolveSibling(path.getFileName() + ".tmp");
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(tempPath, JSON.toJSONString(status, SerializerFeature.WriteDateUseDateFormat),
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
            moveTempFile(tempPath, path);
        } catch (IOException exception) {
            throw new IllegalStateException("写入演示数据快照元数据失败：" + path, exception);
        }
    }

    public static boolean exists(String snapshotDir, String serviceName) {
        Path path = snapshotPath(snapshotDir, serviceName);
        if (Files.exists(path)) {
            return true;
        }
        String resourcePath = "demo-snapshots/" + snapshotFileName(serviceName);
        return DemoDataSnapshotFileStore.class.getClassLoader().getResource(resourcePath) != null;
    }

    public static Date lastModifiedTime(String snapshotDir, String serviceName) {
        Path path = snapshotPath(snapshotDir, serviceName);
        if (!Files.exists(path)) {
            return null;
        }
        try {
            return new Date(Files.getLastModifiedTime(path).toMillis());
        } catch (IOException exception) {
            throw new IllegalStateException("读取演示数据快照时间失败：" + path, exception);
        }
    }

    private static <T> T readClasspath(String serviceName, Class<T> snapshotClass) {
        String resourcePath = "demo-snapshots/" + snapshotFileName(serviceName);
        try (InputStream inputStream = DemoDataSnapshotFileStore.class.getClassLoader()
                .getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                return null;
            }
            try (GZIPInputStream gzipInputStream = new GZIPInputStream(inputStream)) {
                String json = new String(gzipInputStream.readAllBytes(), StandardCharsets.UTF_8);
                return JSON.parseObject(json, snapshotClass);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("读取内置演示数据基准失败：" + resourcePath, exception);
        }
    }

    public static void write(String snapshotDir, String serviceName, Object snapshot) {
        Path path = snapshotPath(snapshotDir, serviceName);
        Path tempPath = path.resolveSibling(path.getFileName() + ".tmp");
        try {
            Files.createDirectories(path.getParent());
            byte[] jsonBytes = JSON.toJSONString(snapshot, SerializerFeature.WriteDateUseDateFormat)
                    .getBytes(StandardCharsets.UTF_8);
            try (OutputStream fileOutputStream = Files.newOutputStream(tempPath,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
                 GZIPOutputStream gzipOutputStream = new GZIPOutputStream(fileOutputStream)) {
                gzipOutputStream.write(jsonBytes);
            }
            moveTempFile(tempPath, path);
        } catch (IOException exception) {
            throw new IllegalStateException("写入演示数据快照失败：" + path, exception);
        }
    }

    private static void moveTempFile(Path tempPath, Path path) throws IOException {
        try {
            Files.move(tempPath, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(tempPath, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static Path snapshotPath(String snapshotDir, String serviceName) {
        return Path.of(snapshotDir).resolve(snapshotFileName(serviceName));
    }

    private static Path metadataPath(String snapshotDir, String serviceName) {
        return Path.of(snapshotDir).resolve(metadataFileName(serviceName));
    }

    private static String snapshotFileName(String serviceName) {
        String safeServiceName = serviceName.replaceAll("[^a-zA-Z0-9_-]", "");
        return safeServiceName + "-demo-data-snapshot.json.gz";
    }

    private static String metadataFileName(String serviceName) {
        String safeServiceName = serviceName.replaceAll("[^a-zA-Z0-9_-]", "");
        return safeServiceName + "-demo-data-snapshot-meta.json";
    }
}
