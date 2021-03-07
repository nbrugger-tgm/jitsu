package com.niton.parser;

/**
 * @param <T> the type to fill the data into
 */
interface DataSource<T> {
	/**
	 * Reads data from the source and fills it into the target
	 *
	 * @param target the Data will be filled into this
	 */
	void fillData(T target);
}

public class Test {
	public static void main(String[] args) {
		new DataStorer<byte[]>(new byte[200], new FileDataSource());
		new DataStorer("String", new FileDataSource());
	}
}

/**
 * Fills the data automaticaly into the store
 *
 * @param <T> the type to store into
 */
class DataStorer<T> {
	public DataStorer(T store, DataSource<T> source) {
		source.fillData(store);
	}
}

class FileDataSource implements DataSource<byte[]> {
	@Override
	public void fillData(byte[] target) {
		//readig a file and fill the array
	}
}