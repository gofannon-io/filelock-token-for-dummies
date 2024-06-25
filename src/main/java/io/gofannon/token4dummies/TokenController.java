package io.gofannon.token4dummies;

import org.apache.commons.io.FileUtils;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;

public class TokenController implements Closeable {

    private static final String LOCK_FILENAME = "token.lock";
    private static final String EXCHANGE_FILENAME = "token.exchange";
    private static final File EXCHANGE_FILE= new File(EXCHANGE_FILENAME);


    private RandomAccessFile writeFile;
    private FileChannel writeChannel;
    private FileLock writeLock;


    public void acquireWriteToken() throws IOException {
        writeFile = new RandomAccessFile(LOCK_FILENAME, "rw");
        writeChannel = writeFile.getChannel();
        writeLock = writeChannel.tryLock();
    }


    public void retryAcquireWriteToken() throws IOException {
        writeLock = writeChannel.tryLock();
    }


    public boolean isWriteLocked() {
        return writeLock != null && writeLock.isValid();
    }


    public void releaseToken() {
        releaseWriteLockQuietly();
        closeWriteChannelQuietly();
        closeWriteFileQuietly();
    }


    private void releaseWriteLockQuietly() {
        if (writeLock != null) {
            try {
                writeLock.release();
            } catch (IOException ignored) {
            } finally {
                writeLock = null;
            }
        }
    }

    private void closeWriteChannelQuietly() {
        if (writeChannel != null) {
            try {
                writeChannel.close();
            } catch (IOException ignored) {
            } finally {
                writeChannel = null;
            }
        }
    }

    private void closeWriteFileQuietly() {
        if (writeFile != null) {
            try {
                writeFile.close();
            } catch (IOException ignored) {
            } finally {
                writeFile = null;
            }
        }
    }


    public void write(String content) throws IOException {
        FileUtils.writeStringToFile(EXCHANGE_FILE, content, StandardCharsets.UTF_8);
    }

    public String read() throws IOException {
        if( !EXCHANGE_FILE.exists())
            return "<no-file>";
        return FileUtils.readFileToString(EXCHANGE_FILE, StandardCharsets.UTF_8);
    }

    @Override
    public void close() {
        releaseToken();
    }
}
