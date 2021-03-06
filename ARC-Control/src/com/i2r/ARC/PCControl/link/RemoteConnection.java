package com.i2r.ARC.PCControl.link;

import java.io.DataInputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Abstract class for a remote connection, which is what is used to read and write data to a remote device.
 * Remote connections are created by a {@link RemoteLink} and are typed to kind of data that will be written 
 * and received from the data streams to and from the socket
 * 
 * The streams generated are {@link DataInputStream} for input and {@link DataOuputStream} for output
 * 
 * @author Johnathan Pagnutti
 * @param <T> the type of data that the {@link RemoteConnection} is going to deal with
 */
public abstract class RemoteConnection<T> {

	public InputStream dataIn;
	public OutputStream dataOut;
}
