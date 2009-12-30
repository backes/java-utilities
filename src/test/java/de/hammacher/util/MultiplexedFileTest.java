package de.hammacher.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.util.AbstractList;
import java.util.Collection;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import de.hammacher.util.MultiplexedFileReader.MultiplexInputStream;
import de.hammacher.util.MultiplexedFileWriter.MultiplexOutputStream;
import de.hammacher.util.MultiplexedFileWriter.MultiplexOutputStream.Reader;


@RunWith(Parameterized.class)
public class MultiplexedFileTest {

	protected static final int numTests = 1000;

	private final long seed;
	private final int num;

	private MultiplexedFileWriter mWriter;
	private File tmpFileName;
	private int blockSize;
	private boolean useMemMap;
	private ByteOrder byteOrder;
	private boolean autoFlush;
	private Random rand;

	private MultiplexOutputStream[] outStreams;
	private byte[][] bytes;
	private int[] streamIds;

	public MultiplexedFileTest(long seed, int num) {
		this.seed = seed;
		this.num = num;
	}

	@Parameters
	public static Collection<Object[]> parameters() {
		final Random rand = new Random();
		return new AbstractList<Object[]>() {

			@Override
			public Object[] get(int index) {
				return new Object[] {
					rand.nextLong(),
					index / 10
				};
			}

			@Override
			public int size() {
				return numTests;
			}

		};
	}

	@Before
	public void setUp() throws IOException {
		this.tmpFileName = File.createTempFile("multiplexed-test-", ".dat");
		this.rand = new Random(this.seed);
		this.blockSize = 8 << this.rand.nextInt(16);
		this.useMemMap = MultiplexedFileReader.is64bitVM && this.rand.nextBoolean();
		this.byteOrder = this.rand.nextBoolean() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
		this.autoFlush = this.rand.nextBoolean();

		this.mWriter = new MultiplexedFileWriter(this.tmpFileName, this.blockSize, this.useMemMap,
			this.byteOrder, this.autoFlush);

		this.outStreams = new MultiplexOutputStream[this.num];
		this.bytes = new byte[this.num][];
		this.streamIds = new int[this.num];
		for (int i = 0; i < this.num; ++i) {
			this.outStreams[i] = this.mWriter.newOutputStream();
			this.streamIds[i] = this.outStreams[i].getId();
			this.bytes[i] = new byte[this.rand.nextInt(128*i+1)];
			this.rand.nextBytes(this.bytes[i]);
			if (this.rand.nextBoolean()) {
				this.outStreams[i].write(this.bytes[i]);
			} else {
				for (int k = 0; k < this.bytes[i].length; ++k) {
					this.outStreams[i].write(this.bytes[i][k]);
				}
			}
		}
	}

	@After
	public void tearDown() throws IOException {
		if (this.mWriter != null)
			this.mWriter.close();
		this.tmpFileName.delete();
	}

	@Test
	public void checkStreamContent() throws Exception {
		try {
			for (int i = 0; i < this.num; ++i) {
				MultiplexOutputStream mOut = this.outStreams[i];
				assertEquals("stream id", this.streamIds[i], mOut.getId());
				byte[] expected = this.bytes[i];
				assertEquals("Stream length does not match", expected.length, mOut.length());

				int startPos = this.rand.nextInt(expected.length+1);
				byte[] expectedPart = new byte[expected.length - startPos];
				System.arraycopy(expected, startPos, expectedPart, 0, expectedPart.length);

				byte[] read = new byte[expected.length-startPos];
				Reader mOutReader = mOut.getReader(startPos);
				assertEquals("stream id", this.streamIds[i], mOutReader.getId());
				readFully(mOutReader, read);
				assertArrayEquals("Read bytes from OutputReader do not match",
					expectedPart, read);
				assertEquals("Position", expected.length, mOutReader.getPosition());
				assertEquals("Expected EOF", -1, mOutReader.read());

				mOutReader = mOut.getReader(startPos);
				for (int k = 0; k < expectedPart.length; ++k) {
					assertEquals("Position", startPos + k, mOutReader.getPosition());
					assertEquals("Read byte does not match", expectedPart[k]&0xff, mOutReader.read());
				}
				assertEquals("Position", expected.length, mOutReader.getPosition());
				assertEquals("Expected EOF", -1, mOutReader.read());
				assertEquals("Position", expected.length, mOutReader.getPosition());
			}

			// now close the file and do the same checks on the MultiplexedFileReader
			this.mWriter.close();
			// let the gargabe collector throw away unneeded parts now
			this.mWriter = null;
			this.outStreams = null;
			System.gc();

			RandomAccessFile randFile = new RandomAccessFile(this.tmpFileName, "r");
			MultiplexedFileReader mReader = new MultiplexedFileReader(randFile,
				MultiplexedFileReader.is64bitVM && this.rand.nextBoolean());
			assertEquals("blocksize", this.blockSize, mReader.getBlockSize());

			for (int i = 0; i < this.num; ++i) {
				assertTrue("Stream not present", mReader.hasStreamId(this.streamIds[i]));
				MultiplexInputStream mIn = mReader.getInputStream(this.streamIds[i]);
				assertEquals("stream id", this.streamIds[i], mIn.getId());
				byte[] expected = this.bytes[i];
				assertEquals("Stream length does not match", expected.length, mIn.getDataLength());

				int startPos = this.rand.nextInt(expected.length+1);
				byte[] expectedPart = new byte[expected.length - startPos];
				System.arraycopy(expected, startPos, expectedPart, 0, expectedPart.length);

				byte[] read = new byte[expected.length-startPos];
				mIn.seek(startPos);
				assertEquals("Position", startPos, mIn.getPosition());
				readFully(mIn, read);
				assertArrayEquals("Read bytes from OutputReader do not match",
					expectedPart, read);
				assertEquals("Position", expected.length, mIn.getPosition());
				assertEquals("Expected EOF", -1, mIn.read());
				assertEquals("Position", expected.length, mIn.getPosition());

				mIn.seek(startPos);
				for (int k = 0; k < expectedPart.length; ++k) {
					assertEquals("Position", startPos + k, mIn.getPosition());
					assertEquals("Read byte does not match", expectedPart[k]&0xff, mIn.read());
				}
				assertEquals("Position", expected.length, mIn.getPosition());
				assertEquals("Expected EOF", -1, mIn.read());
				assertEquals("Position", expected.length, mIn.getPosition());
				mIn.close();
			}
			mReader.close();
		} catch (Throwable t) {
			throw new Exception("Error for seed "+this.seed+", num "+this.num, t);
		}
	}

	private static void readFully(InputStream reader, byte[] buf) throws IOException {
		int read = 0;
		while (read < buf.length) {
			int newRead = reader.read(buf, read, buf.length - read);
			if (newRead <= 0)
				throw new IOException("no more data");
			read += newRead;
		}
	}

}
