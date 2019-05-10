package org.ftoth.general.util.jmf;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Vector;

import javax.media.CaptureDeviceInfo;
import javax.media.CaptureDeviceManager;
import javax.media.Codec;
import javax.media.Control;
import javax.media.Controller;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.Demultiplexer;
import javax.media.Format;
import javax.media.NotRealizedError;
import javax.media.Player;
import javax.media.PlugInManager;
import javax.media.Processor;
import javax.media.Track;
import javax.media.TransitionEvent;
import javax.media.control.TrackControl;
import javax.media.datasink.DataSinkEvent;
import javax.media.datasink.DataSinkListener;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;
import javax.media.protocol.PullBufferDataSource;
import javax.media.protocol.PullDataSource;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushDataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.sun.media.util.Registry;

public class JmfUtil
{
	private static final Log log = LogFactory.getLog(JmfUtil.class);
	
	private static final String INDENT = "    ";

	public static String dumpDataSource(DataSource ds, String id)
	{
		return dumpDataSource(ds, id, "");
	}

	public static final String getPluginName(int type)
	{
		switch(type) {
		case PlugInManager.DEMULTIPLEXER:
			return "DEMULTIPLEXER";
		case PlugInManager.CODEC:
			return "CODEC";
		case PlugInManager.EFFECT:
			return "EFFECT";
		case PlugInManager.MULTIPLEXER:
			return "MULTIPLEXER";
		case PlugInManager.RENDERER:
			return "RENDERER";
		}
		return "?";
	}
	
	public static String dumpDataSource(DataSource ds, String id, String indent)
	{
		StringBuilder b = new StringBuilder();
		b.append(indent + "DataSource[" + id + "]\n");
		if (ds != null) {
			b.append(indent + INDENT + "Type: " + ds.getClass().getName() + "\n");
			String direction = "?";
			if (ds instanceof PushDataSource || ds instanceof PushBufferDataSource) {
				direction = "PUSH";
			}
			else if (ds instanceof PullDataSource || ds instanceof PullBufferDataSource) {
				direction = "PULL";
			}
			b.append(indent + INDENT + "Direction: " + direction + "\n");
			b.append(indent + INDENT + "Content Type: " + ds.getContentType() + "\n");
			try {
				if (ds.getLocator() != null && ds.getLocator().getURL() != null) {
					b.append(indent + INDENT + "URL: " + ds.getLocator().getURL().toString() + "\n");
				}
			}
			catch (MalformedURLException e) {
				log.error("Bad URL!!!");
			}
			b.append(indent + INDENT + "Duration: " + Double.toString(ds.getDuration().getSeconds()) + " secs\n");
			
			b.append(indent + INDENT + "Controls:" + "\n");
			Object[] ictrls = ds.getControls();
			for (Object ictrl : ictrls) {
				b.append(indent + "        " + ictrl.toString() + "\n");
			}
		}
		else {
			b.append(indent + INDENT + "<not initialized>\n");
		}
		return b.toString();
	}

	public static String dumpPlayer(Player player, String id)
	{
		StringBuilder b = new StringBuilder();
		b.append("---------------------- Player[" + id + "] ----------------------\n");
		// state
		b.append(INDENT + "State: " + getControllerStateName(player.getState()) + "\n");
		b.append(INDENT + "Target State: " + getControllerStateName(player.getTargetState()) + "\n");
		
		// controls
		Control[] pcs = player.getControls();		
		b.append(INDENT + "Processor controls:\n");
		for (Object pc : pcs) {
			b.append(INDENT + INDENT + " - " + pc.toString() + "\n");
		}
		return b.toString();
	}
	
	public static String dumpProcessor(Processor processor, String id)
	{
		StringBuilder b = new StringBuilder();
		b.append("---------------------- Processor[" + id + "] ----------------------\n");
		// state
		b.append(INDENT + "State: " + getControllerStateName(processor.getState()) + "\n");
		b.append(INDENT + "Target State: " + getControllerStateName(processor.getTargetState()) + "\n");

		// output
		b.append(INDENT + "Output:\n");
		// content type
		ContentDescriptor cd = processor.getContentDescriptor();
		if (cd != null) {
			b.append(INDENT + INDENT + "Content Type: " + cd + "\n");
		}
		// Output data source
		try {
			DataSource osrc = processor.getDataOutput();
			b.append(dumpDataSource(osrc, "output", INDENT + INDENT));
		}
		catch(NotRealizedError e) {
			b.append(INDENT + INDENT + "Output data source cannot discovered (processor not realized yet)\n");
		}
		
		// supported content types
		ContentDescriptor[] cds = processor.getSupportedContentDescriptors();
		b.append(INDENT + "Supported Content Types:\n");
		for (ContentDescriptor c : cds) {
			b.append(INDENT + INDENT + c + "\n");
		}
		
		// controls
		Control[] pcs = processor.getControls();		
		b.append(INDENT + "Processor controls:");
		for (Object pc : pcs) {
			b.append(INDENT + INDENT + pc.toString() + "\n");
		}

		// tracks
		b.append(INDENT + "========= Tracks =========\n");
		TrackControl[] tracks = processor.getTrackControls();		
		if (tracks == null || tracks.length < 1) {
			b.append("Couldn't find tracks in Processor[" + id + "]" + "\n");			
		}
		for (int n = 0; n < tracks.length; n++) {
			TrackControl track = tracks[n];
			Format format = track.getFormat();
			b.append(INDENT + "Track[" + n + "]\n");
			b.append(INDENT + INDENT + "Enabled: " + track.isEnabled() + "\n");
			b.append(INDENT + INDENT + "Format: " + format + "\n");
			b.append(INDENT + INDENT + "Supported formats:" + "\n");
			Format supported[] = track.getSupportedFormats();
			for (Format sup : supported) {
				b.append(INDENT + INDENT + INDENT + sup + "\n");
			}
		}
		
		return b.toString();
	}
	
	public static String dumpCaptureDevices()
	{
		StringBuilder b = new StringBuilder();
		b.append("---------- Capture devices -----------\n");
		@SuppressWarnings("unchecked")
		Vector<CaptureDeviceInfo> devs = CaptureDeviceManager.getDeviceList(null);
		for (CaptureDeviceInfo dev : devs) {
			b.append(INDENT + dev.getName() + " (" + dev.getLocator().toString() + ")\n");
			b.append(INDENT + "Formats:\n");
			Format[] fmts = dev.getFormats();
			for (Format f : fmts) {
				b.append(INDENT + INDENT + f + "\n");
			}
		}
		return b.toString();
	}
	
	public static String dumpPlugins(boolean detailed)
	{
		Vector<?> items;
		StringBuilder b = new StringBuilder();
		b.append("---------- Plugins -----------\n");
			
		b.append(dumpDemultiplexers(detailed));		
		
		b.append(dumpCodecs(detailed));
		
		b.append(INDENT + "EFFECTs:\n");
		items = PlugInManager.getPlugInList(null, null, PlugInManager.EFFECT);
		for (Object item : items) {
			b.append(INDENT + INDENT + item + "\n");
		}

		b.append(INDENT + "MULTIPLEXERs:\n");
		items = PlugInManager.getPlugInList(null, null, PlugInManager.MULTIPLEXER);
		for (Object item : items) {
			b.append(INDENT + INDENT + item + "\n");
		}

		b.append(INDENT + "RENDERERs:\n");
		items = PlugInManager.getPlugInList(null, null, PlugInManager.RENDERER);
		for (Object item : items) {
			b.append(INDENT + INDENT + item + "\n");
		}
		
		return b.toString();
	}

	
	public static String dumpDemultiplexers(boolean detailed)
	{
		StringBuilder b = new StringBuilder();
		
		b.append(INDENT + "DEMULTIPLEXERs:\n");
		Vector<?> items = PlugInManager.getPlugInList(null, null, PlugInManager.DEMULTIPLEXER);
		for (Object item : items) {
			b.append(INDENT + INDENT + item + "\n");
			
			if (detailed) {
				Object o = instantiate((String) item);
				if (o == null) {
					continue;
				}
				
				if (o instanceof Demultiplexer) {
					Demultiplexer demux = (Demultiplexer) o;
					
					// input formats
					Format[] inFmts = demux.getSupportedInputContentDescriptors();
					b.append(INDENT + INDENT + INDENT + "Input formats:\n");					
					for (Format fmt : inFmts) {
						b.append(INDENT + INDENT + INDENT + INDENT + fmt + "\n");
					}
					
					// output formats
					Track[] tracks = null;
					try {
						tracks = demux.getTracks();
					}
					catch (Exception e) {
					}
					if (tracks != null) {
						b.append(INDENT + INDENT + INDENT + "Tracks:\n");					
						for (Track t : tracks) {
							b.append(INDENT + INDENT + INDENT + INDENT + t.getFormat().toString() + "\n");
						}
					}
				}
			}			
		}
		return b.toString();
	}
	
	public static String dumpCodecs(boolean detailed)
	{
		StringBuilder b = new StringBuilder();
		
		b.append(INDENT + "CODECs:\n");
		Vector<?> items = PlugInManager.getPlugInList(null, null, PlugInManager.CODEC);
		for (Object item : items) {
			b.append(INDENT + INDENT + item + "\n");
			
			if (detailed) {
				Object o = instantiate((String) item);
				if (o == null) {
					continue;
				}
				
				if (o instanceof Codec) {
					Codec codec = (Codec) o;

					// input formats
					Format[] inFmts = codec.getSupportedInputFormats();
					b.append(INDENT + INDENT + INDENT + "Input formats:\n");					
					for (Format fmt : inFmts) {
						b.append(INDENT + INDENT + INDENT + INDENT + fmt + "\n");
					}
					
					// output formats
					Format[] outFmts = codec.getSupportedOutputFormats(null);
					b.append(INDENT + INDENT + INDENT + "Output formats:\n");					
					for (Format fmt : outFmts) {
						b.append(INDENT + INDENT + INDENT + INDENT + fmt + "\n");
					}
					
					// controls
					Object[] ctrls = codec.getControls();
					if (ctrls.length > 0) {
						b.append(INDENT + INDENT + INDENT + "Controls:\n");					
						for (Object ctrl : ctrls) {
							b.append(INDENT + INDENT + INDENT + INDENT + ctrl + "\n");
						}
					}
					
				}
			}
		}
		return b.toString();
	}
	
	public static String dumpControllerEvent(ControllerEvent ev)
	{
		StringBuilder b = new StringBuilder();
		
		b.append("JMF ControllerEvent - " + ev.getClass().getSimpleName() + " - " + ev.toString());

		// AudioDeviceUnavailableEvent, CachingControlEvent, ControllerClosedEvent, DurationUpdateEvent, FormatChangeEvent, MediaTimeSetEvent, RateChangeEvent, StopTimeChangeEvent, 
		if (ev instanceof TransitionEvent) {
			TransitionEvent cev = (TransitionEvent)ev;
			String prevState = getControllerStateName(cev.getPreviousState());
			String curState = getControllerStateName(cev.getCurrentState()); 
			String targetState = getControllerStateName(cev.getTargetState());
			b.append("\n    state: " + prevState + " --> " + curState.toUpperCase() + " ( --> " + targetState + ")");
		}
/*		if (ev instanceof TransitionEvent) {
		
		}*/
		
		return b.toString();
	}
	
	public static final String getControllerStateName(int state)
	{
		switch (state) {
		case Controller.Prefetched:
			return "Prefetched";
		case Controller.Prefetching:
			return "Prefetching";
		case Controller.Realized:
			return "Realized";
		case Controller.Realizing:
			return "Realizing";
		case Controller.Started:
			return "Started";
		case Controller.Unrealized:
			return "Unrealized";
		case Processor.Configured:
			return "Configured";
		case Processor.Configuring:
			return "Configuring";
		}
		return "?";
	}

	public static ControllerListener createEventDumpControllerListener()
	{
		return createEventDumpControllerListener(null);
	}
	
	public static ControllerListener createEventDumpControllerListener(final ControllerListener listener)
	{
		ControllerListener cl = new ControllerListener() {
			@Override
			public void controllerUpdate(ControllerEvent ev)
			{
				if (log.isDebugEnabled()) {
					log.debug(JmfUtil.dumpControllerEvent(ev));
				}
				
				// callback
				if (listener != null) {
					listener.controllerUpdate(ev);
				}
			}
		};
		return cl;
	}

	public static DataSinkListener createEventDumpDataSinkListener()
	{
		DataSinkListener l = new DataSinkListener() {

			@Override
			public void dataSinkUpdate(DataSinkEvent ev)
			{
				if (log.isDebugEnabled()) {
					log.debug(JmfUtil.dumpDataSinkEvent(ev));
				}
			}
		};
		return l;
	}
	
	public static String dumpDataSinkEvent(DataSinkEvent ev)
	{
		StringBuilder b = new StringBuilder();
		
		b.append("JMF DataSinkEvent - " + ev.getClass().getSimpleName() + " - " + ev.toString());
		
		return b.toString();
	}

	public static void configLogging(boolean allow, String targetDir, boolean persistent)
	{
		Registry.set("allowLogging", allow);
		Registry.set("secure.logDir", targetDir);
		
		// if you want to save into jmf.properties
		if (persistent) {
			try {
				Registry.commit();
			}
			catch (IOException e) {
				log.error("Error during log configuration", e);
			}
		}
	}
	
	public static boolean removeAllPlugins()
	{
		boolean result = true;
		
		if (!removeAllPlugins(PlugInManager.DEMULTIPLEXER)) {
			result= false;
		}
		if (!removeAllPlugins(PlugInManager.CODEC)) {
			result= false;
		}
		if (!removeAllPlugins(PlugInManager.EFFECT)) {
			result= false;
		}
		if (!removeAllPlugins(PlugInManager.MULTIPLEXER)) {
			result= false;
		}
		if (!removeAllPlugins(PlugInManager.RENDERER)) {
			result= false;
		}
		return result;
	}
	
	public static boolean removeAllPlugins(int type)
	{
		boolean result = true;
		
		if (log.isInfoEnabled()) {
			log.info("Removing JMF plugins: " + getPluginName(type));
		}
		
		@SuppressWarnings("unchecked")
		Vector<String> plugins = PlugInManager.getPlugInList(null, null, type);
		for (String p : plugins) {
			boolean ok = PlugInManager.removePlugIn(p, type);
			if (!ok) {
				log.warn("[" + p + "] : cannot be removed");
				result = false;
			}
		}		
		return result;
	}
	
	// ------------------------- helper -------------------------
	private static Object instantiate(String className)
	{
		Class<?> c;
		try {
			c = Class.forName((String) className);
			//Constructor<?> constr = c.getConstructor((Class<?>)null);
			
			return c.newInstance();
		}
		catch (Exception e) {
			log.error("Error during instantiate object[" + className + "]");
			e.printStackTrace();
			return null;
		}
	}
}

