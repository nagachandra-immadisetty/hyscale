package io.hyscale.commons.utils;

public interface TailHandler {

	public void handleLine(String line);

	public boolean handleEOF(String line);
}