package de.hammacher.util;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;

public class AppendDateOutputStream extends OutputStream {

	private final OutputStream out;
	private boolean printDate = true;

	public AppendDateOutputStream(final OutputStream out) {
		this.out = out;
	}

	@Override
	public synchronized void write(final int b) throws IOException {
		printDate();
		this.out.write(b);
		if (b == '\n')
			this.printDate = true;
	}

	private void printDate() throws IOException {
		if (!this.printDate)
			return;

		final String curDate = new Date().toString();
		this.out.write(curDate.getBytes());
		this.out.write(' ');
		this.out.write(' ');
		this.printDate = false;
	}

}