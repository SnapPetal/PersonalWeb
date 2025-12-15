package biz.thonbecker.personal.content.domain;

/**
 * Represents the result of an audio file storage operation.
 *
 * @param cdnUrl The CDN URL where the audio file can be accessed
 * @param bucketName The S3 bucket name where the file is stored
 * @param objectKey The S3 object key
 */
public record AudioResult(String cdnUrl, String bucketName, String objectKey) {}
