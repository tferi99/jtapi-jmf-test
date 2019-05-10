package org.ftoth.general.util.jmf;

import javax.media.ControllerListener;
import javax.media.DataSink;
import javax.media.Format;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.Player;
import javax.media.Processor;
import javax.media.ProcessorModel;
import javax.media.datasink.DataSinkListener;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class JmfFactory
{
	private static final Log log = LogFactory.getLog(JmfFactory.class);
	
	public static Processor createRealizedProcessor(String inputDataUrl, Format outputFormat, ContentDescriptor contentType)
			throws Exception
	{
		return createRealizedProcessor(inputDataUrl, outputFormat, contentType, null);
	}

	public static Processor createRealizedProcessor(String inputDataUrl, Format outputFormat, ContentDescriptor contentType, ControllerListener listener)
			throws Exception
	{
		MediaLocator inputLocator = new MediaLocator(inputDataUrl);
		Format[] formats = { outputFormat };

		ProcessorModel m = new ProcessorModel(inputLocator, formats, contentType);
		Processor processor = Manager.createRealizedProcessor(m);

		// logging with callback
		processor.addControllerListener(JmfUtil.createEventDumpControllerListener(listener));
		return processor;
	}

	public static Processor createCustomProcessor(String inputDataUrl, ContentDescriptor contentType, CustomProcessorHelper helper)
			throws Exception
	{
		return createCustomProcessor(inputDataUrl, contentType, helper, null, null);
	}
	
	public static Processor createCustomProcessor(String inputDataUrl, ContentDescriptor contentType, CustomProcessorHelper helper, ControllerListener listener, String processorName)
			throws Exception
	{
		if (log.isDebugEnabled()) {
			log.debug("Creating processor[" + processorName + "] from [" + inputDataUrl + "] for " + contentType);
		}
		
		// creating input data source
		DataSource ds;
		MediaLocator inputLocator = new MediaLocator(inputDataUrl);
		ds = javax.media.Manager.createDataSource(inputLocator);
		
/*		if (log.isDebugEnabled()) {
			log.debug(JmfUtil.dumpDataSource(ds, "input"));
		}*/
		
		
		// creating processor
		Processor processor = Manager.createProcessor(ds);

		// logging with callback
		processor.addControllerListener(JmfUtil.createEventDumpControllerListener(listener));

		// waiting for Configured
		ProcessorStateWaitHelper h = new ProcessorStateWaitHelper();
		boolean result = h.waitForState(processor, Processor.Configured);
		if (result == false) {
			return null;
		}
		
		// output content type
		processor.setContentDescriptor(contentType);
		
		//System.out.println(JmfUtil.dumpProcessor(processor, "BEFORE custom init"));
		
		// calling custom initialization
		if (helper != null) {
			helper.initProcessing(processor);
			helper.initRenderer(processor);
		}
		
		//System.out.println(JmfUtil.dumpProcessor(processor, "AFTER custom init"));
		
		result = h.waitForState(processor, Processor.Realized);
		if (result == false) {
			return null;
		}
		
		return processor;
	}
	
	public static Player createRealizedPlayer(String inputDataUrl)
			throws Exception
	{
		return createRealizedPlayer(inputDataUrl, null);
	}
	
	public static Player createRealizedPlayer(String inputDataUrl, ControllerListener listener)
			throws Exception
	{
		MediaLocator inputLocator = new MediaLocator(inputDataUrl);
		Player p = Manager.createRealizedPlayer(inputLocator);
		p.addControllerListener(JmfUtil.createEventDumpControllerListener(listener));
		return p;
	}

	public static Player createRealizedPlayer(DataSource ds)
			throws Exception
	{
		return Manager.createRealizedPlayer(ds);
	}

	public static DataSink createAndOpenDataSink(DataSource inputSource, String outputDataUrl, DataSinkListener listener)
			throws Exception
	{
		MediaLocator outputLocator = new MediaLocator(outputDataUrl);

		DataSink sink = Manager.createDataSink(inputSource, outputLocator);
		if (listener != null) {
			sink.addDataSinkListener(listener);
		}
		sink.open();

		return sink;
	}
}
