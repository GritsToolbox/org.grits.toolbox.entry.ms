package org.grits.toolbox.entry.ms.exceptions;

public class MSException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 29244258104245833L;

	public final static int MS1_NOT_FOUND = 1;
	public final static int EMPTY_PEAK_LIST = 2;
	public final static int UNABLE_TO_OPEN_PARSER = 3;
	public final static int SCAN_NOT_FOUND = 4;
	
	protected int exceptionType;
	
	public MSException( int exceptionType ) {
		this.exceptionType = exceptionType;
	}
	
	@Override
	public String getMessage() {
		switch( this.exceptionType ) {
			case MS1_NOT_FOUND: return "Unable to find MS1 scan in MS file";
			case EMPTY_PEAK_LIST : return "Empty peak list for MS Spectra chart in initializeChartData.";			
			case UNABLE_TO_OPEN_PARSER : return "Unable to initialize MzXML parser.";			
			case SCAN_NOT_FOUND : return "Unable to parse requested scan from MzXML.";			
		}
		return super.getMessage();
	}
}
