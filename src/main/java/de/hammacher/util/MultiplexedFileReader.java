package de.hammacher.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.Set;

import de.hammacher.util.maps.IntegerMap;
import de.hammacher.util.streams.MyDataInputStream;

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
        private final int[][] pointerBlocks;
        private final byte[] dataBlock;
        private final int[] pos;
        private int remainingInCurrentBlock;

        protected MultiplexInputStream(final int id, final int beginningBlockAddr, final long length) throws IOException {
            this.id = id;
            this.dataLength = length;
            this.depth = compDepth(length);

            this.pos = new int[this.depth+1];
            this.pointerBlocks = new int[this.depth][MultiplexedFileReader.this.blockSize/4];
            this.dataBlock = new byte[MultiplexedFileReader.this.blockSize];
            this.remainingInCurrentBlock = (int) Math.min(this.dataLength, MultiplexedFileReader.this.blockSize);

            if (this.depth == 0) {
                readBlock(beginningBlockAddr, this.dataBlock);
            } else {
                readBlock(beginningBlockAddr, this.pointerBlocks[0]);
                for (int i = 1; i < this.depth; ++i) {
                    readBlock(this.pointerBlocks[i-1][0], this.pointerBlocks[i]);
                }
                readBlock(this.pointerBlocks[this.depth-1][0], this.dataBlock);
            }
        }

        private int compDepth(final long len) throws IOException {
            int d = 0;
            long max = MultiplexedFileReader.this.blockSize;
            while (max <= len) {
                ++d;
                max *= MultiplexedFileReader.this.blockSize/4;
                if (max <= MultiplexedFileReader.this.blockSize)
                    throw new IOException("Illegal stream length: " + len);
            }
            return d;
        }

        public void seek(final long toPos) throws IOException {
            if (toPos < 0 || toPos > this.dataLength)
                throw new IOException("pos must be in the range 0 .. dataLength");
            if (this.depth == 0) {
                this.pos[0] = (int) toPos;
                this.remainingInCurrentBlock = (int)(this.dataLength-toPos);
                return;
            }
            final int[] newPos = getBlocksPos(toPos);
            boolean reRead = false;
            for (int i = 0; i < this.depth; ++i) {
                if (reRead) {
                    final int blockAddr = this.pointerBlocks[i-1][newPos[i-1]];
                    readBlock(blockAddr, this.pointerBlocks[i]);
                } else
                    reRead = reRead || this.pos[i] != newPos[i];
                this.pos[i] = newPos[i];
            }
            if (reRead) {
                readBlock(this.pointerBlocks[this.depth-1][newPos[this.depth-1]], this.dataBlock);
            }
            this.pos[this.depth] = newPos[this.depth];
            this.remainingInCurrentBlock = (int)Math.min(MultiplexedFileReader.this.blockSize-newPos[this.depth], this.dataLength-toPos);
        }

        private int[] getBlocksPos(final long position) {
        	assert position <= this.dataLength;
        	if (this.depth == 0)
        		return new int[] { (int)position };

            final int[] newPos = new int[this.depth+1];
            newPos[this.depth] = (int) (position % MultiplexedFileReader.this.blockSize);
            long remaining = position / MultiplexedFileReader.this.blockSize;
            for (int d = this.depth-1; d > 0; --d) {
                newPos[d] = (int) (remaining % (MultiplexedFileReader.this.blockSize/4));
                remaining = remaining / (MultiplexedFileReader.this.blockSize/4);
            }
            assert remaining <= MultiplexedFileReader.this.blockSize/4;
            newPos[0] = (int) remaining;
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
            return this.dataBlock[this.pos[this.depth]++] & 0xff;
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
                System.arraycopy(this.dataBlock, this.pos[this.depth], b, ptr, read);
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

            for (int d = this.depth-1; d >= 0; --d) {
                if (this.pos[d] + 1 < MultiplexedFileReader.this.blockSize/4) {
                    ++this.pos[d];
                    for (; d < this.depth-1; ++d) {
                        readBlock(this.pointerBlocks[d][this.pos[d]], this.pointerBlocks[d+1]);
                        this.pos[d+1] = 0;
                    }
                    readBlock(this.pointerBlocks[this.depth-1][this.pos[this.depth-1]], this.dataBlock);
                    this.pos[this.depth] = 0;
                    break;
                }
            }
            assert this.pos[this.depth] < MultiplexedFileReader.this.blockSize;
        }

        public long getPosition() {
            if (this.depth == 0)
                return this.pos[0];

            long read = this.pos[0];
            for (int i = 1; i < this.depth; ++i)
                read = MultiplexedFileReader.this.blockSize/4*read + this.pos[i];
            read = MultiplexedFileReader.this.blockSize*read + this.pos[this.depth];
            return read;
        }

        @Override
        public int available() {
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

    // this is just some random integer
    public static final int MAGIC_HEADER = 0xB7A332B2;

    private static final int headerSize = 21; // bytes

    private static final long POS_INT_MASK = 0x8fffffffL;

    // each mapped slice has 1<<30 = 1GiBytes
    protected static final int MAPPING_SLICE_SIZE_BITS = 30;

    public static final boolean is64bitVM =
           "64".equals(System.getProperty("sun.arch.data.model"))
        || System.getProperty("os.arch", "").contains("64");

    protected final int blockSize; // MUST be divisible by 4

    private final ByteOrder byteOrder;

    private final boolean useMemoryMapping;

    protected final FileChannel fileChannel;
    private final MappedByteBuffer[] fileMappings;
    private final int numBlocksInFile;

    private final IntegerMap<StreamDef> streamDefs;

    public MultiplexedFileReader(final RandomAccessFile file, final boolean useMemoryMapping)
            throws IOException {
        this.useMemoryMapping = useMemoryMapping;
        this.fileChannel = file.getChannel();
        this.fileChannel.position(0);

        final long fileSize = this.fileChannel.size();
        if (fileSize < headerSize)
            throw new IOException("File contains no MultiplexedFile (too small)");
        final ByteBuffer headerBuffer = ByteBuffer.allocate(headerSize);
        while (headerBuffer.hasRemaining())
            this.fileChannel.read(headerBuffer, headerBuffer.position());
        headerBuffer.rewind();

        if (headerBuffer.getInt() != MAGIC_HEADER)
            throw new IOException("File contains no MultiplexedFile (illegal header)");
        this.blockSize = headerBuffer.getInt();
        if ((this.blockSize & 0x3) != 0)
            throw new IOException("blocksize must be divisible by 4");
        if (this.blockSize < 8)
            throw new IOException("blockSize must be >= 8");
        if ((1 << MAPPING_SLICE_SIZE_BITS) % this.blockSize != 0)
            throw new IllegalArgumentException("1<<"+MAPPING_SLICE_SIZE_BITS+" must be divisible by the blockSize");
        final int byteOrderInt = headerBuffer.get();
        if (byteOrderInt == 0)
            this.byteOrder = ByteOrder.BIG_ENDIAN;
        else if (byteOrderInt == 1)
            this.byteOrder = ByteOrder.LITTLE_ENDIAN;
        else
            throw new IOException("File contains no MultiplexedFile (illegal header)");

        final int streamDefsStartingBlock = headerBuffer.getInt();
        final long streamDefsLength = headerBuffer.getLong();
        assert !headerBuffer.hasRemaining();

        final long numBlocksInFile0 = (fileSize - headerSize) / this.blockSize;
        if (numBlocksInFile0 > (1l << 32) || fileSize != (headerSize+numBlocksInFile0*this.blockSize))
            throw new IOException("File contains no MultiplexedFile (illegal number of blocks in file)");
        this.numBlocksInFile = (int) numBlocksInFile0;

        // if file mapping is enabled, map all pieces of the file
        if (this.useMemoryMapping) {
        	int numMappings = (int) ((fileSize+((1<<MAPPING_SLICE_SIZE_BITS) - 1)) >> MAPPING_SLICE_SIZE_BITS);
			this.fileMappings = new MappedByteBuffer[numMappings];
			for (int i = 0; i < numMappings; ++i) {
                final long sliceSize = Math.min(1 << MAPPING_SLICE_SIZE_BITS,
                        ((long)this.numBlocksInFile * this.blockSize) - ((long)i << MAPPING_SLICE_SIZE_BITS));
                assert sliceSize > 0;
                this.fileMappings[i] = this.fileChannel.map(MapMode.READ_ONLY,
                		headerSize+((long)i << MAPPING_SLICE_SIZE_BITS), sliceSize);
			}
        } else {
        	this.fileMappings = null;
        }

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

    /**
     * Uses memory mapped files when running inside a 64-bit VM.
     *
     * @see #MultiplexedFileReader(RandomAccessFile, boolean)
     */
    public MultiplexedFileReader(final File filename) throws IOException {
        this(new RandomAccessFile(filename, "r"), is64bitVM);
    }

    protected void readBlock(final int blockAddr, final byte[] buf) throws IOException {
        assert buf.length == this.blockSize;
        if (this.useMemoryMapping) {
            final ByteBuffer mapping = getRawBlockMapping(blockAddr);
            mapping.get(buf, 0, this.blockSize);
        } else {
            final ByteBuffer bbuf = ByteBuffer.wrap(buf, 0, this.blockSize);
            while (bbuf.hasRemaining()) {
            	if (this.fileChannel.read(bbuf,
                        headerSize + ((blockAddr&POS_INT_MASK)*this.blockSize) + bbuf.position())
                		< 0)
            	throw new IOException("Unexpected EOF");
            }
        }
    }

    protected void readBlock(final int blockAddr, final int[] buf) throws IOException {
        assert buf.length == this.blockSize/4;
        if (this.useMemoryMapping) {
            final ByteBuffer mapping = getRawBlockMapping(blockAddr);
            mapping.order(this.byteOrder).asIntBuffer().get(buf, 0, this.blockSize/4);
        } else {
            final ByteBuffer bbuf = ByteBuffer.allocate(this.blockSize);
            final IntBuffer intBuf = bbuf.order(this.byteOrder).asIntBuffer();
            while (bbuf.hasRemaining()) {
                if (this.fileChannel.read(bbuf,
                        headerSize + ((blockAddr&POS_INT_MASK)*this.blockSize) + bbuf.position())
                        < 0)
                	throw new IOException("Unexpected EOF");
            }
            intBuf.get(buf, 0, this.blockSize/4);
        }
    }

    private ByteBuffer getRawBlockMapping(final int blockAddr) throws IOException {
        final long position = (blockAddr&POS_INT_MASK)*this.blockSize;
        final int mappingNr = (int) (position >>> MAPPING_SLICE_SIZE_BITS);
        final int posInMapping = ((int)position) & ((1<<MAPPING_SLICE_SIZE_BITS)-1);
        if (mappingNr < 0 || mappingNr >= this.fileMappings.length)
        	throw new IOException("requesting non-existing part of the file");
        final ByteBuffer duplicate = this.fileMappings[mappingNr].slice();
        duplicate.position(posInMapping);
        return duplicate;
    }

    public Set<Integer> getStreamIds() {
        // unmodifiable by definition
        return this.streamDefs.keySet();
    }

    public boolean hasStreamId(final int streamIndex) {
        return this.streamDefs.containsKey(streamIndex);
    }

    public MultiplexInputStream getInputStream(final int index) throws IOException {
        final StreamDef def = this.streamDefs.get(index);
        if (def == null)
            return null;
        return new MultiplexInputStream(index, def.startAddr, def.length);
    }

    public void close() throws IOException {
        this.fileChannel.close();
    }

    public int getBlockSize() {
        return this.blockSize;
    }

}
