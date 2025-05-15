/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.execution.infra.storage.jpa;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@Converter
public class ReportConverter implements AttributeConverter<String, byte[]> {
    @Override
    public byte[] convertToDatabaseColumn(String report) {
        byte[] reportBytes = report.getBytes(StandardCharsets.UTF_8);
        if (isCompressed(reportBytes)) {
            return reportBytes;
        }
        return compress(report);
    }

    @Override
    public String convertToEntityAttribute(byte[] zippedReport) {
        if (isCompressed(zippedReport)) {
            return decompress(zippedReport);
        }
        return new String(zippedReport, StandardCharsets.UTF_8);
    }

    private byte[] compress(String report) {
            try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                 GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream)) {
                InputStream inputStream = new ByteArrayInputStream(report.getBytes(StandardCharsets.UTF_8));
                byte[] buffer = new byte[1024];
                int bytesRead;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    gzipOutputStream.write(buffer, 0, bytesRead);
                }
                gzipOutputStream.finish();
                return byteArrayOutputStream.toByteArray();

            } catch (IOException e) {
                throw new RuntimeException("Failed to compress report content", e);
            }
        }


    private String decompress(byte[] compressedData) {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(compressedData);
             GZIPInputStream gzipInputStream = new GZIPInputStream(byteArrayInputStream);
             ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            byteArrayOutputStream.write(gzipInputStream.readAllBytes());
            return byteArrayOutputStream.toString(StandardCharsets.UTF_8);

        } catch (IOException e) {
            throw new RuntimeException("Failed to decompress report content", e);
        }
    }

    private boolean isCompressed(byte[] data) {
        return (data != null && data.length >= 2 &&
            (data[0] == (byte) 0x1f && data[1] == (byte) 0x8b));
    }
}
