package de.hammacher.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.Set;

public class MultiplexedFileReader {

    private static class StreamDef {

        public int startAddr;
        public long length;

        public StreamDef(final int startAddr, final long length) {
            this.startAddr = startAddr;
            this.length = length;
        }

    }

    public class MultiplexInputStream extends InputStream {

        private final int id;
        private final int depth;
        private final long dataLength;
        private final byte[][] dataBlocks;
        private final int[] pos;
        private int remainingInCurrentBlock;

        protected MultiplexInputStream(final int id, final int beginningBlockAddr, final long length) throws IOException {
            this.id = id;
            this.dataLength = length;
            this.depth = compDepth(this.dataLength);

            this.pos = new int[this.depth+1];
            this.dataBlocks = new byte[this.depth+1][MultiplexedFileReader.this.blockSize];
            this.remainingInCurrentBlock = (int) Math.min(this.dataLength, MultiplexedFileReader.this.blockSize);

            readBlock(beginningBlockAddr, this.dataBlocks[0]);
            for (int i = 1; i <= this.depth; ++i) {
                readBlock(readInt(this.dataBlocks[i-1], 0), this.dataBlocks[i]);
            }
        }

        private int readInt(final byte[] buf, final int offset) {
            return ((buf[offset] & 0xff) << 24)
                | ((buf[offset+1] & 0xff) << 16)
                | ((buf[offset+2] & 0xff) << 8)
                | (buf[offset+3] & 0xff);
        }

        private int compDepth(final long len) {
            int d = 0;
            long max = MultiplexedFileReader.this.blockSize;
            while (max < len) {
                ++d;
                max *= MultiplexedFileReader.this.blockSize/4;
            }
            return d;
        }

        public void seek(final long toPos) throws IOException {
            if (toPos < 0 || toPos > this.dataLength)
                throw new IOException("pos must be in the range 0 .. dataLength");
            if (toPos == this.dataLength) {
                final int[] newPos = getBlocksPos(toPos, true);
                for (int i = 0; i <= this.depth; ++i)
                    this.pos[i] = newPos[i];
                this.remainingInCurrentBlock = 0;
            } else {
                final int[] newPos = getBlocksPos(toPos, false);
                // read the data blocks
                for (int i = 0; i < this.depth; ++i) {
                    if (newPos[i] != this.pos[i]) {
                        readBlock(readInt(this.dataBlocks[i], newPos[i]), this.dataBlocks[i+1]);
                        this.pos[i] = newPos[i];
                    }
                }
                this.pos[this.depth] = newPos[this.depth];

                this.remainingInCurrentBlock = (int) Math.min(
                    MultiplexedFileReader.this.blockSize - this.pos[this.depth],
                    this.dataLength - toPos);
            }

        }

        /**
         *
         * @param position
         * @param moveAfter if true, then the positions will point after the given position
         *                  (as if you would have done several read()s to get there), otherwise
         *                  they point in front of that position, i.e. you can directly read from
         *                  there without moveToNextBlock()
         * @return an array describing the position in the blocks at each level
         * @throws IOException
         */
        private int[] getBlocksPos(final long position, final boolean moveAfter) throws IOException {
            long remaining = position;
            final int[] newPos = new int[this.depth+1];
            for (int d = this.depth; d >= 0; --d) {
                newPos[d] = moveAfter
                    ? (int) ((remaining-1) % MultiplexedFileReader.this.blockSize) + 1
                    : (int) (remaining % MultiplexedFileReader.this.blockSize);
                remaining = remaining / MultiplexedFileReader.this.blockSize * 4;
            }
            if (remaining != 0)
                throw new IOException("internal error");
            return newPos;
        }

        @Override
        public int read() throws IOException {
            if (this.remainingInCurrentBlock == 0) {
                moveToNextBlock();
                if (this.remainingInCurrentBlock == 0)
                    return -1;
            }
            --this.remainingInCurrentBlock;
            return this.dataBlocks[this.depth][this.pos[this.depth]++] & 0xff;
        }

        @Override
        public int read(final byte[] b, final int off, final int len) throws IOException {
            if (b == null)
                throw new NullPointerException();
            if (off < 0 || len < 0 || len + off > b.length)
                throw new IndexOutOfBoundsException();
            if (len == 0)
                return 0;

            int ptr = off;
            final int end = off + len;
            while (ptr < end) {
                if (this.remainingInCurrentBlock == 0) {
                    moveToNextBlock();
                    if (this.remainingInCurrentBlock == 0)
                        return ptr == off ? -1 : ptr-off;
                }
                final int read = Math.min(end - ptr, this.remainingInCurrentBlock);
                System.arraycopy(this.dataBlocks[this.depth], this.pos[this.depth], b, ptr, read);
                ptr += read;
                this.remainingInCurrentBlock -= read;
                this.pos[this.depth] += read;
            }
            return len;
        }

        private void moveToNextBlock() throws IOException {
            final long read = getPosition();
            final long remaining = this.dataLength - read;
            if (remaining <= 0)
                return;
            this.remainingInCurrentBlock = (int) Math.min(remaining, MultiplexedFileReader.this.blockSize);

            readNext(this.depth);
            assert this.pos[this.depth] < MultiplexedFileReader.this.blockSize;
        }

        private void readNext(final int level) throws IOException {
            assert level > 0;
            if (this.pos[level-1] + 4 == MultiplexedFileReader.this.blockSize) {
                readNext(level-1);
                assert this.pos[level-1] < MultiplexedFileReader.this.blockSize;
            } else {
                this.pos[level-1] += 4;
            }
            readBlock(readInt(this.dataBlocks[level-1], this.pos[level-1]), this.dataBlocks[level]);
            this.pos[level] = 0;
        }

        public long getPosition() {
            long read = this.pos[0];
            for (int i = 1; i <= this.depth; ++i)
                read = MultiplexedFileReader.this.blockSize/4*read + this.pos[i];
            return read;
        }

        @Override
        public int available() throws IOException {
            return (int) Math.min(Integer.MAX_VALUE, this.dataLength - getPosition());
        }

        public int getId() {
            return this.id;
        }

        public long getDataLength() {
            return this.dataLength;
        }

        @Override
        public void close() {
            // nothing to do
        }

        public boolean isEOF() throws IOException {
            if (this.remainingInCurrentBlock == 0) {
                moveToNextBlock();
                if (this.remainingInCurrentBlock == 0)
                    return true;
            }
            return false;
        }
    }

    private static final long POS_INT_MASK = 0xffffffffL;

    protected final int blockSize; // MUST be divisible by 4

    protected final RandomAccessFile file;

    private final IntegerMap<StreamDef> streamDefs;

    public MultiplexedFileReader(final RandomAccessFile file) throws IOException {
        this.file = file;

        file.seek(0);

        if (file.length() < MultiplexedFileWriter.headerSize || file.readInt() != MultiplexedFileWriter.MAGIC_HEADER)
            throw new IOException("File contains no MultiplexedFile");
        this.blockSize = file.readInt();
        if ((this.blockSize & 0x3) != 0)
            throw new IOException("blocksize must be divisible by 4");

        final int streamDefsStartingBlock = file.readInt();
        final long streamDefsLength = file.readLong();

        // read the stream defs
        final MultiplexInputStream streamDefStream = new MultiplexInputStream(-1, streamDefsStartingBlock, streamDefsLength);
        final MyDataInputStream str = new MyDataInputStream(streamDefStream);
        final int numStreams = (int) (streamDefStream.getDataLength()/16);
        if ((long)numStreams*16 != streamDefStream.getDataLength())
            throw new IOException("corrupted data");
        this.streamDefs = new IntegerMap<StreamDef>();
        for (int i = 0; i < numStreams; ++i) {
            final int id = str.readInt();
            final int start = str.readInt();
            final long length = str.readLong();
            if (length < 0 || this.streamDefs.put(id, new StreamDef(start, length)) != null)
                throw new IOException("corrupted data");
        }
        str.close();
    }

    public synchronized void readBlock(final int blockAddr, final byte[] buf) throws IOException {
        this.file.seek(MultiplexedFileWriter.headerSize+(blockAddr&POS_INT_MASK)*this.blockSize);
        this.file.readFully(buf, 0, this.blockSize);
    }

    public MultiplexedFileReader(final File filename) throws IOException {
        this(new RandomAccessFile(filename, "r"));
    }

    public Set<Integer> getStreamIds() {
        // unmodifiable by definition
        return this.streamDefs.keySet();
    }

    public MultiplexInputStream getInputStream(final int index) throws IOException {
        final StreamDef def = this.streamDefs.get(index);
        if (def == null)
            return null;
        return new MultiplexInputStream(index, def.startAddr, def.length);
    }

    public void close() throws IOException {
        this.file.close();
    }

    public int getBlockSize() {
        return this.blockSize;
    }

}
