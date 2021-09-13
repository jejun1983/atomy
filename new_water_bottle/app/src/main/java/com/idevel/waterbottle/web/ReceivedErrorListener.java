package com.idevel.waterbottle.web;

/**
 * The listener interface for receiving receivedError events.
 * The class that is interested in processing a receivedError
 * event implements this interface, and the object created
 * with that class is registered with a component using the
 * component's <code>addReceivedErrorListener<code> method. When
 * the receivedError event occurs, that object's appropriate
 * method is invoked.
 *
 * @see ReceivedErrorEvent
 */
public interface ReceivedErrorListener {

	/**
	 * Show error page.
	 */
	public void showErrorPage();
}
